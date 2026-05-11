"""
Stadium Sync Backend — FastAPI Microservice
Prediction engine, crowd analytics, transit decisions, and sync service.
"""

from fastapi import FastAPI, HTTPException, Depends, Header
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime, timedelta
import random
import uuid
import math

app = FastAPI(title="Stadium Sync API", version="1.0.0", description="Backend for Stadium Mobility Management")

app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_credentials=True, allow_methods=["*"], allow_headers=["*"])

# ── Models ─────────────────────────────────────────────────
class LoginRequest(BaseModel):
    email: str
    password: str
    role: str = "OPERATOR"

class LoginResponse(BaseModel):
    id: str
    name: str
    email: str
    role: str
    token: str

class MatchData(BaseModel):
    id: str = "match_001"
    team1: str = "Mumbai Indians"
    team2: str = "Chennai Super Kings"
    venue: str = "Wankhede Stadium, Mumbai"
    score1: str = "142/4"
    score2: str = ""
    overs1: str = "16.2"
    overs2: str = ""
    current_run_rate: float = 8.69
    required_run_rate: float = 10.27
    match_phase: str = "DEATH_OVERS"
    status: str = "LIVE"
    format: str = "T20"
    start_time: int = 0
    last_updated: int = 0

class PredictionResponse(BaseModel):
    estimated_end_time: int
    confidence_percent: int
    crowd_release_score: float
    transit_urgency_score: float
    estimated_minutes_left: int
    probability_distribution: List[float]

class CrowdPressureData(BaseModel):
    gate_id: str
    gate_name: str
    pressure_level: str
    density_percent: int
    trend: str
    estimated_people: int

class TransitRoute(BaseModel):
    id: str
    name: str
    type: str
    status: str
    capacity: int
    current_load: int
    estimated_delay_min: int

class TransitActionRequest(BaseModel):
    action_type: str
    route_id: str
    route_name: str = ""
    description: str = ""
    operator_name: str = ""

class SyncPayload(BaseModel):
    actions: List[dict] = []
    audit_logs: List[dict] = []

class AnalyticsSummary(BaseModel):
    total_crowd_pressure: float
    predicted_passenger_wave: int
    transport_actions_count: int
    sync_health_percent: int
    offline_queue_count: int
    notification_delivery_rate: float
    peak_crowd_time: str
    total_transit_capacity: int
    avg_response_time_ms: int

# ── State ──────────────────────────────────────────────────
action_log = []

# ── Auth ───────────────────────────────────────────────────
@app.post("/api/v1/auth/login", response_model=LoginResponse)
async def login(req: LoginRequest):
    return LoginResponse(
        id=str(uuid.uuid4()), name=req.email.split("@")[0].title(),
        email=req.email, role=req.role,
        token=f"jwt_{uuid.uuid4().hex[:32]}"
    )

# ── Match ──────────────────────────────────────────────────
@app.get("/api/v1/match/live", response_model=MatchData)
async def get_live_match():
    now = int(datetime.now().timestamp() * 1000)
    overs = round(random.uniform(10, 19), 1)
    score = int(overs * random.uniform(7, 10))
    wickets = random.randint(2, 7)
    phase = "POWERPLAY" if overs <= 6 else "MIDDLE_OVERS" if overs <= 15 else "DEATH_OVERS"
    return MatchData(
        score1=f"{score}/{wickets}", overs1=f"{overs:.1f}",
        current_run_rate=round(score / overs, 2), required_run_rate=round(random.uniform(8, 14), 2),
        match_phase=phase, start_time=now - 5400000, last_updated=now
    )

