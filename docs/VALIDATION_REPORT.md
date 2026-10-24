# Validation report for this delivered ZIP

Checks performed in the packaging environment on 2026-10-19:

- Python ML service syntax compilation: PASS.
- ML API smoke tests with FastAPI TestClient: PASS for health, login risk/impossible travel, payment impossible travel, customer-history isolation, and exclusion of blocked outcomes from normal history.
- Docker Compose YAML parse: PASS; confirms nine runtime services, persistent `mlbehaviour` volume, and no Kafka/Ollama service.
- Maven POM XML parse: PASS.
- JSON syntax validation, including Postman collection: PASS.
- ZIP archive integrity check: PASS.

Limitations: Maven and Docker executables were not available in the packaging environment, so a complete Java Maven build and a real `docker compose up --build` integration test could not be run here. Run those commands in Docker Desktop before presenting/deploying the project. The bundled Random Forest is trained on synthetic teaching data and is not validated for real banking use.
