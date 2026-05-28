"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Loader2 } from "lucide-react";
import CVAnalysisResult from "@/components/features/cv/CVAnalysisResult"; 
import { getJobAnalysisResult } from "@/services/candidateService"; 
import { useAuth } from "@/context/Authcontext"; //
import PremiumFeatureLock from "@/components/common/PremiumFeatureLock"; //

export default function JobAnalysisPage() {
  const params = useParams();
  const router = useRouter();
  const jobId = params.jodId;
  
  const { user } = useAuth(); // Lấy thông tin người dùng từ context
  const [analysisData, setAnalysisData] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  // Logic kiểm tra quyền VIP: Admin hoặc Role có chứa đuôi _VIP
  const isVip = user?.userRole === 'ADMIN' || user?.userRole?.includes('_VIP');

  useEffect(() => {
    const fetchAnalysis = async () => {
      try {
        setLoading(true);
        // Gọi API lấy kết quả phân tích
        const data = await getJobAnalysisResult(Number(jobId));
        setAnalysisData(data);
      } catch (error) {
        console.error("Lỗi tải phân tích:", error);
      } finally {
        setLoading(false);
      }
    };

    if (jobId) {
      fetchAnalysis();
    }
  }, [jobId]);

  return (
    <div className="min-h-screen bg-gray-50 pb-10">
      {/* Header điều hướng */}
      <div className="bg-white border-b sticky top-0 z-10">
        <div className="max-w-5xl mx-auto px-4 h-16 flex items-center">
          <button
            onClick={() => router.back()}
            className="flex items-center gap-2 text-gray-600 hover:text-blue-600 transition-colors"
          >
            <ArrowLeft size={20} />
            <span>Quay lại tìm việc</span>
          </button>
        </div>
      </div>

      <div className="max-w-5xl mx-auto px-4 mt-8">
        {loading ? (
          <div className="flex flex-col items-center justify-center py-20">
            <Loader2 className="animate-spin text-blue-600 mb-4" size={48} />
            <p className="text-gray-500 text-lg">
              AI đang đọc kỹ JD và CV của bạn...
            </p>
            <p className="text-gray-400 text-sm">
              Quá trình này có thể mất 5-10 giây.
            </p>
          </div>
        ) : (
          <div className="space-y-6">
            {/* 1. Phần tiêu đề Job - Luôn hiển thị cho tất cả user */}
            <div className="bg-gradient-to-r from-purple-600 to-indigo-600 text-white p-8 rounded-2xl shadow-lg">
              <h1 className="text-3xl font-bold mb-2">
                Báo cáo mức độ phù hợp
              </h1>
              <p className="opacity-90">
                Phân tích chuyên sâu cho vị trí:{" "}
                <span className="font-bold text-yellow-300">
                  {analysisData?.jobTitle}
                </span>
              </p>
            </div>

            {/* 2. Component Kết quả - Phân biệt quyền VIP */}
            {isVip ? (
              // Nếu là VIP: Hiển thị giao diện gốc không bị giới hạn
              <CVAnalysisResult result={analysisData} />
            ) : (
              // Nếu là tài khoản thường: Bọc bằng lớp khóa PremiumFeatureLock
              <PremiumFeatureLock 
                title="Báo cáo phân tích AI" 
                description="Tính năng phân tích CV chuyên sâu yêu cầu tài khoản VIP để mở khóa toàn bộ điểm số và gợi ý từ AI."
              >
                <CVAnalysisResult result={analysisData} />
              </PremiumFeatureLock>
            )}
          </div>
        )}
      </div>
    </div>
  );
}