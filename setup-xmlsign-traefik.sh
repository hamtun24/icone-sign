#!/bin/bash

# Bash script for setting up XML Sign Application with Traefik Ingress Controller
# Run with: ./setup-xmlsign-traefik.sh

set -e

echo "Setting up XML Sign Application with Traefik Ingress Controller"
echo "=============================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ $1${NC}"
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

    # Check htpasswd for dashboard auth
    if ! command -v htpasswd &> /dev/null; then
        print_warning "htpasswd is not installed. Traefik dashboard auth will be skipped."
        print_info "Install with: sudo apt-get install apache2-utils (Ubuntu/Debian) or brew install httpd (macOS)"
    fi
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
RUN apk add --no-cache curl
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \\
  CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF
    
    docker build -f Dockerfile.build -t iconesigndocker-springboot-app:v1.2.5 .
    print_status "Spring Boot image built"
    
    cd ..
    
    # Build Next.js image
    print_info "Building Next.js image..."
    cd nextjs
    docker build -t iconesigndocker-nextjs-app:v1.1 .
    print_status "Next.js image built"
    
    cd ..
}

# Create or recreate Kind cluster with additional port mappings
setup_kind_cluster() {
    print_info "Setting up Kind cluster with Traefik support..."
    
    # Check if cluster exists
    if kind get clusters 2>/dev/null | grep -q "xmlsign-cluster"; then
        print_warning "Cluster xmlsign-cluster already exists. Deleting it..."
        kind delete cluster --name xmlsign-cluster
    fi
    
    # Create updated kind config for Traefik
    print_info "Creating Kind cluster configuration..."
    cat > kind-traefik-config.yaml << EOF
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
  - containerPort: 8080
    hostPort: 8080
    protocol: TCP
EOF
    
    print_status "Creating new Kind cluster with Traefik configuration..."
    kind create cluster --name xmlsign-cluster --config=kind-traefik-config.yaml
    
    # Wait for cluster to be ready
    print_info "Waiting for cluster to be ready..."
    kubectl wait --for=condition=Ready nodes --all --timeout=300s
    print_status "Kind cluster is ready"
    
    # Install local storage provisioner for PVC support
    print_info "Installing local storage provisioner..."
    kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/v0.0.26/deploy/local-path-storage.yaml
    kubectl patch storageclass local-path -p '{"metadata": {"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'
    print_status "Storage provisioner installed"
}

# Load Docker images into Kind
load_docker_images() {
    print_info "Loading Docker images into Kind cluster..."
    
    kind load docker-image iconesigndocker-springboot-app:v1.2.5 --name xmlsign-cluster
    print_status "Spring Boot image loaded"
    
    kind load docker-image iconesigndocker-nextjs-app:v1.1 --name xmlsign-cluster
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
    
    # Generate secure random values for application secrets
    JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
    ANCE_SEAL_PIN=$(openssl rand -base64 32 | tr -d '\n')
    
    # Create the app secret with all required values
    kubectl create secret generic app-secrets \
        --from-literal=POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
        --from-literal=JWT_SECRET="$JWT_SECRET" \
        --from-literal=ANCE_SEAL_PIN="$ANCE_SEAL_PIN" \
        -n xmlsign --dry-run=client -o yaml | kubectl apply -f -
    
    print_status "Application secrets created"
}

# Create Cloudflare secrets
create_cloudflare_secrets() {
    print_info "Setting up Cloudflare integration..."
    
    echo ""
    echo "Cloudflare API Authentication Options:"
    echo "1. API Token (Recommended) - More secure, scoped permissions"
    echo "2. Global API Key - Full account access"
    echo ""
    read -p "Choose authentication method (1 for API Token, 2 for Global API Key): " auth_method
    
    if [ "$auth_method" = "1" ]; then
        echo ""
        print_info "Using Cloudflare API Token..."
        echo "To create an API Token:"
        echo "1. Go to https://dash.cloudflare.com/profile/api-tokens"
        echo "2. Click 'Create Token'"
        echo "3. Use 'Custom token' template"
        echo "4. Permissions: Zone:DNS:Edit, Zone:Zone:Read"
        echo "5. Zone Resources: Include - All zones (or specific zone)"
        echo ""
        read -s -p "Enter your Cloudflare API Token: " CF_API_TOKEN
        echo
        
        if [ -z "$CF_API_TOKEN" ]; then
            print_error "API Token is required for DNS challenge"
            exit 1
        fi
        
        kubectl create secret generic cloudflare-secrets \
            --from-literal=CF_DNS_API_TOKEN="$CF_API_TOKEN" \
            -n xmlsign --dry-run=client -o yaml | kubectl apply -f -
            
    else
        echo ""
        print_info "Using Cloudflare Global API Key..."
        echo "Find your Global API Key at: https://dash.cloudflare.com/profile/api-tokens"
        echo ""
        read -p "Enter your Cloudflare email: " CF_EMAIL
        read -s -p "Enter your Cloudflare Global API Key: " CF_API_KEY
        echo
        
        if [ -z "$CF_EMAIL" ] || [ -z "$CF_API_KEY" ]; then
            print_error "Both email and API key are required"
            exit 1
        fi
        
        kubectl create secret generic cloudflare-secrets \
            --from-literal=CF_API_EMAIL="$CF_EMAIL" \
            --from-literal=CF_API_KEY="$CF_API_KEY" \
            -n xmlsign --dry-run=client -o yaml | kubectl apply -f -
    fi
    
    print_status "Cloudflare secrets created"
}

