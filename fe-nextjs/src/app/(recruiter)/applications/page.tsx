"use client";

import { useEffect, useState } from "react";
import { recruitmentService } from "@/services/recruitmentService";
import {
  JobPosting as RecruiterJob,
  CandidateApplication as RecruiterApplication,
  ApplicationStatus,
  JobStatus,
} from "@/types/recruitment";

import CVAnalysisResult from "@/components/features/cv/CVAnalysisResult";
import PremiumFeatureLock from "@/components/common/PremiumFeatureLock";
// Import UseAuth để check quyền
import { useAuth } from "@/context/Authcontext";

import {
  Briefcase,
  Sparkles,
  CheckCircle,
  XCircle,
  Clock,
  ChevronRight,
  Download,
  X,
  FileText,
  Loader2,
  Filter,
  Mail,
  Phone,
  Eye,
  AlertCircle,
} from "lucide-react";
import toast from "react-hot-toast";
const getStatusLabel = (status: ApplicationStatus | JobStatus) => {
  switch (status) {
    // --- Application Status ---
    case ApplicationStatus.PENDING:
      return "Chờ duyệt";
    case ApplicationStatus.INTERVIEW:
      return "Phỏng vấn";
    case ApplicationStatus.OFFERED:
      return "Đã mời";
    case ApplicationStatus.HIRED:
      return "Đã tuyển";
    case ApplicationStatus.REJECTED:
      return "Đã loại";

    // --- Job Status (Thêm các case này để fix lỗi) ---
    // Lưu ý: Kiểm tra lại enum JobStatus của bạn để khớp case
    case JobStatus.PUBLISHED:
      return "Đang tuyển";
    case JobStatus.DRAFT:
      return "Bản nháp";
    case JobStatus.CLOSED:
      return "Đã đóng";

    default:
      return status;
  }
};

// 2. Cập nhật hàm getStatusBadgeColor tương tự
const getStatusBadgeColor = (status: ApplicationStatus | JobStatus) => {
  switch (status) {
    // --- Application Colors ---
    case ApplicationStatus.PENDING:
      return "bg-yellow-100 text-yellow-800";
    case ApplicationStatus.INTERVIEW:
      return "bg-blue-100 text-blue-800";
    case ApplicationStatus.HIRED:
      return "bg-green-100 text-green-800";
    case ApplicationStatus.REJECTED:
      return "bg-red-100 text-red-800";

    // --- Job Colors ---
    case JobStatus.PUBLISHED:
      return "bg-green-100 text-green-800";
    case JobStatus.DRAFT:
      return "bg-gray-100 text-gray-800";
    case JobStatus.CLOSED:
      return "bg-gray-200 text-gray-600";

    default:
      return "bg-gray-100 text-gray-800";
  }
};

const safeSplit = (input: any): string[] => {
  if (Array.isArray(input)) return input;
  if (typeof input === "string" && input.trim().length > 0) {
    return input
      .split(",")
      .map((s) => s.trim())
      .filter((s) => s !== "");
  }
  return [];
};

