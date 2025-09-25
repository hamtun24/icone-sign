# IconeSign API

A comprehensive Spring Boot API for TTN E-Facturation and ANCE SEAL XML digital signature services.

## Features
- TTN E-Facturation: Save, consult, and retrieve invoices
- ANCE SEAL: XML signing, validation, batch processing
- Unified dashboard, operation logging, error handling, health monitoring

## Prerequisites
- Java 17+, PostgreSQL 15+, Maven 3.8+
- Valid TTN and ANCE SEAL credentials

## Local Development
1. **Database Setup**
   - Create PostgreSQL DB and user (see original instructions)
2. **Configuration**
   - Edit `src/main/resources/application.properties` for DB, TTN, ANCE SEAL
3. **Build & Run**
   ```bash
   mvn clean package
   java -jar target/unified-operations-api-*.jar
   ```

## Docker & Kubernetes
- See project root `docker-compose.yml` for local multi-container setup
- See `UnifiedOperationsApi/Dockerfile` for backend containerization
- See `k8s-backend.yaml` for Kubernetes deployment (internal only)
- Secrets/configs: `k8s-secrets.yaml`, `k8s-configmap.yaml`

## CI/CD & Cloudflare Tunnel
- Automated with `Jenkinsfile` (build, deploy, kubectl apply)
- Cloudflare Tunnel runs as a container (`k8s-cloudflare.yaml`) for secure frontend exposure

## API Endpoints & Usage
- See original README for endpoint details and curl examples

## Monitoring & Logging
- Console and file logging, DB audit trail
- Health endpoints: `/api/v1/health`, `/api/v1/ttn/health`, `/api/v1/ance-seal/health`

## Security
- Only frontend is exposed to the internet
- API and DB are internal (Docker/K8s network only)

---
For fullstack deployment, see the global README in the project root.
