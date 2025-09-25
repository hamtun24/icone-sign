#!/bin/bash
set -e

echo "🚀 Setting up XML Sign Application on Kind Kubernetes Cluster"
echo "============================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Check if required tools are installed
check_prerequisites() {
    echo "🔍 Checking prerequisites..."
    
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    print_status "Docker is installed"
    
    if ! command -v kind &> /dev/null; then
        print_error "Kind is not installed. Please install Kind first."
        echo "Install with: curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64 && chmod +x ./kind && sudo mv ./kind /usr/local/bin/kind"
        exit 1
    fi
    print_status "Kind is installed"
    
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed. Please install kubectl first."
        exit 1
    fi
    print_status "kubectl is installed"
}

# Create Kind cluster configuration
create_kind_config() {
    echo "📝 Creating Kind cluster configuration..."
    
    cat > kind-config.yaml << 'EOF'
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: xmlsign-cluster
nodes:
- role: control-plane
  kubeadmConfigPatches:
  - |
    kind: InitConfiguration
    nodeRegistration:
      kubeletExtraArgs:
        node-labels: "ingress-ready=true"
  extraPortMappings:
  - containerPort: 80
    hostPort: 80
    protocol: TCP
  - containerPort: 443
    hostPort: 443
    protocol: TCP
  - containerPort: 3000
    hostPort: 3000
    protocol: TCP
  - containerPort: 8080
    hostPort: 8080
    protocol: TCP
EOF
    
    print_status "Kind configuration created"
}

# Create or recreate Kind cluster
setup_kind_cluster() {
    echo "🔧 Setting up Kind cluster..."
    
    # Check if cluster exists
    if kind get clusters | grep -q xmlsign-cluster; then
        print_warning "Cluster 'xmlsign-cluster' already exists. Deleting it..."
        kind delete cluster --name xmlsign-cluster
    fi
    
    print_status "Creating new Kind cluster..."
    kind create cluster --config=kind-config.yaml

    # Wait for cluster to be ready
    echo "⏳ Waiting for cluster to be ready..."
    kubectl wait --for=condition=Ready nodes --all --timeout=300s
    print_status "Kind cluster is ready"

    # Copy SQL dump into Kind node (for Windows/Kind)
   # if [ -f "./docker/init/xmlsign_db_dump.sql" ]; then
   #     KIND_NODE=$(docker ps --filter name=xmlsign-cluster-control-plane -q)
    #    if [ -n "$KIND_NODE" ]; then
    #        docker exec $KIND_NODE mkdir -p /tmp/docker/init
    #        docker cp ./docker/init/xmlsign_db_dump.sql $KIND_NODE:/tmp/docker/init/xmlsign_db_dump.sql
    #        print_status "Copied xmlsign_db_dump.sql into Kind node container"
    #    else
    #        print_warning "Kind node container not found; could not copy SQL dump"
     #   fi
    #fi
}

# Check if Docker images exist
check_docker_images() {
    echo "🔍 Checking Docker images..."
    
    if ! docker image inspect iconesigndocker-springboot-app:v1.2.5 &> /dev/null; then
        print_error "Docker image 'iconesigndocker-springboot-app:v1.2.5' not found!"
        echo "Please build your Spring Boot image first."
        exit 1
    fi
    print_status "Spring Boot image found"
    
    if ! docker image inspect iconesigndocker-nextjs-app:v1.0 &> /dev/null; then
        print_error "Docker image 'iconesigndocker-nextjs-app:v1.0' not found!"
        echo "Please build your Next.js image first."
        exit 1
    fi
    print_status "Next.js image found"
}

# Load Docker images into Kind
load_images() {
    echo "📦 Loading Docker images into Kind cluster..."
    
    kind load docker-image iconesigndocker-springboot-app:v1.2.5 --name xmlsign-cluster
    print_status "Spring Boot image loaded"
    
    kind load docker-image iconesigndocker-nextjs-app:v1.0 --name xmlsign-cluster
    print_status "Next.js image loaded"
    
    
   
}

