#!/bin/bash
set -e

echo "🔧 Fixing Docker image loading issues..."

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

# Step 1: Pull fresh images to ensure they're complete
echo "📦 Pulling fresh Docker images..."
docker pull postgres:15
print_status "PostgreSQL image pulled"

docker pull cloudflare/cloudflared:latest
print_status "Cloudflared image pulled"

# Step 2: Load images one by one with error handling
load_image_with_retry() {
    local image=$1
    local max_retries=3
    local retry=0
    
    while [ $retry -lt $max_retries ]; do
        echo "Loading $image (attempt $((retry + 1))/$max_retries)..."
        if kind load docker-image "$image" --name xmlsign-cluster; then
            print_status "$image loaded successfully"
            return 0
        else
            print_warning "Failed to load $image, retrying..."
            retry=$((retry + 1))
            sleep 2
        fi
    done
    
    print_error "Failed to load $image after $max_retries attempts"
    return 1
}

echo "📦 Loading images into Kind cluster with retry logic..."

# Load your custom images first (they should work since they were found)
load_image_with_retry "iconesigndocker-springboot-app:v1.0.1"
load_image_with_retry "iconesigndocker-nextjs-app"



print_status "All images loaded successfully!"

# Step 3: Verify images are in the cluster
echo "🔍 Verifying images in Kind cluster..."
docker exec xmlsign-cluster-control-plane crictl images | grep -E "(postgres|cloudflare|iconesigndocker)" || {
    print_warning "Some images might not be visible via crictl, but they should still work"
}

print_status "Image loading completed!"
echo ""
echo "Now you can proceed with the deployment:"
echo "kubectl apply -f k8s-manifests.yaml"