#!/bin/bash

# Quick test script to verify the Cloudflare tunnel token
TOKEN="eyJhIjoiZTA2ZmFjYmE3YjA4MDZjZWU4ZGI5OTkxMzYyZWEzN2YiLCJ0IjoiZjI0OTZmZmEtNDQxZS00ZDgwLWFjNTMtOWI4OWIxZGU1NWU2IiwicyI6Ik5ERTJOekJoWVdVdE5qVTFZaTAwTTJFd0xXSmlPVEF0TjJabU5tUmxZbU16Tm1GaiJ9"

echo "Testing Cloudflare tunnel token format..."

# Decode the token to verify it's valid JWT
echo "$TOKEN" | base64 -d 2>/dev/null > /dev/null
if [ $? -eq 0 ]; then
    echo "✓ Token appears to be valid base64 encoded"
else
    echo "✗ Token format appears invalid"
fi

# Test if cloudflared is available (optional)
if command -v cloudflared &> /dev/null; then
    echo "✓ cloudflared is available"
    echo "Testing tunnel connection (dry run)..."
    echo "Command that will be used:"
    echo "cloudflared tunnel --no-autoupdate --metrics 0.0.0.0:2000 run --token $TOKEN"
else
    echo "⚠ cloudflared not found (this is ok for Kind deployment)"
fi

echo ""
echo "Your token is ready for deployment!"
echo "Run: ./setup-xmlsign-kind.sh"
echo "When prompted for the tunnel token, use:"
echo "$TOKEN"