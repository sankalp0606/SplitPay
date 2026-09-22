import api from './api';
import {
  CreatePaymentOrderRequest,
  PaymentOrder,
  PaymentPart,
  PaymentPlanPreviewRequest,
  PaymentPlanPreviewResponse,
} from '../types/payment';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const paymentService = {
  async createPaymentOrder(data: CreatePaymentOrderRequest): Promise<PaymentOrder> {
    const response = await api.post<PaymentOrder>('/payment-orders', data);
    return response.data;
  },

  async previewPlan(data: PaymentPlanPreviewRequest): Promise<PaymentPlanPreviewResponse> {
    const response = await api.post<PaymentPlanPreviewResponse>('/payment-orders/preview', data);
    return response.data;
  },

  async getOrderById(id: string): Promise<PaymentOrder> {
    const response = await api.get<PaymentOrder>(`/payment-orders/${id}`);
    return response.data;
  },

  async getOrderByReference(reference: string): Promise<PaymentOrder> {
    const response = await api.get<PaymentOrder>(`/payment-orders/ref/${reference}`);
    return response.data;
  },

  async getOrders(page = 0, size = 10): Promise<PageResponse<PaymentOrder>> {
    const response = await api.get<PageResponse<PaymentOrder>>(`/payment-orders?page=${page}&size=${size}`);
    return response.data;
  },

  async getPartById(id: string): Promise<PaymentPart> {
    const response = await api.get<PaymentPart>(`/payment-parts/${id}`);
    return response.data;
  },

  // Admin APIs
  async getAdminMetrics(): Promise<Record<string, number>> {
    const response = await api.get<Record<string, number>>('/admin/metrics');
    return response.data;
  },

  async getAdminOrders(page = 0, size = 15): Promise<PageResponse<PaymentOrder>> {
    const response = await api.get<PageResponse<PaymentOrder>>(`/admin/orders?page=${page}&size=${size}`);
    return response.data;
  },

  async getAdminEvents(page = 0, size = 20): Promise<PageResponse<any>> {
    const response = await api.get<PageResponse<any>>(`/admin/events?page=${page}&size=${size}`);
    return response.data;
  },

  async getAdminTransactions(page = 0, size = 20): Promise<PageResponse<any>> {
    const response = await api.get<PageResponse<any>>(`/admin/transactions?page=${page}&size=${size}`);
    return response.data;
  },

  async getAdminAuditLogs(page = 0, size = 20): Promise<PageResponse<any>> {
    const response = await api.get<PageResponse<any>>(`/admin/audit-logs?page=${page}&size=${size}`);
    return response.data;
  },
};
