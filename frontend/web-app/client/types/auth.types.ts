// API Response Types matching API_REFERENCE.md

export interface User {
  id: number;
  name: string;
  email: string;
  roles?: string[];
  permissions?: string[];
  emailVerified?: boolean;
  twoFactorEnabled?: boolean;
  createdAt?: string;
  organizationId?: number;
}

export interface LoginRequest {
  email: string;
  password: string;
  totpCode?: string;
}

export interface LoginResponse {
  success: boolean;
  message: string;
  data: {
    accessToken: string;
    tokenType: string;
    expiresIn: number;
    user: User;
    requiresTwoFactor: boolean;
    refreshToken?: string;
  };
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface RegisterResponse {
  success: boolean;
  message: string;
  data: {
    id: number;
    email: string;
    name: string;
    emailVerified: boolean;
  };
}

export interface ApiResponse<T = any> {
  success: boolean;
  message: string | null;
  data: T;
  errors?: Array<{
    field: string;
    message: string;
  }>;
  error?: string;
  timestamp?: string;
  path?: string;
}

export interface ApiError {
  success: false;
  message: string;
  data: null;
  errors?: Array<{
    field: string;
    message: string;
  }>;
}
