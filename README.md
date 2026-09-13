# FleetOpt - Fleet Management & Route Optimization

A modern, high-performance mobile application designed for fleet tracking, logistics optimization, and real-time operations management.

## Tech Stack & Tooling
- **Platform**: Android
- **Language**: Kotlin / Jetpack Compose
- **IDE**: Android Studio & Visual Studio Code
- **Version Control**: Git & GitHub ([@Ganeshperala07](https://github.com/Ganeshperala07))
- **Testing & Debugging**: Physical Android Device (via USB Debugging)
- **Runtime Environment**: OpenJDK 25 (Android Studio JBR)

---

## Developer Setup & Guidelines

### RAM & Performance Optimization
To ensure optimal performance on 8 GB RAM systems:
1. **Physical Device Testing**: Always test apps on a connected physical Android phone via USB debugging instead of heavy virtual emulators.
2. **Gradle Memory Limits**: Configured in `~/.gradle/gradle.properties` with `-Xmx2048m` heap cap and parallel build caching enabled.

### Useful Commands

#### Verify ADB & Connected Phone
```powershell
adb devices -l
```

#### Run Gradle Build (when project template is created)
```powershell
./gradlew assembleDebug
```

---

## Project Structure
```text
fleetopt/
├── .gitignore          # Android and build-artifact ignore rules
├── README.md           # Project documentation and quick start
└── app/                # Android application source (Kotlin/Compose)
```

---

## Author
- **Developer**: [Ganeshperala07](https://github.com/Ganeshperala07)
