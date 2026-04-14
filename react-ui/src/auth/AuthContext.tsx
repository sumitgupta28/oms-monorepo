import React, { createContext, useContext, useEffect, useState, useCallback } from "react";
import { jwtDecode } from "jwt-decode";
import { loginWithPassword, refreshAccessToken, logoutUser, TokenResponse } from "./authApi";

interface JwtPayload {
  sub: string; email: string; given_name: string; family_name: string;
  realm_access: { roles: string[] }; exp: number;
}
export interface AuthUser {
  id: string; email: string; firstName: string; lastName: string; roles: string[];
}
interface AuthContextValue {
  user: AuthUser | null; loading: boolean; token: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  hasRole: (role: string) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser]       = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [token, setToken]     = useState<string | null>(null);

  const applyTokens = useCallback((tokens: TokenResponse) => {
    localStorage.setItem("access_token",  tokens.access_token);
    localStorage.setItem("refresh_token", tokens.refresh_token);
    setToken(tokens.access_token);
    const decoded = jwtDecode<JwtPayload>(tokens.access_token);
    setUser({ id: decoded.sub, email: decoded.email,
      firstName: decoded.given_name, lastName: decoded.family_name,
      roles: decoded.realm_access?.roles ?? [] });
    const expiresInMs = decoded.exp * 1000 - Date.now() - 30_000;
    setTimeout(async () => {
      const rt = localStorage.getItem("refresh_token");
      if (rt) { try { applyTokens(await refreshAccessToken(rt)); } catch { clearAuth(); } }
    }, Math.max(expiresInMs, 0));
  }, []);

  const clearAuth = () => {
    localStorage.removeItem("access_token"); localStorage.removeItem("refresh_token");
    setUser(null); setToken(null);
  };

  useEffect(() => {
    const rt = localStorage.getItem("refresh_token");
    if (rt) refreshAccessToken(rt).then(applyTokens).catch(clearAuth).finally(() => setLoading(false));
    else setLoading(false);
  }, [applyTokens]);

  const login  = async (email: string, password: string) => { applyTokens(await loginWithPassword(email, password)); };
  const logout = async () => {
    const rt = localStorage.getItem("refresh_token");
    if (rt) await logoutUser(rt).catch(() => {});
    clearAuth();
  };

  return (
    <AuthContext.Provider value={{ user, loading, token,
      login, logout, hasRole: (r) => user?.roles.includes(r) ?? false }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be inside AuthProvider");
  return ctx;
};
