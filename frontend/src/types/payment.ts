export type PaymentOrderStatus =
  | 'PENDING'
  | 'PARTIALLY_PAID'
  | 'COMPLETED'
  | 'FAILED'
  | 'EXPIRED'
  | 'CANCELLED';

export type PaymentPartStatus =
  | 'PENDING'
  | 'SUCCESS'
  | 'FAILED'
  | 'EXPIRED'
  | 'CANCELLED';

export type SplittingStrategyType =
  | 'MAX_PART_AMOUNT'
  | 'EQUAL_SPLIT'
  | 'CUSTOM_SPLIT';

export interface PaymentPart {
  id: string;
  paymentOrderId: string;
  partNumber: number;
  paymentReference: string;
  amount: number;
  currency: string;
  status: PaymentPartStatus;
  upiUri: string;
  qrDataUri: string;
  createdAt: string;
  updatedAt: string;
}

export interface PaymentOrder {
  id: string;
  orderReference: string;
  recipientName: string;
  upiId: string;
  totalAmount: number;
  currency: string;
  status: PaymentOrderStatus;
  splittingStrategy: SplittingStrategyType;
  maxPartAmount?: number;
  notes?: string;
  paidAmount: number;
  remainingAmount: number;
  partCount: number;
  parts: PaymentPart[];
  createdAt: string;
  updatedAt: string;
}

export interface CreatePaymentOrderRequest {
  recipientName: string;
  upiId: string;
  totalAmount: number;
  splittingStrategy?: SplittingStrategyType;
  maxPartAmount?: number;
  notes?: string;
  customParts?: number[];
}

export interface PaymentPlanPreviewRequest {
  totalAmount: number;
  splittingStrategy?: SplittingStrategyType;
  maxPartAmount?: number;
  customParts?: number[];
}

export interface PaymentPlanPreviewResponse {
  totalAmount: number;
  partCount: number;
  parts: number[];
  splittingStrategy: SplittingStrategyType;
  maxPartAmount: number;
}
