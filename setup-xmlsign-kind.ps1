# PowerShell script for setting up XML Sign Application on Kind Kubernetes Cluster
# Run with: .\setup-xmlsign-kind.ps1

param(
    [switch]$Force
)

# Set error action preference
$ErrorActionPreference = "Stop"

Write-Host "Setting up XML Sign Application on Kind Kubernetes Cluster" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# Function to print colored output
function Write-Status {
    param([string]$Message)
    Write-Host "Success: $Message" -ForegroundColor Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "Warning: $Message" -ForegroundColor Yellow
}

function Write-ErrorMsg {
    param([string]$Message)
    Write-Host "Error: $Message" -ForegroundColor Red
}

# Check if required tools are installed
function Test-Prerequisites {
    Write-Host "Checking prerequisites..." -ForegroundColor White
    
    # Check Docker
    try {
        $null = Get-Command docker -ErrorAction Stop
        docker --version | Out-Null
        Write-Status "Docker is installed"
    } catch {
        Write-ErrorMsg "Docker is not installed. Please install Docker Desktop for Windows first."
        Write-Host "Download from: https://www.docker.com/products/docker-desktop" -ForegroundColor Blue
        exit 1
    }
    
    # Check Kind
    try {
        $null = Get-Command kind -ErrorAction Stop
        kind --version | Out-Null
        Write-Status "Kind is installed"
    } catch {
        Write-ErrorMsg "Kind is not installed. Please install Kind first."
        Write-Host "Install with: choco install kind" -ForegroundColor Blue
        exit 1
    }
    
    # Check kubectl
    try {
        $null = Get-Command kubectl -ErrorAction Stop
        kubectl version --client=true 2>$null | Out-Null
        Write-Status "kubectl is installed"
    } catch {
        Write-ErrorMsg "kubectl is not installed. Please install kubectl first."
        Write-Host "Install with: choco install kubernetes-cli" -ForegroundColor Blue
        exit 1
    }
}

# Create Kind cluster configuration
function New-KindConfig {
    Write-Host "Creating Kind cluster configuration..." -ForegroundColor White
    
    $kindConfig = @'
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
'@
    
    $kindConfig | Out-File -FilePath "kind-config.yaml" -Encoding UTF8
    Write-Status "Kind configuration created"
}

# Create or recreate Kind cluster
function Initialize-KindCluster {
    Write-Host "Setting up Kind cluster..." -ForegroundColor White
    
    # Check if cluster exists
    $existingClusters = kind get clusters 2>$null
    if ($existingClusters -contains "xmlsign-cluster") {
        Write-Warning "Cluster xmlsign-cluster already exists. Deleting it..."
        kind delete cluster --name xmlsign-cluster
    }
    
    Write-Status "Creating new Kind cluster..."
    kind create cluster --config=kind-config.yaml
    
    # Wait for cluster to be ready
    Write-Host "Waiting for cluster to be ready..." -ForegroundColor White
    kubectl wait --for=condition=Ready nodes --all --timeout=300s
    Write-Status "Kind cluster is ready"
    
    # Copy SQL dump into Kind node
    $sqlDumpPath = Join-Path $PWD "docker"
    $sqlDumpPath = Join-Path $sqlDumpPath "init"
    $sqlDumpPath = Join-Path $sqlDumpPath "xmlsign_db_dump.sql"
    
    if (Test-Path $sqlDumpPath) {
        try {
            $kindNode = docker ps --filter "name=xmlsign-cluster-control-plane" --format "{{.ID}}"
            if ($kindNode) {
                docker exec $kindNode mkdir -p /tmp/docker/init
                docker cp $sqlDumpPath "${kindNode}:/tmp/docker/init/xmlsign_db_dump.sql"
                Write-Status "Copied xmlsign_db_dump.sql into Kind node container"
            } else {
                Write-Warning "Kind node container not found"
            }
        } catch {
            Write-Warning "Failed to copy SQL dump into Kind node"
        }
    }
}

