# 🚛 FleetOpt — Intelligent CNG Virtual Pipeline & HCV Dispatch DSS

[![Platform](https://img.shields.io/badge/Platform-Android%2026%2B-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.11.00-blue.svg)](https://developer.android.com/jetpack/compose)
[![Release](https://img.shields.io/badge/Release-v1.1.0-emerald.svg)](https://github.com/Ganeshperala07/fleetopt/releases)
[![Developers](https://img.shields.io/badge/Developers-P.%20Ganesh%20%26%20Yesh-teal.svg)](https://github.com/Ganeshperala07/fleetopt)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)](#)

> **A physics-informed, AI-powered Decision Support System (DSS) engineered for City Gas Distribution (CGD) networks.** FleetOpt coordinates Heavy Commercial Vehicle (HCV) mobile cascade tankers between City Gate Mother compression hubs and decentralized Daughter Booster filling stations across a 100 km regional operational perimeter.

---

## ⚡ Key Capabilities & Engineering Highlights

* 🗺️ **100 KM Regional Operational Perimeter Circle**: Zero-key OpenStreetMap & Leaflet geographic engine rendering an explicit 100 km perimeter circle around Shamshabad/Hyderabad CGS with 25 km, 50 km, and 75 km concentric rings. Plots CGS Mother Hubs, Daughter Stations (color-coded by pressure with pulsing critical indicators), live HCV tankers, and highway corridors.
* 🎬 **Animated Boot Screen & Developer Attribution**: Smooth spring scale and alpha animation of the app logo with a pulsing cyan/teal halo ring and attribution badge for **P. Ganesh & Yesh**. Compatible with 100% of Android devices (Android 8 to 15).
* 🔬 **Real-Gas Thermodynamics (AGA-8)**: Implements the AGA-8 Detail Equation of State to model natural gas compressibility ($Z \approx 0.835$ @ 200 bar), 50-bar dryout cutoffs, and Joule-Thomson expansion cooling ($-0.45\text{ K/bar}$).
* 🧠 **Multi-Factor Dispatch Optimization**: Weights station inventory urgency (40%), corridor distance (20%), real-time traffic flow (15%), vehicle mechanical readiness (15%), and cascade capacity matching (10%).
* 🛡️ **Cascade Overpressurization Prevention**: Mathematically clamps payloads to $\min(\text{capacity}, \max(50.0, \text{topUpDemand}))$ to ensure station cascades never exceed the 230 bar operating ceiling.
* 🧪 **In-Memory Simulation Sandbox**: Non-destructive digital twin overlays synthetic pressure decays in memory; persistent Room SQLite database records remain pristine.
* 🎨 **Monochromatic Industrial UI/UX**: Full Light / Dark / System monochromatic theme system with high-contrast semantic accents (Emerald, Amber, Crimson, Cyan) and Data Provenance tracking badges (`REAL`, `SIMULATED`, `CALCULATED`, `FORECAST`, `MANUAL`).
* 🔒 **Hardened Admin Security**: Salted SHA-256 PIN hashing (`1234` default, `7788` emergency recovery), zero-lockout retry policy, and 5-minute session timeout.
* ⚡ **89% Binary Optimization**: ProGuard/R8 code shrinking reduces release APK size down to **2.0 MB**.

---

## 📱 Application Map (Primary Bottom Navigation + Operations Hub)

| Module | Navigation | Key Features |
| :--- | :--- | :--- |
| **Dashboard** | Bottom Nav #1 | High-level KPIs, 100 km Corridor Radar Showcase card, 1-tap map launch, urgent stockout queue, Next Best Action recommendation card. |
| **Dispatch DSS** | Bottom Nav #2 | Multi-factor match scoring, 5 explicit rationales, pre-dispatch confirmation sheet, WhatsApp driver manifest, "View Route on 100km Map" button. |
| **100km Corridor Map** | Bottom Nav #3 | Tri-Engine Switcher (100km Geo Map, Tactical Radar, Google Maps), full 100km perimeter circle, interactive station selection, instant dispatch action. |
| **Fleet Board** | Bottom Nav #4 | Live tanker telemetry, hydro-test certificate tracking, PRV burst disc verification, quick driver dialer (`CALL_PHONE`), Admin CRUD. |
| **Stations Network** | Bottom Nav #5 | 3-bank cascade cylinder gauges, pressure arc meters, Mother vs Daughter stations, manual telemetry entry slider, Admin CRUD. |
| **Trips History** | Operations Hub | Monotonic trip ID tracking (`TRIP-YYYYMMDD-XXX`), complete and cancel workflows. |
| **Alerts Center** | Operations Hub | Ranked real-time operational notifications (Critical, Warning, Info). |
| **Analytics & Forecasting** | Operations Hub | 7-day WMA forecasting curves, diurnal sales rush profiles (1.6x morning, 1.7x evening), and cost savings breakdown. |
| **Settings Console** | Operations Hub | Theme selector (System/Light/Dark), simulation scenario triggers, and Admin PIN management. |

---

## 📦 Direct APK Downloads

Pre-compiled and signed binaries are available in [GitHub Releases](https://github.com/Ganeshperala07/fleetopt/releases):

* **[Download Production Release APK (2.0 MB)](https://github.com/Ganeshperala07/fleetopt/releases/download/v1.1.0/fleetopt-v1.1.0-release.apk)**
* **[Download Debug APK (18.7 MB)](https://github.com/Ganeshperala07/fleetopt/releases/download/v1.1.0/fleetopt-v1.1.0-debug.apk)**

---

## 📊 Presentation & Competition Resources

A complete slide-by-slide competition pitch deck and technical report is available in:
📄 **[`docs/competition_presentation_report.md`](docs/competition_presentation_report.md)**

---

## 🛠️ Build & Installation

### Requirements
* **JDK**: 17 or 21 (`C:\Users\dappu\.jdk\jdk-21\jdk-21.0.12.1+1`)
* **Android SDK**: Compile SDK 35, Min SDK 26 (Android 8.0+)
* **Build System**: Gradle 8.11.1 with Android Gradle Plugin 8.7.2

### Run Unit Tests
```bash
./gradlew test
```

### Build Production Release APK (2.0 MB)
```bash
./gradlew assembleRelease
```
*Output: `app/build/outputs/apk/release/app-release.apk`*

### Build Debug APK (18.7 MB)
```bash
./gradlew assembleDebug
```
*Output: `app/build/outputs/apk/debug/app-debug.apk`*

---

## 👥 Authors & Developers
* **P. Ganesh & Yesh**
* **GitHub Profile**: [@Ganeshperala07](https://github.com/Ganeshperala07)
* **Repository**: [https://github.com/Ganeshperala07/fleetopt](https://github.com/Ganeshperala07/fleetopt)
