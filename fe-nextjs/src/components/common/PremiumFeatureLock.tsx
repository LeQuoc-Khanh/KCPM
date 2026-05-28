'use client';

import React from 'react';
import Link from 'next/link';
import { useAuth } from '@/context/Authcontext';

interface Props {
  children: React.ReactNode;
  title?: string;
  description?: string;
  className?: string; // Thêm className để tùy chỉnh giao diện ngoài
}

export default function PremiumFeatureLock({ 
  children, 
  title = "Tính năng cao cấp", 
  description = "Nâng cấp tài khoản VIP để mở khóa sức mạnh AI." ,
  className = ""
}: Props) {
  
  const { user } = useAuth();
  
  // Logic check VIP: Nếu là Admin hoặc có _VIP
  const isUnlocked = user?.userRole === 'ADMIN' || user?.userRole?.includes('_VIP');

  if (isUnlocked) {
    return <>{children}</>;
  }

  return (
    <div className={`relative rounded-xl overflow-hidden border border-gray-200 bg-white ${className}`}>
      {/* Nội dung bên dưới bị làm mờ */}
      <div className="filter blur-md opacity-30 pointer-events-none select-none p-4 min-h-[100px]" aria-hidden="true">
        {children}
      </div>

      {/* Lớp phủ thông báo */}
      <div className="absolute inset-0 z-10 flex flex-col items-center justify-center bg-white/40 backdrop-blur-[1px] p-2 text-center">
        <div className="bg-white/90 p-4 rounded-xl shadow-lg border border-yellow-100 max-w-xs w-full mx-2 transform transition-all hover:scale-105">
          <div className="text-2xl mb-2">🔒</div>
          <h3 className="text-sm font-bold text-gray-900 mb-1">{title}</h3>
          <p className="text-xs text-gray-600 mb-3">{description}</p>
          
          <Link 
            href="/vip-upgrade" 
            className="block w-full bg-gradient-to-r from-yellow-500 to-orange-600 hover:from-yellow-600 hover:to-orange-700 text-white text-xs font-bold py-2 px-4 rounded-lg shadow-md transition-all"
          >
            Mở khóa VIP
          </Link>
        </div>
      </div>
    </div>
  );
}