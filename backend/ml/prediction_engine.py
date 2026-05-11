"""
Stadium Sync ML Pipeline — Match End Time Prediction
Uses XGBoost with synthetic training data for cricket match duration prediction.
"""

import numpy as np
import pandas as pd
from typing import Dict, List, Tuple
import json
import os

try:
    import xgboost as xgb
    from sklearn.model_selection import train_test_split
    from sklearn.metrics import mean_absolute_error, r2_score
    from sklearn.preprocessing import LabelEncoder
    import joblib
    ML_AVAILABLE = True
except ImportError:
    ML_AVAILABLE = False


class SyntheticDataGenerator:
    """Generates realistic cricket match training data."""

    FORMATS = {"T20": 20, "ODI": 50, "TEST": 90}

    @staticmethod
    def generate(n_samples: int = 5000) -> pd.DataFrame:
        np.random.seed(42)
        records = []
        for _ in range(n_samples):
            fmt = np.random.choice(["T20", "ODI"])
            total_overs = SyntheticDataGenerator.FORMATS[fmt]
            current_over = round(np.random.uniform(1, total_overs), 1)
            overs_left = total_overs - current_over
            wickets = min(10, int(np.random.exponential(3)))
            run_rate = np.random.uniform(4.0, 12.0) if fmt == "T20" else np.random.uniform(3.0, 8.0)
            score = int(current_over * run_rate)
            phase_idx = 0 if current_over <= 6 else (1 if current_over <= 15 else 2)  # PP/MID/DEATH
            rain_delay = np.random.choice([0, 10, 20, 30], p=[0.85, 0.08, 0.05, 0.02])
            review_delay = np.random.choice([0, 3, 5], p=[0.9, 0.07, 0.03])
            # Target: actual minutes remaining
            base_minutes = overs_left * 4.2
            noise = np.random.normal(0, 3)
            actual_minutes_left = max(0, base_minutes + rain_delay + review_delay + noise)

            records.append({
                "format": fmt, "current_over": current_over, "overs_left": overs_left,
                "score": score, "wickets": wickets, "run_rate": round(run_rate, 2),
                "phase": phase_idx, "rain_delay": rain_delay, "review_delay": review_delay,
                "actual_minutes_left": round(actual_minutes_left, 1)
            })
        return pd.DataFrame(records)


class MatchEndPredictor:
    """XGBoost-based match end time predictor."""

    def __init__(self, model_path: str = "models/match_predictor.json"):
        self.model_path = model_path
        self.model = None
        self.label_encoder = LabelEncoder()
        self.feature_cols = ["current_over", "overs_left", "score", "wickets", "run_rate",
                             "phase", "rain_delay", "review_delay", "format_encoded"]

    def train(self, data: pd.DataFrame = None) -> Dict:
        if not ML_AVAILABLE:
            return {"error": "ML libraries not installed"}

        if data is None:
            data = SyntheticDataGenerator.generate()

        data["format_encoded"] = self.label_encoder.fit_transform(data["format"])
        X = data[self.feature_cols]
        y = data["actual_minutes_left"]

        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

        self.model = xgb.XGBRegressor(
            n_estimators=200, max_depth=6, learning_rate=0.1,
            subsample=0.8, colsample_bytree=0.8, random_state=42
        )
        self.model.fit(X_train, y_train, eval_set=[(X_test, y_test)], verbose=False)

        predictions = self.model.predict(X_test)
        mae = mean_absolute_error(y_test, predictions)
        r2 = r2_score(y_test, predictions)

        os.makedirs(os.path.dirname(self.model_path), exist_ok=True)
        self.model.save_model(self.model_path)
        joblib.dump(self.label_encoder, self.model_path.replace(".json", "_encoder.pkl"))

        return {"mae": round(mae, 2), "r2": round(r2, 4), "samples": len(data), "model_path": self.model_path}

    def predict(self, current_over: float, score: int, wickets: int, run_rate: float,
                match_format: str = "T20", rain_delay: int = 0, review_delay: int = 0) -> Dict:
        total_overs = 20 if match_format == "T20" else 50
        overs_left = max(0, total_overs - current_over)
        phase = 0 if current_over <= 6 else (1 if current_over <= 15 else 2)

        # Fallback: rules-based prediction if model not available
        if self.model is None:
            minutes_left = int(overs_left * 4.2 + rain_delay + review_delay)
            confidence = min(95, int(55 + (total_overs - overs_left) / total_overs * 40))
            return {
                "estimated_minutes_left": minutes_left,
                "confidence_percent": confidence,
                "crowd_release_score": round((total_overs - overs_left) / total_overs * 100, 1),
                "transit_urgency_score": round(min(100, max(0, 100 - overs_left * 5)), 1),
                "method": "rules_engine"
            }

        fmt_encoded = self.label_encoder.transform([match_format])[0] if match_format in self.label_encoder.classes_ else 0
        features = np.array([[current_over, overs_left, score, wickets, run_rate, phase, rain_delay, review_delay, fmt_encoded]])
        predicted_minutes = max(0, float(self.model.predict(features)[0]))
        confidence = min(95, int(55 + (total_overs - overs_left) / total_overs * 40))

        return {
            "estimated_minutes_left": int(predicted_minutes),
            "confidence_percent": confidence,
            "crowd_release_score": round((total_overs - overs_left) / total_overs * 100, 1),
            "transit_urgency_score": round(min(100, max(0, 100 - overs_left * 5)), 1),
            "method": "xgboost"
        }

    def load(self):
        if not ML_AVAILABLE:
            return False
        try:
            self.model = xgb.XGBRegressor()
            self.model.load_model(self.model_path)
            self.label_encoder = joblib.load(self.model_path.replace(".json", "_encoder.pkl"))
            return True
        except Exception:
            return False


