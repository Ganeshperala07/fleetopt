# CNG FleetOpt DSS — Technical References, Standards & Toolchain Documentation

This document compiles the academic literature, engineering standards, thermodynamic equations, algorithms, toolchains, software libraries, and reference websites utilized to architect and build the **FleetOpt Native Android Decision Support System (DSS)**.

---

## 1. Reference Web Application
- **Reference Web DSS**: [fleetopt.lovable.app](https://fleetopt.lovable.app/)
  - *Role*: Provided the baseline functional specification, industrial SCADA visual palette, 7 navigation tabs, and operational concepts for cascade logistics in City Gas Distribution (CGD).
  - *Native Android Enhancements*: Enhanced with real-time compressibility physics ($Z$), Joule-Thomson cooling, dynamic "+ Add HCV" fleet management, offline Corridor Radar Canvas, and custom Android launcher icon branding.

---

## 2. Natural Gas Thermodynamics & Physical Standards

### A. Real Gas Compressibility & Inventory Physics
1. **Compressibility Factor $Z(P, T)$**:
   - **Reference**: *AGA Report No. 8 (American Gas Association)*: *Compressibility Factors of Natural Gas and Other Related Hydrocarbon Gases* (ISO 12213-2).
   - **Formulation Used**: For methane ($CH_4$, molar mass $M = 16.043 \text{ g/mol}$, specific gas constant $R_s = 518.26 \text{ J/(kg}\cdot\text{K)}$):
     $$Z(P, T) = 1.0 - 0.00216 \cdot P + 0.0000045 \cdot P^2 + 0.0008 \cdot (298.15 - T)$$
     Where $P$ is pressure in bar, $T$ is temperature in Kelvin.
   - **Implementation**: [`CngTelemetryCalculator.kt`](file:///C:/Users/dappu/downloads/fleetopt/app/src/main/java/com/fleetopt/app/core/physics/CngTelemetryCalculator.kt).

2. **Residual Usable Gas Cutoff (50 bar threshold)**:
   - **Reference**: *Bureau of Indian Standards (BIS) / Gas Authority of India (GAIL)*: *Standard Operating Procedures for Daughter Booster Stations (DBS)*.
   - *Rationale*: A CNG daughter station booster compressor cannot efficiently suck gas below 50 bar without severe cavitation and overheating. Hence, total inventory below 50 bar is non-recoverable "heel gas":
     $$m_{\text{usable}} = \max\left(0, m(P_{\text{current}}) - m(P=50\text{ bar})\right)$$

3. **Joule-Thomson Expansion Cooling**:
   - **Reference**: *Perry's Chemical Engineers' Handbook*, Section 2: Physical and Chemical Data.
   - *Rationale*: When CNG rapidly depressurizes from a 230-bar tanker cascade across a manifold into a depleted station cascade (e.g. 50 bar), Joule-Thomson cooling occurs:
     $$\Delta T = \mu_{JT} \cdot \Delta P \quad (\mu_{JT} \approx -0.45 \text{ K/bar})$$
     A 100 bar pressure drop results in up to ~45°C instantaneous temperature drop, requiring temperature compensation in inventory measurement.

4. **Regulatory & Cylinder Safety Standards**:
   - **PESO (Petroleum and Explosives Safety Organization, India)**: *Gas Cylinders Rules, 2016*.
   - **ISO 11439**: *Gas cylinders — High pressure cylinders for the on-board storage of natural gas as a fuel for automotive vehicles*.
   - *Requirement*: Mandatory Hydro-testing every 3 years; annual Pressure Relief Device (PRD/PRV) valve recertification. Vehicles failing these checks are dynamically blacklisted by the AI engine.

---

## 3. Demand Forecasting & Operations Research Algorithms

### A. Time-Series Diurnal Curve Modeling
- **Reference**: *Box, G. E., Jenkins, G. M., & Reinsel, G. C.*: *Time Series Analysis: Forecasting and Control*.
- **Implementation**: [`CngDemandForecastingEngine.kt`](file:///C:/Users/dappu/downloads/fleetopt/app/src/main/java/com/fleetopt/app/core/forecasting/CngDemandForecastingEngine.kt).
- **Diurnal Curve**: Incorporates double-peaked morning (07:00–10:00) and evening (17:00–21:00) city traffic commuter rushes:
  $$F(t) = \text{Baseline} \times M_{\text{diurnal}}(t) \times S_{\text{calendar}}$$
- **7-Day Weighted Moving Average (WMA)**:
  - Day-7 (same day last week): **30% weight** (captures day-of-week seasonality like commercial fleet schedules).
  - Day-1 to Day-6: Weighted from 20% down to 5%.

### B. Multi-Factor AI Dispatch Heuristic
- **Reference**: *Toth, P., & Vigo, D.*: *The Vehicle Routing Problem* (SIAM Monographs on Discrete Mathematics).
- **Implementation**: [`AiRecommendationEngine.kt`](file:///C:/Users/dappu/downloads/fleetopt/app/src/main/java/com/fleetopt/app/core/engine/AiRecommendationEngine.kt).
- **Multi-Factor Score Calculation**:
  $$\text{Score} = 0.40 \cdot S_{\text{demand}} + 0.20 \cdot S_{\text{proximity}} + 0.15 \cdot S_{\text{traffic}} + 0.15 \cdot S_{\text{availability}} + 0.10 \cdot S_{\text{capacity}}$$
- **Interlock Gates**:
  - `Station.occupiedBays >= Station.totalBays` $\rightarrow$ Interlocked (No docking slot).
  - `Hcv.isHydroTestValid == false` $\rightarrow$ Disqualified (Safety violation).
  - `Hcv.isPrvCertified == false` $\rightarrow$ Disqualified (Safety violation).
  - `Hcv.status == BREAKDOWN` $\rightarrow$ Disqualified.

---

## 4. Android SDK, Libraries & Frameworks

| Component | Library & Version | Official Reference |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.0.20 | [kotlinlang.org](https://kotlinlang.org/) |
| **UI Framework** | Jetpack Compose BOM 2024.11.00 | [developer.android.com/jetpack/compose](https://developer.android.com/jetpack/compose) |
| **Design System** | Material 3 (`androidx.compose.material3:material3`) | [m3.material.io](https://m3.material.io/) |
| **Navigation** | Navigation Compose 2.8.4 | [developer.android.com/guide/navigation](https://developer.android.com/guide/navigation) |
| **Local Database** | Room 2.6.1 + KSP 2.0.20-1.0.25 | [developer.android.com/training/data-storage/room](https://developer.android.com/training/data-storage/room) |
| **Coroutines & Flow**| KotlinX Coroutines Android 1.9.0 | [github.com/Kotlin/kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) |
| **Google Maps** | Maps Compose 4.4.1 + Play Services Maps 19.0.0 | [developers.google.com/maps/documentation/android-sdk](https://developers.google.com/maps/documentation/android-sdk) |
| **Testing** | JUnit 4.13.2 | [junit.org](https://junit.org/junit4/) |

---

## 5. Toolchains & Environment Setup

1. **Java Development Kit**:
   - **Eclipse Adoptium OpenJDK 21 LTS** (`jdk-21.0.12.1+1` Hotspot).
   - [adoptium.net](https://adoptium.net/)
   - Provides native `jlink.exe`, `javac.exe`, and JVM 21 needed for Android Gradle Plugin 8.7.2.
2. **Build Automation**:
   - **Gradle 8.11.1** with Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`).
   - Memory Optimization: `-Xmx2048m -XX:MaxMetaspaceSize=512m` in `gradle.properties`.
3. **Android Build Tools & SDK**:
   - **Target SDK**: Android 15 (API 35).
   - **Minimum SDK**: Android 8.0 Oreo (API 26) — ensures 98%+ global Android device compatibility.
   - **Build-Tools**: `34.0.0` (including `aapt2.exe`, `d8.bat`).
4. **ADB & Deployment**:
   - **Android Debug Bridge**: Version `1.0.41` (Revision 35.0.2).
   - USB MTP Shell API (`Shell.Application` COM object) for direct device deployment.

---

## 6. Project Code Structure

```
fleetopt/
├── app/
│   ├── build.gradle.kts                       # App dependencies, Compose BOM, Room, Maps
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml            # Permissions, app icon, activities, metadata
│       │   ├── res/                           # Mipmap launcher icons & brand drawable
│       │   └── java/com/fleetopt/app/
│       │       ├── core/
│       │       │   ├── physics/               # CngTelemetryCalculator.kt
│       │       │   ├── forecasting/           # CngDemandForecastingEngine.kt
│       │       │   ├── engine/                # AiRecommendationEngine.kt
│       │       │   ├── simulation/            # TelemetrySimulationEngine.kt
│       │       │   └── notification/          # StockoutNotificationManager.kt
│       │       ├── data/
│       │       │   ├── local/entity/          # StationEntity, HcvEntity, HourlySalesEntity, TripEntity
│       │       │   ├── local/dao/             # StationDao, HcvDao, HourlySalesDao, DispatchTripDao
│       │       │   ├── local/                 # FleetOptDatabase.kt (Pre-seeded DB)
│       │       │   └── repository/            # FleetOptRepository.kt
│       │       └── ui/
│       │           ├── components/            # Cascade gauges, radar canvas, status badges
│       │           ├── navigation/            # Screen routes & NavGraph
│       │           ├── screens/               # 7 primary screens (Dashboard, AI, Fleet, etc.)
│       │           ├── theme/                 # Dark industrial SCADA theme
│       │           └── viewmodel/             # FleetOptViewModel.kt
│       └── test/java/com/fleetopt/app/        # Physics, forecasting, and AI engine unit tests
├── build.gradle.kts                           # Top-level plugins (AGP, Kotlin, KSP)
├── settings.gradle.kts                        # Repositories (Google, MavenCentral)
├── gradle.properties                          # JVM heap configuration & Java home
└── local.properties                           # Android SDK directory path
```
