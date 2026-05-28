"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/context/Authcontext";
import { useRouter } from "next/navigation";
import { paymentService } from "@/services/paymentService";
import { toast } from "react-hot-toast";
import { useConfirm } from "@/context/ConfirmDialogContext";
import { ArrowLeft, Clock } from "lucide-react";

// --- CẤU HÌNH GÓI DỊCH VỤ ---
const VIP_PACKAGES = {
  CANDIDATE: {
    name: "Candidate Pro",
    description: "Nâng cấp hồ sơ, chinh phục nhà tuyển dụng với quyền năng AI.",
    price: "200k",
    period: "/ 30 ngày",
    themeGradient: "from-blue-600 via-indigo-700 to-purple-800",
    buttonGradient: "from-blue-500 to-indigo-600",
    features: [
      "AI Phân tích & Chấm điểm CV chi tiết",
      "Phỏng vấn thử 1-1 với AI (Mock Interview)",
      "Huy hiệu Ứng viên Tài năng (VIP)",
    ],
  },
  RECRUITER: {
    name: "Recruiter Premium",
    description:
      "Tìm kiếm nhân tài nhanh chóng, tối ưu hóa quy trình tuyển dụng.",
    price: "100k",
    period: "/ 30 ngày",
    themeGradient: "from-orange-600 via-red-700 to-pink-800",
    buttonGradient: "from-orange-500 to-red-600",
    features: ["Phân tích hồ sơ ứng viên bằng AI"],
  },
};

