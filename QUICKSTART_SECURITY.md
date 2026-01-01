# Quick Start Guide - Security Fixes

## ⚠️ CRITICAL: Required Before Startup

The application will **NOT START** without setting the JWT secret environment variable:

```bash
# Generate a secure secret (64+ bytes)
openssl rand -base64 64

# Windows PowerShell
$env:JWT_SECRET="your-generated-secret-here"

# Linux/Mac
export JWT_SECRET="your-generated-secret-here"

# Or add to application-prod.yml
jwt:
  secret: ${JWT_SECRET}
```

---

## What Was Fixed

### 🔴 Critical Issues (Fixed)
1. ✅ **JWT Secret Exposure** - No default value, fails fast if not set
2. ✅ **Session Revocation N+1** - Added Caffeine cache, 99% fewer DB queries
3. ✅ **Password Reset Timing Attack** - Constant-time responses
4. ✅ **2FA Brute Force** - Rate limiting: 5 attempts per 5 minutes

### 🟡 Medium Priority (Fixed)
5. ✅ **Email Spam** - Rate limiting: 3 emails per hour
6. ✅ **Session Cleanup** - Scheduled daily at 2 AM
7. ✅ **Hardcoded Constants** - Externalized to application.yml
8. ✅ **JWT Exception Handling** - Specific error types with headers

---

## New Files Created

```
backend/src/main/java/com/synergyhub/
├── config/
│   └── CacheConfig.java                    # Caffeine cache configuration
├── service/security/
│   └── RateLimitService.java               # Rate limiting for 2FA & emails
└── exception/
    └── TooManyRequestsException.java       # HTTP 429 exception
```

---

## Files Modified

### Configuration
- `application.yml` - Added rate limiting, 2FA configs, removed JWT default
- `pom.xml` - Added Caffeine and Spring Cache dependencies

### Services
- `SessionService.java` - Added caching & scheduled cleanup
- `TwoFactorAuthService.java` - Added rate limiting & externalized constants
- `PasswordResetService.java` - Added constant-time response
- `RegistrationService.java` - Added rate limiting to email resends

### Security
- `JwtTokenProvider.java` - Enhanced secret validation
- `JwtAuthenticationFilter.java` - Improved exception handling

---

## Configuration Reference

All new configurations in `application.yml`:

```yaml
jwt:
  secret: ${JWT_SECRET:#{null}}  # REQUIRED - set via environment

security:
  rate-limit:
    two-factor-attempts: 5
    two-factor-window-minutes: 5
    email-resend-attempts: 3
    email-resend-window-minutes: 60
    password-reset-min-response-ms: 200
  
  two-factor:
    backup-codes-count: 10
    backup-code-length: 8
```

---

## Testing the Fixes

### 1. Test JWT Secret Validation
```bash
# Should fail with clear error message
mvn spring-boot:run
# Expected: "JWT_SECRET environment variable must be set"
```

### 2. Test Rate Limiting (2FA)
```bash
# Make 6 attempts - 6th should fail with 429
for i in {1..6}; do
  curl -X POST http://localhost:8080/api/auth/2fa/verify \
    -H "Content-Type: application/json" \
    -d '{"email":"test@example.com","code":"123456"}'
done
```

### 3. Test Cache Performance
```bash
# Login once, then make multiple requests
# Check logs - should see minimal DB queries for session checks
```

---

## Production Deployment

### Mandatory Steps
1. ✅ Set `JWT_SECRET` environment variable (64+ bytes)
2. ✅ Run database migrations (if any)
3. ✅ Verify scheduled tasks are enabled
4. ✅ Test rate limiting behavior

### Optional (For Multi-Instance)
- Migrate from Caffeine to Redis for distributed cache
- Migrate rate limiting to Redis for consistency

---

## Monitoring

Watch these logs after deployment:

```bash
# Successful cache usage
"Session revocation cache hit ratio: 98%"

# Rate limiting in action
"Rate limit exceeded for 2FA attempts: user@example.com"

# Scheduled cleanup
"Cleaned up 42 expired and revoked sessions"
```

---

## Rollback Plan

If issues occur:

1. **JWT Secret Issues**
   - Set environment variable correctly
   - Check for 64+ byte length

2. **Cache Issues**
   - Disable caching temporarily by commenting `@Cacheable` annotations
   - Check Caffeine dependency is loaded

3. **Rate Limiting Too Aggressive**
   - Adjust limits in application.yml
   - Restart application

---

For complete details, see [SECURITY_IMPROVEMENTS.md](SECURITY_IMPROVEMENTS.md)
