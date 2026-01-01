import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import authService from '@/services/auth.service';

interface User {
  id: number;
  name: string;
  email: string;
  roles?: string[];
  permissions?: string[];
  twoFactorEnabled?: boolean;
  emailVerified?: boolean;
}

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (email: string, password: string, totpCode?: string) => Promise<any>;
  register: (data: any) => Promise<any>;
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

  const login = async (email: string, password: string, totpCode?: string) => {
    const response = await authService.login(email, password, totpCode);
    
    if (response.success && response.data.user) {
      setUser(response.data.user);
    }
    
    return response;
  };

  const register = async (data: any) => {
    const response = await authService.register(data);
    return response;
  };

  const logout = () => {
    authService.logout();
    setUser(null);
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
