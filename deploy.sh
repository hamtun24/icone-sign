#!/bin/bash
set -e

echo "Preparing Postgres init scripts..."
mkdir -p /tmp/docker/init
cp ./xmlsign_db_dump.sql /tmp/docker/init/

echo "Deploying to Kubernetes..."
kubectl apply -f k8s-manifests.yaml

echo "Waiting for deployments to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/postgres -n xmlsign
kubectl wait --for=condition=available --timeout=300s deployment/springboot-app -n xmlsign
kubectl wait --for=condition=available --timeout=300s deployment/nextjs-app -n xmlsign
kubectl wait --for=condition=available --timeout=300s deployment/cloudflared -n xmlsign

echo "Deployment completed successfully!"
echo ""
echo "Checking pod status..."
kubectl get pods -n xmlsign

echo ""
echo "Services:"
kubectl get services -n xmlsign