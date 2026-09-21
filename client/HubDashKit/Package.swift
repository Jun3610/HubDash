// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "HubDashKit",
    // platforms를 명시하지 않으면 SwiftPM이 macOS 10.13을 기본으로 잡아 swift-testing 매크로가 컴파일되지 않는다.
    platforms: [.macOS(.v13), .iOS(.v16)],
    products: [
        .library(name: "HubDashKit", targets: ["HubDashKit"]),
    ],
    targets: [
        .target(name: "HubDashKit"),
        .testTarget(name: "HubDashKitTests", dependencies: ["HubDashKit"]),
    ]
)