# ── Prediction ─────────────────────────────────────────────
@app.post("/api/v1/prediction/match-end", response_model=PredictionResponse)
async def predict_match_end(match: MatchData):
    overs = float(match.overs1)
    overs_left = max(20.0 - overs, 0)
    minutes_left = int(overs_left * 4.2)
    confidence = min(95, int(55 + (20 - overs_left) * 2.5))
    crowd_release = min(100, (20 - overs_left) / 20 * 100)
    urgency = min(100, max(0, 100 - overs_left * 5))
    return PredictionResponse(
        estimated_end_time=int((datetime.now() + timedelta(minutes=minutes_left)).timestamp() * 1000),
        confidence_percent=confidence, crowd_release_score=round(crowd_release, 1),
        transit_urgency_score=round(urgency, 1), estimated_minutes_left=minutes_left,
        probability_distribution=[0.05, 0.1, 0.2, 0.35, 0.2, 0.1]
    )

# ── Crowd ──────────────────────────────────────────────────
@app.get("/api/v1/crowd/pressure", response_model=List[CrowdPressureData])
async def get_crowd_pressure():
    gates = ["Gate A - North", "Gate B - East", "Gate C - South", "Gate D - West", "Gate E - VIP", "Gate F - Metro"]
    return [CrowdPressureData(
        gate_id=f"gate_{i}", gate_name=g,
        pressure_level=("CRITICAL" if (d := random.randint(20, 95)) > 80 else "HIGH" if d > 60 else "MODERATE" if d > 35 else "LOW"),
        density_percent=d, trend=random.choice(["RISING", "STABLE", "FALLING"]),
        estimated_people=d * 50
    ) for i, g in enumerate(gates)]

# ── Transit ────────────────────────────────────────────────
@app.get("/api/v1/transit/routes", response_model=List[TransitRoute])
async def get_routes():
    return [
        TransitRoute(id="metro_1", name="Metro Blue Line - Churchgate", type="METRO", status="ACTIVE", capacity=2400, current_load=1800, estimated_delay_min=0),
        TransitRoute(id="bus_1", name="BEST Route 83 - Worli", type="BUS", status="ACTIVE", capacity=60, current_load=45, estimated_delay_min=0),
        TransitRoute(id="shuttle_1", name="Stadium Shuttle A", type="SHUTTLE", status="ACTIVE", capacity=30, current_load=25, estimated_delay_min=0),
        TransitRoute(id="ride_1", name="Pickup Zone Alpha", type="RIDE_PICKUP", status="ACTIVE", capacity=200, current_load=120, estimated_delay_min=3),
    ]

@app.post("/api/v1/transit/action")
async def execute_transit_action(req: TransitActionRequest):
    entry = {"id": str(uuid.uuid4()), **req.dict(), "timestamp": datetime.now().isoformat(), "status": "EXECUTED"}
    action_log.append(entry)
    return {"status": "success", "action": entry}

@app.get("/api/v1/transit/actions")
async def get_action_log():
    return action_log[-50:]

# ── Sync ───────────────────────────────────────────────────
@app.post("/api/v1/sync/batch")
async def batch_sync(payload: SyncPayload):
    synced = len(payload.actions) + len(payload.audit_logs)
    return {"status": "success", "synced_count": synced, "timestamp": datetime.now().isoformat()}

# ── Analytics ──────────────────────────────────────────────
@app.get("/api/v1/analytics/summary", response_model=AnalyticsSummary)
async def get_analytics():
    return AnalyticsSummary(
        total_crowd_pressure=67.5, predicted_passenger_wave=32000,
        transport_actions_count=len(action_log), sync_health_percent=94,
        offline_queue_count=0, notification_delivery_rate=97.2,
        peak_crowd_time="21:45", total_transit_capacity=5200, avg_response_time_ms=340
    )

# ── Notifications ──────────────────────────────────────────
@app.post("/api/v1/notifications/trigger")
async def trigger_notification(title: str = "Alert", message: str = "Event occurred", priority: str = "INFO"):
    return {"id": str(uuid.uuid4()), "title": title, "message": message, "priority": priority,
            "timestamp": datetime.now().isoformat(), "delivered": True}

# ── Health ─────────────────────────────────────────────────
@app.get("/api/v1/health")
async def health_check():
    return {"status": "healthy", "timestamp": datetime.now().isoformat(), "version": "1.0.0"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
