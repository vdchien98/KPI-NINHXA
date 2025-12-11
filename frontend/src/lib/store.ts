import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface User {
  id: number;
  email: string;
  fullName: string;
  phone?: string;
  avatar?: string;
  role?: {
    id: number;
    name: string;
    description?: string;
  };
  organizations?: {
    id: number;
    name: string;
    code?: string;
  }[];
  department?: {
    id: number;
    name: string;
    code?: string;
    organizationId?: number;
  };
  position?: {
    id: number;
    name: string;
  };
  isActive: boolean;
  lastLogin?: string;
  createdAt?: string;
}

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  setAuth: (user: User, token: string) => void;
  updateUser: (user: Partial<User>) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      setAuth: (user, token) => {
        localStorage.setItem('token', token);
        set({ user, token, isAuthenticated: true });
      },
      updateUser: (updatedUser) => {
        set((state) => ({
          user: state.user ? { ...state.user, ...updatedUser } : null,
        }));
      },
      logout: () => {
        localStorage.removeItem('token');
        set({ user: null, token: null, isAuthenticated: false });
      },
    }),
    {
      name: 'auth-storage',
    }
  )
);

