export type Role = 'USER' | 'MERCHANT' | 'ADMIN';

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  businessName?: string;
  defaultUpiId?: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInMs: number;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  role?: Role;
  businessName?: string;
  defaultUpiId?: string;
}
