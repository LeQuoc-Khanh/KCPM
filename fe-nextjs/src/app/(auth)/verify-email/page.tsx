"use client";

import { useState, Suspense, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { verifyEmail, resendVerification } from "@/services/authService"; // Import thêm resendVerification
import toast from "react-hot-toast";
import Link from "next/link";

function VerifyEmailForm() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // Lấy email từ URL: localhost:3000/verify-email?email=abc@gmail.com
  const emailFromUrl = searchParams.get("email") || "";

  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [loading, setLoading] = useState(false);

  // State cho bộ đếm ngược gửi lại mã (60s)
  const [countdown, setCountdown] = useState(0);

  // Cập nhật state khi URL thay đổi (hoặc khi load trang lần đầu)
  useEffect(() => {
    if (emailFromUrl) {
      setEmail(emailFromUrl);
    }
  }, [emailFromUrl]);

  // Logic đếm ngược (Countdown Timer)
  useEffect(() => {
    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [countdown]);

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!email || !code) {
      toast.error("Vui lòng nhập đầy đủ Email và Mã xác thực");
      return;
    }

    setLoading(true);
    const loadingToast = toast.loading("Đang kiểm tra mã...");

    try {
      // Gọi API verifyEmail
      await verifyEmail(email, code);

      toast.dismiss(loadingToast);
      toast.success("Xác thực thành công! Đang chuyển hướng...", {
        duration: 3000,
        style: {
          borderRadius: "10px",
          background: "#333",
          color: "#fff",
        },
      });

      // Chuyển về trang đăng nhập sau 2 giây
      setTimeout(() => {
        router.push("/login");
      }, 2000);
    } catch (err: any) {
      toast.dismiss(loadingToast);
      console.error("Verify Error:", err);

      const msg =
        err.response?.data?.message ||
        "Mã xác thực không chính xác hoặc đã hết hạn.";

      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  // Hàm xử lý khi bấm "Gửi lại mã"
  const handleResend = async () => {
    if (!email) {
      toast.error("Vui lòng nhập email để gửi lại mã.");
      return;
    }

    const loadingToast = toast.loading("Đang gửi lại mã...");
    try {
      await resendVerification(email);

      toast.dismiss(loadingToast);
      toast.success("Đã gửi mã mới! Vui lòng kiểm tra email.");

      // Bắt đầu đếm ngược 60 giây
      setCountdown(60);
    } catch (error: any) {
      toast.dismiss(loadingToast);
      const msg =
        error.response?.data?.message ||
        "Không thể gửi lại mã. Vui lòng thử lại sau.";
      toast.error(msg);
    }
  };

  return (
    <div className="w-full max-w-md mx-auto p-6 bg-white rounded-lg shadow-md my-10">
      <div className="text-center mb-8">
        <h2 className="text-3xl font-bold text-gray-900">Xác thực tài khoản</h2>
        <p className="mt-2 text-sm text-gray-600">
          Vui lòng nhập mã xác thực 6 số đã được gửi đến email: <br />
          <span className="font-medium text-blue-600">{email || "..."}</span>
        </p>
      </div>

      <form className="space-y-6" onSubmit={handleVerify}>
        {/* Input Email */}
        <div>
          <label
            htmlFor="email"
            className="block text-sm font-medium text-gray-700"
          >
            Email nhận mã
          </label>
          <div className="mt-1">
            <input
              id="email"
              type="email"
              required
              className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="nhap-email-cua-ban@example.com"
            />
          </div>
        </div>

        {/* Input Code */}
        <div>
          <label
            htmlFor="code"
            className="block text-sm font-medium text-gray-700"
          >
            Mã xác thực (6 ký tự)
          </label>
          <div className="mt-1">
            <input
              id="code"
              type="text"
              required
              maxLength={6}
              placeholder="VD: A1B2C3"
              className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm tracking-widest uppercase font-bold text-center text-xl"
              value={code}
              onChange={(e) => setCode(e.target.value.toUpperCase())}
            />
          </div>
        </div>

        {/* Nút Submit */}
        <div>
          <button
            type="submit"
            disabled={loading}
            className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 transition-colors"
          >
            {loading ? "Đang xác thực..." : "Kích hoạt tài khoản"}
          </button>
        </div>
      </form>

      {/* Phần Gửi lại mã */}
      <div className="mt-6 text-center border-t pt-4">
        <p className="text-sm text-gray-600">
          Chưa nhận được mã?{" "}
          {countdown > 0 ? (
            <span className="font-medium text-gray-400 cursor-not-allowed">
              Gửi lại sau {countdown}s
            </span>
          ) : (
            <button
              type="button"
              className="font-medium text-blue-600 hover:text-blue-500 hover:underline focus:outline-none"
              onClick={handleResend}
            >
              Gửi lại mã
            </button>
          )}
        </p>

        <div className="mt-4">
          <Link
            href="/login"
            className="text-sm font-medium text-gray-500 hover:text-gray-900 transition-colors"
          >
            ← Quay lại đăng nhập
          </Link>
        </div>
      </div>
    </div>
  );
}

// Bắt buộc phải bọc trong Suspense khi dùng useSearchParams trong Next.js App Router
export default function VerifyPage() {
  return (
    <Suspense
      fallback={
        <div className="text-center p-10">Đang tải trang xác thực...</div>
      }
    >
      <VerifyEmailForm />
    </Suspense>
  );
}
