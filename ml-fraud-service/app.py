"""NexaPay behavioural ML scorer.
The Random Forest is trained on reproducible synthetic teaching data. A persistent,
per-customer event store adds personalised signals; only confirmed/approved outcomes
are added to the baseline profile. This is a teaching demo, not a production banking model.
"""
from datetime import datetime, timezone
from pathlib import Path
import math, os, sqlite3
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from sklearn.ensemble import RandomForestClassifier
import numpy as np

app = FastAPI(title="NexaPay Behavioural Fraud ML", version="2.0.0")
DB_PATH = os.getenv("BEHAVIOUR_DB", "/data/behaviour.db")
Path(DB_PATH).parent.mkdir(parents=True, exist_ok=True)
FEATURES = ["amount_to_average", "recent_transactions", "new_device", "known_receiver", "impossible_travel", "account_age_days", "balance_ratio", "new_account_no_history"]

# Synthetic training data: useful for demonstration of the ML pipeline, not a claim of real fraud accuracy.
rng = np.random.default_rng(20261019)
n = 12000
ratio = np.clip(rng.lognormal(mean=0.1, sigma=1.0, size=n), 0.1, 40)
recent = rng.poisson(1.5, size=n).clip(0, 20)
new_device = rng.binomial(1, .18, size=n)
known_receiver = rng.binomial(1, .72, size=n)
impossible = rng.binomial(1, .025, size=n)
age = rng.integers(0, 2500, size=n)
balance = rng.beta(1.4, 3.0, size=n)
new_no_history = ((age < 7) & (rng.random(n) < .65)).astype(int)
logit = (-4.0 + 2.9*impossible + 1.2*new_device + 1.15*(1-known_receiver) + .75*(recent >= 5) + .7*new_no_history + .45*(ratio >= 8) + .55*((new_device == 1) & (known_receiver == 0)) + .35*((balance >= .85) & (new_device == 1)))
prob = 1 / (1 + np.exp(-logit))
y = rng.binomial(1, prob)
model = RandomForestClassifier(n_estimators=140, max_depth=9, min_samples_leaf=5, class_weight="balanced", random_state=42, n_jobs=-1)
model.fit(np.column_stack([ratio, recent, new_device, known_receiver, impossible, age, balance, new_no_history]), y)

def connect():
    c = sqlite3.connect(DB_PATH, timeout=10)
    c.row_factory = sqlite3.Row
    c.execute("""CREATE TABLE IF NOT EXISTS approved_events (
        id INTEGER PRIMARY KEY AUTOINCREMENT, customer_id TEXT NOT NULL, event_type TEXT NOT NULL,
        occurred_at TEXT NOT NULL, amount REAL, latitude REAL, longitude REAL, city TEXT,
        device_id TEXT, receiver TEXT, UNIQUE(customer_id,event_type,occurred_at,amount,device_id,receiver))""")
    c.execute("CREATE INDEX IF NOT EXISTS ix_events_customer_time ON approved_events(customer_id, occurred_at DESC)")
    return c

class TransactionFeatures(BaseModel):
    amount: float = Field(ge=0)
    customer_id: str = "demo-customer"
    occurred_at: datetime | None = None
    latitude: float | None = Field(default=None, ge=-90, le=90)
    longitude: float | None = Field(default=None, ge=-180, le=180)
    city: str | None = None
    device_id: str | None = None
    receiver: str | None = None
    amount_to_average: float = Field(default=1.0, ge=0)
    recent_transactions: int = Field(default=0, ge=0)
    new_device: bool = False
    known_receiver: bool = True
    impossible_travel: bool = False
    account_age_days: int = Field(default=365, ge=0)
    balance_ratio: float = Field(default=0, ge=0, le=1)
    new_account_no_history: bool = False

class Feedback(BaseModel):
    customer_id: str
    event_type: str = "PAYMENT"
    occurred_at: datetime
    amount: float | None = None
    latitude: float | None = None
    longitude: float | None = None
    city: str | None = None
    device_id: str | None = None
    receiver: str | None = None
    outcome: str = "APPROVED"

class LoginFeatures(BaseModel):
    customer_id: str
    occurred_at: datetime | None = None
    latitude: float | None = Field(default=None, ge=-90, le=90)
    longitude: float | None = Field(default=None, ge=-180, le=180)
    city: str | None = None
    device_id: str | None = None


