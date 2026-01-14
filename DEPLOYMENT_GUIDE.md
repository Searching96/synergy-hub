# SynergyHub Deployment Guide

This guide covers the complete deployment of SynergyHub with **SSO (Google/GitHub)** and **Video Conferencing (LiveKit)**.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        VIETNAM (Web Host)                           │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  Frontend (Static Files)                                      │  │
│  │  - React SPA served via Apache/Nginx                          │  │
│  │  - Connects to Singapore VPS via HTTPS                        │  │
│  └───────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                                   │
                                   │ HTTPS API Calls
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      SINGAPORE VPS (Docker)                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │
│  │ Backend  │ │  MySQL   │ │ LiveKit  │ │  Redis   │ │  MinIO   │   │
│  │  :8080   │ │  :3306   │ │  :7880   │ │  :6379   │ │   :443   │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Part 1: VPS Setup (Singapore)

### 1.1 Prerequisites

- Ubuntu 22.04+ VPS with Docker & Docker Compose installed
- Domain pointing to VPS (e.g., `api.synergyhub.com`)
- SSL certificate (use Certbot/Let's Encrypt)

### 1.2 Clone Repository

```bash
git clone <your-repo-url> /opt/synergyhub
cd /opt/synergyhub
```

### 1.3 Configure Environment

Copy and edit the environment file:

```bash
cp deployment.env.example .env
nano .env
```

**Required Variables:**

```env
# Database
DB_ROOT_PASSWORD=<strong-root-password>
DB_NAME=synergyhub
DB_USER=synergyhub_user
DB_PASSWORD=<strong-db-password>

# Security
JWT_SECRET=<64-char-random-string>
DB_ENCRYPTION_KEY=<32-char-random-string>

# MinIO (Running on same VPS, port 443)
MINIO_ENDPOINT=https://localhost:443
MINIO_ACCESS_KEY=<minio-access-key>
MINIO_SECRET_KEY=<minio-secret-key>

# CORS (Your Vietnam Frontend Domain)
CORS_ALLOWED_ORIGINS=https://synergyhub.vn

# OAuth2 (SSO) - Get from Google/GitHub Developer Console
GOOGLE_CLIENT_ID=<from-google-console>
GOOGLE_CLIENT_SECRET=<from-google-console>
GITHUB_CLIENT_ID=<from-github-settings>
GITHUB_CLIENT_SECRET=<from-github-settings>

# LiveKit (Video Conferencing)
LIVEKIT_API_KEY=devkey
LIVEKIT_API_SECRET=<generate-a-strong-secret>
```

### 1.4 Start Services

```bash
docker compose up -d
```

Verify all services are running:

```bash
docker compose ps
```

Expected output:
```
NAME                    STATUS
synergyhub-backend      Up
synergyhub-db           Up
synergyhub-livekit      Up
synergyhub-redis        Up
```

### 1.5 Configure Firewall

```bash
# Allow required ports
sudo ufw allow 8080/tcp   # Backend API
sudo ufw allow 7880/tcp   # LiveKit HTTP
sudo ufw allow 7881/tcp   # LiveKit TCP
sudo ufw allow 7882/udp   # LiveKit UDP (WebRTC)
```

---

## Part 2: Frontend Deployment (Vietnam)

### 2.1 Build Frontend Locally

```bash
cd frontend/web-app

# Set API URL to your Singapore VPS
export VITE_API_URL=https://api.synergyhub.com/api
export VITE_LIVEKIT_URL=wss://api.synergyhub.com:7880

# Install dependencies and build
pnpm install
pnpm run build:client
```

### 2.2 Upload to Web Host

Upload the contents of `frontend/web-app/dist/client/` to your web host's public directory.

### 2.3 Configure Apache (.htaccess)

The repository includes a `.htaccess` file for Apache. Ensure `mod_rewrite` is enabled:

```apache
<IfModule mod_rewrite.c>
  RewriteEngine On
  RewriteBase /
  RewriteRule ^index\.html$ - [L]
  RewriteCond %{REQUEST_FILENAME} !-f
  RewriteCond %{REQUEST_FILENAME} !-d
  RewriteRule . /index.html [L]
</IfModule>
```

---

## Part 3: OAuth2 (SSO) Setup

### 3.1 Google OAuth2

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Navigate to **APIs & Services > Credentials**
4. Click **Create Credentials > OAuth client ID**
5. Configure:
   - Application type: **Web application**
   - Authorized redirect URIs: `https://api.synergyhub.com/api/oauth2/callback/google`
6. Copy **Client ID** and **Client Secret** to your `.env`

### 3.2 GitHub OAuth2

1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
2. Click **New OAuth App**
3. Configure:
   - Homepage URL: `https://synergyhub.vn`
   - Authorization callback URL: `https://api.synergyhub.com/api/oauth2/callback/github`
4. Copy **Client ID** and **Client Secret** to your `.env`

### 3.3 Backend Configuration

Add to `backend/src/main/resources/application-prod.yml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: email, profile
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: user:email, read:user

app:
  oauth2:
    redirect-uri: https://synergyhub.vn/oauth2/redirect
```

---

## Part 4: LiveKit (Video) Setup

### 4.1 Generate API Keys

```bash
# Generate a secure API secret
openssl rand -base64 32
```

Update your `.env`:
```env
LIVEKIT_API_KEY=APIxxxxxxxxxxx
LIVEKIT_API_SECRET=<generated-secret>
```

### 4.2 Production LiveKit Config

Edit `livekit.yaml` for production:

```yaml
port: 7880
bind_address: 0.0.0.0
rtc:
  port_range_start: 50000
  port_range_end: 60000
  use_external_ip: true
  
redis:
  address: redis:6379

keys:
  APIxxxxxxxxxxx: <your-api-secret>
```

### 4.3 SSL for LiveKit (Required for Production)

For WebRTC to work in browsers, LiveKit needs HTTPS. Use a reverse proxy (Nginx/Caddy) or configure LiveKit's built-in TLS:

```yaml
# Add to livekit.yaml
tls:
  cert_file: /etc/livekit/cert.pem
  key_file: /etc/livekit/key.pem
```

---

## Part 5: Verification

### 5.1 Backend Health Check

```bash
curl https://api.synergyhub.com/actuator/health
# Expected: {"status":"UP"}
```

### 5.2 Frontend Access

Open `https://synergyhub.vn` in browser. You should see the landing page.

### 5.3 SSO Test

1. Click **Sign in with Google** on login page
2. Complete Google authentication
3. Verify redirect to dashboard

### 5.4 Video Test

1. Create a meeting in any project
2. Join the meeting
3. Verify camera/microphone prompts appear
4. Test with a second user to confirm connectivity

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| CORS errors | Verify `CORS_ALLOWED_ORIGINS` matches your frontend domain exactly |
| OAuth redirect fails | Check callback URLs in Google/GitHub match your backend |
| Video not connecting | Ensure ports 7880-7882 are open and LiveKit has SSL |
| Database connection failed | Check `DB_*` variables and MySQL container logs |

### View Logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f backend
docker compose logs -f livekit
```

---

## Quick Reference

| Service | Internal Port | External Port | URL |
|---------|---------------|---------------|-----|
| Backend API | 8080 | 8080 | `https://api.synergyhub.com/api` |
| MySQL | 3306 | - | Internal only |
| LiveKit | 7880 | 7880 | `wss://api.synergyhub.com:7880` |
| Redis | 6379 | - | Internal only |
| Frontend | - | 80/443 | `https://synergyhub.vn` |
