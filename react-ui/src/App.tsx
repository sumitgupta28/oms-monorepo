import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import PublicLayout    from "./layouts/PublicLayout";
import ProtectedLayout from "./layouts/ProtectedLayout";
import LoginPage       from "./pages/auth/LoginPage";
import RegisterPage    from "./pages/auth/RegisterPage";
import ProductCatalogPage from "./pages/products/ProductCatalogPage";
import ChatPage        from "./pages/chat/ChatPage";
import AdminDashboard  from "./pages/admin/AdminDashboard";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<PublicLayout/>}>
          <Route path="/"          element={<Navigate to="/products" replace/>}/>
          <Route path="/products"  element={<ProductCatalogPage/>}/>
          <Route path="/login"     element={<LoginPage/>}/>
          <Route path="/register"  element={<RegisterPage/>}/>
        </Route>
        <Route element={<ProtectedLayout requiredRole="CUSTOMER"/>}>
          <Route path="/chat"   element={<ChatPage/>}/>
        </Route>
        <Route element={<ProtectedLayout requiredRole="ADMIN"/>}>
          <Route path="/admin"  element={<AdminDashboard/>}/>
        </Route>
        <Route path="*" element={<Navigate to="/products" replace/>}/>
      </Routes>
    </BrowserRouter>
  );
}