# Prepare local directories
prepare_directories() {
    echo "📁 Preparing local directories..."
    
    mkdir -p /tmp/temp/download
    mkdir -p /tmp/certificates  
    mkdir -p /tmp/docker/init

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

    # Ensure xmlsign_db_dump.sql is copied for Kind cluster Postgres init (host)
    if [ -f "./docker/init/xmlsign_db_dump.sql" ]; then
        cp ./docker/init/xmlsign_db_dump.sql /tmp/docker/init/
        print_status "Copied xmlsign_db_dump.sql to /tmp/docker/init/ (host)"
    else
        print_warning "xmlsign_db_dump.sql not found in ./docker/init/"
    fi

    # Set permissions (ignore errors on Windows)
    chmod -R 755 /tmp/temp 2>/dev/null || true
    chmod -R 755 /tmp/certificates 2>/dev/null || true
    chmod -R 755 /tmp/docker 2>/dev/null || true

    print_status "Directory permissions set"
}

# Deploy to Kubernetes
deploy_application() {
    echo "🚀 Deploying application to Kubernetes..."
    
    if [ ! -f "k8s-manifests.yaml" ]; then
        print_error "k8s-manifests.yaml not found! Please make sure you have the Kubernetes manifest file."
        exit 1
    fi
    
    kubectl apply -f k8s-manifests.yaml
    print_status "Kubernetes manifests applied"
    
    echo "⏳ Waiting for deployments to be ready..."
    
    # Wait for PostgreSQL first
    kubectl wait --for=condition=available --timeout=300s deployment/postgres -n xmlsign
    print_status "PostgreSQL is ready"
    
    # Wait for Spring Boot
    kubectl wait --for=condition=available --timeout=300s deployment/springboot-app -n xmlsign  
    print_status "Spring Boot app is ready"
    
    # Wait for Next.js
    kubectl wait --for=condition=available --timeout=300s deployment/nextjs-app -n xmlsign
    print_status "Next.js app is ready"
    
    # Wait for Cloudflared
    kubectl wait --for=condition=available --timeout=300s deployment/cloudflared -n xmlsign
    print_status "Cloudflared is ready"
}

# Show status and access information
show_status() {
    echo ""
    echo "🎉 Deployment completed successfully!"
    echo "===================================="
    echo ""
    
    echo "📊 Pod Status:"
    kubectl get pods -n xmlsign
    echo ""
    
    echo "🌐 Services:"
    kubectl get services -n xmlsign
    echo ""
    
    echo "🔗 Access your application:"
    echo "1. Next.js Frontend:"
    echo "   kubectl port-forward -n xmlsign service/nextjs-service 3000:3000"
    echo "   Then open: http://localhost:3000"
    echo ""
    echo "2. Spring Boot API:"
    echo "   kubectl port-forward -n xmlsign service/springboot-service 8080:8080"
    echo "   Then open: http://localhost:8080"
    echo ""
    echo "3. View logs:"
    echo "   kubectl logs -f -n xmlsign deployment/nextjs-app"
    echo "   kubectl logs -f -n xmlsign deployment/springboot-app"
    echo "   kubectl logs -f -n xmlsign deployment/postgres"
    echo "   kubectl logs -f -n xmlsign deployment/cloudflared"
    echo ""
    echo "4. Clean up when done:"
    echo "   kind delete cluster --name xmlsign-cluster"
    echo ""
    
    # Check if Cloudflare tunnel is working
    echo "🌍 Cloudflare Tunnel Status:"
    kubectl logs -n xmlsign deployment/cloudflared --tail=5 | grep -i "connection" || echo "Check tunnel logs for connection status"
}

# Main execution
main() {
    echo "Starting setup process..."
    echo ""
    
    check_prerequisites
    create_kind_config
    setup_kind_cluster
    check_docker_images
    load_images
    prepare_directories
    deploy_application
    show_status
    
    echo ""
    print_status "Setup completed! Your XML Sign application is now running on Kind Kubernetes cluster."
}

# Run main function
main "$@"