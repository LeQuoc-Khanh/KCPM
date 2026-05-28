"use client";

import { useState, FormEvent, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { login } from "@/services/authService";
import { useAuth } from "@/context/Authcontext";
import toast from "react-hot-toast";
import GoogleLoginButton from "@/components/features/auth/GoogleLoginButton";
import { UserRole } from "@/types/auth";

// Tách Form ra component riêng để dùng Suspense
function LoginForm() {
  const router = useRouter();
  const { login: setAuthUser } = useAuth();
  const searchParams = useSearchParams();

  // Lấy link cần quay về (nếu có)
  const callbackUrl = searchParams.get("callbackUrl");

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleLogin = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    const loadingToast = toast.loading("Đang xác thực...");

    try {
      const authData = await login({ email, password });

      if (authData && authData.user) {
        setAuthUser(authData.user);
        toast.dismiss(loadingToast);
        toast.success("Đăng nhập thành công!");

        // --- LOGIC ĐIỀU HƯỚNG ---
        if (callbackUrl) {
          // Nếu có link cũ (VD: trang mua VIP) -> Quay về đó
          setTimeout(() => {
            router.push(decodeURIComponent(callbackUrl));
          }, 500);
        } else {
          // Mặc định cũ
          setTimeout(() => {
            const role = authData.user.userRole;
            if (role === UserRole.ADMIN) router.push("/admin/dashboard");
            else if (
              role.includes(UserRole.RECRUITER) ||
              role.includes(UserRole.RECRUITER_VIP)
            )
              router.push("/dashboard-recruiter");
            else router.push("/dashboard-candidate");
          }, 500);
        }
      } else {
        toast.dismiss(loadingToast);
        setError("Không lấy được thông tin người dùng.");
      }
    } catch (err: any) {
      toast.dismiss(loadingToast);
      console.error("Login Error:", err);
      const msg =
        err.response?.data?.message || "Email hoặc mật khẩu không chính xác.";
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="space-y-6" onSubmit={handleLogin}>
      <div>
        <label
          htmlFor="email"
          className="block text-sm font-medium text-gray-700"
        >
          Email
        </label>
        <div className="mt-1">
          <input
            id="email"
            name="email"
            type="email"
            required
            className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </div>
      </div>

      <div>
        <label
          htmlFor="password"
          className="block text-sm font-medium text-gray-700"
        >
          Mật khẩu
        </label>
        <div className="mt-1 relative">
          <input
            id="password"
            name="password"
            type={showPassword ? "text" : "password"}
            required
            className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm pr-10"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <button
            type="button"
            className="absolute inset-y-0 right-0 px-3 flex items-center text-gray-500 hover:text-gray-700 focus:outline-none"
            onClick={() => setShowPassword(!showPassword)}
          >
            {showPassword ? (
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth={1.5}
                stroke="currentColor"
                className="w-5 h-5"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z"
                />
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                />
              </svg>
            ) : (
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth={1.5}
                stroke="currentColor"
                className="w-5 h-5"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88"
                />
              </svg>
            )}
          </button>
        </div>
      </div>

      <div className="flex items-center justify-between">
        <div className="flex items-center">
          <input
            id="remember-me"
            name="remember-me"
            type="checkbox"
            className="h-4 w-4 text-blue-600 border-gray-300 rounded"
          />
          <label
            htmlFor="remember-me"
            className="ml-2 block text-sm text-gray-900"
          >
            Ghi nhớ đăng nhập
          </label>
        </div>
        <div className="text-sm">
          <a
            href="/forgot-password"
            className="font-medium text-blue-600 hover:text-blue-500"
          >
            Quên mật khẩu?
          </a>
        </div>
      </div>

      <div>
        <button
          type="submit"
          disabled={loading}
          className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50"
        >
          {loading ? "Đang xử lý..." : "Đăng nhập"}
        </button>
      </div>
    </form>
  );
}

// Component chính
export default function LoginPage() {
  return (
    <div className="w-full max-w-md mx-auto p-6">
      <h2 className="mb-6 text-center text-2xl font-bold text-gray-900">
        Đăng nhập vào tài khoản
      </h2>

      {/* QUAN TRỌNG: Bọc LoginForm trong Suspense */}
      <Suspense
        fallback={<div className="text-center p-4">Đang tải form...</div>}
      >
        <LoginForm />
      </Suspense>

      <div className="mt-6">
        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-gray-300" />
          </div>
          <div className="relative flex justify-center text-sm">
            <span className="px-2 bg-white text-gray-500">
              Hoặc tiếp tục với tư cách người ứng tuyển
            </span>
          </div>
        </div>
        <div className="mt-4">
          <GoogleLoginButton />
        </div>
      </div>

      <p className="mt-6 text-center text-sm text-gray-600">
        Chưa có tài khoản?{" "}
        <Link
          href="/register"
          className="font-medium text-blue-600 hover:text-blue-500"
        >
          Đăng ký ngay
        </Link>
      </p>
    </div>
  );
}
