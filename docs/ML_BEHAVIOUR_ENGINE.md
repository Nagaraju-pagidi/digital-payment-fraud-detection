# NexaPay personal-behaviour fraud engine

## What is implemented

- The fraud service calls the ML service during the real payment analysis flow, before a payment decision is published to Payment Service.
- Requests include the paying customer's ID, payment timestamp, amount, device, recipient, and supplied latitude/longitude/city. Successful logins are also scored before completion; a high-risk login requires email OTP step-up verification.
- The ML service keeps an isolated SQLite history per customer. It does not use another customer's payment history to construct a baseline.
- Only final `APPROVED` decisions are sent to `/feedback` and added to the ML service's normal-behaviour history. Holds and blocked transactions are not learned as normal.
- Signals include personal amount deviation, payment velocity, a new device, a new recipient, first activity during 00:00–04:59 where that customer has no approved night activity, and impossible travel against the customer's last approved payment coordinates and timestamp.
- The impossible-travel calculation uses approximate straight-line distance (Haversine). It is a risk signal, not proof: GPS/IP/VPN locations may be inaccurate. The engine should request verification/review instead of assuming fraud from location alone.
- The existing rule engine and the ML score are combined. A large amount alone is not configured as an automatic block; context and multiple signals matter.
- SQLite data persists in the `mlbehaviour` Docker volume across container recreation.

## Important model limitation

The Random Forest is trained on synthetic teaching data. Personal-history features are evaluated at runtime and blended with its score, but this is **not a validated production fraud model**. There is no claim of real-world accuracy. Production use needs consented, representative, labelled outcomes; train/validation/test splits; calibration, fairness and drift checks; and analyst feedback/retraining governance.

## Run

From the folder containing `docker-compose.yml`:

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f ml-fraud-service fraud-service
```

ML health: `GET http://localhost:8091/health`. Login risk: `POST /score-login`. Payment risk: `POST /score`. Port 8091 is exposed only for local demonstration/Postman testing; do not expose this endpoint publicly in production.

## Postman scenarios

Import `NexaPay_ML_Behaviour_Tests.postman_collection.json`. The test calls run against the ML scorer directly to test its features; the end-to-end payment UI/API still runs the full rule + ML + payment decision flow.

1. Seed Customer A successful login history: `POST /feedback` with Hyderabad coordinates at 20:00.
2. Login travel: `POST /score-login` for Delhi coordinates five minutes later. Expect `stepUpRequired: true` and `impossibleTravel: true`.
3. Seed Customer A approved payment history: `POST /feedback` with Hyderabad coordinates at 20:00.
4. Impossible travel: `POST /score` for Customer A with Delhi coordinates five minutes later. Expect `impossibleTravel: true`, travel distance/elapsed time and a high-risk signal.
5. Customer isolation: send the same score for Customer B. Expect `personalHistoryEvents: 0` until Customer B has their own approved events.
6. Unusual hour: seed Customer A with only daytime approvals, then score a 02:00 payment; expect the unusual-hour signal.
7. New device + new recipient: score an event with a never-seen device and receiver; expect both signals.
8. Large legitimate payment: seed several approved payments and score a large payment on a known device/receiver in the usual location/time. The amount signal can increase risk but is not, by itself, an automatic block instruction.
9. Unapproved outcome: `/feedback` with `HOLD` or `BLOCKED` must not update the normal-behaviour baseline.

## API fields

`POST /score` accepts `customer_id`, `occurred_at` (ISO-8601), `amount`, `latitude`, `longitude`, `city`, `device_id`, `receiver`, plus the existing model inputs. `/feedback` stores only approved/confirmed-legitimate outcomes. `/test/behavior-analysis` is a convenience alias for `/score`.
