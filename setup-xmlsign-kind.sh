#!/bin/bash

# Bash script for setting up XML Sign Application on Kind Kubernetes Cluster
# Run with: ./setup-xmlsign-kind.sh

set -e

echo "Setting up XML Sign Application on Kind Kubernetes Cluster"
echo "============================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}Success: $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}Warning: $1${NC}"
}

print_error() {
    echo -e "${RED}Error: $1${NC}"
}

print_info() {
    echo -e "${CYAN}$1${NC}"
}

# Check if required tools are installed
check_prerequisites() {
    print_info "Checking prerequisites..."
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        echo "Install from: https://docs.docker.com/get-docker/"
        exit 1
    fi
    
    if ! docker ps &> /dev/null; then
        print_error "Docker is not running. Please start Docker service."
        exit 1
    fi
    print_status "Docker is installed and running"
    
    # Check Kind
    if ! command -v kind &> /dev/null; then
        print_error "Kind is not installed. Please install Kind first."
        echo "Install with:"
        echo "  # For Linux"
        echo "  curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64"
        echo "  chmod +x ./kind"
        echo "  sudo mv ./kind /usr/local/bin/kind"
        echo ""
        echo "  # For macOS"
        echo "  brew install kind"
        exit 1
    fi
    print_status "Kind is installed"
    
    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed. Please install kubectl first."
        echo "Install with:"
        echo "  # For Linux"
        echo "  curl -LO \"https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl\""
        echo "  chmod +x kubectl"
        echo "  sudo mv kubectl /usr/local/bin/"
        echo ""
        echo "  # For macOS"
        echo "  brew install kubectl"
        exit 1
    fi
    print_status "kubectl is installed"
}

# Build Docker images
build_docker_images() {
    print_info "Building Docker images..."
    
    # Build Spring Boot image
    print_info "Building Spring Boot image..."
    cd springboot
    
    # Find the JAR file automatically
    JAR_FILE=$(find target -name "*.jar" -not -name "*.original" | head -n 1)
    if [ -z "$JAR_FILE" ]; then
        print_error "Spring Boot JAR not found. Please build it first with: mvn clean package -DskipTests"
        exit 1
    fi
    print_info "Found JAR file: $JAR_FILE"
    
    # Always create a consistent Dockerfile for the build
    print_info "Creating optimized Dockerfile for Spring Boot..."
    cat > Dockerfile.build << EOF
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY $JAR_FILE app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \\
  CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF
    
    docker build -f Dockerfile.build -t iconesigndocker-springboot-app:v1.0.1 .
    print_status "Spring Boot image built"
    
    cd ..
    
    # Build Next.js image
    print_info "Building Next.js image..."
    cd nextjs
    docker build -t iconesigndocker-nextjs-app .
    print_status "Next.js image built"
    
    cd ..
}

# Create or recreate Kind cluster
setup_kind_cluster() {
    print_info "Setting up Kind cluster..."
    
    # Check if cluster exists
    if kind get clusters 2>/dev/null | grep -q "xmlsign-cluster"; then
        print_warning "Cluster xmlsign-cluster already exists. Deleting it..."
        kind delete cluster --name xmlsign-cluster
    fi
    
    print_status "Creating new Kind cluster..."
    kind create cluster --config=kind-config.yaml
    
    # Wait for cluster to be ready
    print_info "Waiting for cluster to be ready..."
    kubectl wait --for=condition=Ready nodes --all --timeout=300s
    print_status "Kind cluster is ready"
}

# Load Docker images into Kind
load_docker_images() {
    print_info "Loading Docker images into Kind cluster..."
    
    kind load docker-image iconesigndocker-springboot-app:v1.0.1 --name xmlsign-cluster
    print_status "Spring Boot image loaded"
    
    kind load docker-image iconesigndocker-nextjs-app --name xmlsign-cluster
    print_status "Next.js image loaded"
}

