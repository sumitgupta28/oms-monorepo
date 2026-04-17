# Keycloak Validation Guide — `oms` Realm

**Prerequisites:** infra stack is up (`docker compose -f docker-compose.infra.yml up -d`)

---

## Step 1 — Confirm the container is healthy

```bash
docker compose -f docker-compose.infra.yml ps keycloak
```

Expected: `Status` shows `healthy`. If it shows `starting`, wait ~30 s and re-run.

If stuck unhealthy, tail logs:
```bash
docker compose -f docker-compose.infra.yml logs --tail=50 keycloak
```

---

## Step 2 — Verify realm OIDC discovery endpoint

This is the single most important check — if this responds, the realm imported correctly.

```bash
curl -s http://localhost:8180/realms/oms/.well-known/openid-configuration | jq '{issuer, authorization_endpoint, token_endpoint}'
```

Expected output:
```json
{
  "issuer": "http://localhost:8180/realms/oms",
  "authorization_endpoint": "http://localhost:8180/realms/oms/protocol/openid-connect/auth",
  "token_endpoint": "http://localhost:8180/realms/oms/protocol/openid-connect/token"
}
```

---

## Step 3 — Log in to the Admin Console (UI check)

1. Open [http://localhost:8180](http://localhost:8180) in a browser
2. Click **Administration Console**
3. Log in with `admin` / `admin`
4. Verify the realm selector (top-left) shows **oms**

---

## Step 4 — Verify realm settings

Admin Console → **Realm Settings** tab, confirm:

| Setting | Expected value |
|---|---|
| Display name | Order Management System |
| User registration | ON |
| Login with email | ON |
| Reset password | ON |
| SSL required | None |
| Access token lifespan | 5 minutes (300 s) |
| SSO session max lifespan | 10 hours (36000 s) |

---

## Step 5 — Verify roles

Admin Console → **Realm roles**, confirm three roles exist:

| Role | Description |
|---|---|
| `CUSTOMER` | Regular customer |
| `ADMIN` | System administrator |
| `AGENT_MANAGER` | AI agent activity viewer |

Also confirm `CUSTOMER` is the default role: **Realm Settings → User registration → Default roles** shows `CUSTOMER`.

---

## Step 6 — Verify clients

Admin Console → **Clients**, confirm:

| Client ID | Type | Key settings |
|---|---|---|
| `react-ui` | Public | Standard flow + Direct access grants enabled; redirect URI `http://localhost:3000/*`; web origin `http://localhost:3000` |
| `gateway` | Confidential | Service accounts enabled; secret `gateway-secret-local` |

To check the `gateway` secret: click `gateway` → **Credentials** tab → value should be `gateway-secret-local`.

---

## Step 7 — Verify users exist

Admin Console → **Users**, confirm:

| Email | Password | Roles |
|---|---|---|
| `admin@oms.com` | `admin123` | ADMIN, CUSTOMER, AGENT_MANAGER |
| `customer@oms.com` | `customer123` | CUSTOMER |

---

## Step 8 — Obtain a token for the `customer` user

```bash
curl -s -X POST http://localhost:8180/realms/oms/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=react-ui" \
  -d "username=customer@oms.com" \
  -d "password=customer123" | jq '{access_token: .access_token[0:20], token_type, expires_in}'
```

Expected:
```json
{
  "access_token": "<truncated>",
  "token_type": "Bearer",
  "expires_in": 300
}
```

---

## Step 9 — Decode and verify token claims (roles)

```bash
# Grab the full token
TOKEN=$(curl -s -X POST http://localhost:8180/realms/oms/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=react-ui" \
  -d "username=customer@oms.com" \
  -d "password=customer123" | jq -r '.access_token')

# Decode the payload (base64 middle segment)
echo $TOKEN | cut -d'.' -f2 | base64 -d 2>/dev/null | jq '{sub, email, realm_access}'
```

Expected output:
```json
{
  "sub": "<uuid>",
  "email": "customer@oms.com",
  "realm_access": {
    "roles": ["CUSTOMER", "default-roles-oms", "offline_access", "uma_authorization"]
  }
}
```

Repeat for the admin user (`admin@oms.com` / `admin123`) and verify `realm_access.roles` includes `ADMIN`, `CUSTOMER`, and `AGENT_MANAGER`.

---

## Step 10 — Verify `gateway` client credentials (service account)

```bash
curl -s -X POST http://localhost:8180/realms/oms/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=gateway" \
  -d "client_secret=gateway-secret-local" | jq '{access_token: .access_token[0:20], token_type, expires_in}'
```

Expected: a valid `Bearer` token with `expires_in: 300`.

---

## Step 11 — Verify token introspection

```bash
ACCESS_TOKEN=$(curl -s -X POST http://localhost:8180/realms/oms/protocol/openid-connect/token \
  -d "grant_type=password&client_id=react-ui&username=admin@oms.com&password=admin123" | jq -r '.access_token')

curl -s -X POST http://localhost:8180/realms/oms/protocol/openid-connect/token/introspect \
  -u "gateway:gateway-secret-local" \
  -d "token=$ACCESS_TOKEN" | jq '{active, email, realm_access}'
```

Expected: `"active": true` with correct email and roles.

---

## Step 12 — Register a new user via the `react-ui` client

Two approaches are shown — use whichever fits your validation context.

### Option A — Browser-based self-registration (end-to-end UI check)

Open the URL below in a browser. Keycloak will show the registration form configured for the `oms` realm.

```
http://localhost:8180/realms/oms/protocol/openid-connect/registrations?client_id=react-ui&response_type=code&redirect_uri=http://localhost:3000/
```

Fill in first name, last name, email, and password, then submit. After success Keycloak redirects back to `http://localhost:3000/`.

Verify the new user appears in **Admin Console → Users**.

---

### Option B — CLI registration via Admin REST API

```bash
# 1. Obtain a master-realm admin token
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8180/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=admin" | jq -r '.access_token')

# 2. Create the user in the oms realm
curl -s -o /dev/null -w "%{http_code}" \
  -X POST http://localhost:8180/admin/realms/oms/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser@oms.com",
    "email":    "newuser@oms.com",
    "firstName": "New",
    "lastName":  "User",
    "enabled":   true,
    "credentials": [{"type": "password", "value": "newuser123", "temporary": false}]
  }'
```

Expected HTTP response: **`201`**

```bash
# 3. Confirm the user exists and is enabled
curl -s "http://localhost:8180/admin/realms/oms/users?email=newuser%40oms.com" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.[0] | {id, email, enabled}'
```

Expected:
```json
{
  "id": "<uuid>",
  "email": "newuser@oms.com",
  "enabled": true
}
```

```bash
# 4. Verify the new user can authenticate via the react-ui client
curl -s -X POST http://localhost:8180/realms/oms/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=react-ui" \
  -d "username=newuser@oms.com" \
  -d "password=newuser123" | jq '{access_token: .access_token[0:20], token_type, expires_in}'
```

Expected:
```json
{
  "access_token": "<truncated>",
  "token_type": "Bearer",
  "expires_in": 300
}
```

```bash
# 5. Decode the token and confirm the CUSTOMER default role was assigned
TOKEN=$(curl -s -X POST http://localhost:8180/realms/oms/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=react-ui" \
  -d "username=newuser@oms.com" \
  -d "password=newuser123" | jq -r '.access_token')

echo $TOKEN | cut -d'.' -f2 | base64 -d 2>/dev/null | jq '{sub, email, realm_access}'
```

Expected: `realm_access.roles` includes `CUSTOMER` (the default role).

---

## Step 13 — Verify events are recorded

After the token requests and registration above:

Admin Console → **Events** → should show `LOGIN` and `REGISTER` events for the users.

This confirms `eventsEnabled: true` and the following event types are wired up:
`LOGIN`, `LOGIN_ERROR`, `LOGOUT`, `REGISTER`, `REGISTER_ERROR`, `UPDATE_PASSWORD`, `RESET_PASSWORD`, `TOKEN_REFRESH`

---

## Quick pass/fail summary

| # | Check | How to verify | Pass condition |
|---|---|---|---|
| 1 | Container healthy | `docker compose -f docker-compose.infra.yml ps keycloak` | Status: `healthy` |
| 2 | OIDC discovery | `curl .well-known/openid-configuration` | HTTP 200; issuer = `http://localhost:8180/realms/oms` |
| 3 | Realm imported | Admin Console → realm selector | `oms` realm visible |
| 4 | Realm settings | Admin Console → Realm Settings | SSL=none, token lifespan=300 s |
| 5 | 3 roles exist | Admin Console → Realm roles | CUSTOMER, ADMIN, AGENT_MANAGER |
| 6 | Default role | Realm Settings → Default roles | CUSTOMER assigned |
| 7 | 2 clients exist | Admin Console → Clients | `react-ui` (public), `gateway` (confidential) |
| 8 | 2 users exist | Admin Console → Users | admin@oms.com, customer@oms.com |
| 9 | Customer token | `curl` password grant | HTTP 200; `expires_in: 300` |
| 10 | Token roles | JWT decode | `realm_access.roles` matches configured roles |
| 11 | Gateway client creds | `curl` client_credentials | HTTP 200; valid Bearer token |
| 12 | Token introspection | `curl` introspect with gateway creds | `"active": true` |
| 13 | New user registration | Browser form or Admin REST API | HTTP 201; user enabled; CUSTOMER role in JWT |
| 14 | Events logged | Admin Console → Events | LOGIN + REGISTER events present |