// --- MODAL AI ANALYSIS ---
const AnalysisModal = ({
  app,
  onClose,
}: {
  app: RecruiterApplication;
  onClose: any;
}) => {
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (app) {
      setLoading(true);
      recruitmentService
        .getApplicationAnalysis(app.id)
        .then((res: any) => {
          const mappedData = {
            ...res,
            matchPercentage: res.matchScore ?? res.matchPercentage ?? 0,
            evaluation: res.evaluation || "Chưa có đánh giá chi tiết.",
            matchedSkillsList: safeSplit(res.matchedSkillsList),
            missingSkillsList: safeSplit(res.missingSkillsList),
            otherHardSkillsList: safeSplit(res.otherHardSkillsList),
            otherSoftSkillsList: safeSplit(res.otherSoftSkillsList),
            recommendedSkillsList: safeSplit(res.recommendedSkillsList),
            matchedSkillsCount:
              res.matchedSkillsCount || safeSplit(res.matchedSkillsList).length,
            missingSkillsCount:
              res.missingSkillsCount || safeSplit(res.missingSkillsList).length,
            otherHardSkillsCount:
              res.otherHardSkillsCount ||
              safeSplit(res.otherHardSkillsList).length,
            otherSoftSkillsCount:
              res.otherSoftSkillsCount ||
              safeSplit(res.otherSoftSkillsList).length,
            recommendedSkillsCount:
              res.recommendedSkillsCount ||
              safeSplit(res.recommendedSkillsList).length,
            candidateName:
              res.candidateName || res.studentName || app.studentName,
            jobTitle: res.jobTitle || app.jobTitle,
          };
          setData(mappedData);
        })
        .catch((err) => {
          console.error("Lỗi lấy dữ liệu AI:", err);
          setData({
            matchPercentage: app.matchScore || 0,
            evaluation: app.aiEvaluation || "Chưa có đánh giá chi tiết.",
            missingSkillsList: safeSplit(app.missingSkillsList),
            matchedSkillsList: [],
            otherHardSkillsList: [],
            otherSoftSkillsList: [],
            recommendedSkillsList: [],
            matchedSkillsCount: 0,
            missingSkillsCount: safeSplit(app.missingSkillsList).length,
            otherHardSkillsCount: 0,
            otherSoftSkillsCount: 0,
            recommendedSkillsCount: 0,
            jobTitle: app.jobTitle,
            candidateName: app.studentName,
          });
        })
        .finally(() => setLoading(false));
    }
  }, [app]);

  if (!app) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-in fade-in">
      <div className="bg-white w-full max-w-5xl max-h-[90vh] rounded-2xl shadow-2xl flex flex-col overflow-hidden">
        <div className="flex justify-between items-center px-6 py-4 border-b border-gray-100 bg-white z-10">
          <h3 className="text-xl font-bold text-gray-800 flex items-center gap-2">
            <Sparkles className="text-purple-600" /> Phân tích AI Chi tiết
          </h3>
          <button onClick={onClose}>
            <X className="text-gray-400 hover:text-gray-600" />
          </button>
        </div>
        <div className="flex-1 overflow-y-auto p-6 bg-gray-50 custom-scrollbar">
          {loading ? (
            <div className="flex flex-col items-center justify-center py-20 text-gray-500">
              <Loader2 className="w-10 h-10 animate-spin text-purple-600 mb-3" />
              <p>Đang tải dữ liệu phân tích...</p>
            </div>
          ) : (
            <CVAnalysisResult result={data} isRecruiterView={true} />
          )}
        </div>
      </div>
    </div>
  );
};

