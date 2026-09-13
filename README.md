# 🚛 FleetOpt — Intelligent CNG Virtual Pipeline & HCV Dispatch DSS

[![Platform](https://img.shields.io/badge/Platform-Android%2026%2B-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.11.00-blue.svg)](https://developer.android.com/jetpack/compose)
[![Release](https://img.shields.io/badge/Release-v1.1.0-emerald.svg)](https://github.com/Ganeshperala07/fleetopt)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)](#)

> **A physics-informed, AI-powered Decision Support System (DSS) engineered for City Gas Distribution (CGD) networks.** FleetOpt solves the critical operational bottleneck of virtual pipeline logistics—coordinating Heavy Commercial Vehicle (HCV) mobile cascade tankers between City Gate Mother compression hubs and decentralized Daughter Booster filling stations.

---

## ⚡ Key Capabilities & Engineering Highlights

* 🔬 **Real-Gas Thermodynamics (AGA-8)**: Implements the AGA-8 Detail Equation of State to model natural gas compressibility ($Z \approx 0.835$ @ 200 bar), 50-bar dryout cutoffs, and Joule-Thomson expansion cooling ($-0.45\text{ K/bar}$).
* 🧠 **Multi-Factor Dispatch Optimization**: Weights station inventory urgency (40%), corridor distance (20%), real-time traffic flow (15%), vehicle mechanical readiness (15%), and cascade capacity matching (10%).
* 🛡️ **Cascade Overpressurization Prevention**: Mathematically clamps payloads to $\min(\text{capacity}, \max(50.0, \text{topUpDemand}))$ to ensure station cascades never exceed the 230 bar operating ceiling.
* 🧪 **In-Memory Simulation Sandbox**: Non-destructive digital twin overlays synthetic pressure decays in memory; persistent Room SQLite database records remain pristine.
* 🎨 **Monochromatic Industrial UI/UX**: Full Light / Dark / System monochromatic theme system with high-contrast semantic accents (Emerald, Amber, Crimson, Cyan) and Data Provenance tracking badges (`REAL`, `SIMULATED`, `CALCULATED`, `FORECAST`, `MANUAL`).
* 🔒 **Hardened Admin Security**: Salted SHA-256 PIN hashing (`1234` default, `7788` emergency recovery), zero-lockout retry policy, and 5-minute session timeout.
* ⚡ **89% Binary Optimization**: ProGuard/R8 code shrinking reduces APK size from 18.5 MB down to **2.0 MB**.

---

## 📱 Application Map (10 Modules)

| Module | Route | Key Features |
| :--- | :--- | :--- |
| **Dashboard** | `dashboard` | High-level KPIs, urgent stockout queue, Next Best Action recommendation card, diurnal demand preview. |
| **Dispatch DSS** | `dispatch` | Multi-factor match scoring, 5 explicit rationales, pre-dispatch confirmation sheet, WhatsApp driver manifest. |
| **Fleet Board** | `fleet` | Live tanker telemetry, hydro-test certificate tracking, PRV burst disc verification, quick driver dialer (`CALL_PHONE`). |
| **Stations Network** | `stations` | 3-bank cascade cylinder gauges, pressure arc meters, Mother vs Daughter stations, manual telemetry entry slider. |
| **Operations Hub** | `more` | Central diagnostic and operations navigation center. |
| **Trips History** | `trips` | Monotonic trip ID tracking (`TRIP-YYYYMMDD-XXX`), complete and cancel workflows. |
| **Alerts Center** | `alerts` | Ranked real-time operational notifications (Critical, Warning, Info). |
| **Live Corridor Map** | `map` | Google Maps corridor routing with instant fallback to internal vector **Radar Canvas**. |
| **Analytics & Forecasting** | `analytics` | 7-day WMA forecasting curves, diurnal sales rush profiles (1.6x morning, 1.7x evening), and cost savings breakdown. |
| **Settings Console** | `settings` | Theme selector (System/Light/Dark), simulation scenario triggers, and Admin PIN management. |

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

### Build Debug APK (18.5 MB)
```bash
./gradlew assembleDebug
```
*Output: `app/build/outputs/apk/debug/app-debug.apk`*

---

## 👤 Author
* **Developer**: [Ganeshperala07](https://github.com/Ganeshperala07)
* **Repository**: [https://github.com/Ganeshperala07/fleetopt](https://github.com/Ganeshperala07/fleetopt)