def haversine(lat1, lon1, lat2, lon2):
    r=6371.0
    p1,p2=math.radians(lat1),math.radians(lat2)
    dp=math.radians(lat2-lat1); dl=math.radians(lon2-lon1)
    a=math.sin(dp/2)**2+math.cos(p1)*math.cos(p2)*math.sin(dl/2)**2
    return 2*r*math.asin(min(1,math.sqrt(a)))

def as_utc(value):
    if value is None: return datetime.now(timezone.utc)
    return value.replace(tzinfo=timezone.utc) if value.tzinfo is None else value.astimezone(timezone.utc)

@app.on_event("startup")
def startup():
    c=connect(); c.close()

@app.get("/health")
def health():
    c=connect()
    count=c.execute("SELECT COUNT(*) FROM approved_events").fetchone()[0]; c.close()
    return {"status":"UP","model":"RandomForestClassifier-demo","trainingData":"synthetic-demo","historyStore":"SQLite persistent per-customer approved events","approvedEventsStored":count,"features":FEATURES}

@app.post("/score")
def score(t: TransactionFeatures):
    at=as_utc(t.occurred_at)
    conn=connect()
    events=conn.execute("SELECT * FROM approved_events WHERE customer_id=? AND event_type='PAYMENT' ORDER BY occurred_at DESC LIMIT 100", (t.customer_id,)).fetchall()
    location_events=conn.execute("SELECT * FROM approved_events WHERE customer_id=? AND latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY occurred_at DESC LIMIT 100", (t.customer_id,)).fetchall()
    all_events=conn.execute("SELECT * FROM approved_events WHERE customer_id=? ORDER BY occurred_at DESC LIMIT 200", (t.customer_id,)).fetchall()
    reasons=[]; behavioural=0
    # Derive personal baseline only from this customer's approved history.
    amounts=[float(e["amount"]) for e in events if e["amount"] is not None]
    avg=float(np.mean(amounts)) if amounts else None
    recent_count=sum(1 for e in events if (at-as_utc(datetime.fromisoformat(e["occurred_at"]))).total_seconds() <= 600 and (at-as_utc(datetime.fromisoformat(e["occurred_at"]))).total_seconds() >= 0)
    if recent_count >= 4:
        behavioural += 18; reasons.append(f"High personal transaction velocity: {recent_count} approved payments in 10 minutes")
    if avg and avg > 0 and t.amount > max(avg*4, avg+10000):
        behavioural += 12; reasons.append("Payment is unusually large compared with this customer's own approved-payment average")
    if t.device_id and all_events and all(e["device_id"] != t.device_id for e in all_events if e["device_id"]):
        behavioural += 10; reasons.append("Device has not appeared in this customer's approved payment history")
    if t.receiver and events and all((e["receiver"] or "").lower() != t.receiver.lower() for e in events):
        behavioural += 8; reasons.append("Recipient is new to this customer's approved payment history")
    if at.hour < 5 and all_events:
        usual_night=sum(1 for e in all_events if as_utc(datetime.fromisoformat(e["occurred_at"])).hour < 5)
        if usual_night == 0:
            behavioural += 8; reasons.append("Unusual hour: this customer has no successful logins or approved payments between 00:00 and 04:59")
    # Compare current location with the last known approved payment location, using time and distance.
    located=location_events
    impossible=False; travel_km=None; elapsed_minutes=None
    if t.latitude is not None and t.longitude is not None and located:
        last=located[0]; last_at=as_utc(datetime.fromisoformat(last["occurred_at"]))
        elapsed_minutes=max(0,(at-last_at).total_seconds()/60)
        travel_km=haversine(float(last["latitude"]),float(last["longitude"]),t.latitude,t.longitude)
        # 500km in <30min or 1000km in <60min is physically implausible; location sources can be noisy.
        impossible=(travel_km > 500 and elapsed_minutes < 30) or (travel_km > 1000 and elapsed_minutes < 60)
    impossible = impossible or t.impossible_travel
    if impossible:
        behavioural += 45; reasons.append(f"Impossible-travel signal: about {travel_km:.0f} km in {elapsed_minutes:.0f} minutes between approved and current locations" if travel_km is not None else "Impossible-travel signal supplied by upstream risk rules")
    conn.close()
    avg_for_model=avg if avg and avg>0 else None
    amount_ratio=t.amount/avg_for_model if avg_for_model else t.amount_to_average
    row=np.array([[amount_ratio, max(t.recent_transactions,recent_count), int(t.new_device), int(not (t.known_receiver and (t.receiver is None or any((e['receiver'] or '').lower()==t.receiver.lower() for e in events)))), int(impossible), t.account_age_days, t.balance_ratio, int(t.new_account_no_history and not events)]],dtype=float)
    ml=float(model.predict_proba(row)[0][1])*100
    # Keep a single high amount from dominating; personal context and travel are stronger evidence.
    risk=max(0,min(100,round(0.65*ml+0.35*behavioural)))
    if impossible: risk=max(risk,75)
    if reasons and risk < 25: risk=25
    return {"riskScore":risk,"fraudProbability":round(risk/100,4),"mlRiskScore":round(ml),"behaviouralRiskScore":behavioural,
            "prediction":"SUSPICIOUS" if risk>=65 else "REVIEW" if risk>=35 else "LIKELY_LEGITIMATE",
            "model":"RandomForestClassifier-demo+personal-history","trainingData":"synthetic-demo",
            "personalHistoryEvents":len(events),"personalLoginEvents":sum(1 for e in all_events if e["event_type"] == "LOGIN"),"personalLocationEvents":len(location_events),"signals":reasons,"impossibleTravel":impossible,"travelDistanceKm":round(travel_km,1) if travel_km is not None else None,
            "travelElapsedMinutes":round(elapsed_minutes,1) if elapsed_minutes is not None else None,
            "notice":"Teaching demonstration only. IP/VPN/GPS location may be inaccurate; verify suspicious signals rather than auto-blocking on location or amount alone."}


