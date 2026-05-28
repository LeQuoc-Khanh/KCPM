"use client";

import { useState, useRef, useEffect } from "react";
import Link from "next/link";
import { useAuth } from "@/context/Authcontext";
import { useRouter } from "next/navigation";
import { Building } from "lucide-react";

export default function UserMenu() {
  const { user, logout } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const router = useRouter();

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [menuRef]);

  const handleLogout = () => {
    logout();
    setIsOpen(false);
    router.push("/login");
  };

  if (!user) return null;

  const userAvatar =
    user.profileImageUrl ||
    `https://ui-avatars.com/api/?name=${encodeURIComponent(user.fullName || "User")}&background=random&color=fff&size=128`;

  // Check VIP
  const isVip = user.userRole?.includes("_VIP");
  const isAdmin = user.userRole === "ADMIN"; // Biến tiện ích kiểm tra admin
  const isRecruiter =
    user.userRole?.includes("RECRUITER") ||
    user.userRole?.includes("RECRUITER_VIP");
  const isCandidate =
    user.userRole?.includes("CANDIDATE") ||
    user.userRole?.includes("CANDIDATE_VIP");

  return (
    <div className="relative" ref={menuRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center space-x-2 focus:outline-none hover:bg-gray-100 p-2 rounded-lg transition duration-150"
      >
        <div className="relative">
          <img
            src={userAvatar}
            alt={user.fullName}
            className={`w-8 h-8 md:w-10 md:h-10 rounded-full object-cover ${isVip ? "border-2 border-yellow-400" : "border border-gray-200"}`}
          />
          {isVip && (
            <span className="absolute -bottom-1 -right-1 bg-yellow-400 text-[8px] font-bold px-1 rounded-sm text-white border border-white">
              VIP
            </span>
          )}
        </div>
        <span className="font-medium text-gray-700 hidden md:block text-sm md:text-base">
          {user.fullName}
        </span>
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className={`h-4 w-4 text-gray-500 transition-transform ${isOpen ? "rotate-180" : ""}`}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M19 9l-7 7-7-7"
          />
        </svg>
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-64 bg-white rounded-md shadow-lg py-1 z-50 border border-gray-100 ring-1 ring-black ring-opacity-5">
          <div className="px-4 py-3 border-b border-gray-100 bg-gray-50">
            <p className="text-sm font-semibold text-gray-900">
              {user.fullName}
            </p>
            <p className="text-xs text-gray-500 truncate mb-1">{user.email}</p>
            {isVip ? (
              <span className="text-xs font-bold text-white bg-gradient-to-r from-yellow-400 to-orange-500 px-2 py-0.5 rounded-full inline-block shadow-sm">
                Thành viên VIP
              </span>
            ) : (
              <span className="text-xs text-blue-600 font-medium bg-blue-50 px-2 py-0.5 rounded">
                {user.userRole === "CANDIDATE"
                  ? "Ứng viên"
                  : user.userRole === "RECRUITER"
                    ? "Nhà tuyển dụng"
                    : "Admin"}
              </span>
            )}
          </div>

          <div className="p-2 space-y-1">
            {/* Chỉ hiển thị nút nâng cấp VIP nếu KHÔNG phải là ADMIN */}
            {!isAdmin && (
              <Link
                href="/vip-upgrade"
                className={`flex items-center justify-center px-4 py-2 rounded-md text-sm font-bold transition-all mb-2 ${
                  isVip
                    ? "bg-green-50 text-green-700 hover:bg-green-100 border border-green-200"
                    : "bg-gradient-to-r from-yellow-400 to-orange-500 text-white shadow-md hover:shadow-lg hover:scale-[1.02]"
                }`}
                onClick={() => setIsOpen(false)}
              >
                {isVip ? "✨ Gia hạn VIP" : "👑 Nâng cấp VIP"}
              </Link>
            )}

            {isCandidate && (
              <Link
                href="/cv-builder"
                className="flex items-center px-2 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-md"
                onClick={() => setIsOpen(false)}
              >
                📝 Tạo CV
              </Link>
            )}

            {isCandidate && (
              <Link
                href="/profile"
                className="flex items-center px-2 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-md"
                onClick={() => setIsOpen(false)}
              >
                👤 Xem hồ sơ
              </Link>
            )}
            {isRecruiter && (
              <Link
                href="/recruiter/company"
                className="flex items-center px-2 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-md"
                onClick={() => setIsOpen(false)}
              >
                <Building className="w-4 h-4 mr-2" /> Công ty của tôi
              </Link>
            )}

            {!isAdmin && (
              <Link
                href={
                  user.userRole.includes("RECRUITER")
                    ? "/dashboard-recruiter"
                    : "/dashboard-candidate"
                }
                className="flex items-center px-2 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-md"
                onClick={() => setIsOpen(false)}
              >
                📊 Dashboard
              </Link>
            )}

            {/* Nếu là Admin thì có thể thêm link dashboard Admin */}
            {isAdmin && (
              <Link
                href="/admin/dashboard"
                className="flex items-center px-2 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-md"
                onClick={() => setIsOpen(false)}
              >
                🛡️ Admin Dashboard
              </Link>
            )}
          </div>

          <div className="border-t border-gray-100 py-1">
            <button
              onClick={handleLogout}
              className="flex w-full items-center px-4 py-2 text-sm text-red-600 hover:bg-red-50"
            >
              🚪 Đăng xuất
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
