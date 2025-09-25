#!/bin/bash
set -e

echo "🚀 Deploying XML Sign Application to existing Kind cluster"
echo "========================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Check if cluster exists
if ! kind get clusters | grep -q xmlsign-cluster; then
    print_error "Kind cluster 'xmlsign-cluster' not found!"
    echo "Please create the cluster first or run the complete setup script."
    exit 1
fi

# Check if kubectl context is set
if ! kubectl cluster-info --context kind-xmlsign-cluster &>/dev/null; then
    print_error "Cannot connect to xmlsign-cluster. Please check your kubectl context."
    exit 1
fi

print_status "Connected to xmlsign-cluster"

# Prepare local directories (if not already done)
echo "📁 Preparing local directories..."
mkdir -p /tmp/temp/download
mkdir -p /tmp/certificates  
mkdir -p /tmp/docker/init

# Copy the SQL dump to the init directory
cp ./xmlsign_db_dump.sql /tmp/docker/init/

# Copy existing files if they exist
if [ -d "./temp/download" ]; then
    cp -r ./temp/download/* /tmp/temp/download/ 2>/dev/null || true
    print_status "Copied temp/download files"
fi

if [ -d "./src/main/resources/certificates" ]; then
    cp -r ./src/main/resources/certificates/* /tmp/certificates/ 2>/dev/null || true
    print_status "Copied certificate files"
fi

if [ -d "./docker/init" ]; then
    cp -r ./docker/init/* /tmp/docker/init/ 2>/dev/null || true
    print_status "Copied database init scripts"
fi

# Set permissions (ignore errors on Windows)
chmod -R 755 /tmp/temp 2>/dev/null || true
chmod -R 755 /tmp/certificates 2>/dev/null || true
chmod -R 755 /tmp/docker 2>/dev/null || true

print_status "Directory permissions set"

# Deploy to Kubernetes
print_status "Deploying application to Kubernetes..."

# Apply the large SQL ConfigMap separately
if [ -f "xmlsign-db-dump.yaml" ]; then
    kubectl apply -f xmlsign-db-dump.yaml
    print_status "Applied xmlsign-db-dump.yaml ConfigMap"
fi

if [ ! -f "k8s-manifests.yaml" ]; then
    print_error "k8s-manifests.yaml not found! Please make sure you have the Kubernetes manifest file."
    exit 1
fi

kubectl apply -f k8s-manifests.yaml
print_status "Kubernetes manifests applied"

echo "⏳ Waiting for deployments to be ready (this may take a few minutes)..."

# Function to check if a deployment is ready
check_deployment() {
    local deployment=$1
    local namespace=$2
    local timeout=$3
    
    echo "Waiting for $deployment to be ready..."
    if kubectl wait --for=condition=available --timeout=${timeout}s deployment/$deployment -n $namespace; then
        print_status "$deployment is ready"
        return 0
    else
        print_error "$deployment failed to become ready within ${timeout} seconds"
        echo "Checking pod status for $deployment:"
        kubectl get pods -n $namespace -l app=$deployment
        echo "Recent events:"
        kubectl get events -n $namespace --sort-by='.lastTimestamp' | tail -5
        return 1
    fi
}

# Wait for deployments with longer timeouts
check_deployment "postgres" "xmlsign" 300 || {
    print_warning "PostgreSQL not ready, but continuing..."
}

check_deployment "springboot-app" "xmlsign" 300 || {
    print_warning "Spring Boot app not ready, but continuing..."
}

check_deployment "nextjs-app" "xmlsign" 300 || {
    print_warning "Next.js app not ready, but continuing..."
}

check_deployment "cloudflared" "xmlsign" 180 || {
    print_warning "Cloudflared not ready, but continuing..."
}

# Show final status
echo ""
echo "🎉 Deployment process completed!"
echo "==============================="
echo ""

echo "📊 Current Pod Status:"
kubectl get pods -n xmlsign
echo ""

echo "🌐 Services:"
kubectl get services -n xmlsign
echo ""

echo "📋 Recent Events:"
kubectl get events -n xmlsign --sort-by='.lastTimestamp' | tail -10
echo ""

echo "🔗 Access your application:"
echo "1. Next.js Frontend (run this in a new terminal):"
echo "   kubectl port-forward -n xmlsign service/nextjs-service 3000:3000"
echo "   Then open: http://localhost:3000"
echo ""
echo "2. Spring Boot API (run this in a new terminal):"
echo "   kubectl port-forward -n xmlsign service/springboot-service 8080:8080"
echo "   Then open: http://localhost:8080"
echo ""
echo "🔍 Debugging commands:"
echo "   kubectl logs -f -n xmlsign deployment/postgres"
echo "   kubectl logs -f -n xmlsign deployment/springboot-app"
echo "   kubectl logs -f -n xmlsign deployment/nextjs-app"
echo "   kubectl logs -f -n xmlsign deployment/cloudflared"
echo ""
echo "🧹 Clean up when done:"
echo "   kind delete cluster --name xmlsign-cluster"
