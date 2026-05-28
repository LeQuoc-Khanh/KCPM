// fe-nextjs/src/middleware.ts
import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const roleRoutes: Record<string, string[]> = {
  "/admin": ["ADMIN"],
  // SỬA: Thêm các role VIP vào danh sách cho phép
  "/dashboard-recruiter": ["RECRUITER", "RECRUITER_VIP"],
  "/dashboard-candidate": ["CANDIDATE", "CANDIDATE_VIP"],
};

const authRoutes = ["/login", "/register"];

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  const token = request.cookies.get("accessToken")?.value;
  const userRole = request.cookies.get("userRole")?.value;

  // 1. CHƯA ĐĂNG NHẬP
  const isProtectedRoute = Object.keys(roleRoutes).some((route) =>
    pathname.startsWith(route),
  );
  if (isProtectedRoute && !token) {
    const url = new URL("/login", request.url);
    url.searchParams.set("callbackUrl", pathname);
    return NextResponse.redirect(url);
  }

  // 2. ĐÃ ĐĂNG NHẬP (Redirect về đúng dashboard)
  if (token && authRoutes.includes(pathname)) {
    if (userRole === "ADMIN") {
      return NextResponse.redirect(new URL("/admin/dashboard", request.url));
    }
    // SỬA: Dùng .includes() để chấp nhận cả RECRUITER và RECRUITER_VIP
    if (userRole?.includes("RECRUITER")) {
      return NextResponse.redirect(
        new URL("/dashboard-recruiter", request.url),
      );
    }
    // Mặc định còn lại là CANDIDATE (thường + VIP)
    return NextResponse.redirect(new URL("/dashboard-candidate", request.url));
  }

  // 3. KIỂM TRA QUYỀN (RBAC) - SỬA LẠI LOGIC MỀM DẺO HƠN
  if (token && userRole) {
    if (pathname.startsWith("/admin") && userRole !== "ADMIN") {
      return NextResponse.redirect(new URL("/403", request.url));
    }

    // SỬA: Kiểm tra lỏng hơn bằng includes
    if (
      pathname.startsWith("/dashboard-recruiter") &&
      !userRole.includes("RECRUITER")
    ) {
      return NextResponse.redirect(new URL("/403", request.url));
    }

    // SỬA: Kiểm tra lỏng hơn bằng includes
    if (
      pathname.startsWith("/dashboard-candidate") &&
      !userRole.includes("CANDIDATE")
    ) {
      return NextResponse.redirect(new URL("/403", request.url));
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    "/admin/:path*",
    "/dashboard-candidate/:path*",
    "/dashboard-recruiter/:path*",
    "/login",
    "/register",
  ],
};
