import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '@/services/auth.service';
import type { User, LoginResponse, RegisterResponse } from '@/types/auth.types';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (email: string, password: string, totpCode?: string) => Promise<LoginResponse>;
  register: (data: { name: string; email: string; password: string; confirmPassword: string }) => Promise<RegisterResponse>;
  logout: () => void;
  setUser: (user: User | null) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    // Check for existing token on mount
    const initAuth = () => {
      const token = authService.getToken();
      const storedUser = authService.getCurrentUser();

      if (token && storedUser) {
        setUser(storedUser);
      }

      setLoading(false);
    };

    initAuth();
  }, []);

  const login = async (email: string, password: string, totpCode?: string): Promise<LoginResponse> => {
    const response = await authService.login(email, password, totpCode);

    if (response.success && response.data.user) {
      setUser(response.data.user);
    }

    return response;
  };

  const register = async (data: { name: string; email: string; password: string; confirmPassword: string }): Promise<RegisterResponse> => {
    const response = await authService.register(data);
    return response;
  };

  const logout = () => {
    authService.logout();
    setUser(null);
    navigate('/login');
  };

  const value = {
    user,
    isAuthenticated: !!user,
    loading,
    login,
    register,
    logout,
    setUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