@app.post("/score-login")
def score_login(t: LoginFeatures):
    at=as_utc(t.occurred_at)
    conn=connect()
    history=conn.execute("SELECT * FROM approved_events WHERE customer_id=? ORDER BY occurred_at DESC LIMIT 200",(t.customer_id,)).fetchall()
    located=[e for e in history if e["latitude"] is not None and e["longitude"] is not None]
    reasons=[]; risk=0; travel_km=None; elapsed_minutes=None
    if history and t.device_id and all(e["device_id"] != t.device_id for e in history if e["device_id"]):
        risk+=20; reasons.append("Login from a device not seen in this customer's successful login or approved payment history")
    if at.hour < 5 and history and not any(as_utc(datetime.fromisoformat(e["occurred_at"])).hour < 5 for e in history):
        risk+=15; reasons.append("Login at an unusual hour for this customer")
    impossible=False
    if located and t.latitude is not None and t.longitude is not None:
        last=located[0]; last_at=as_utc(datetime.fromisoformat(last["occurred_at"]))
        elapsed_minutes=max(0,(at-last_at).total_seconds()/60)
        travel_km=haversine(float(last["latitude"]),float(last["longitude"]),t.latitude,t.longitude)
        impossible=(travel_km>500 and elapsed_minutes<30) or (travel_km>1000 and elapsed_minutes<60)
    if impossible:
        risk+=70; reasons.append(f"Impossible-travel login signal: about {travel_km:.0f} km in {elapsed_minutes:.0f} minutes")
    conn.close()
    risk=min(100,risk)
    return {"riskScore":risk,"stepUpRequired":risk>=60,"signals":reasons,"impossibleTravel":impossible,
            "travelDistanceKm":round(travel_km,1) if travel_km is not None else None,
            "travelElapsedMinutes":round(elapsed_minutes,1) if elapsed_minutes is not None else None,
            "notice":"Location may be inaccurate; high-risk logins require OTP verification, not automatic account blocking."}

@app.post("/feedback")
def feedback(f: Feedback):
    if f.outcome.upper() not in {"APPROVED","CONFIRMED_LEGITIMATE"}:
        return {"stored":False,"reason":"Only final approved/confirmed-legitimate events update the normal-behaviour baseline."}
    at=as_utc(f.occurred_at).isoformat()
    c=connect()
    c.execute("""INSERT OR IGNORE INTO approved_events(customer_id,event_type,occurred_at,amount,latitude,longitude,city,device_id,receiver)
        VALUES(?,?,?,?,?,?,?,?,?)""",(f.customer_id,f.event_type.upper(),at,f.amount,f.latitude,f.longitude,f.city,f.device_id,f.receiver))
    c.commit(); total=c.execute("SELECT COUNT(*) FROM approved_events WHERE customer_id=?",(f.customer_id,)).fetchone()[0]; c.close()
    return {"stored":True,"customerId":f.customer_id,"customerApprovedHistoryEvents":total,"outcome":f.outcome.upper()}

@app.post("/test/behavior-analysis")
def behavior_analysis(t: TransactionFeatures):
    return score(t)
