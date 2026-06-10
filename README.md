<div align="center">

<img src="https://img.shields.io/badge/StadiumSync-v1.0.0-6C63FF?style=for-the-badge&logo=android&logoColor=white" />

# StadiumSync

### AI-Powered Stadium Mobility & Operations Platform

*Real-time crowd intelligence - Transit orchestration - Offline-first Android app*

[![Android](https://img.shields.io/badge/Android-API_26%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-2024.09-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.115-009688?style=flat-square&logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

</div>

---

## Overview

**StadiumSync** is a professional-grade, offline-first Android application built for stadium operations teams managing large-scale cricket events. It combines real-time crowd pressure monitoring, AI-powered match-end prediction, and intelligent transit orchestration, all built to work seamlessly even without internet connectivity.

The platform bridges the gap between match dynamics and mobility planning: as the game progresses, StadiumSync predicts crowd surge timing and proactively coordinates metro, bus, shuttle, and ride-pickup routes, enabling operators to act before chaos, not after.

---

## Features

### Android App

| Module | Description |
|---|---|
| **Dashboard** | Live match overview, crowd status, quick-action tiles |
| **Live Match** | Real-time scoreboard, run rate, phase tracking, AI end-time prediction |
| **Crowd Heatmap** | Gate-wise pressure levels (LOW to CRITICAL), trend indicators, density analytics |
| **Transit Control** | Orchestrate metro holds, bus dispatches, shuttle diversions in one tap |
| **Route Suggestions** | AI-scored multi-modal route suggestions with crowd, ETA, fare, and CO2 data |
| **Ticket Scanner** | QR/barcode scan, seat navigation, gate routing |
| **Notifications** | Priority-tiered alerts (CRITICAL / WARNING / INFO) with delivery tracking |
| **Offline Mode** | Full functionality with WorkManager sync queue, actions replay on reconnect |
| **Analytics** | Ops summary: crowd pressure score, passenger wave predictions, response times |
| **Settings** | Role-based profile, theme toggle, sync preferences |

### Backend (FastAPI)

| Endpoint | Description |
|---|---|
| `POST /api/v1/auth/login` | JWT-based role authentication |
| `GET /api/v1/match/live` | Live match data (score, overs, phase) |
| `POST /api/v1/prediction/match-end` | ML-powered match end prediction with confidence scores |
| `GET /api/v1/crowd/pressure` | Gate-wise crowd pressure with trend data |
| `GET /api/v1/transit/routes` | Active transit routes with capacity and load |
| `POST /api/v1/transit/action` | Log and execute transit control actions |
| `POST /api/v1/sync/batch` | Offline-first batch sync endpoint |
| `GET /api/v1/analytics/summary` | Full operations analytics summary |
| `POST /api/v1/notifications/trigger` | Push notification trigger |
| `GET /api/v1/health` | Health check |

---

## Architecture

```
StadiumSync/
|-- app/                                    # Android Application
|   |-- src/main/
|   |   |-- java/com/stadiumsync/app/
|   |   |   |-- core/
|   |   |   |   |-- datastore/             # DataStore preferences
|   |   |   |   |-- di/                    # Hilt dependency injection
|   |   |   |   `-- network/               # Network monitor (connectivity)
|   |   |   |-- data/
|   |   |   |   |-- local/                 # Room database, DAOs, Entities
|   |   |   |   |-- remote/mock/           # Mock API services (dev/offline)
|   |   |   |   `-- repository/            # Repository implementations
|   |   |   |-- domain/
|   |   |   |   |-- model/                 # Domain models (Match, Crowd, Transit)
|   |   |   |   |-- repository/            # Repository interfaces
|   |   |   |   `-- usecase/               # Business logic use cases
|   |   |   |-- notification/              # FCM notification helper
|   |   |   |-- presentation/
|   |   |   |   |-- components/            # Shared Compose UI components
|   |   |   |   |-- navigation/            # NavHost + bottom navigation
|   |   |   |   |-- screen/                # Feature screens + ViewModels
|   |   |   |   |   |-- analytics/
|   |   |   |   |   |-- crowd/
|   |   |   |   |   |-- home/
|   |   |   |   |   |-- login/
|   |   |   |   |   |-- match/
|   |   |   |   |   |-- notifications/
|   |   |   |   |   |-- offline/
|   |   |   |   |   |-- route/
|   |   |   |   |   |-- settings/
|   |   |   |   |   |-- ticket/
|   |   |   |   |   `-- transit/
|   |   |   |   `-- theme/                 # Material 3 theme + typography
|   |   |   |-- sync/                      # WorkManager sync workers
|   |   |   |-- MainActivity.kt
|   |   |   `-- StadiumSyncApp.kt          # Hilt application entry point
|   |   `-- res/                           # Resources (drawables, values, xml)
|   |-- schemas/                           # Room database migration schemas
|   |-- build.gradle.kts
|   `-- proguard-rules.pro
|
|-- backend/                               # Python FastAPI Microservice
|   |-- ml/
|   |   `-- prediction_engine.py           # XGBoost / scikit-learn prediction
|   |-- main.py                            # FastAPI app, routes, models
|   |-- requirements.txt                   # Python dependencies
|   |-- Dockerfile
|   `-- docker-compose.yml                 # API + PostgreSQL + Redis stack
|
|-- gradle/
|   |-- libs.versions.toml                 # Version catalog
|   `-- wrapper/
|-- build.gradle.kts
|-- settings.gradle.kts
|-- gradlew / gradlew.bat
`-- README.md
```

### Design Pattern

The Android app follows **Clean Architecture** with three layers:

```
Presentation  -->  ViewModel  -->  UseCase  -->  Repository  -->  Data Source
(Compose UI)       (StateFlow)    (Domain)      (Interface)      (Room / API)
```

---

## Tech Stack

### Android

| Layer | Technology |
|---|---|
| **UI** | Jetpack Compose - Material 3 - Navigation Compose |
| **Architecture** | Clean Architecture - MVVM - StateFlow |
| **DI** | Hilt (Dagger) - KSP |
| **Database** | Room 2.6 - SQLite |
| **Network** | Retrofit 2.11 - OkHttp 4 - Kotlinx Serialization |
| **Async** | Kotlin Coroutines 1.8 - Flow |
| **Offline Sync** | WorkManager 2.9 |
| **Preferences** | DataStore Preferences 1.1 |
| **Language** | Kotlin 2.0.0 |
| **Min SDK** | API 26 (Android 8.0) |
| **Target SDK** | API 35 (Android 15) |

### Backend

| Layer | Technology |
|---|---|
| **Framework** | FastAPI 0.115 - Uvicorn |
| **ML Engine** | XGBoost 2.1 - scikit-learn 1.5 - NumPy - Pandas |
| **Database** | PostgreSQL 16 - SQLAlchemy 2.0 - Alembic |
| **Cache** | Redis 7.4 |
| **Auth** | JWT (python-jose) - bcrypt (passlib) |
| **Container** | Docker - Docker Compose |

---

## Getting Started

### Prerequisites

- **Android Studio** Ladybug (2024.2) or later
- **JDK 17** or later
- **Android device / emulator** running API 26+
- **Python 3.11+** (for backend)
- **Docker and Docker Compose** (for full backend stack)

---

### Android App Setup

**1. Clone the repository**

```bash
git clone https://github.com/harshal-paltse/Stadium-Sync-.git
cd Stadium-Sync-
```

**2. Open in Android Studio**

```
File -> Open -> select the root folder
```

**3. Sync Gradle**

```bash
./gradlew build
```

**4. Run the app**

```
Select an emulator or connected device -> Run
```

> The app ships with **mock data** so it runs fully offline with no backend required for development.

---

### Backend Setup

#### Option A - Docker (Recommended)

```bash
cd backend
docker compose up --build -d
```

This spins up:
- **API** at `http://localhost:8000`
- **PostgreSQL** at `localhost:5432`
- **Redis** at `localhost:6379`

#### Option B - Local Python

```bash
cd backend
python -m venv venv
source venv/bin/activate   # Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

#### API Documentation

Once running, open:
- **Swagger UI** at `http://localhost:8000/docs`
- **ReDoc** at `http://localhost:8000/redoc`

---

## Default Credentials (Dev / Mock)

| Role | Email | Password |
|---|---|---|
| Admin | `admin@stadiumsync.com` | `admin123` |
| Operator | `operator@stadiumsync.com` | `operator123` |
| Transit Officer | `transit@stadiumsync.com` | `transit123` |

> These are mock credentials for development only. Replace with secure auth in production.

---

## Key Domain Models

```kotlin
// Match phases drive the entire prediction pipeline
enum class MatchPhase {
    PRE_MATCH, POWERPLAY, MIDDLE_OVERS, DEATH_OVERS,
    INNINGS_BREAK, SECOND_INNINGS, COMPLETED
}

// Crowd pressure - real-time per gate
data class CrowdPressure(
    val gateId: String,
    val gateName: String,
    val pressureLevel: PressureLevel,   // LOW / MODERATE / HIGH / CRITICAL
    val densityPercent: Int,
    val trend: PressureTrend,           // RISING / STABLE / FALLING
    val estimatedPeople: Int
)

// Transit actions are logged and synced offline
enum class TransitActionType {
    HOLD_METRO, DISPATCH_BUS, OPEN_ALTERNATE_ZONE, DIVERT_ROUTE, SEND_CROWD_ALERT
}

// Role-based access control
enum class UserRole { ADMIN, OPERATOR, VIEWER, TRANSIT_OFFICER }
```

---

## Offline-First Sync Flow

```
User performs action  (e.g. HOLD_METRO)
         |
         v
    Is Online?
    +----------+----------+
   YES                   NO
    |                     |
Execute               Queue in
immediately           Room DB
    |                     |
    +----------+----------+
               |
               v
    WorkManager SyncWorker
    (runs when connectivity restored)
               |
               v
    POST /api/v1/sync/batch
               |
               v
    Mark actions as SYNCED
```

---

## Contributing

```bash
# 1. Fork the repo
# 2. Create a feature branch
git checkout -b feature/your-feature-name

# 3. Commit your changes
git commit -m "feat: add your feature description"

# 4. Push to your branch
git push origin feature/your-feature-name

# 5. Open a Pull Request
```

Please follow [Conventional Commits](https://www.conventionalcommits.org/) for commit messages.

---

## License

```
MIT License - Copyright (c) 2026 Harshal Paltse

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software.
```

---

<div align="center">

**Built with heart for smarter stadium operations**

[Report Bug](https://github.com/harshal-paltse/Stadium-Sync-/issues) - [Request Feature](https://github.com/harshal-paltse/Stadium-Sync-/issues)

</div>