# Create Traefik dashboard authentication
create_dashboard_auth() {
    if command -v htpasswd &> /dev/null; then
        print_info "Creating Traefik dashboard authentication..."
        
        read -p "Enter username for Traefik dashboard (default: admin): " DASHBOARD_USER
        if [ -z "$DASHBOARD_USER" ]; then
            DASHBOARD_USER="admin"
        fi
        
        read -s -p "Enter password for Traefik dashboard: " DASHBOARD_PASS
        echo
        
        if [ -z "$DASHBOARD_PASS" ]; then
            print_error "Dashboard password is required"
            exit 1
        fi
        
        # Create htpasswd entry
        AUTH_STRING=$(htpasswd -nb "$DASHBOARD_USER" "$DASHBOARD_PASS")
        
        kubectl create secret generic traefik-dashboard-auth \
            --from-literal=users="$AUTH_STRING" \
            -n xmlsign --dry-run=client -o yaml | kubectl apply -f -
            
        print_status "Traefik dashboard authentication created"
    else
        print_warning "Skipping Traefik dashboard auth (htpasswd not available)"
    fi
}

# Update configuration files with user's domain
update_domain_configs() {
    print_info "Configuring domain settings..."
    
    read -p "Enter your domain name (e.g., example.com): " DOMAIN_NAME
    read -p "Enter your email for Let's Encrypt notifications: " LETSENCRYPT_EMAIL
    
    if [ -z "$DOMAIN_NAME" ]; then
        print_error "Domain name is required"
        exit 1
    fi
    
    if [ -z "$LETSENCRYPT_EMAIL" ]; then
        print_error "Email is required for Let's Encrypt"
        exit 1
    fi
    
    # Update traefik config with email
    sed -i.bak "s/your-email@example.com/$LETSENCRYPT_EMAIL/g" traefik-ingress.yaml
    
    # Update ingress with domain
    sed -i.bak "s/yourdomain.com/$DOMAIN_NAME/g" traefik-ingress.yaml
    sed -i.bak "s/traefik.yourdomain.com/traefik.$DOMAIN_NAME/g" traefik-ingress.yaml
    
    print_status "Domain configuration updated"
    
    # Store domain info for later use
    export DOMAIN_NAME
    export LETSENCRYPT_EMAIL
}

# Deploy Traefik
deploy_traefik() {
    print_info "Deploying Traefik ingress controller..."
    
    # Install Traefik CRDs first
    print_info "Installing Traefik CRDs..."
    kubectl apply -f https://raw.githubusercontent.com/traefik/traefik/v3.0/docs/content/reference/dynamic-configuration/kubernetes-crd-definition-v1.yml
    
    # Wait for CRDs to be established
    print_info "Waiting for CRDs to be ready..."
    kubectl wait --for condition=established --timeout=60s crd/middlewares.traefik.containo.us || true
    print_status "Traefik CRDs installed"
    
    if [ ! -f "traefik-ingress.yaml" ]; then
        print_error "traefik-ingress.yaml not found! Please make sure you have the Traefik manifest file."
        exit 1
    fi
    
    kubectl apply -f traefik-ingress.yaml
    print_status "Traefik manifests applied"
    
    print_info "Waiting for Traefik to be ready..."
    kubectl wait --for=condition=available --timeout=300s deployment/traefik -n xmlsign
    print_status "Traefik is ready"
}

# Deploy application
deploy_application() {
    print_info "Deploying application to Kubernetes..."
    
    if [ ! -f "k8s-manifests-traefik.yaml" ]; then
        print_error "k8s-manifests-traefik.yaml not found! Please make sure you have the Kubernetes manifest file."
        exit 1
    fi
    
    # Create namespace first
    kubectl create namespace xmlsign --dry-run=client -o yaml | kubectl apply -f -
    print_status "Namespace created"
    
    # Create all secrets
    create_secrets
    create_cloudflare_secrets
    create_dashboard_auth
    
    # Deploy Traefik first
    deploy_traefik
    
    # Apply application manifests
    kubectl apply -f k8s-manifests-traefik.yaml
    print_status "Application manifests applied"
    
    print_info "Waiting for deployments to be ready..."
    
    # Wait for PostgreSQL first
    kubectl wait --for=condition=available --timeout=300s deployment/postgres -n xmlsign
    print_status "PostgreSQL is ready"
    
    # Wait for Spring Boot
    kubectl wait --for=condition=available --timeout=500s deployment/springboot-app -n xmlsign
    print_status "Spring Boot app is ready"
    
    # Wait for Next.js
    kubectl wait --for=condition=available --timeout=300s deployment/nextjs-app -n xmlsign
    print_status "Next.js app is ready"
}

