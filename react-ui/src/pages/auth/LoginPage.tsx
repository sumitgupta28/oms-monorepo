import React, { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/AuthContext";

export default function LoginPage() {
  const { login }  = useAuth();
  const navigate   = useNavigate();
  const location   = useLocation();
  const from       = (location.state as any)?.from ?? "/chat";

  const [email,    setEmail]    = useState("");
  const [password, setPassword] = useState("");
  const [error,    setError]    = useState("");
  const [loading,  setLoading]  = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault(); setError(""); setLoading(true);
    try {
      await login(email, password);
      navigate(from, { replace: true });
    } catch (err: any) {
      setError(err.message?.includes("Invalid") ? "Incorrect email or password." : "Login failed. Try again.");
    } finally { setLoading(false); }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="flex w-full max-w-3xl rounded-xl overflow-hidden border border-gray-100 shadow-sm">
        <div className="hidden md:flex flex-col justify-between w-56 bg-purple-900 p-8">
          <div>
            <h1 className="text-purple-100 font-semibold text-lg">OMS</h1>
            <p className="text-purple-300 text-xs mt-1">Order Management System</p>
          </div>
          <ul className="space-y-3">
            {["Browse products without signing in","AI-powered order assistant","Track all your orders"].map(f=>(
              <li key={f} className="flex gap-2 items-start">
                <span className="w-1.5 h-1.5 rounded-full bg-teal-400 mt-1.5 shrink-0"/>
                <span className="text-purple-300 text-xs leading-relaxed">{f}</span>
              </li>
            ))}
          </ul>
        </div>
        <div className="flex-1 bg-white p-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-1">Welcome back</h2>
          <p className="text-sm text-gray-500 mb-6">Sign in — works for all roles</p>
          {error && <div className="mb-4 p-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1.5">Email</label>
              <input type="email" required value={email} onChange={e=>setEmail(e.target.value)}
                placeholder="you@example.com"
                className="w-full px-3 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-sm focus:outline-none focus:border-purple-400"/>
            </div>
            <div>
              <div className="flex justify-between mb-1.5">
                <label className="text-xs font-medium text-gray-600">Password</label>
                <span className="text-xs text-purple-600 cursor-pointer">Forgot password?</span>
              </div>
              <input type="password" required value={password} onChange={e=>setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full px-3 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-sm focus:outline-none focus:border-purple-400"/>
            </div>
            <button type="submit" disabled={loading}
              className="w-full py-2.5 rounded-lg bg-purple-700 text-white text-sm font-medium hover:bg-purple-800 disabled:opacity-60">
              {loading ? "Signing in…" : "Sign in"}
            </button>
          </form>
          <div className="relative my-4"><div className="absolute inset-0 flex items-center"><div className="w-full border-t border-gray-100"/></div>
            <div className="relative flex justify-center text-xs text-gray-400 bg-white px-2 w-fit mx-auto">or</div>
          </div>
          <Link to="/products" className="block w-full py-2.5 rounded-lg border border-gray-200 text-center text-sm text-gray-500 hover:bg-gray-50">
            Continue as guest — browse products only
          </Link>
          <p className="text-center text-xs text-gray-500 mt-5">
            No account? <Link to="/register" className="text-purple-600 font-medium hover:underline">Create one free</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
