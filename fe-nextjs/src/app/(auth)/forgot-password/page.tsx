"use client";

import { useState, FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { forgotPassword } from "@/services/authService";
import toast from "react-hot-toast";

export default function ForgotPasswordPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    const loadingToast = toast.loading("Đang gửi yêu cầu...");

    try {
      await forgotPassword(email);
      toast.dismiss(loadingToast);
      toast.success("Mã xác nhận đã được gửi vào email!");

      // Chuyển hướng sang trang nhập mã reset sau 2s
      setTimeout(() => {
        router.push("/reset-password");
      }, 2000);
    } catch (err: any) {
      toast.dismiss(loadingToast);
      const msg = err.response?.data?.message || "Không tìm thấy email này.";
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full max-w-md mx-auto p-6 mt-10 bg-white shadow-md rounded-lg">
      <h2 className="mb-6 text-center text-2xl font-bold text-gray-900">
        Quên mật khẩu
      </h2>
      <p className="mb-6 text-center text-gray-600 text-sm">
        Nhập email của bạn, chúng tôi sẽ gửi mã xác thực để đặt lại mật khẩu.
      </p>

      <form className="space-y-6" onSubmit={handleSubmit}>
        <div>
          <label
            htmlFor="email"
            className="block text-sm font-medium text-gray-700"
          >
            Email đã đăng ký
          </label>
          <div className="mt-1">
            <input
              id="email"
              type="email"
              required
              className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none disabled:opacity-50 transition-colors"
        >
          {loading ? "Đang gửi..." : "Gửi mã xác nhận"}
        </button>
      </form>

      <div className="mt-4 text-center">
        <Link
          href="/login"
          className="text-sm font-medium text-blue-600 hover:text-blue-500"
        >
          Quay lại đăng nhập
        </Link>
      </div>
    </div>
  );
}
