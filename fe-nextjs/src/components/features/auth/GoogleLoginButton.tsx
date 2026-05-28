"use client";

import { GoogleLogin } from "@react-oauth/google";
import { googleLogin } from "@/services/authService";
import { useAuth } from "@/context/Authcontext"; 
import { useRouter } from "next/navigation";
import toast from "react-hot-toast";

// 1. Định nghĩa các kiểu chữ được Google hỗ trợ
interface GoogleLoginButtonProps {
  textType?: "signin_with" | "signup_with" | "continue_with" | "signin";
}

// 2. Nhận props vào component (Mặc định là signin_with nếu không truyền gì)
export default function GoogleLoginButton({ textType = "signin_with" }: GoogleLoginButtonProps) {
  const router = useRouter();
  const { login: setAuthUser } = useAuth();

  const handleGoogleSuccess = async (credentialResponse: any) => {
    // ... (Giữ nguyên logic xử lý cũ của bạn) ...
    if (credentialResponse.credential) {
      const loadingToast = toast.loading("Đang kết nối Google...");
      try {
        const userResponse = await googleLogin(credentialResponse.credential);
        const userObject = userResponse.user ;
        
        if (!userObject) throw new Error("Không tìm thấy user");

        setAuthUser(userObject);
        
        toast.dismiss(loadingToast);
        toast.success("Thành công!");
      } catch (err) {
        toast.dismiss(loadingToast);
        toast.error("Thất bại.");
      }
    }
  };

  return (
    <div className="w-full flex justify-center mt-4">
      <GoogleLogin
        onSuccess={handleGoogleSuccess}
        onError={() => toast.error("Lỗi kết nối Google")}
        useOneTap={false}
        theme="outline"
        size="large"
        width="300"
        shape="rectangular"
        // 3. Truyền biến textType vào đây
        text={textType} 
      />
    </div>
  );
}