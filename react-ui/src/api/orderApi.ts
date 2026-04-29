import api from "./axiosClient";

export interface OrderItem { productId: string; productName: string; quantity: number; unitPrice: number; }
export interface Order { id: string; status: string; totalAmount: number; items: OrderItem[]; createdAt: string; trackingNumber?: string; }
export interface OrderPage { content: Order[]; totalElements: number; totalPages: number; number: number; size: number; }

export const orderApi = {
  getMyOrders: () => api.get<Order[]>("/api/v1/orders/my").then(r => r.data),
  getOrder:    (id: string) => api.get<Order>(`/api/v1/orders/${id}`).then(r => r.data),
  getAllOrders: (page = 0, size = 50) =>
    api.get<OrderPage>(`/api/v1/orders?page=${page}&size=${size}&sort=createdAt,desc`).then(r => r.data),
  cancelOrder: (id: string, reason: string) =>
    api.patch<Order>(`/api/v1/orders/${id}/cancel`, { reason }).then(r => r.data),
};
