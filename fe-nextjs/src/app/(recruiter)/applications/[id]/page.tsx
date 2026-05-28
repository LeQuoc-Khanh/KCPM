"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import {
  ArrowLeft,
  Mail,
  Phone,
  Calendar,
  Download,
  CheckCircle,
  XCircle,
  FileText,
  Briefcase,
  Sparkles,
} from "lucide-react";
import { recruitmentService } from "@/services/recruitmentService";
import { ApplicationStatus } from "@/types/recruitment";
// [NEW] Import component khóa tính năng
import PremiumFeatureLock from "@/components/common/PremiumFeatureLock";

// Helper convert list string
const parseSkillString = (str?: string | string[]) => {
  if (!str) return [];
  if (Array.isArray(str)) return str;
  return str
    .split(",")
    .map((s) => s.trim())
    .filter((s) => s.length > 0);
};

export default function ApplicationDetailPage() {
  const params = useParams();
  const id = Number(params.id);

  // State chứa dữ liệu gộp (Basic Info + AI Analysis)
  const [appDetail, setAppDetail] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);

  useEffect(() => {
    if (id) fetchDetail();
  }, [id]);

  const fetchDetail = async () => {
    setLoading(true);
    try {
      // Gọi song song 2 API
      const [basicInfo, aiAnalysis] = await Promise.allSettled([
        recruitmentService.getApplicationDetail(id),
        recruitmentService.getApplicationAnalysis(id),
      ]);

      let mergedData = {};

      // 1. Xử lý thông tin cơ bản (Bắt buộc phải có)
      if (basicInfo.status === "fulfilled" && basicInfo.value) {
        mergedData = { ...basicInfo.value };
      } else {
        console.error("Lỗi lấy thông tin cơ bản");
      }

      // 2. Xử lý thông tin AI (Có thể có hoặc chưa phân tích)
      if (aiAnalysis.status === "fulfilled" && aiAnalysis.value) {
        mergedData = {
          ...mergedData,
          // Ưu tiên lấy dữ liệu AI đè lên nếu có
          matchScore:
            aiAnalysis.value.matchPercentage ?? aiAnalysis.value.matchScore,
          aiEvaluation:
            aiAnalysis.value.evaluation || aiAnalysis.value.aiEvaluation,
          matchedSkillsList: aiAnalysis.value.matchedSkillsList,
          missingSkillsList: aiAnalysis.value.missingSkillsList,
        };
      }

      setAppDetail(mergedData);
    } catch (error) {
      console.error("Lỗi tải chi tiết:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleStatusChange = async (newStatus: ApplicationStatus) => {
    if (!confirm(`Xác nhận chuyển trạng thái sang: ${newStatus}?`)) return;
    setUpdating(true);
    try {
      await recruitmentService.updateApplicationStatus(id, newStatus);
      setAppDetail((prev: any) =>
        prev ? { ...prev, status: newStatus } : null,
      );
      alert("Cập nhật thành công!");
    } catch (error) {
      console.error("Lỗi cập nhật:", error);
      alert("Cập nhật thất bại.");
    } finally {
      setUpdating(false);
    }
  };

  if (loading)
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600"></div>
      </div>
    );

  if (!appDetail)
    return (
      <div className="p-12 text-center">
        <h3 className="text-xl text-gray-800 font-bold">
          Không tìm thấy dữ liệu
        </h3>
        <Link
          href="/dashboard-recruiter"
          className="text-blue-600 hover:underline mt-2 inline-block"
        >
          Quay lại danh sách
        </Link>
      </div>
    );

  // Xử lý dữ liệu hiển thị
  const matchedSkills = parseSkillString(appDetail.matchedSkillsList);
  const missingSkills = parseSkillString(appDetail.missingSkillsList);
  const status = appDetail.status || ApplicationStatus.PENDING;

  const studentName =
    appDetail.studentName || appDetail.candidateName || "Ứng viên";
  const jobTitle = appDetail.jobTitle || "Vị trí ứng tuyển";
  const appliedDate = appDetail.appliedAt || appDetail.createdAt;

  return (
    <div className="min-h-screen bg-gray-50 pb-10">
      {/* Header */}
      <div className="bg-white border-b px-6 py-4 shadow-sm sticky top-0 z-20">
        <Link
          href="/applications"
          className="text-gray-500 hover:text-blue-600 flex items-center mb-4 text-sm font-medium w-fit"
        >
          <ArrowLeft size={16} className="mr-1" /> Quay lại danh sách
        </Link>

        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 bg-gradient-to-br from-blue-100 to-blue-200 rounded-full flex items-center justify-center text-blue-700 font-bold text-2xl shadow-inner border border-blue-200">
              {studentName.charAt(0).toUpperCase()}
            </div>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">
                {studentName}
              </h1>
              <p className="text-gray-500 flex items-center text-sm mt-1">
                <Briefcase size={14} className="mr-1.5" /> Ứng tuyển:{" "}
                <span className="text-blue-600 font-semibold ml-1">
                  {jobTitle}
                </span>
              </p>
            </div>
          </div>

          <div className="flex flex-wrap gap-2">
            {/* Các nút thao tác */}
            {status === ApplicationStatus.PENDING && (
              <>
                <button
                  disabled={updating}
                  onClick={() => handleStatusChange(ApplicationStatus.REJECTED)}
                  className="px-4 py-2 border border-red-200 text-red-600 rounded-lg hover:bg-red-50 text-sm font-semibold flex items-center transition-colors"
                >
                  <XCircle size={18} className="mr-2" /> Từ chối
                </button>
                <button
                  disabled={updating}
                  onClick={() =>
                    handleStatusChange(ApplicationStatus.SCREENING)
                  }
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 text-sm font-semibold flex items-center shadow-sm transition-all hover:shadow"
                >
                  <CheckCircle size={18} className="mr-2" /> Duyệt hồ sơ
                </button>
              </>
            )}
            {status === ApplicationStatus.SCREENING && (
              <button
                disabled={updating}
                onClick={() => handleStatusChange(ApplicationStatus.INTERVIEW)}
                className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 text-sm font-semibold flex items-center shadow-md transition-all hover:-translate-y-0.5"
              >
                <Calendar size={18} className="mr-2" /> Mời phỏng vấn
              </button>
            )}

            {/* Badges trạng thái */}
            {status === ApplicationStatus.REJECTED && (
              <span className="px-4 py-2 bg-red-100 text-red-700 border border-red-200 rounded-lg font-bold flex items-center">
                <XCircle size={16} className="mr-2" /> Đã từ chối
              </span>
            )}
            {status === ApplicationStatus.HIRED && (
              <span className="px-4 py-2 bg-green-100 text-green-700 border border-green-200 rounded-lg font-bold flex items-center">
                <CheckCircle size={16} className="mr-2" /> Đã tuyển dụng
              </span>
            )}
            {status === ApplicationStatus.INTERVIEW && (
              <span className="px-4 py-2 bg-purple-100 text-purple-700 border border-purple-200 rounded-lg font-bold flex items-center">
                <Calendar size={16} className="mr-2" /> Chờ phỏng vấn
              </span>
            )}
          </div>
        </div>
      </div>

      <div className="container mx-auto px-4 md:px-6 mt-8 grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Cột trái: AI & CV */}
        <div className="lg:col-span-2 space-y-8">
          
          {/* --- AI Analysis Section: Đã được tích hợp Khóa VIP --- */}
          <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden">
            {/* Header: Luôn hiển thị Match Score để thu hút (Teaser) */}
            <div className="bg-gradient-to-r from-indigo-600 to-violet-600 px-6 py-4 flex justify-between items-center text-white">
              <h3 className="font-bold text-lg flex items-center gap-2">
                <Sparkles size={20} className="text-yellow-300" /> Phân tích AI
              </h3>
              <div className="bg-white/20 backdrop-blur-sm px-3 py-1 rounded-full text-sm font-bold border border-white/30">
                {appDetail.matchScore || 0}% Phù hợp
              </div>
            </div>

            <div className="p-6">
              {/* [LOCKED] Bọc nội dung chi tiết trong PremiumFeatureLock 
                  Component này sẽ tự check role: Nếu Recruiter thường -> Khóa; Nếu VIP/Admin -> Mở.
              */}
              <PremiumFeatureLock 
                title="Chi tiết Đánh giá AI" 
                description="Nâng cấp lên gói VIP để xem phân tích chi tiết điểm mạnh, điểm yếu và gợi ý phỏng vấn từ AI."
              >
                <div className="bg-indigo-50 p-4 rounded-xl border border-indigo-100 text-indigo-900 mb-6 text-sm leading-relaxed">
                  <span className="font-bold mr-1">💡 Đánh giá:</span>
                  {appDetail.aiEvaluation ||
                    appDetail.evaluation ||
                    "Đang chờ phân tích từ AI..."}
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  {/* Kỹ năng phù hợp */}
                  <div className="space-y-3">
                    <h4 className="text-sm font-bold text-gray-700 flex items-center uppercase tracking-wider">
                      <CheckCircle size={16} className="text-green-500 mr-2" />{" "}
                      Điểm mạnh
                    </h4>
                    <div className="flex flex-wrap gap-2">
                      {matchedSkills.length > 0 ? (
                        matchedSkills.map((s, i) => (
                          <span
                            key={i}
                            className="px-3 py-1.5 bg-green-50 text-green-700 border border-green-200 rounded-lg text-xs font-semibold"
                          >
                            {s}
                          </span>
                        ))
                      ) : (
                        <span className="text-gray-400 text-sm italic">
                          Chưa xác định
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Kỹ năng thiếu */}
                  <div className="space-y-3">
                    <h4 className="text-sm font-bold text-gray-700 flex items-center uppercase tracking-wider">
                      <XCircle size={16} className="text-red-500 mr-2" /> Cần cải
                      thiện
                    </h4>
                    <div className="flex flex-wrap gap-2">
                      {missingSkills.length > 0 ? (
                        missingSkills.map((s, i) => (
                          <span
                            key={i}
                            className="px-3 py-1.5 bg-red-50 text-red-700 border border-red-200 rounded-lg text-xs font-semibold"
                          >
                            {s}
                          </span>
                        ))
                      ) : (
                        <span className="text-gray-400 text-sm italic">
                          Không có
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              </PremiumFeatureLock>
            </div>
          </div>

          {/* CV Viewer */}
          <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden flex flex-col h-[800px]">
            <div className="px-6 py-4 border-b border-gray-100 flex justify-between items-center bg-gray-50/50">
              <h3 className="font-bold text-gray-800 flex items-center">
                <FileText size={18} className="mr-2 text-blue-600" /> Xem trước
                CV
              </h3>
              {appDetail.cvUrl && (
                <a
                  href={appDetail.cvUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 hover:text-blue-600 hover:border-blue-300 transition text-sm font-medium shadow-sm"
                >
                  <Download size={16} /> Tải xuống gốc
                </a>
              )}
            </div>
            <div className="flex-1 bg-gray-100 relative">
              {appDetail.cvUrl ? (
                <iframe
                  src={`https://docs.google.com/gview?url=${appDetail.cvUrl}&embedded=true`}
                  className="w-full h-full absolute inset-0"
                  title="CV Preview"
                ></iframe>
              ) : (
                <div className="flex flex-col items-center justify-center h-full text-gray-400">
                  <FileText size={48} className="mb-4 opacity-20" />
                  <p>Ứng viên chưa cập nhật CV hoặc đường dẫn bị lỗi</p>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Cột phải: Sidebar thông tin */}
        <div className="space-y-6">
          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-200 sticky top-24">
            <h3 className="font-bold text-gray-900 mb-6 pb-2 border-b border-gray-100 text-lg">
              Thông tin liên hệ
            </h3>
            <div className="space-y-5">
              <div className="flex items-start group">
                <div className="p-2 bg-blue-50 text-blue-600 rounded-lg mr-4 group-hover:bg-blue-100 transition-colors">
                  <Mail size={20} />
                </div>
                <div>
                  <p className="text-xs text-gray-500 uppercase font-bold tracking-wide mb-0.5">
                    Email
                  </p>
                  <a
                    href={`mailto:${appDetail.email}`}
                    className="text-gray-900 font-medium hover:text-blue-600 hover:underline text-sm break-all"
                  >
                    {appDetail.email || "---"}
                  </a>
                </div>
              </div>

              <div className="flex items-start group">
                <div className="p-2 bg-green-50 text-green-600 rounded-lg mr-4 group-hover:bg-green-100 transition-colors">
                  <Phone size={20} />
                </div>
                <div>
                  <p className="text-xs text-gray-500 uppercase font-bold tracking-wide mb-0.5">
                    Số điện thoại
                  </p>
                  <p className="text-gray-900 font-medium text-sm">
                    {appDetail.phone || "---"}
                  </p>
                </div>
              </div>

              <div className="flex items-start group">
                <div className="p-2 bg-orange-50 text-orange-600 rounded-lg mr-4 group-hover:bg-orange-100 transition-colors">
                  <Calendar size={20} />
                </div>
                <div>
                  <p className="text-xs text-gray-500 uppercase font-bold tracking-wide mb-0.5">
                    Ngày ứng tuyển
                  </p>
                  <p className="text-gray-900 font-medium text-sm">
                    {appliedDate
                      ? new Date(appliedDate).toLocaleDateString("vi-VN")
                      : "---"}
                  </p>
                </div>
              </div>
            </div>

            <div className="mt-8 pt-6 border-t border-gray-100">
              <h3 className="font-bold text-gray-900 mb-3 text-sm">
                Ghi chú nội bộ
              </h3>
              <div className="bg-yellow-50 p-4 rounded-xl border border-yellow-100 text-sm text-gray-700 min-h-[100px] italic">
                {appDetail.recruiterNote ||
                  "Chưa có ghi chú nào cho ứng viên này."}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}