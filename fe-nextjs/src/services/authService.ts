import api from "./api";
import {
  RegisterRequest,
  LoginRequest,
  AuthResponseData,
  BackendResponse,
} from "@/types/auth";
import {
  setToken,
  setRefreshToken,
  removeToken,
  setUserRole,
} from "@/utils/authStorage";

// --- ĐĂNG KÝ ---
// Giữ nguyên logic Multipart, trả về BackendResponse
export const register = async (
  data: RegisterRequest,
  avatarFile?: File | null
): Promise<BackendResponse<AuthResponseData>> => {
  const formData = new FormData();
  
  // Đóng gói JSON vào Blob
  formData.append(
    "request",
    new Blob([JSON.stringify(data)], { type: "application/json" })
  );

  // Đính kèm file ảnh nếu có
  if (avatarFile) {
    formData.append("avatar", avatarFile);
  }

  const response = await api.post<BackendResponse<AuthResponseData>>(
    "/auth/register",
    formData,
    {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    }
  );
  return response.data;
};

// --- ĐĂNG NHẬP ---
// Bây giờ login cũng trả về cấu trúc chuẩn BackendResponse
export const login = async (data: LoginRequest): Promise<AuthResponseData> => {
  const response = await api.post<BackendResponse<AuthResponseData>>(
    "/auth/login",
    data
  );
  
  // Lấy dữ liệu từ tầng 'data' của MessageResponse
  // response.data (axios body) -> .data (trường data của Backend)
  const authData = response.data.data;

  // Nếu không có data (trường hợp lỗi logic trả về 200 nhưng data null)
  if (!authData) {
      throw new Error("Không nhận được dữ liệu xác thực từ server");
  }

  // Lưu token
  if (authData.accessToken) {
    setToken(authData.accessToken);
    setRefreshToken(authData.refreshToken);
    setUserRole(authData.user.userRole);
  }
  return authData;
};

// --- XÁC THỰC EMAIL ---
export const verifyEmail = async (email: string, code: string) => {
  const response = await api.post("/auth/verify-email", null, {
    params: { email, code },
  });
  return response.data;
};

// --- GỬI LẠI MÃ XÁC THỰC ---
export const resendVerification = async (email: string) => {
  const response = await api.post("/auth/resend-verification", null, {
    params: { email },
  });
  return response.data;
};

// --- ĐĂNG NHẬP GOOGLE ---
export const googleLogin = async (
  googleToken: string,
  userRole: string = "CANDIDATE"
) => {
  // Backend giờ trả về BackendResponse
  const response = await api.post<BackendResponse<AuthResponseData>>(
    "/auth/google", 
    { googleToken, userRole }
  );

  // Lấy data chuẩn hóa
  const authData = response.data.data;

  if (!authData || !authData.accessToken) {
    console.error("❌ Không lấy được Auth Data hợp lệ:", authData);
    throw new Error("Dữ liệu trả về từ Server không hợp lệ");
  }

  if (authData.accessToken) {
    setToken(authData.accessToken);
    setRefreshToken(authData.refreshToken);
    if (authData.user) {
      setUserRole(authData.user.userRole);
    }
  }

  return authData;
};

// --- ĐĂNG XUẤT ---
export const logout = async () => {
  try {
    await api.post("/auth/logout");
  } catch (error) {
    console.error("Logout error:", error);
  } finally {
    removeToken();
    if (typeof window !== "undefined") {
      window.location.href = "/login";
    }
  }
};

// --- QUÊN MẬT KHẨU ---
export const forgotPassword = async (email: string) => {
  const response = await api.post("/auth/forgot-password", { email });
  return response.data;
};

// --- ĐẶT LẠI MẬT KHẨU ---
export const resetPassword = async (token: string, newPassword: string) => {
  const response = await api.post("/auth/reset-password", {
    token,
    newPassword,
  });
  return response.data;
};