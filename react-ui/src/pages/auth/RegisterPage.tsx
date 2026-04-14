import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { registerUser } from "../../auth/authApi";
import { useAuth } from "../../auth/AuthContext";

export default function RegisterPage() {
  const { login } = useAuth();
  const navigate  = useNavigate();
  const [form, setForm] = useState({ firstName:"", lastName:"", email:"", password:"", confirm:"" });
  const [error,   setError]   = useState("");
  const [loading, setLoading] = useState(false);

  const update = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm(p => ({ ...p, [k]: e.target.value }));

  const strength = (pw: string) => {
    let s = 0;
    if (pw.length >= 8) s++; if (/[A-Z]/.test(pw)) s++;
    if (/[0-9]/.test(pw)) s++; if (/[^A-Za-z0-9]/.test(pw)) s++;
    return ["Too short","Weak","Fair","Good","Strong"][s];
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (form.password !== form.confirm) { setError("Passwords do not match."); return; }
    if (form.password.length < 8) { setError("Password must be at least 8 characters."); return; }
    setError(""); setLoading(true);
    try {
      await registerUser({ firstName: form.firstName, lastName: form.lastName, email: form.email, password: form.password });
      await login(form.email, form.password);
      navigate("/chat", { replace: true });
    } catch (err: any) {
      setError(err.message || "Registration failed.");
    } finally { setLoading(false); }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="flex w-full max-w-3xl rounded-xl overflow-hidden border border-gray-100 shadow-sm">
        <div className="hidden md:flex flex-col justify-between w-56 bg-teal-800 p-8">
          <div>
            <h1 className="text-teal-100 font-semibold text-lg">OMS</h1>
            <p className="text-teal-300 text-xs mt-1">Create your account</p>
          </div>
          <ol className="space-y-4 list-none">
            {["Fill in your details","Account created instantly","Assigned Customer role","Start ordering via AI"].map((s,i)=>(
              <li key={i} className="flex gap-2.5 items-start">
                <span className="w-5 h-5 rounded-full bg-teal-600 text-teal-100 text-xs flex items-center justify-center shrink-0 font-medium">{i+1}</span>
                <span className="text-teal-300 text-xs leading-relaxed">{s}</span>
              </li>
            ))}
          </ol>
        </div>
        <div className="flex-1 bg-white p-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-1">Create account</h2>
          <p className="text-sm text-gray-500 mb-5">Free to join — or <Link to="/products" className="text-teal-600 hover:underline">browse as a guest</Link></p>
          {error && <div className="mb-4 p-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              {(["firstName","lastName"] as const).map(k=>(
                <div key={k}>
                  <label className="block text-xs font-medium text-gray-600 mb-1.5">{k==="firstName"?"First name":"Last name"}</label>
                  <input type="text" required value={form[k]} onChange={update(k)} placeholder={k==="firstName"?"Alex":"Chen"}
                    className="w-full px-3 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-sm focus:outline-none focus:border-teal-400"/>
                </div>
              ))}
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1.5">Email <span className="text-gray-400 font-normal">(your username)</span></label>
              <input type="email" required value={form.email} onChange={update("email")} placeholder="you@example.com"
                className="w-full px-3 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-sm focus:outline-none focus:border-teal-400"/>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1.5">Password</label>
              <input type="password" required value={form.password} onChange={update("password")} placeholder="Min 8 characters"
                className="w-full px-3 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-sm focus:outline-none focus:border-teal-400"/>
              {form.password && <p className="text-xs text-gray-400 mt-1">{strength(form.password)}</p>}
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1.5">Confirm password</label>
              <input type="password" required value={form.confirm} onChange={update("confirm")} placeholder="••••••••"
                className={`w-full px-3 py-2.5 rounded-lg border bg-gray-50 text-sm focus:outline-none ${form.confirm&&form.confirm!==form.password?"border-red-300":"border-gray-200 focus:border-teal-400"}`}/>
            </div>
            <button type="submit" disabled={loading}
              className="w-full py-2.5 rounded-lg bg-teal-600 text-white text-sm font-medium hover:bg-teal-700 disabled:opacity-60">
              {loading?"Creating…":"Create my account"}
            </button>
          </form>
          <p className="text-center text-xs text-gray-500 mt-5">Already have an account? <Link to="/login" className="text-teal-600 font-medium hover:underline">Sign in</Link></p>
        </div>
      </div>
    </div>
  );
}
