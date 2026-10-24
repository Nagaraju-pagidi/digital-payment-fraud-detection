# Project extension plan

The core NexaPay + FraudShield demo includes customer login, payment workflows, a fraud analyst console, deterministic rules, and customer-specific behavioural ML signals.

Recommended intern extensions:
1. Replace synthetic training labels with a properly consented, representative, labelled dataset.
2. Build a reproducible training pipeline with train/validation/test separation, metrics, calibration, and drift monitoring.
3. Add analyst feedback labels such as confirmed fraud, confirmed legitimate, and unresolved; do not treat every hold/block as confirmed fraud.
4. Add privacy controls, retention policies, access auditing, and authenticated internal ML endpoints.
5. Add automated tests for travel thresholds, location uncertainty, time-zone boundaries, customer isolation, and amount-only false positives.
6. Add an explicit model version and decision explanation to every stored fraud case.

The demo currently uses internal HTTP and a transactional outbox; Kafka and Ollama are intentionally excluded to keep local setup small.
