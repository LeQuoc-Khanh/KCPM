import type { Metadata } from "next";
import "./globals.css";
import { AuthProvider } from "@/context/Authcontext";
import GoogleOAuthWrapper from "@/components/providers/GoogleOAuthWrapper";
import AIChatWidget from "@/components/features/chat/AIChatWidget";
import { Toaster } from "react-hot-toast";
import { ConfirmDialogProvider } from "@/context/ConfirmDialogContext";

export const metadata: Metadata = {
  title: "CareerMate",
  description: "Nền tảng tuyển dụng việc làm",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>
        <GoogleOAuthWrapper>
          <AuthProvider>
            <ConfirmDialogProvider>
              {/* Cấu hình Toaster để thông báo tự động tắt */}
              <Toaster
                position="top-right" // Đổi vị trí sang góc phải trên (chuẩn Web App)
                reverseOrder={false}
                toastOptions={{
                  // Cấu hình chung cho khung thông báo
                  className: "",
                  style: {
                    border: "1px solid #E5E7EB", // Viền xám nhẹ
                    padding: "16px",
                    color: "#1F2937", // Chữ màu xám đậm
                    boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1)", // Đổ bóng mềm
                    borderRadius: "8px",
                    fontSize: "14px",
                    fontWeight: "500",
                  },
                  // Cấu hình Success (Thành công)
                  success: {
                    duration: 3000,
                    iconTheme: {
                      primary: "#10B981", // Màu xanh Emerald chuẩn
                      secondary: "white",
                    },
                    style: {
                      borderLeft: "4px solid #10B981", // Vạch màu xanh bên trái tạo điểm nhấn
                    },
                  },
                  // Cấu hình Error (Lỗi)
                  error: {
                    duration: 4000,
                    iconTheme: {
                      primary: "#EF4444", // Màu đỏ chuẩn
                      secondary: "white",
                    },
                    style: {
                      borderLeft: "4px solid #EF4444", // Vạch màu đỏ bên trái
                    },
                  },
                  // Cấu hình Loading
                  loading: {
                    style: {
                      borderLeft: "4px solid #3B82F6", // Vạch màu xanh dương
                    },
                  },
                }}
              />

              {children}
            </ConfirmDialogProvider>

            <AIChatWidget />
          </AuthProvider>
        </GoogleOAuthWrapper>
      </body>
    </html>
  );
}
