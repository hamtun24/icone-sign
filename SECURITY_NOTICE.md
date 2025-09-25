# 🚨 CRITICAL SECURITY NOTICE

**Your Cloudflare tunnel token was temporarily exposed and must be rotated immediately.**

## Immediate Actions Required:

### 1. Rotate Your Tunnel Token (Do this now!)
```bash
# Delete the compromised tunnel
cloudflared tunnel delete xmlsign-frontend

# Create a new tunnel with a fresh token
cloudflared tunnel create xmlsign-frontend

# Get the new token
cloudflared tunnel token xmlsign-frontend
```

### 2. Configure Public Hostname
In your Cloudflare dashboard:
- Go to Zero Trust > Access > Tunnels
- Find your tunnel and configure Public Hostname
- Set Service to: `http://nextjs-service:3000`

### 3. Use New Token for Deployment
When running `./setup-xmlsign-kind.sh`, use your newly generated token.

## What I've Fixed:
- ✅ Removed the compromised token file
- ✅ Fixed Cloudflare tunnel command for proper environment variable expansion
- ✅ Corrected all health checks and probes
- ✅ Ensured production-ready configuration

## 4. Clean Git History (Critical!)
```bash
# Remove the exposed token from git history
git filter-branch --force --index-filter \
'git rm --cached --ignore-unmatch test-tunnel-token.sh' \
--prune-empty --tag-name-filter cat -- --all

# Force push to rewrite history
git push origin --force --all
```

**Your deployment is ready once you complete all security steps above!**