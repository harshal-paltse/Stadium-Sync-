# Stadium Sync

**Intelligent Stadium Mobility Management Platform**

A production-ready Kotlin Android app that predicts match completion, estimates crowd exit pressure, and coordinates transport decisions for metro, buses, shuttles, and ride pickup zones around IPL stadiums.

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│                Android App (Kotlin)              │
│  Jetpack Compose · Material 3 · MVVM · Hilt     │
├──────────┬──────────┬──────────┬────────────────┤
│ Present. │  Domain  │   Data   │   Infra        │
│ Screens  │ UseCases │ Repos    │ Room/DataStore  │
│ ViewMods │ Models   │ DAOs     │ WorkManager     │
│ Nav/Theme│ Repos(I) │ Mock Svc │ Notifications   │
└──────────┴──────────┴────┬─────┴────────────────┘
                           │
                    ┌──────┴──────┐
                    │ FastAPI     │
                    │ Backend     │
                    ├─────────────┤
                    │ PostgreSQL  │
                    │ Redis       │
                    │ XGBoost ML  │
                    └─────────────┘
```

## Quick Start

### Android App

1. **Open in Android Studio** — Import the `Stadium2` folder
2. Android Studio will download the Gradle wrapper and dependencies automatically
3. **Build & Run** — Select an emulator or device with API 26+, click Run
4. **Login** — Use any email/password or tap "Continue Offline"

### Backend (Optional)

```bash
cd backend
docker-compose up -d
```

Or run locally:
```bash
cd backend
pip install -r requirements.txt
python main.py
```

API docs available at `http://localhost:8000/docs`

### ML Pipeline

```bash
cd backend/ml
pip install xgboost scikit-learn pandas numpy joblib
python prediction_engine.py
```

## Screens

| Screen | Description |
|--------|-------------|
| **Login** | Operator authentication with offline login option |
| **Home Dashboard** | Live match summary, crowd metrics, transit status, quick actions |
| **Live Match** | Scorecard, predicted end time, confidence gauge, match intelligence |
| **Crowd Heatmap** | Canvas-based stadium density map, gate-level pressure bars |
| **Transit Control** | Route status list, action execution with confirmation, recommendations |
| **Route Suggestions** | Crowd dispersal routes with time estimates and crowd levels |
| **Notifications** | Alert history with priority filtering (Critical/Warning/Info) |
| **Offline Mode** | Network status, sync controls, cached data display, pending queue |
| **Settings** | Dark mode toggle, notification preferences, sync interval, logout |
| **Analytics** | Crowd trend chart, metrics dashboard, transport action log |

## Features

### Offline-First Architecture
- **Room Database** caches all match, crowd, transit, and notification data locally
- **WorkManager** retries sync automatically when network returns
- **Local notification rules** trigger alerts from cached data without network
- **Offline banner** and clear status indicators throughout the app
- **Deferred sync queue** stores actions and syncs in batch on reconnection

### Prediction Engine
- XGBoost model for match end-time forecasting
- Rules engine fallback when ML confidence is low
- Crowd rush level classification
- Transit urgency scoring
- Synthetic data generator for training

### Transport Coordination
- Real-time status for metro, bus, shuttle, and ride pickup zones
- Action system: Hold Metro, Dispatch Bus, Open Alternate Zone, Divert Route
- Operator confirmation dialogs with audit logging
- Automated recommendations based on crowd pressure thresholds

### Theme System
- **Light mode** (default): White background, black text
- **Dark mode**: Toggle via Settings screen
- Material 3 with custom color tokens for status chips
- Professional enterprise-grade typography and spacing

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Network | Retrofit + OkHttp |
| Local DB | Room |
| Settings | DataStore Preferences |
| Background | WorkManager |
| State | Coroutines + StateFlow |
| Navigation | Navigation Compose |
| Backend | FastAPI (Python) |
| ML | XGBoost + scikit-learn |
| Database | PostgreSQL + Redis |
| Container | Docker + Docker Compose |

## Project Structure

```
app/src/main/java/com/stadiumsync/app/
├── core/
│   ├── di/AppModule.kt           # Hilt DI modules
│   ├── network/NetworkMonitor.kt  # Connectivity observer
│   └── datastore/UserPreferences.kt
├── data/
│   ├── local/
│   │   ├── entity/Entities.kt    # Room entities
│   │   ├── dao/Daos.kt           # Room DAOs
│   │   └── StadiumSyncDatabase.kt
│   ├── remote/mock/MockServices.kt
│   └── repository/RepositoryImpls.kt
├── domain/
│   ├── model/Models.kt
│   ├── repository/Repositories.kt
│   └── usecase/UseCases.kt
├── presentation/
│   ├── theme/Theme.kt
│   ├── components/Components.kt
│   ├── navigation/Navigation.kt
│   └── screen/{login,home,match,crowd,transit,route,notifications,offline,settings,analytics}/
├── sync/SyncWorkers.kt
├── notification/NotificationHelper.kt
├── StadiumSyncApp.kt
└── MainActivity.kt
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/login` | Operator authentication |
| GET | `/api/v1/match/live` | Live match data |
| POST | `/api/v1/prediction/match-end` | Predict match end time |
| GET | `/api/v1/crowd/pressure` | Crowd pressure by gate |
| GET | `/api/v1/transit/routes` | Transit route status |
| POST | `/api/v1/transit/action` | Execute transit action |
| POST | `/api/v1/sync/batch` | Batch sync offline data |
| GET | `/api/v1/analytics/summary` | Analytics dashboard data |
| POST | `/api/v1/notifications/trigger` | Trigger notification |
| GET | `/api/v1/health` | Health check |

## Configuration

### API Keys (Optional)
- Cricket data API key in `core/util/Constants.kt`
- Google Maps API key in `AndroidManifest.xml`
- Firebase `google-services.json` in `app/` directory

### Backend URL
- Default: `http://10.0.2.2:8000` (Android emulator to localhost)
- Change in Settings screen or `UserPreferences.kt`

## License

Proprietary — Stadium Sync Operations Platform
