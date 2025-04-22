// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorPedometer",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "CapacitorPedometer",
            targets: ["PedometerPluginPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0")
    ],
    targets: [
        .target(
            name: "PedometerPluginPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/PedometerPluginPlugin"),
        .testTarget(
            name: "PedometerPluginPluginTests",
            dependencies: ["PedometerPluginPlugin"],
            path: "ios/Tests/PedometerPluginPluginTests")
    ]
)