# Show DNS configuration instructions
show_dns_instructions() {
    print_info "DNS Configuration Required:"
    echo "=========================================="
    echo ""
    echo "To complete the setup, you need to configure DNS in Cloudflare:"
    echo ""
    echo "1. Log in to your Cloudflare dashboard"
    echo "2. Select your domain: $DOMAIN_NAME"
    echo "3. Go to DNS > Records"
    echo "4. Add the following A records pointing to your server's public IP:"
    echo ""
    
    # Try to get the public IP
    PUBLIC_IP=$(curl -s -4 ifconfig.me 2>/dev/null || curl -s -4 icanhazip.com 2>/dev/null || echo "YOUR_PUBLIC_IP")
    
    echo "   Type: A, Name: @, Content: $PUBLIC_IP, TTL: Auto"
    echo "   Type: A, Name: www, Content: $PUBLIC_IP, TTL: Auto"
    echo "   Type: A, Name: traefik, Content: $PUBLIC_IP, TTL: Auto"
    echo ""
    echo "5. Make sure the records are proxied (orange cloud icon) for security"
    echo ""
    print_warning "Replace $PUBLIC_IP with your actual public IP address if different"
    echo ""
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
    
    print_info "Ingress:"
    kubectl get ingress -n xmlsign
    echo ""
    
    print_info "Access your application:"
    echo "=========================================="
    echo ""
    echo -e "${YELLOW}1. Main Application:${NC}"
    echo "   https://$DOMAIN_NAME"
    echo "   https://www.$DOMAIN_NAME"
    echo ""
    echo -e "${YELLOW}2. Traefik Dashboard (if auth configured):${NC}"
    echo "   https://traefik.$DOMAIN_NAME"
    echo ""
    echo -e "${YELLOW}3. Local Access (for testing):${NC}"
    echo "   Frontend: kubectl port-forward -n xmlsign service/nextjs-service 3000:3000"
    echo "   Backend:  kubectl port-forward -n xmlsign service/springboot-service 8080:8080"
    echo "   Traefik:  kubectl port-forward -n xmlsign service/traefik 8080:8080"
    echo ""
    echo -e "${YELLOW}4. View logs:${NC}"
    echo "   kubectl logs -f -n xmlsign deployment/nextjs-app"
    echo "   kubectl logs -f -n xmlsign deployment/springboot-app"
    echo "   kubectl logs -f -n xmlsign deployment/postgres"
    echo "   kubectl logs -f -n xmlsign deployment/traefik"
    echo ""
    echo -e "${YELLOW}5. Monitor certificates:${NC}"
    echo "   kubectl get certificates -n xmlsign"
    echo "   kubectl describe certificate xmlsign-app-tls -n xmlsign"
    echo ""
    echo -e "${YELLOW}6. Clean up when done:${NC}"
    echo "   kind delete cluster --name xmlsign-cluster"
    echo ""
    
    show_dns_instructions
    
    print_info "TLS Certificate Status:"
    echo "Certificates will be automatically requested from Let's Encrypt via Cloudflare DNS challenge"
    echo "This may take a few minutes after DNS propagation"
    echo ""
    
    # Show certificate status if available
    if kubectl get certificates -n xmlsign &>/dev/null; then
        kubectl get certificates -n xmlsign
    fi
    
    echo ""
    print_status "Setup completed! Your XML Sign application is now running with Traefik ingress controller."
    print_info "Frontend and backend are exposed via HTTPS with automatic TLS certificates from Let's Encrypt."
    echo ""
    print_warning "Make sure to configure DNS records in Cloudflare as shown above!"
}

# Troubleshooting function
show_troubleshooting() {
    echo ""
    print_info "Troubleshooting Commands:"
    echo "=========================="
    echo ""
    echo "Check Traefik logs:"
    echo "  kubectl logs -n xmlsign deployment/traefik"
    echo ""
    echo "Check certificate requests:"
    echo "  kubectl get certificaterequests -n xmlsign"
    echo "  kubectl describe certificaterequest <name> -n xmlsign"
    echo ""
    echo "Check Traefik configuration:"
    echo "  kubectl exec -n xmlsign deployment/traefik -- traefik version"
    echo ""
    echo "Test DNS resolution:"
    echo "  nslookup $DOMAIN_NAME"
    echo "  dig $DOMAIN_NAME"
    echo ""
    echo "Force certificate renewal:"
    echo "  kubectl delete certificate xmlsign-app-tls -n xmlsign"
    echo "  # Certificate will be automatically recreated"
    echo ""
}

# Main execution
main() {
    print_info "Starting setup process with Traefik ingress controller..."
    echo ""
    
    check_prerequisites
    build_docker_images
    setup_kind_cluster
    load_docker_images
    update_domain_configs
    deploy_application
    show_status
    show_troubleshooting
    
    echo ""
    print_status "Setup completed! Your XML Sign application is now running with Traefik + Cloudflare integration."
}

# Run main function
main "$@"