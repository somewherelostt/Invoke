use log::info;
use std::sync::{Arc, Mutex};
use tauri::{Emitter, Manager};

mod commands;
mod coordinator;
mod settings;
mod tray;
mod agent;
mod managers;

pub use coordinator::{CoordinatorState, Stage};

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    env_logger::init();

    tauri::Builder::default()
        .plugin(tauri_plugin_os::init())
        .plugin(tauri_plugin_store::Builder::new().build())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_fs::init())
        .plugin(tauri_plugin_single_instance::init(|app, _args, _cwd| {
            let _ = app.get_webview_window("main").map(|w| { let _ = w.show(); });
        }))
        .plugin(tauri_plugin_clipboard_manager::init())
        .plugin(tauri_plugin_global_shortcut::Builder::new().build())
        .setup(|app| {
            info!("🔮 INVOKE starting...");

            // Setup system tray
            tray::setup_tray(app)?;

            // Initialize coordinator state
            let coordinator = coordinator::CoordinatorState::new();
            app.manage(coordinator);

            // Initialize audio state
            let audio_inner = managers::audio::AudioManager::new_state();
            app.manage(commands::AudioState(Arc::new(audio_inner)));

            // Initialize settings
            let settings = settings::AppSettings::default();
            
            // Initialize tool executor (tokio Mutex for async)
            let tool_executor = agent::tools::ToolExecutor::new(&settings.composio_api_key);
            app.manage(commands::ToolState(Arc::new(tokio::sync::Mutex::new(tool_executor))));
            
            app.manage(Mutex::new(settings));

            // Register global shortcut: Alt+Space to toggle recording
            use tauri_plugin_global_shortcut::GlobalShortcutExt;
            let shortcut = app.global_shortcut();
            shortcut.on_shortcut("Alt+Space", move |app, _shortcut, _event| {
                log::info!("⌨️ Global shortcut triggered");
                let _ = app.emit("toggle-recording", ());
            })?;
            info!("⌨️ Global shortcut registered: Alt+Space");

            info!("🔮 INVOKE ready.");
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            commands::start_recording,
            commands::stop_recording,
            commands::get_status,
            commands::update_settings,
            commands::get_settings,
            commands::invoke_action,
        ])
        .run(tauri::generate_context!())
        .expect("error while running INVOKE");
}
