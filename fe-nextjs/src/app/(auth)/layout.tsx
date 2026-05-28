import React from 'react';
import Link from 'next/link';

// Bắt buộc phải có export default function
export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex flex-col justify-center items-center bg-gray-50 py-12 sm:px-6 lg:px-8">
      {/* Header / Logo */}
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center mb-8">
        <Link href="/" className="flex justify-center items-center gap-2 mb-2 no-underline">
            {/* <div className="w-10 h-10 bg-blue-600 rounded-lg flex items-center justify-center text-white font-bold text-2xl">
              C
            </div> */}
            <span className="text-3xl font-bold text-gray-900">CareerMate</span>
        </Link>
        <h2 className="mt-2 text-center text-sm text-gray-600">
          Nền tảng tuyển dụng hỗ trợ bởi AI
        </h2>
      </div>

      {/* Main Card Content */}
      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10 border border-gray-100">
          {children}
        </div>
      </div>
    </div>
  );
}