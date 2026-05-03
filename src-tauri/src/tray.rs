use tauri::{
    menu::{Menu, MenuItem},
    tray::TrayIconBuilder,
    App, Emitter, Manager,
};

pub fn setup_tray(app: &App) -> Result<(), Box<dyn std::error::Error>> {
    let show = MenuItem::with_id(app, "show", "Show INVOKE", true, None::<&str>)?;
    let record = MenuItem::with_id(app, "record", "🎙️ Record", true, None::<&str>)?;
    let quit = MenuItem::with_id(app, "quit", "Quit", true, None::<&str>)?;
    let menu = Menu::with_items(app, &[&show, &record, &quit])?;

    let _tray = TrayIconBuilder::new()
        .menu(&menu)
        .tooltip("INVOKE — Speak. Actions are invoked.")
        .on_menu_event(|app, event| match event.id.as_ref() {
            "show" => {
                if let Some(window) = app.get_webview_window("main") {
                    let _ = window.show();
                    let _ = window.set_focus();
                }
            }
            "record" => {
                // Emit event to frontend to toggle recording
                let _ = app.emit("toggle-recording", ());
            }
            "quit" => {
                app.exit(0);
            }
            _ => {}
        })
        .build(app)?;

    Ok(())
}
