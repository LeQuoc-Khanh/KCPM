import axios from "axios";
import { getToken, removeToken } from "@/utils/authStorage"; // ✅ cần có removeToken
import { toast } from "react-hot-toast";
// Nếu bạn chưa có removeToken thì tạm dùng localStorage.removeItem(key) bên dưới.

const api = axios.create({
  baseURL: "http://localhost:8080/api",
  headers: {
    "Content-Type": "application/json",
  },
});

// ✅ helper: decode exp của JWT (optional)
function getJwtExp(token?: string | null): number | null {
  try {
    if (!token) return null;
    const parts = token.split(".");
    if (parts.length !== 3) return null;
    const payload = JSON.parse(
      atob(parts[1].replace(/-/g, "+").replace(/_/g, "/")),
    );
    return typeof payload.exp === "number" ? payload.exp : null; // exp là seconds
  } catch {
    return null;
  }
}

function clearAuthAndRedirect(message?: string) {
  if (typeof window === "undefined") return;

  // ✅ thông báo (bạn có thể thay bằng toast của bạn)
  if (message) toast.error(message);

  // ✅ xoá token để khỏi dùng token cũ nữa
  try {
    removeToken?.();
  } catch {
    // fallback nếu chưa có removeToken
    localStorage.removeItem("token");
    localStorage.removeItem("access_token");
    localStorage.removeItem("auth_token");
  }

  // redirect
  if (!window.location.pathname.startsWith("/login")) {
    window.location.href = "/login";
  }
}

// ✅ Request: gắn token
api.interceptors.request.use(
  (config) => {
    const token = getToken();

    // ✅ (optional) chặn trước nếu token đã hết hạn (tránh gọi API vô ích)
    const exp = getJwtExp(token);
    if (exp && Date.now() >= exp * 1000) {
      clearAuthAndRedirect(
        "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
      );
      return Promise.reject(new axios.Cancel("Token expired (client-side)"));
    }

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// ✅ Response: bắt bảo trì / unauthorized / banned
api.interceptors.response.use(
  (res) => res,
  (error) => {
    const status = error?.response?.status;
    const data = error?.response?.data;

    // 0) Account banned: 403
    if (status === 403 && data?.code === "ACCOUNT_BANNED") {
      clearAuthAndRedirect("Tài khoản của bạn đã bị khóa.");
      return Promise.reject(error);
    }

    // 1) Maintenance mode: 503
    if (status === 503) {
      const msg = "Hệ thống đang bảo trì, vui lòng thử lại sau.";
      if (typeof window !== "undefined") {
        localStorage.setItem("maintenance_message", msg);
        if (!window.location.pathname.startsWith("/login")) {
          window.location.href = "/maintenance";
        }
      }
      return Promise.reject(error);
    }

    // 2) Unauthorized: 401
    if (status === 401) {
      // ✅ nếu backend có trả code token expired thì hiện msg rõ
      const msg =
        data?.code === "TOKEN_EXPIRED"
          ? "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
          : data?.message || "Bạn cần đăng nhập để tiếp tục.";

      clearAuthAndRedirect(msg);
      return Promise.reject(error);
    }

    return Promise.reject(error);
  },
);

export default api;
