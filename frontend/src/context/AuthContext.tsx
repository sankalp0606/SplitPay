import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, LoginRequest, RegisterRequest } from '../types/auth';
import { authService } from '../services/authService';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const storedUser = localStorage.getItem('splitpay_user');
    return storedUser ? JSON.parse(storedUser) : null;
  });
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('splitpay_token'));
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    const verifyAuth = async () => {
      if (token) {
        try {
          const currentUser = await authService.getCurrentUser();
          setUser(currentUser);
          localStorage.setItem('splitpay_user', JSON.stringify(currentUser));
        } catch {
          logout();
        }
      }
      setIsLoading(false);
    };

    verifyAuth();
  }, [token]);

  const login = async (credentials: LoginRequest) => {
    const response = await authService.login(credentials);
    setToken(response.token);
    setUser(response.user);
    localStorage.setItem('splitpay_token', response.token);
    localStorage.setItem('splitpay_user', JSON.stringify(response.user));
  };

  const register = async (data: RegisterRequest) => {
    const response = await authService.register(data);
    setToken(response.token);
    setUser(response.user);
    localStorage.setItem('splitpay_token', response.token);
    localStorage.setItem('splitpay_user', JSON.stringify(response.user));
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('splitpay_token');
    localStorage.removeItem('splitpay_user');
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token,
        isLoading,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
