# Local Deployment Guide - Kind + Cloudflare Tunnel

This guide will help you deploy the XML Sign application locally using Kind (Kubernetes in Docker) with only the frontend exposed through Cloudflare tunnel.

## Prerequisites

1. **Docker** - Install from https://docs.docker.com/get-docker/
2. **Kind** - Kubernetes in Docker
3. **kubectl** - Kubernetes CLI
4. **Cloudflare account** with a tunnel configured

### Installing Kind and kubectl

#### Linux
```bash
# Install Kind
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/kind

# Install kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/
```

#### macOS
```bash
# Install Kind and kubectl using Homebrew
brew install kind kubectl
```

## Setting up Cloudflare Tunnel

1. **Create a Cloudflare Tunnel** (if you haven't already):
   ```bash
   # Install cloudflared
   # For Linux:
   wget -q https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb
   sudo dpkg -i cloudflared-linux-amd64.deb
   
   # For macOS:
   brew install cloudflared
   
   # Login to Cloudflare
   cloudflared tunnel login
   
   # Create a tunnel
   cloudflared tunnel create xmlsign-frontend
   
   # Get your tunnel token
   cloudflared tunnel token xmlsign-frontend
   ```

2. **Configure your tunnel**:
   - Copy your tunnel token from the cloudflared command
   - **Important**: In the Cloudflare dashboard, configure your tunnel's Public Hostname to point to `http://nextjs-service:3000`
   - Keep the token ready as the setup script will prompt for it

## Deployment Steps

1. **Clone and prepare the project**:
   ```bash
   # Make sure you're in the project root directory
   cd /path/to/your/xmlsign-project
   ```

2. **Build the Spring Boot application**:
   ```bash
   cd springboot
   mvn clean package -DskipTests
   cd ..
   ```

3. **Run the setup script**:
   ```bash
   ./setup-xmlsign-kind.sh
   ```
   
   The script will prompt you for:
   - PostgreSQL password (or press Enter for default)
   - Your Cloudflare tunnel token

## What the deployment includes

### Architecture Overview
- **Frontend (Next.js)**: Exposed via Cloudflare tunnel only
- **Backend (Spring Boot)**: Internal cluster access only
- **Database (PostgreSQL)**: Internal cluster access only
- **Security**: Only frontend is publicly accessible

### Services Deployed
- PostgreSQL database (internal)
- Spring Boot API (internal, accessible only to frontend)
- Next.js frontend (exposed via Cloudflare tunnel)
- Cloudflared tunnel proxy

## Accessing Your Application

### Frontend Access
- **Public Access**: Through your Cloudflare tunnel URL
- **Local Access** (for debugging): `kubectl port-forward -n xmlsign service/nextjs-service 3000:3000`

### Backend Access (Debugging Only)
- **Internal Only**: `kubectl port-forward -n xmlsign service/springboot-service 8080:8080`

### Database Access (Debugging Only)
- **Internal Only**: `kubectl port-forward -n xmlsign service/postgres-service 5432:5432`

## Monitoring and Logs

### View Pod Status
```bash
kubectl get pods -n xmlsign
```

### View Logs
```bash
# Frontend logs
kubectl logs -f -n xmlsign deployment/nextjs-app

# Backend logs
kubectl logs -f -n xmlsign deployment/springboot-app

# Database logs
kubectl logs -f -n xmlsign deployment/postgres

# Cloudflare tunnel logs
kubectl logs -f -n xmlsign deployment/cloudflared
```

### Check Tunnel Connection
```bash
kubectl logs -n xmlsign deployment/cloudflared --tail=20
```

## Troubleshooting

### Common Issues

1. **Images not found**:
   ```bash
   # Rebuild images
   cd springboot && docker build -t iconesigndocker-springboot-app:v1.0.1 .
   cd ../nextjs && docker build -t iconesigndocker-nextjs-app .
   
   # Reload into Kind
   kind load docker-image iconesigndocker-springboot-app:v1.0.1 --name xmlsign-cluster
   kind load docker-image iconesigndocker-nextjs-app --name xmlsign-cluster
   ```

2. **Tunnel not connecting**:
   - Verify your tunnel token is correct and base64 encoded
   - Check cloudflared logs: `kubectl logs -n xmlsign deployment/cloudflared`
   - Ensure your Cloudflare tunnel is active in the dashboard

3. **Backend connection issues**:
   - Check if Spring Boot is connecting to PostgreSQL
   - Verify environment variables: `kubectl describe pod -n xmlsign <springboot-pod-name>`

### Scaling

To scale components:
```bash
# Scale frontend
kubectl scale deployment nextjs-app --replicas=3 -n xmlsign

# Scale backend
kubectl scale deployment springboot-app --replicas=3 -n xmlsign
```

## Cleanup

To remove everything:
```bash
# Delete the Kind cluster
kind delete cluster --name xmlsign-cluster

# Remove Docker images (optional)
docker rmi iconesigndocker-springboot-app:v1.0.1
docker rmi iconesigndocker-nextjs-app
```

## Security Notes

- Only the frontend is exposed publicly through Cloudflare tunnel
- Backend API and database are accessible only within the Kubernetes cluster
- All backend API calls from the frontend are proxied through Next.js
- PostgreSQL is not exposed externally
- Secrets are managed through Kubernetes secrets

## Configuration Files

- `kind-config.yaml` - Kind cluster configuration
- `k8s-manifests-frontend-only.yaml` - Kubernetes resources with frontend-only exposure
- `setup-xmlsign-kind.sh` - Automated setup script

Your application is now running with enterprise-grade security, exposing only the necessary frontend interface while keeping all backend services protected within the cluster.