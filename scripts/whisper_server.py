#!/usr/bin/env python3
"""
Invoke Whisper Server — Local STT via faster-whisper.
Receives WAV audio via POST /transcribe, returns text.
Runs on localhost:8394.
"""

import io
import tempfile
import argparse
from flask import Flask, request, jsonify
from faster_whisper import WhisperModel

app = Flask(__name__)
model = None
model_size = "tiny"


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "model": model_size})


@app.route("/transcribe", methods=["POST"])
def transcribe():
    if "file" not in request.files:
        return jsonify({"error": "No file provided"}), 400

    audio_file = request.files["file"]
    language = request.form.get("language", "en")

    # Save to temp file (faster-whisper needs a file path)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        audio_file.save(tmp)
        tmp_path = tmp.name

    try:
        segments, info = model.transcribe(
            tmp_path,
            language=language,
            beam_size=1,          # Fastest setting
            best_of=1,
            temperature=0.0,
            condition_on_previous_text=False,
            vad_filter=True,       # Voice activity detection
            vad_parameters=dict(
                min_silence_duration_ms=300,
                speech_pad_ms=200,
            ),
        )

        text = " ".join(seg.text.strip() for seg in segments).strip()

        return jsonify({
            "text": text,
            "language": info.language,
            "language_probability": info.language_probability,
            "duration": info.duration,
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500
    finally:
        import os
        os.unlink(tmp_path)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", default="tiny", help="Whisper model size")
    parser.add_argument("--port", type=int, default=8394, help="Server port")
    parser.add_argument("--device", default="cpu", help="cpu or cuda")
    parser.add_argument("--compute-type", default="int8", help="int8, float16, etc.")
    args = parser.parse_args()

    model_size = args.model
    print(f"Loading Whisper {model_size} on {args.device} ({args.compute_type})...")
    model = WhisperModel(args.model, device=args.device, compute_type=args.compute_type)
    print(f"Whisper {model_size} loaded! Starting server on :{args.port}")
    app.run(host="127.0.0.1", port=args.port, debug=False)
