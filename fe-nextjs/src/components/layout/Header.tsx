"use client";

import React from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  Briefcase,
  FileText,
  LayoutDashboard,
  Users,
  Settings,
  PlusCircle,
  Shield,
  Trophy,
} from "lucide-react";
import { useAuth } from "@/context/Authcontext";
import UserMenu from "@/components/features/auth/UserMenu";
import NotificationBell from "@/components/common/NotificationBell";

interface NavItem {
  label: string;
  href: string;
  icon: React.ElementType;
}

// 1. Menu cho Candidate Thường (KHÔNG CÓ Phỏng vấn)
const CANDIDATE_NAV_FREE: NavItem[] = [
  { label: "Tổng quan", href: "/dashboard-candidate", icon: LayoutDashboard },
  { label: "Việc làm", href: "/jobs", icon: Briefcase },
  { label: "Xếp hạng", href: "/leaderboard", icon: Trophy },
  // Đã ẩn "Phỏng vấn" ở đây
];

// 2. Menu cho Candidate VIP (CÓ Phỏng vấn)
const CANDIDATE_NAV_VIP: NavItem[] = [
  { label: "Tổng quan", href: "/dashboard-candidate", icon: LayoutDashboard },
  { label: "Việc làm", href: "/jobs", icon: Briefcase },
  { label: "Phỏng vấn", href: "/interview", icon: Users }, // Chỉ VIP mới thấy
  { label: "Xếp hạng", href: "/leaderboard", icon: Trophy },
];

const RECRUITER_NAV: NavItem[] = [
  { label: "Tổng quan", href: "/dashboard-recruiter", icon: LayoutDashboard },
  { label: "Đăng tin", href: "/recruiter/manage-jobs", icon: PlusCircle },
  { label: "Ứng viên", href: "/applications", icon: Users },
  { label: "Xếp hạng", href: "/leaderboard", icon: Trophy },
];

const ADMIN_NAV: NavItem[] = [
  { label: "Dashboard", href: "/admin/dashboard", icon: LayoutDashboard },
  { label: "Người dùng", href: "/admin/users", icon: Users },
  { label: "Nội dung", href: "/admin/content", icon: FileText },
  { label: "Xếp hạng", href: "/admin/leaderboard", icon: Trophy },
  { label: "Cài đặt", href: "/admin/settings", icon: Settings },
  { label: "Phân quyền", href: "/admin/roles", icon: Shield },
];

// 3. Map Role vào Menu riêng biệt
const ROLE_MENUS: Record<string, NavItem[]> = {
  // Gói thường dùng menu rút gọn
  CANDIDATE: CANDIDATE_NAV_FREE,

  // Gói VIP dùng menu đầy đủ
  CANDIDATE_VIP: CANDIDATE_NAV_VIP,

  // Nhà tuyển dụng (Giữ nguyên logic cũ hoặc tách ra nếu cần sau này)
  RECRUITER: RECRUITER_NAV,
  RECRUITER_VIP: RECRUITER_NAV,

  ADMIN: ADMIN_NAV,
};

export default function Header() {
  const pathname = usePathname();
  const router = useRouter();
  const { user } = useAuth();

  // Lấy menu dựa trên userRole chính xác
  const currentNavItems = user?.userRole ? ROLE_MENUS[user.userRole] || [] : [];

  const isActive = (path: string) =>
    pathname === path || pathname.startsWith(path + "/")
      ? "text-blue-600 border-b-2 border-blue-600"
      : "text-gray-500 hover:text-gray-700 hover:border-b-2 hover:border-gray-300";

  const handleLogoClick = () => {
    if (!user) {
      router.push("/");
      return;
    }

    if (user.userRole === "ADMIN") {
      router.push("/admin/dashboard");
    } else if (user.userRole.includes("RECRUITER")) {
      router.push("/dashboard-recruiter");
    } else {
      router.push("/dashboard-candidate");
    }
  };

  return (
    <nav className="bg-white shadow-sm sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          {/* Left Side: Logo & Menu */}
          <div className="flex">
            {/* Logo */}
            <div
              className="flex-shrink-0 flex items-center gap-2 cursor-pointer"
              onClick={handleLogoClick}
            >
              <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center text-white font-bold">
                C
              </div>
              <span className="font-bold text-xl text-gray-800">
                CareerMate
              </span>
            </div>

            {/* Dynamic Menu Links */}
            <div className="hidden sm:ml-6 sm:flex sm:space-x-8">
              {currentNavItems.map((item) => (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`inline-flex items-center px-1 pt-1 text-sm font-medium transition-colors duration-200 ${isActive(item.href)}`}
                >
                  <item.icon className="w-4 h-4 mr-2" />
                  {item.label}
                </Link>
              ))}
            </div>
          </div>

          {/* Right Side: Bell & UserMenu */}
          <div className="flex items-center gap-4">
            {user && <NotificationBell />}
            <UserMenu />
          </div>
        </div>
      </div>
    </nav>
  );
}
