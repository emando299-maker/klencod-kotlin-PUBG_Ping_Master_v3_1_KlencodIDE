# PUBG Ping Master 3.1

KlencodIDE Android Kotlin project. Java 17 and Kotlin JVM target 17; compileSdk 34; targetSdk 34; Build Tools 34.0.0.

## Features
- Foreground Game Mode continues general network tests while switching to PUBG (persistent notification).
- Separate TCP connect-time checks to Cloudflare and Google on port 443.
- Separate UDP DNS request/response RTT checks to port 53.
- Rolling latency chart, average, median, min, max and consecutive-sample jitter.
- Keeps up to 400 recent samples locally; screen shows the latest 10 sample records.

## Important limitations
TCP connect time and UDP DNS RTT are not ICMP ping and are not PUBG game-server ping. This app does not read PUBG internal telemetry, modify game traffic, optimize routes, or reduce ping. Android battery restrictions may stop background work. Repeated checks consume battery and a small amount of data. Android 13+ notification permission should be granted for visible foreground monitoring.

## Build
Open the extracted folder in KlencodIDE and build `:app:assembleDebug`. This source has not been compiled or device-tested in this environment; verify the build before installing.
