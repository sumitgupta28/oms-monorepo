import api from "./axiosClient";
import axios from "axios";

export interface Product { id: string; name: string; description: string; category: string; price: number; stockQty: number; imageUrl?: string; }
export interface ProductPage { content: Product[]; totalElements: number; totalPages: number; }

const PUBLIC_BASE = import.meta.env.VITE_GATEWAY_URL;

export const productApi = {
  getAll: (page = 0, size = 20) =>
    axios.get<ProductPage>(`${PUBLIC_BASE}/api/v1/products?page=${page}&size=${size}`).then(r => r.data),
  getById: (id: string) =>
    axios.get<Product>(`${PUBLIC_BASE}/api/v1/products/${id}`).then(r => r.data),
  search: (q: string) =>
    axios.get<{ results: Product[]; total: number }>(`${PUBLIC_BASE}/api/v1/products/search?q=${encodeURIComponent(q)}`).then(r => r.data),
};
