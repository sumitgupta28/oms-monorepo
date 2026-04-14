import api from "./axiosClient";

export interface OrderItem { productId: string; productName: string; quantity: number; unitPrice: number; }
export interface Order { id: string; status: string; totalAmount: number; items: OrderItem[]; createdAt: string; trackingNumber?: string; }

export const orderApi = {
  getMyOrders: () => api.get<Order[]>("/api/orders/my").then(r => r.data),
  getOrder:    (id: string) => api.get<Order>(`/api/orders/${id}`).then(r => r.data),
  getAllOrders: () => api.get<Order[]>("/api/orders").then(r => r.data),
  cancelOrder: (id: string, reason: string) =>
    api.patch<Order>(`/api/orders/${id}/cancel`, { reason }).then(r => r.data),
};