class CrowdRushClassifier:
    """Lightweight classifier for crowd rush level prediction."""

    @staticmethod
    def predict(overs_left: float, wickets: int, density_percent: int, is_weekend: bool = True) -> Dict:
        risk_score = 0.0
        risk_score += max(0, (20 - overs_left)) * 2.5  # Closer to end = higher risk
        risk_score += max(0, (10 - wickets)) * 3        # More wickets standing = more crowd
        risk_score += density_percent * 0.4              # Current density
        if is_weekend:
            risk_score *= 1.2

        risk_score = min(100, risk_score)
        level = "CRITICAL" if risk_score > 80 else "HIGH" if risk_score > 60 else "MODERATE" if risk_score > 35 else "LOW"

        return {"risk_score": round(risk_score, 1), "level": level, "method": "rules_classifier"}


class TransitUrgencyScorer:
    """Scores transit urgency based on match and crowd state."""

    @staticmethod
    def score(minutes_left: int, avg_density: float, active_routes: int, total_capacity: int) -> Dict:
        time_factor = max(0, (30 - minutes_left)) / 30 * 40
        crowd_factor = avg_density / 100 * 35
        capacity_factor = max(0, 1 - total_capacity / max(1, avg_density * 50)) * 25
        urgency = min(100, time_factor + crowd_factor + capacity_factor)
        action = "IMMEDIATE" if urgency > 75 else "PREPARE" if urgency > 45 else "MONITOR"

        return {"urgency_score": round(urgency, 1), "recommended_action": action, "time_factor": round(time_factor, 1),
                "crowd_factor": round(crowd_factor, 1), "capacity_factor": round(capacity_factor, 1)}


if __name__ == "__main__":
    print("=== Stadium Sync ML Pipeline ===\n")

    # Train model
    predictor = MatchEndPredictor()
    if ML_AVAILABLE:
        result = predictor.train()
        print(f"Training complete: MAE={result['mae']} min, R²={result['r2']}")
    else:
        print("ML libraries not installed. Using rules engine fallback.")

    # Test prediction
    pred = predictor.predict(current_over=16.3, score=142, wickets=4, run_rate=8.69)
    print(f"\nPrediction: {json.dumps(pred, indent=2)}")

    # Crowd classification
    crowd = CrowdRushClassifier.predict(overs_left=3.3, wickets=4, density_percent=78)
    print(f"\nCrowd Risk: {json.dumps(crowd, indent=2)}")

    # Transit urgency
    transit = TransitUrgencyScorer.score(minutes_left=14, avg_density=72.0, active_routes=9, total_capacity=5200)
    print(f"\nTransit Urgency: {json.dumps(transit, indent=2)}")