# Check if Docker images exist
function Test-DockerImages {
    Write-Host "Checking Docker images..." -ForegroundColor White
    
    try {
        docker image inspect iconesigndocker-springboot-app:v1.0.1 2>$null | Out-Null
        Write-Status "Spring Boot image found"
    } catch {
        Write-ErrorMsg "Docker image iconesigndocker-springboot-app:v1.0.1 not found!"
        Write-Host "Please build your Spring Boot image first." -ForegroundColor Yellow
        exit 1
    }
    
    try {
        docker image inspect iconesigndocker-nextjs-app 2>$null | Out-Null
        Write-Status "Next.js image found"
    } catch {
        Write-ErrorMsg "Docker image iconesigndocker-nextjs-app not found!"
        Write-Host "Please build your Next.js image first." -ForegroundColor Yellow
        exit 1
    }
}

# Load Docker images into Kind
function Import-DockerImages {
    Write-Host "Loading Docker images into Kind cluster..." -ForegroundColor White
    
    kind load docker-image iconesigndocker-springboot-app:v1.0.1 --name xmlsign-cluster
    Write-Status "Spring Boot image loaded"
    
    kind load docker-image iconesigndocker-nextjs-app --name xmlsign-cluster
    Write-Status "Next.js image loaded"
}

# Prepare local directories
function Initialize-Directories {
    Write-Host "Preparing local directories..." -ForegroundColor White
    
    # Create directories
    $tempPath = $env:TEMP
    $directories = @(
        (Join-Path $tempPath "temp\download"),
        (Join-Path $tempPath "certificates"),
        (Join-Path $tempPath "docker\init")
    )
    
    foreach ($dir in $directories) {
        if (-not (Test-Path $dir)) {
            New-Item -ItemType Directory -Path $dir -Force | Out-Null
        }
    }
    
    # Copy existing files if they exist
    $tempDownloadPath = Join-Path $PWD "temp\download"
    if (Test-Path $tempDownloadPath) {
        try {
            $destPath = Join-Path $tempPath "temp\download"
            Copy-Item -Path "$tempDownloadPath\*" -Destination $destPath -Recurse -Force -ErrorAction SilentlyContinue
            Write-Status "Copied temp/download files"
        } catch {
            Write-Warning "Could not copy temp/download files"
        }
    }
    
    $certificatesPath = Join-Path $PWD "src\main\resources\certificates"
    if (Test-Path $certificatesPath) {
        try {
            $destPath = Join-Path $tempPath "certificates"
            Copy-Item -Path "$certificatesPath\*" -Destination $destPath -Recurse -Force -ErrorAction SilentlyContinue
            Write-Status "Copied certificate files"
        } catch {
            Write-Warning "Could not copy certificate files"
        }
    }
    
    $dockerInitPath = Join-Path $PWD "docker\init"
    if (Test-Path $dockerInitPath) {
        try {
            $destPath = Join-Path $tempPath "docker\init"
            Copy-Item -Path "$dockerInitPath\*" -Destination $destPath -Recurse -Force -ErrorAction SilentlyContinue
            Write-Status "Copied database init scripts"
        } catch {
            Write-Warning "Could not copy database init scripts"
        }
    }
    
    # Copy SQL dump file specifically
    $sqlDumpSourcePath = Join-Path $PWD "docker\init\xmlsign_db_dump.sql"
    if (Test-Path $sqlDumpSourcePath) {
        $sqlDumpDestPath = Join-Path $tempPath "docker\init\xmlsign_db_dump.sql"
        Copy-Item -Path $sqlDumpSourcePath -Destination $sqlDumpDestPath -Force
        Write-Status "Copied xmlsign_db_dump.sql to temp directory"
    } else {
        Write-Warning "xmlsign_db_dump.sql not found in docker\init"
    }
    
    Write-Status "Directories prepared"
}

