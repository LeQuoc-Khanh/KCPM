"use client";

import React, { createContext, useContext, useState, useEffect } from "react";
import { getToken, removeToken } from "@/utils/authStorage";
import { useRouter } from "next/navigation";
import { User, UserRole } from "@/types/auth";

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  login: (userData: User) => void;
  logout: () => void;
  updateUser: (updatedData: Partial<User>) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    // Logic khôi phục user khi F5 trang
    const initAuth = async () => {
      const token = getToken();
      const storedUser = localStorage.getItem('currentUser');
      
      if (token && storedUser) {
        try {
            setUser(JSON.parse(storedUser));
        } catch (e) {
            console.error("Lỗi parse user từ localStorage", e);
            localStorage.removeItem('currentUser');
        }
      } else {
        if (!token) localStorage.removeItem('currentUser');
      }
      setIsLoading(false);
    };
    initAuth();
  }, []);

  const login = (userData: User) => {
    if (!userData) return;

    console.log("✅ AuthContext: Đăng nhập thành công với user:", userData);

    setUser(userData);
    localStorage.setItem('currentUser', JSON.stringify(userData));

    // ĐIỀU HƯỚNG DỰA TRÊN ROLE
    switch (userData?.userRole) {
      case UserRole.ADMIN:
        router.push('/admin/dashboard'); 
        break;
      case UserRole.RECRUITER:
      case UserRole.RECRUITER_VIP:
        router.push('/dashboard-recruiter');
        break;
      case UserRole.CANDIDATE:
      case UserRole.CANDIDATE_VIP:
        router.push('/dashboard-candidate');
        break;
      default:
        console.warn("⚠️ Role không xác định, chuyển về trang Dashboard Candidate");
        router.push('/dashboard-candidate');
        break;
    }
  };

  const updateUser = (updatedData: Partial<User>) => {
    if (user) {
      const newUser = { ...user, ...updatedData };
      setUser(newUser);
      localStorage.setItem('currentUser', JSON.stringify(newUser));
    }
  };

  const logout = () => {
    setUser(null);
    removeToken();
    localStorage.removeItem('currentUser');
    router.push("/login");
  };

  return (
    <AuthContext.Provider value={{ user, isLoading, login, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthProvider");
  return context;
};