import SwiftUI
import WidgetKit
import Shared

@main
struct FitBharatApp: App {
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea()
        }
        .onChange(of: scenePhase) { phase in
            // The app keeps the widget data current; redraw the widget once the user leaves the app.
            if phase != .active { WidgetCenter.shared.reloadAllTimelines() }
        }
    }
}

/// Hosts the shared Compose Multiplatform UI.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