# Deploy to Kubernetes
function Deploy-Application {
    Write-Host "Deploying application to Kubernetes..." -ForegroundColor White
    
    if (-not (Test-Path "k8s-manifests.yaml")) {
        Write-ErrorMsg "k8s-manifests.yaml not found! Please make sure you have the Kubernetes manifest file."
        exit 1
    }
    
    kubectl apply -f k8s-manifests.yaml
    Write-Status "Kubernetes manifests applied"
    
    Write-Host "Waiting for deployments to be ready..." -ForegroundColor White
    
    # Wait for PostgreSQL first
    kubectl wait --for=condition=available --timeout=300s deployment/postgres -n xmlsign
    Write-Status "PostgreSQL is ready"
    
    # Wait for Spring Boot
    kubectl wait --for=condition=available --timeout=300s deployment/springboot-app -n xmlsign
    Write-Status "Spring Boot app is ready"
    
    # Wait for Next.js
    kubectl wait --for=condition=available --timeout=300s deployment/nextjs-app -n xmlsign
    Write-Status "Next.js app is ready"
    
    # Wait for Cloudflared
    kubectl wait --for=condition=available --timeout=300s deployment/cloudflared -n xmlsign
    Write-Status "Cloudflared is ready"
}

# Show status and access information
function Show-Status {
    Write-Host ""
    Write-Host "Deployment completed successfully!" -ForegroundColor Green
    Write-Host "=================================" -ForegroundColor Green
    Write-Host ""
    
    Write-Host "Pod Status:" -ForegroundColor Cyan
    kubectl get pods -n xmlsign
    Write-Host ""
    
    Write-Host "Services:" -ForegroundColor Cyan
    kubectl get services -n xmlsign
    Write-Host ""
    
    Write-Host "Access your application:" -ForegroundColor Cyan
    Write-Host "1. Next.js Frontend:" -ForegroundColor Yellow
    Write-Host "   kubectl port-forward -n xmlsign service/nextjs-service 3000:3000"
    Write-Host "   Then open: http://localhost:3000"
    Write-Host ""
    Write-Host "2. Spring Boot API:" -ForegroundColor Yellow
    Write-Host "   kubectl port-forward -n xmlsign service/springboot-service 8080:8080"
    Write-Host "   Then open: http://localhost:8080"
    Write-Host ""
    Write-Host "3. View logs:" -ForegroundColor Yellow
    Write-Host "   kubectl logs -f -n xmlsign deployment/nextjs-app"
    Write-Host "   kubectl logs -f -n xmlsign deployment/springboot-app"
    Write-Host "   kubectl logs -f -n xmlsign deployment/postgres"
    Write-Host "   kubectl logs -f -n xmlsign deployment/cloudflared"
    Write-Host ""
    Write-Host "4. Clean up when done:" -ForegroundColor Yellow
    Write-Host "   kind delete cluster --name xmlsign-cluster"
    Write-Host ""
    
    # Check Cloudflare tunnel status
    Write-Host "Cloudflare Tunnel Status:" -ForegroundColor Cyan
    try {
        $tunnelLogs = kubectl logs -n xmlsign deployment/cloudflared --tail=5 2>$null
        if ($tunnelLogs) {
            Write-Host $tunnelLogs -ForegroundColor White
        } else {
            Write-Host "Check tunnel logs for connection status" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "Could not retrieve tunnel logs" -ForegroundColor Yellow
    }
}

# Main execution
function Start-Setup {
    Write-Host "Starting setup process..." -ForegroundColor White
    Write-Host ""
    
    try {
        Test-Prerequisites
        New-KindConfig
        Initialize-KindCluster
        Test-DockerImages
        Import-DockerImages
        Initialize-Directories
        Deploy-Application
        Show-Status
        
        Write-Host ""
        Write-Status "Setup completed! Your XML Sign application is now running on Kind Kubernetes cluster."
    } catch {
        Write-ErrorMsg "Setup failed"
        Write-Host $_.Exception.Message -ForegroundColor Red
        exit 1
    }
}

# Run main function
Start-Setup