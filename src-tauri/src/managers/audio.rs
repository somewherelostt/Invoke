use anyhow::Result;
use cpal::traits::{DeviceTrait, HostTrait, StreamTrait};
use cpal::{SampleFormat, Stream, StreamConfig};
use hound::{WavSpec, WavWriter};
use log::{info, warn};
use std::cell::RefCell;
use std::io::Cursor;
use std::sync::{Arc, Mutex};

const TARGET_SAMPLE_RATE: u32 = 16000;

type SharedSamples = Arc<Mutex<Vec<f32>>>;

/// Audio recording state (Send + Sync safe)
pub struct AudioStateInner {
    pub samples: SharedSamples,
    pub recording: Arc<Mutex<bool>>,
}

thread_local! {
    static AUDIO_STREAM: RefCell<Option<Stream>> = RefCell::new(None);
}

/// Audio manager functions
pub struct AudioManager;

impl AudioManager {
    pub fn new_state() -> AudioStateInner {
        AudioStateInner {
            samples: Arc::new(Mutex::new(Vec::new())),
            recording: Arc::new(Mutex::new(false)),
        }
    }

    pub fn start_recording(state: &AudioStateInner) -> Result<()> {
        let is_rec = *state.recording.lock().unwrap();
        if is_rec {
            return Ok(());
        }

        let host = cpal::default_host();
        let device = host
            .default_input_device()
            .ok_or_else(|| anyhow::anyhow!("No input device available"))?;

        info!("🎙️ Using input device: {}", device.name().unwrap_or_default());

        let configs: Vec<_> = device.supported_input_configs()?.collect();
        let supported = configs
            .into_iter()
            .filter(|c| c.channels() <= 2)
            .min_by_key(|c| (c.min_sample_rate().0 as i32 - TARGET_SAMPLE_RATE as i32).abs())
            .ok_or_else(|| anyhow::anyhow!("No suitable input config found"))?;

        let sample_format = supported.sample_format();
        let config = supported.with_max_sample_rate().config();

        info!("🎙️ Audio config: {}Hz, {}ch, {:?}, fmt={:?}",
            config.sample_rate.0, config.channels, config.buffer_size, sample_format);

        state.samples.lock().unwrap().clear();

        let samples = state.samples.clone();
        let channels = config.channels as usize;
        let input_rate = config.sample_rate.0;

        let stream: Stream = match sample_format {
            SampleFormat::F32 => {
                device.build_input_stream(
                    &config,
                    move |data: &[f32], _: &cpal::InputCallbackInfo| {
                        let mono = to_mono_f32(data, channels);
                        let resampled = resample(&mono, input_rate);
                        if let Ok(mut s) = samples.lock() {
                            s.extend_from_slice(&resampled);
                        }
                    },
                    |err| warn!("Audio error: {}", err),
                    None,
                )?
            }
            SampleFormat::I16 => {
                let samples = samples.clone();
                device.build_input_stream(
                    &config,
                    move |data: &[i16], _: &cpal::InputCallbackInfo| {
                        let f32_data: Vec<f32> = data.iter().map(|&s| s as f32 / i16::MAX as f32).collect();
                        let mono = to_mono_f32(&f32_data, channels);
                        let resampled = resample(&mono, input_rate);
                        if let Ok(mut s) = samples.lock() {
                            s.extend_from_slice(&resampled);
                        }
                    },
                    |err| warn!("Audio error: {}", err),
                    None,
                )?
            }
            SampleFormat::U16 => {
                let samples = samples.clone();
                device.build_input_stream(
                    &config,
                    move |data: &[u16], _: &cpal::InputCallbackInfo| {
                        let f32_data: Vec<f32> = data.iter().map(|&s| (s as f32 - 32768.0) / 32768.0).collect();
                        let mono = to_mono_f32(&f32_data, channels);
                        let resampled = resample(&mono, input_rate);
                        if let Ok(mut s) = samples.lock() {
                            s.extend_from_slice(&resampled);
                        }
                    },
                    |err| warn!("Audio error: {}", err),
                    None,
                )?
            }
            fmt => anyhow::bail!("Unsupported sample format: {:?}", fmt),
        };

        stream.play()?;

        AUDIO_STREAM.with(|s| *s.borrow_mut() = Some(stream));
        *state.recording.lock().unwrap() = true;

        info!("🎙️ Recording started (resampling to 16kHz mono)");
        Ok(())
    }

    pub fn stop_recording(state: &AudioStateInner) -> Result<Vec<f32>> {
        let is_rec = *state.recording.lock().unwrap();
        if !is_rec {
            return Ok(vec![]);
        }

        // Drop the stream
        AUDIO_STREAM.with(|s| *s.borrow_mut() = None);
        *state.recording.lock().unwrap() = false;

        let samples = state.samples.lock().unwrap().clone();
        let duration = samples.len() as f32 / TARGET_SAMPLE_RATE as f32;
        info!("🎙️ Recording stopped. {} samples ({:.1}s)", samples.len(), duration);

        Ok(samples)
    }

    pub fn samples_to_wav(samples: &[f32]) -> Result<Vec<u8>> {
        let spec = WavSpec {
            channels: 1,
            sample_rate: TARGET_SAMPLE_RATE,
            bits_per_sample: 16,
            sample_format: hound::SampleFormat::Int,
        };

        let mut buf = Cursor::new(Vec::new());
        let mut writer = WavWriter::new(&mut buf, spec)?;

        for &sample in samples {
            let s = (sample.clamp(-1.0, 1.0) * i16::MAX as f32) as i16;
            writer.write_sample(s)?;
        }
        writer.finalize()?;

        Ok(buf.into_inner())
    }

    pub fn is_recording(state: &AudioStateInner) -> bool {
        *state.recording.lock().unwrap()
    }
}

fn resample(mono: &[f32], input_rate: u32) -> Vec<f32> {
    if input_rate == TARGET_SAMPLE_RATE {
        return mono.to_vec();
    }
    let ratio = TARGET_SAMPLE_RATE as f64 / input_rate as f64;
    let new_len = (mono.len() as f64 * ratio) as usize;
    if new_len == 0 {
        return vec![];
    }
    (0..new_len)
        .map(|i| {
            let src_idx = i as f64 / ratio;
            let idx = src_idx as usize;
            let frac = src_idx - idx as f64;
            let next_idx = (idx + 1).min(mono.len().saturating_sub(1));
            mono[idx] * (1.0 - frac) as f32 + mono[next_idx] * frac as f32
        })
        .collect()
}

fn to_mono_f32(data: &[f32], channels: usize) -> Vec<f32> {
    if channels <= 1 {
        return data.to_vec();
    }
    data.chunks(channels)
        .map(|chunk| chunk.iter().sum::<f32>() / channels as f32)
        .collect()
}