# Create secrets
create_secrets() {
    print_info "Creating Kubernetes secrets..."
    
    # Prompt for database password
    read -s -p "Enter PostgreSQL password (press Enter for default 'postgres123'): " POSTGRES_PASSWORD
    echo
    if [ -z "$POSTGRES_PASSWORD" ]; then
        POSTGRES_PASSWORD="postgres123"
    fi
    
    # Prompt for Cloudflare tunnel token
    echo "Please provide your Cloudflare tunnel token."
    echo "If you don't have one, create it with:"
    echo "  cloudflared tunnel create xmlsign-frontend"
    echo "  cloudflared tunnel token xmlsign-frontend"
    echo ""
    read -s -p "Enter your Cloudflare tunnel token: " TUNNEL_TOKEN
    echo
    
    if [ -z "$TUNNEL_TOKEN" ]; then
        print_error "Tunnel token is required. Please create a Cloudflare tunnel first."
        exit 1
    fi
    
    # Create the secret
    kubectl create secret generic app-secrets \
        --from-literal=POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
        --from-literal=TUNNEL_TOKEN="$TUNNEL_TOKEN" \
        -n xmlsign --dry-run=client -o yaml | kubectl apply -f -
    
    print_status "Secrets created"
}

# Deploy to Kubernetes
deploy_application() {
    print_info "Deploying application to Kubernetes..."
    
    if [ ! -f "k8s-manifests-frontend-only.yaml" ]; then
        print_error "k8s-manifests-frontend-only.yaml not found! Please make sure you have the Kubernetes manifest file."
        exit 1
    fi
    
    # Create namespace first
    kubectl create namespace xmlsign --dry-run=client -o yaml | kubectl apply -f -
    print_status "Namespace created"
    
    # Create secrets before other resources
    create_secrets
    
    # Apply all other manifests
    kubectl apply -f k8s-manifests-frontend-only.yaml
    print_status "Kubernetes manifests applied"
    
    print_info "Waiting for deployments to be ready..."
    
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
    print_status "Deployment completed successfully!"
    echo "================================="
    echo ""
    
    print_info "Pod Status:"
    kubectl get pods -n xmlsign
    echo ""
    
    print_info "Services:"
    kubectl get services -n xmlsign
    echo ""
    
    print_info "Access your application:"
    echo -e "${YELLOW}1. Next.js Frontend (Local Access):${NC}"
    echo "   kubectl port-forward -n xmlsign service/nextjs-service 3000:3000"
    echo "   Then open: http://localhost:3000"
    echo ""
    echo -e "${YELLOW}2. Spring Boot API (Internal Only - for debugging):${NC}"
    echo "   kubectl port-forward -n xmlsign service/springboot-service 8080:8080"
    echo "   Then open: http://localhost:8080"
    echo ""
    echo -e "${YELLOW}3. View logs:${NC}"
    echo "   kubectl logs -f -n xmlsign deployment/nextjs-app"
    echo "   kubectl logs -f -n xmlsign deployment/springboot-app"
    echo "   kubectl logs -f -n xmlsign deployment/postgres"
    echo "   kubectl logs -f -n xmlsign deployment/cloudflared"
    echo ""
    echo -e "${YELLOW}4. Clean up when done:${NC}"
    echo "   kind delete cluster --name xmlsign-cluster"
    echo ""
    
    # Check Cloudflare tunnel status
    print_info "Cloudflare Tunnel Status:"
    echo "Note: Make sure you have set your TUNNEL_TOKEN in the secret before deploying"
    tunnel_logs=$(kubectl logs -n xmlsign deployment/cloudflared --tail=10 2>/dev/null || echo "Could not retrieve tunnel logs")
    echo "$tunnel_logs"
    echo ""
    print_info "Your frontend should be accessible via your Cloudflare tunnel URL!"
}

# Main execution
main() {
    print_info "Starting setup process..."
    echo ""
    
    check_prerequisites
    build_docker_images
    setup_kind_cluster
    load_docker_images
    deploy_application
    show_status
    
    echo ""
    print_status "Setup completed! Your XML Sign application is now running on Kind Kubernetes cluster."
    print_info "Frontend is exposed via Cloudflare tunnel, backend is internal only."
}

# Run main function
main "$@"