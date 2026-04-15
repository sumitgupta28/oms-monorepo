const KC_BASE = `${import.meta.env.VITE_KEYCLOAK_URL}/realms/${import.meta.env.VITE_KEYCLOAK_REALM}`;
const CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID;

export interface TokenResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
}

export async function loginWithPassword(email: string, password: string): Promise<TokenResponse> {
  const res = await fetch(`${KC_BASE}/protocol/openid-connect/token`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "password", client_id: CLIENT_ID,
      username: email, password, scope: "openid profile email",
    }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.error_description || "Login failed");
  }
  return res.json();
}

export async function registerUser(data: {
  firstName: string; lastName: string; email: string; password: string;
}): Promise<void> {
  const adminToken = await getAdminToken();
  const res = await fetch(
    `${import.meta.env.VITE_KEYCLOAK_URL}/admin/realms/${import.meta.env.VITE_KEYCLOAK_REALM}/users`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${adminToken}` },
      body: JSON.stringify({
        firstName: data.firstName, lastName: data.lastName,
        email: data.email, username: data.email, enabled: true,
        credentials: [{ type: "password", value: data.password, temporary: false }],
      }),
    }
  );
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.errorMessage || "Registration failed");
  }
}

async function getAdminToken(): Promise<string> {
  const res = await fetch(`${KC_BASE}/protocol/openid-connect/token`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "password", client_id: CLIENT_ID,
      username: "admin", password: "admin", scope: "openid",
    }),
  });
  const data = await res.json();
  return data.access_token;
}

export async function refreshAccessToken(refreshToken: string): Promise<TokenResponse> {
  const res = await fetch(`${KC_BASE}/protocol/openid-connect/token`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "refresh_token", client_id: CLIENT_ID, refresh_token: refreshToken,
    }),
  });
  if (!res.ok) throw new Error("Session expired");
  return res.json();
}

export async function logoutUser(refreshToken: string): Promise<void> {
  await fetch(`${KC_BASE}/protocol/openid-connect/logout`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ client_id: CLIENT_ID, refresh_token: refreshToken }),
  });
}
