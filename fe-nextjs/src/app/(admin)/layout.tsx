'use client';
import React from 'react';
import Header from '@/components/layout/Header';

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-gray-100"> {/* Admin thường dùng màu nền tối hơn chút hoặc khác biệt */}
      <Header /> 
      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        {children}
      </main>
    </div>
  );
}