// --- MODAL XEM CV & DUYỆT ---
const CVDetailModal = ({
  app,
  onClose,
  onUpdateStatus,
}: {
  app: RecruiterApplication;
  onClose: any;
  onUpdateStatus: any;
}) => {
  if (!app) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      <div className="bg-white w-full max-w-7xl h-[90vh] rounded-2xl shadow-2xl flex flex-col overflow-hidden">
        {/* Header Modal */}
        <div className="flex justify-between items-center px-6 py-4 border-b border-gray-200 bg-white">
          {/* ... Giữ nguyên phần Header ... */}
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center text-blue-700 font-bold text-lg">
              {app.studentName?.charAt(0).toUpperCase() || "U"}
            </div>
            <div>
              <h2 className="text-lg font-bold text-gray-800">
                {app.studentName || "Ứng viên"}
              </h2>
              <p className="text-sm text-gray-500">
                Ứng tuyển vị trí:{" "}
                <span className="font-medium text-gray-700">
                  {app.jobTitle}
                </span>
              </p>
            </div>
            <div
              className={`ml-4 px-3 py-1 rounded-full text-sm font-bold border flex items-center gap-1 ${
                (app.matchScore || 0) >= 50
                  ? "bg-green-50 text-green-700 border-green-200"
                  : "bg-yellow-50 text-yellow-700 border-yellow-200"
              }`}
            >
              <Sparkles size={14} /> {app.matchScore || 0}% Phù hợp
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-full transition"
          >
            <X size={24} />
          </button>
        </div>

        {/* Body: PDF Viewer */}
        <div className="flex-1 bg-gray-50 p-6 overflow-hidden flex flex-col">
          {app.cvUrl ? (
            <div className="w-full h-full flex flex-col">
              <div className="flex-1 bg-gray-200 rounded-xl border border-gray-300 overflow-hidden relative shadow-inner">
                <iframe
                  src={`https://docs.google.com/gview?url=${app.cvUrl}&embedded=true`}
                  className="w-full h-full absolute inset-0"
                ></iframe>
              </div>
              <div className="mt-3 flex justify-between items-center px-1">
                <p className="text-sm text-gray-500 italic">
                  * Nếu không thấy nội dung, hãy mở file gốc.
                </p>
                <a
                  href={app.cvUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="flex items-center gap-2 text-blue-600 hover:underline text-sm font-semibold transition bg-blue-50 px-3 py-1.5 rounded-lg hover:bg-blue-100"
                >
                  <Download size={16} /> Xem file gốc / Tải về ↗
                </a>
              </div>
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center h-full text-gray-400">
              <FileText size={48} className="mb-4 opacity-50" />
              <p>Ứng viên chưa cập nhật CV</p>
            </div>
          )}
        </div>

        {/* Footer: Action Bar */}
        <div className="px-6 py-4 bg-white border-t border-gray-200 flex justify-between items-center shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)]">
          <div className="text-sm text-gray-500">
            Trạng thái:{" "}
            {/* --- FIX 1: Dùng getStatusLabel thay vì app.status --- */}
            <span
              className={`font-bold ${
                app.status === ApplicationStatus.INTERVIEW ||
                app.status === ApplicationStatus.HIRED
                  ? "text-green-600"
                  : app.status === ApplicationStatus.REJECTED
                    ? "text-red-600"
                    : "text-yellow-600"
              }`}
            >
              {getStatusLabel(app.status)}
            </span>
          </div>
          <div className="flex gap-3">
            <button
              onClick={onClose}
              className="px-5 py-2.5 rounded-lg text-gray-600 font-medium hover:bg-gray-100 transition"
            >
              Đóng
            </button>

            {app.status === ApplicationStatus.PENDING && (
              <>
                <button
                  onClick={() =>
                    onUpdateStatus(app.id, ApplicationStatus.REJECTED)
                  }
                  className="px-5 py-2.5 rounded-lg border border-red-200 text-red-600 font-semibold hover:bg-red-50 flex items-center gap-2 transition"
                >
                  <XCircle size={18} /> Từ chối
                </button>
                <button
                  onClick={() =>
                    onUpdateStatus(app.id, ApplicationStatus.INTERVIEW)
                  }
                  className="px-6 py-2.5 rounded-lg bg-blue-600 text-white font-semibold hover:bg-blue-700 shadow-lg shadow-blue-200 flex items-center gap-2 transition transform hover:-translate-y-0.5"
                >
                  <CheckCircle size={18} /> Duyệt phỏng vấn
                </button>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default function ApplicationsPage() {
  const { user } = useAuth(); // Lấy user info
  const isVip = user?.userRole === "ADMIN" || user?.userRole?.includes("VIP");

  const [jobs, setJobs] = useState<RecruiterJob[]>([]);
  const [currentApplications, setCurrentApplications] = useState<
    RecruiterApplication[]
  >([]);
  const [selectedJobId, setSelectedJobId] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState<"all" | "matching">("all");
  const [loading, setLoading] = useState(false);
  const [viewCVApp, setViewCVApp] = useState<RecruiterApplication | null>(null);
  const [analyzeApp, setAnalyzeApp] = useState<RecruiterApplication | null>(
    null,
  );

  // 1. Fetch Danh sách Jobs
  useEffect(() => {
    const fetchJobs = async () => {
      try {
        const jobsData = await recruitmentService.getMyJobs();
        setJobs(jobsData || []);
        if (jobsData && jobsData.length > 0) {
          setSelectedJobId(jobsData[0].id);
        }
      } catch (error) {
        console.error("Lỗi lấy danh sách Job:", error);
      }
    };
    fetchJobs();
  }, []);

  // 2. Fetch Danh sách Hồ sơ
  useEffect(() => {
    if (!selectedJobId) return;
    const fetchApps = async () => {
      setLoading(true);
      try {
        const appsData =
          await recruitmentService.getApplicationsByJob(selectedJobId);
        setCurrentApplications(appsData || []);
      } catch (error) {
        console.error("Lỗi lấy hồ sơ:", error);
        setCurrentApplications([]);
      } finally {
        setLoading(false);
      }
    };
    fetchApps();
  }, [selectedJobId]);

  // Handle Update Status
  const handleStatusUpdate = async (
    id: number,
    newStatus: ApplicationStatus,
  ) => {
    const actionName =
      newStatus === ApplicationStatus.INTERVIEW ? "DUYỆT PHỎNG VẤN" : "TỪ CHỐI";
    if (!confirm(`Xác nhận ${actionName} hồ sơ này?`)) return;

    try {
      await recruitmentService.updateApplicationStatus(id, newStatus);
      setCurrentApplications((prev) =>
        prev.map((app) =>
          app.id === id ? { ...app, status: newStatus } : app,
        ),
      );
      if (viewCVApp && viewCVApp.id === id) {
        setViewCVApp(null);
      }
      toast.success("Cập nhật thành công!");
    } catch (e) {
      toast.error("Lỗi cập nhật trạng thái");
    }
  };

  // Logic lọc và sắp xếp
  const displayApplications = currentApplications
    .filter((app) => {
      if (activeTab === "matching") return (app.matchScore || 0) >= 50;
      return true;
    })
    .sort((a, b) => (b.matchScore || 0) - (a.matchScore || 0));

  const getScoreColor = (score: number) => {
    if (score >= 80) return "text-green-700 bg-green-50 border-green-200";
    if (score >= 50) return "text-blue-700 bg-blue-50 border-blue-200";
    return "text-gray-600 bg-gray-50 border-gray-200";
  };

  const ApplicationsList = () => (
    <div className="space-y-4">
      {displayApplications.map((app) => (
        <div
          key={app.id}
          className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm hover:shadow-md transition-all group"
        >
          <div className="flex items-start gap-5">
            <div className="w-14 h-14 rounded-full bg-gradient-to-br from-blue-50 to-blue-100 flex items-center justify-center text-blue-700 font-bold text-xl shrink-0 border border-blue-200">
              {app.studentName?.charAt(0).toUpperCase() || "U"}
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex justify-between items-start">
                <div>
                  <h3 className="text-lg font-bold text-gray-900 group-hover:text-blue-600 transition-colors">
                    {app.studentName}
                  </h3>
                  <div className="flex items-center gap-3 mt-2">
                    <div
                      className={`flex items-center gap-1 px-3 py-1 rounded-full text-xs font-bold border ${getScoreColor(app.matchScore || 0)}`}
                    >
                      <Sparkles className="w-3 h-3" /> {app.matchScore || 0}%
                    </div>
                    <span className="text-xs text-gray-400 flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      {app.appliedAt
                        ? new Date(app.appliedAt).toLocaleDateString("vi-VN")
                        : "N/A"}
                    </span>
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-medium ${getStatusBadgeColor(app.status)}`}
                    >
                      {getStatusLabel(app.status)}
                    </span>
                  </div>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => setAnalyzeApp(app)}
                    className="flex items-center gap-2 px-3 py-2 bg-purple-50 text-purple-700 rounded-lg text-sm font-semibold hover:bg-purple-100 border border-purple-200 transition"
                  >
                    <Sparkles size={16} /> AI Phân tích
                  </button>
                  <button
                    onClick={() => setViewCVApp(app)}
                    className="flex items-center gap-2 px-3 py-2 bg-white text-gray-700 rounded-lg text-sm font-semibold hover:bg-gray-50 border border-gray-300 transition shadow-sm"
                  >
                    <Eye size={16} /> Xem CV
                  </button>
                </div>
              </div>
              <div className="mt-3 bg-gray-50 rounded-lg p-3 text-sm border border-gray-100">
                <p className="text-gray-700 italic line-clamp-1">
                  <Sparkles className="w-3 h-3 inline mr-1 text-purple-500" />"
                  {app.aiEvaluation || "Chưa có nhận xét"}"
                </p>
                {app.missingSkillsList && (
                  <div className="flex items-start gap-2 mt-1 pt-1 border-t border-gray-200">
                    <AlertCircle className="w-3 h-3 text-red-500 mt-0.5 shrink-0" />
                    <p className="text-xs">
                      <span className="font-semibold text-gray-500 uppercase mr-1">
                        Kỹ năng thiếu:
                      </span>
                      <span className="text-gray-800 font-medium">
                        {app.missingSkillsList}
                      </span>
                    </p>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      ))}
    </div>
  );

  // --- LOGIC RENDER DANH SÁCH CHÍNH ---
  // Nếu đang tab Matching VÀ không phải VIP => Hiện khóa cứng
  const renderContent = () => {
    if (activeTab === "matching" && !isVip) {
      return (
        <PremiumFeatureLock
          title="Danh sách Top Ứng viên (AI)"
          description="Nâng cấp tài khoản VIP để xem danh sách ứng viên được AI sàng lọc và sắp xếp theo độ phù hợp."
        >
          {/* Div rỗng để chiếm chỗ, không render list thật để bảo mật */}
          <div className="h-64 w-full bg-gray-50/50 rounded-xl border border-dashed border-gray-200" />
        </PremiumFeatureLock>
      );
    }
    // Các trường hợp còn lại hiện list bình thường
    return <ApplicationsList />;
  };

  return (
    <div className="h-[calc(100vh-64px)] bg-gray-50 flex overflow-hidden">
      {viewCVApp && (
        <CVDetailModal
          app={viewCVApp}
          onClose={() => setViewCVApp(null)}
          onUpdateStatus={handleStatusUpdate}
        />
      )}
      {analyzeApp && (
        <AnalysisModal app={analyzeApp} onClose={() => setAnalyzeApp(null)} />
      )}

      <div className="w-1/3 min-w-[300px] max-w-[400px] bg-white border-r border-gray-200 flex flex-col">
        <div className="p-5 border-b border-gray-100">
          <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
            <Briefcase className="w-5 h-5 text-blue-600" /> Vị trí đang tuyển
          </h2>
          <p className="text-xs text-gray-500 mt-1">
            Chọn công việc để xem hồ sơ
          </p>
        </div>
        <div className="flex-1 overflow-y-auto p-3 space-y-2">
          {jobs.length === 0 && (
            <div className="text-center py-10 text-gray-400 text-sm">
              Chưa có tin tuyển dụng nào
            </div>
          )}
          {jobs.map((job) => (
            <button
              key={job.id}
              onClick={() => setSelectedJobId(job.id)}
              className={`w-full text-left p-4 rounded-lg border transition-all group ${
                selectedJobId === job.id
                  ? "bg-white border-blue-500 ring-1 ring-blue-500 shadow-md z-10"
                  : "bg-white border-gray-200 hover:border-blue-300 hover:shadow-sm"
              }`}
            >
              <div className="flex justify-between items-start mb-1">
                <h3
                  className={`font-semibold text-sm line-clamp-2 ${selectedJobId === job.id ? "text-blue-700" : "text-gray-900"}`}
                >
                  {job.title}
                </h3>
                {selectedJobId === job.id && (
                  <ChevronRight className="w-4 h-4 text-blue-500" />
                )}
              </div>
              <div className="flex items-center gap-2 text-xs text-gray-500 mt-2">
                {/* --- FIX 2: Sửa lỗi hiển thị Job Status ở sidebar --- */}
                <span
                  className={`px-2 py-0.5 rounded text-[10px] border ${getStatusBadgeColor(job.status)}`}
                >
                  {getStatusLabel(job.status)}
                </span>
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* RIGHT CONTENT */}
      <div className="flex-1 flex flex-col min-w-0 bg-gray-50/50">
        <div className="bg-white border-b border-gray-200 px-6 py-4 flex justify-between items-center shadow-sm z-10">
          <div>
            <h1 className="text-xl font-bold text-gray-900">
              {jobs.find((j) => j.id === selectedJobId)?.title ||
                "Chi tiết công việc"}
            </h1>
            {selectedJobId && (
              <div className="flex gap-4 mt-1 text-sm">
                <button
                  onClick={() => setActiveTab("all")}
                  className={`transition-colors ${
                    activeTab === "all"
                      ? "text-blue-600 font-bold border-b-2 border-blue-600"
                      : "text-gray-500"
                  }`}
                >
                  Tất cả ({currentApplications.length})
                </button>
                <div className="w-px h-4 bg-gray-300 my-auto"></div>
                <button
                  onClick={() => setActiveTab("matching")}
                  className={`flex items-center gap-1 transition-colors ${
                    activeTab === "matching"
                      ? "text-purple-600 font-bold border-b-2 border-purple-600"
                      : "text-gray-500"
                  }`}
                >
                  <Sparkles className="w-3.5 h-3.5" /> AI Đề xuất (
                  {
                    currentApplications.filter((a) => (a.matchScore || 0) >= 50)
                      .length
                  }
                  )
                </button>
              </div>
            )}
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          {loading ? (
            <div className="space-y-4">
              {[1, 2, 3].map((i) => (
                <div
                  key={i}
                  className="h-32 bg-white rounded-lg animate-pulse shadow-sm"
                />
              ))}
            </div>
          ) : !selectedJobId ? (
            <div className="h-full flex flex-col items-center justify-center text-gray-400">
              <Briefcase size={48} className="mb-4 opacity-20" />
              <p>Vui lòng chọn một công việc bên trái</p>
            </div>
          ) : displayApplications.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center text-gray-400">
              <Filter size={48} className="mb-4 opacity-20" />
              <p>Chưa có hồ sơ nào cho bộ lọc này</p>
            </div>
          ) : (
            <>
              {/* LOGIC KHÓA TÍNH NĂNG Ở ĐÂY */}
              {renderContent()}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