export default function VipUpgradePage() {
  const { user, updateUser } = useAuth();
  const router = useRouter();
  const confirm = useConfirm();
  const [loading, setLoading] = useState(false);
  const [checkingAuth, setCheckingAuth] = useState(true);

  // 1. Kiểm tra đăng nhập
  useEffect(() => {
    const timer = setTimeout(() => {
      if (!user) {
        const callbackUrl = encodeURIComponent("/vip-upgrade");
        router.push(`/login?callbackUrl=${callbackUrl}`);
      } else {
        setCheckingAuth(false);
      }
    }, 500);
    return () => clearTimeout(timer);
  }, [user, router]);

  if (checkingAuth)
    return (
      <div className="min-h-screen flex items-center justify-center font-medium text-gray-500">
        Đang tải thông tin gói...
      </div>
    );

  // 2. Xác định Role & Gói
  const isVip = user?.userRole?.includes("_VIP");
  const packageType = user?.userRole?.includes("RECRUITER")
    ? "RECRUITER"
    : "CANDIDATE";
  const currentPkg = VIP_PACKAGES[packageType];

  // Logic hiển thị ngày hết hạn (String)
  const expiryDate = user?.vipExpirationDate
    ? new Date(user.vipExpirationDate).toLocaleDateString("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      })
    : "Chưa cập nhật";

  // 3. Hàm tính toán % tiến trình thời gian
  const calculateProgress = () => {
    if (!user?.vipExpirationDate) return { percent: 0, daysLeft: 0 };

    const expiry = new Date(user.vipExpirationDate).getTime();
    const now = new Date().getTime();

    // Tính thời gian còn lại (ms)
    const timeLeft = expiry - now;

    // Nếu đã hết hạn
    if (timeLeft <= 0) return { percent: 0, daysLeft: 0 };

    // Quy đổi ra ngày còn lại
    const daysLeft = Math.ceil(timeLeft / (1000 * 60 * 60 * 24));

    // Giả sử chu kỳ gói là 30 ngày (2592000000 ms) để hiển thị thanh loading
    const totalDuration = 30 * 24 * 60 * 60 * 1000;

    // Tính phần trăm
    let percent = (timeLeft / totalDuration) * 100;

    // Giới hạn hiển thị 0-100%
    if (percent > 100) percent = 100;
    if (percent < 0) percent = 0;

    return { percent, daysLeft };
  };

  const { percent: progressPercent, daysLeft } = calculateProgress();

  // 4. Xử lý thanh toán
  const handlePayment = async () => {
    const isConfirmed = await confirm({
      title: isVip ? "Xác nhận gia hạn VIP" : "Xác nhận nâng cấp VIP",
      message: isVip
        ? `Gói ${currentPkg.name} của bạn sẽ được gia hạn thêm 30 ngày. Bạn có muốn tiếp tục?`
        : `Bạn sắp đăng ký gói ${currentPkg.name}. Bạn sẽ được mở khóa toàn bộ tính năng cao cấp ngay lập tức.`,
      confirmLabel: "Thanh toán ngay",
      cancelLabel: "Để sau",
      isDanger: false,
    });

    if (!isConfirmed) return;

    setLoading(true);
    const toastId = toast.loading("Đang xử lý giao dịch...");

    try {
      const authData = await paymentService.upgradeToVip();
      updateUser(authData.user);

      toast.dismiss(toastId);
      toast.custom(
        (t) => (
          <div
            className={`${t.visible ? "animate-enter" : "animate-leave"} 
          max-w-md w-full bg-white shadow-2xl rounded-2xl ring-1 ring-black ring-opacity-5 overflow-hidden border-2 border-yellow-400`}
          >
            <div className="p-4 flex items-start">
              <div className="flex-shrink-0 pt-0.5">
                <div className="h-12 w-12 bg-yellow-100 rounded-full flex items-center justify-center animate-bounce">
                  <span className="text-2xl">👑</span>
                </div>
              </div>
              <div className="ml-3 flex-1">
                <p className="text-lg font-bold text-gray-900">
                  {isVip ? "Gia hạn thành công!" : "Chào mừng VIP mới!"}
                </p>
                <p className="mt-1 text-sm text-gray-500">
                  Quyền lợi <b>{currentPkg.name}</b> đã được kích hoạt.
                </p>
              </div>
            </div>
            <button
              onClick={() => toast.dismiss(t.id)}
              className="w-full border-t border-gray-100 p-3 flex justify-center text-sm font-bold text-indigo-600 hover:bg-gray-50 transition-colors"
            >
              Tuyệt vời
            </button>
          </div>
        ),
        { duration: 5000 },
      );

      router.refresh();
    } catch (error: any) {
      toast.dismiss(toastId);
      const msg =
        error.response?.data?.message ||
        error.response?.data ||
        "Giao dịch thất bại";

      toast.custom(
        (t) => (
          <div
            className={`${t.visible ? "animate-enter" : "animate-leave"} 
          max-w-md w-full bg-white shadow-xl rounded-xl border-l-4 border-red-500 flex ring-1 ring-black ring-opacity-5`}
          >
            <div className="flex-shrink-0 p-4">
              <svg
                className="h-6 w-6 text-red-500"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
            </div>
            <div className="p-4 pl-0 w-full">
              <h3 className="font-bold text-gray-900">Giao dịch thất bại</h3>
              <p className="text-sm text-gray-500 mt-1">
                {typeof msg === "string" ? msg : "Lỗi hệ thống"}
              </p>
            </div>
            <button
              onClick={() => toast.dismiss(t.id)}
              className="ml-auto p-4 text-gray-400 hover:text-gray-600"
            >
              ✕
            </button>
          </div>
        ),
        { duration: 5000 },
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 py-12 px-4 sm:px-6 lg:px-8 font-sans">
      <div className="max-w-5xl mx-auto">
        {/* --- NÚT QUAY LẠI --- */}
        <div className="mb-6">
          <button
            onClick={() => router.back()}
            className="flex items-center text-gray-500 hover:text-gray-800 transition-colors bg-white px-4 py-2 rounded-full shadow-sm hover:shadow-md"
          >
            <ArrowLeft size={20} className="mr-2" />
            <span className="font-medium">Quay lại</span>
          </button>
        </div>

        <div className="bg-white rounded-3xl shadow-2xl overflow-hidden flex flex-col md:flex-row min-h-[600px] transition-all hover:shadow-[0_20px_60px_rgba(0,0,0,0.15)]">
          {/* CỘT TRÁI: THÔNG TIN GÓI */}
          <div
            className={`w-full md:w-1/2 p-10 bg-gradient-to-br ${currentPkg.themeGradient} text-white flex flex-col justify-between relative overflow-hidden`}
          >
            <div className="absolute top-0 left-0 w-full h-full opacity-20 bg-[url('https://www.transparenttextures.com/patterns/cubes.png')]"></div>

            <div className="relative z-10">
              <div className="inline-block px-3 py-1 bg-white/20 rounded-full border border-white/30 mb-4 backdrop-blur-md">
                <span className="text-white text-xs font-bold tracking-wider uppercase">
                  Gói{" "}
                  {packageType === "CANDIDATE" ? "Ứng viên" : "Nhà tuyển dụng"}
                </span>
              </div>
              <h2 className="text-4xl font-extrabold mb-3 drop-shadow-md">
                {currentPkg.name}
              </h2>
              <p className="text-white/80 text-lg mb-8 leading-relaxed font-light">
                {currentPkg.description}
              </p>

              <div className="space-y-6">
                {currentPkg.features.map((feature, index) => (
                  <FeatureItem key={index} text={feature} />
                ))}
              </div>
            </div>

            <div className="relative z-10 mt-10 pt-8 border-t border-white/20">
              <p className="text-sm text-white/60 mb-1 font-medium">
                Giá niêm yết
              </p>
              <div className="flex items-baseline gap-2">
                <span className="text-5xl font-bold text-white tracking-tight">
                  {currentPkg.price}
                </span>
                <span className="text-white/60 font-medium">
                  {currentPkg.period}
                </span>
              </div>
            </div>
          </div>

          {/* CỘT PHẢI: HÀNH ĐỘNG */}
          <div className="w-full md:w-1/2 p-10 flex flex-col justify-center items-center text-center bg-white relative">
            {isVip ? (
              // --- TRƯỜNG HỢP: ĐANG LÀ VIP ---
              <div className="bg-green-50 p-8 rounded-2xl border border-green-100 w-full mb-8 shadow-sm transition-transform hover:scale-[1.02]">
                <div className="text-6xl mb-4 animate-bounce">👑</div>
                <h3 className="text-2xl font-bold text-green-800 mb-2">
                  Đang là thành viên VIP
                </h3>
                <p className="text-gray-600 mb-2 flex items-center justify-center gap-2">
                  <Clock className="w-5 h-5 text-green-600" />
                  Hết hạn vào:{" "}
                  <span className="font-bold text-gray-900 text-lg">
                    {expiryDate}
                  </span>
                </p>

                {/* Thanh tiến trình thực tế */}
                <div className="w-full bg-green-200 rounded-full h-2.5 mb-2 dark:bg-gray-700 overflow-hidden">
                  <div
                    className="bg-green-600 h-2.5 rounded-full transition-all duration-1000 ease-out"
                    style={{ width: `${progressPercent}%` }}
                  ></div>
                </div>

                <div className="flex justify-between text-xs font-semibold px-1">
                  <span className="text-green-600">
                    Còn lại {daysLeft > 0 ? daysLeft : 0} ngày
                  </span>
                  <span className="text-green-700">Trạng thái: Hoạt động</span>
                </div>
              </div>
            ) : (
              // --- TRƯỜNG HỢP: TÀI KHOẢN THƯỜNG ---
              <div className="bg-gray-50 p-8 rounded-2xl border border-gray-100 w-full mb-8 shadow-sm">
                <div className="text-6xl mb-4 transform transition hover:rotate-12">
                  {packageType === "CANDIDATE" ? "🚀" : "💎"}
                </div>
                <h3 className="text-xl font-bold text-gray-800 mb-2">
                  Tài khoản thường
                </h3>

                {/* Nếu có ngày hết hạn cũ (từng là VIP) thì hiện ra */}
                {user?.vipExpirationDate && (
                  <div className="mb-4 inline-flex items-center gap-2 bg-red-50 px-4 py-1.5 rounded-full border border-red-100">
                    <Clock className="w-4 h-4 text-red-500" />
                    <span className="text-sm text-red-600 font-medium">
                      Đã hết hạn vào: {expiryDate}
                    </span>
                  </div>
                )}

                <p className="text-gray-500 leading-relaxed">
                  Nâng cấp ngay để mở khóa toàn bộ quyền năng của hệ thống.
                </p>
              </div>
            )}

            <button
              onClick={handlePayment}
              disabled={loading}
              className={`w-full py-5 px-6 rounded-xl font-bold text-lg text-white shadow-xl transition-all transform hover:-translate-y-1 active:scale-95 bg-gradient-to-r ${currentPkg.buttonGradient} ${loading ? "opacity-70 cursor-not-allowed" : ""}`}
            >
              {loading ? (
                <span className="flex items-center justify-center gap-2">
                  <svg
                    className="animate-spin h-5 w-5 text-white"
                    fill="none"
                    viewBox="0 0 24 24"
                  >
                    <circle
                      className="opacity-25"
                      cx="12"
                      cy="12"
                      r="10"
                      stroke="currentColor"
                      strokeWidth="4"
                    ></circle>
                    <path
                      className="opacity-75"
                      fill="currentColor"
                      d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                    ></path>
                  </svg>
                  Đang xử lý...
                </span>
              ) : isVip ? (
                "GIA HẠN NGAY"
              ) : (
                "NÂNG CẤP NGAY"
              )}
            </button>

            <p className="mt-6 text-xs text-gray-400 max-w-xs mx-auto flex items-center justify-center gap-1">
              <svg
                className="w-3 h-3"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"
                ></path>
              </svg>
              Thanh toán an toàn (Demo Mode)
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

// Sub-component hiển thị quyền lợi
function FeatureItem({ text }: { text: string }) {
  return (
    <div className="flex items-start gap-3 group">
      <div className="flex-shrink-0 mt-1 w-6 h-6 rounded-full bg-white/20 text-yellow-300 flex items-center justify-center group-hover:bg-yellow-400 group-hover:text-black transition-colors duration-300">
        <svg
          className="w-4 h-4"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2.5"
            d="M5 13l4 4L19 7"
          ></path>
        </svg>
      </div>
      <span className="font-medium text-lg text-white/90 group-hover:text-white transition-colors duration-300">
        {text}
      </span>
    </div>
  );
}
