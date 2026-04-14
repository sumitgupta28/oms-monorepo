import React from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

interface Props { requiredRole?: string; }

export default function ProtectedLayout({ requiredRole }: Props) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return <div className="flex items-center justify-center min-h-screen"><div className="text-gray-500">Loading...</div></div>;
  if (!user)   return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  if (requiredRole && !user.roles.includes(requiredRole))
    return <div className="flex items-center justify-center min-h-screen text-red-500">403 — Access denied</div>;
  return <Outlet />;
}
