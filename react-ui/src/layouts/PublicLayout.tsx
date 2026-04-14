import React from "react";
import { Link, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export default function PublicLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white border-b border-gray-100 px-6 py-3 flex items-center justify-between sticky top-0 z-10">
        <Link to="/products" className="font-semibold text-gray-900 text-lg">OMS Store</Link>
        <div className="flex items-center gap-5">
          <Link to="/products" className="text-sm text-gray-600 hover:text-gray-900">Products</Link>
          {user ? (
            <>
              <Link to="/chat"   className="text-sm text-gray-600 hover:text-gray-900">Chat</Link>
              <Link to="/orders" className="text-sm text-gray-600 hover:text-gray-900">My Orders</Link>
              {user.roles.includes("ADMIN") && (
                <Link to="/admin" className="text-sm text-gray-600 hover:text-gray-900">Admin</Link>
              )}
              <span className="text-sm text-gray-400">{user.firstName}</span>
              <button onClick={async () => { await logout(); navigate("/products"); }}
                className="text-sm text-gray-500 hover:text-gray-900">Sign out</button>
            </>
          ) : (
            <>
              <Link to="/login"    className="text-sm text-gray-600 hover:text-gray-900">Sign in</Link>
              <Link to="/register" className="text-sm px-4 py-1.5 rounded-lg bg-purple-700 text-white hover:bg-purple-800">Register</Link>
            </>
          )}
        </div>
      </nav>
      <Outlet />
    </div>
  );
}
