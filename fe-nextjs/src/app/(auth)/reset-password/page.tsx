"use client";

import { useState, FormEvent } from "react";
import { useRouter } from "next/navigation";
import { resetPassword } from "@/services/authService";
import toast from "react-hot-toast";

export default function ResetPasswordPage() {
  const router = useRouter();
  const [token, setToken] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    
    if (newPassword.length < 6) {
        toast.error("Mật khẩu phải có ít nhất 6 ký tự");
        setLoading(false);
        return;
    }

    const loadingToast = toast.loading("Đang cập nhật mật khẩu...");

    try {
      await resetPassword(token, newPassword);
      toast.dismiss(loadingToast);
      toast.success("Đặt lại mật khẩu thành công!");

      // Chuyển về trang login sau 2s
      setTimeout(() => {
        router.push("/login");
      }, 2000);
    } catch (err: any) {
      toast.dismiss(loadingToast);
      const msg = err.response?.data?.message || "Mã Token không hợp lệ hoặc đã hết hạn.";
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full max-w-md mx-auto p-6 mt-10 bg-white shadow-md rounded-lg">
     
      <h2 className="mb-6 text-center text-2xl font-bold text-gray-900">
        Đặt lại mật khẩu
      </h2>

      <form className="space-y-6" onSubmit={handleSubmit}>
        <div>
          <label className="block text-sm font-medium text-gray-700">
            Mã Token (Kiểm tra email)
          </label>
          <div className="mt-1">
            <input
              type="text"
              required
              placeholder="Nhập mã token từ email..."
              className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              value={token}
              onChange={(e) => setToken(e.target.value)}
            />
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700">
            Mật khẩu mới
          </label>
          <div className="mt-1">
            <input
              type="password"
              required
              placeholder="Nhập mật khẩu mới..."
              className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
            />
          </div>
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-green-600 hover:bg-green-700 focus:outline-none disabled:opacity-50 transition-colors"
        >
          {loading ? "Đang xử lý..." : "Đổi mật khẩu"}
        </button>
      </form>
    </div>
  );
}