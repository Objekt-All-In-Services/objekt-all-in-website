import SwiftUI

@main
struct ObjektAllInApp: App {
    @StateObject private var workspace = Workspace()
    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(workspace)
                .tint(Color(red: 0.72, green: 0.50, blue: 0.21))
        }
    }
}
