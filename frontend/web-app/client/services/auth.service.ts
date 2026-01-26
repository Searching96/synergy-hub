import api from "./api";
import type {
  ApiResponse,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  User,
} from "@/types/auth.types";

const authService = {
  /**
   * Login user
   */
  async login(email: string, password: string, totpCode: string | null = null): Promise<LoginResponse> {
    const payload: LoginRequest = { email, password };
    if (totpCode) {
      payload.totpCode = totpCode;
    }

    const response = await api.post<LoginResponse>("/auth/login", payload);

    if (response.data.success && response.data.data.accessToken) {
      localStorage.setItem("token", response.data.data.accessToken);
      localStorage.setItem("user", JSON.stringify(response.data.data.user));

      // Explicitly store organizationId if present
      if (response.data.data.user.organizationId) {
        localStorage.setItem("organizationId", String(response.data.data.user.organizationId));
      }

      // Store refresh token if provided
      if (response.data.data.refreshToken) {
        localStorage.setItem("refreshToken", response.data.data.refreshToken);
      }
    }

    return response.data;
  },

  /**
   * Register new user
   */
  async register(data: RegisterRequest): Promise<RegisterResponse> {
    const { confirmPassword, ...registerData } = data;
    const response = await api.post<RegisterResponse>("/auth/register", registerData);
    return response.data;
  },

  /**
   * Refresh access token using refresh token
   * Used to extend session without forcing re-login
   */
  async refreshToken(): Promise<boolean> {
    try {
      const refreshToken = localStorage.getItem("refreshToken");
      if (!refreshToken) return false;

      const response = await api.post<LoginResponse>("/auth/refresh", { refreshToken });

      if (response.data.success && response.data.data.accessToken) {
        localStorage.setItem("token", response.data.data.accessToken);
        if (response.data.data.refreshToken) {
          localStorage.setItem("refreshToken", response.data.data.refreshToken);
        }
        return true;
      }
      return false;
    } catch {
      // Refresh failed, token is expired
      return false;
    }
  },

  /**
   * Logout user
   */
  async logout(): Promise<void> {
    try {
      await api.post("/users/logout");
    } catch (error) {
      console.warn("Logout failed on server:", error);
    } finally {
      localStorage.removeItem("token");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("user");
      localStorage.removeItem("organizationId");
    }
  },

  /**
   * Get current user from localStorage
   */
  getCurrentUser(): User | null {
    const userStr = localStorage.getItem("user");
    return userStr ? (JSON.parse(userStr) as User) : null;
  },

  /**
   * Get token from localStorage
   */
  getToken(): string | null {
    return localStorage.getItem("token");
  },

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return !!localStorage.getItem("token");
  },

  /**
   * Request password reset
   */
  async forgotPassword(email: string): Promise<ApiResponse<unknown>> {
    const response = await api.post<ApiResponse<unknown>>("/auth/forgot-password", { email });
    return response.data;
  },

  /**
   * Reset password with token
   */
  async resetPassword(token: string, newPassword: string, confirmPassword: string): Promise<ApiResponse<unknown>> {
    const response = await api.post<ApiResponse<unknown>>("/auth/reset-password", {
      token,
      newPassword,
      confirmPassword,
    });
    return response.data;
  },

  /**
   * Verify email with token
   */
  async verifyEmail(email: string, token: string): Promise<ApiResponse<unknown>> {
    const response = await api.post<ApiResponse<unknown>>("/auth/verify-email", { token });
    return response.data;
  },

  /**
   * Resend verification email
   */
  async resendVerificationEmail(email: string): Promise<ApiResponse<unknown>> {
    const response = await api.post<ApiResponse<unknown>>("/auth/resend-verification", { email });
    return response.data;
  },
};

export default authService;
