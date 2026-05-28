"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { getMyApplications } from "@/services/candidateService";
import { interviewService } from "@/services/interviewService";
import { InterviewSession } from "@/types/interview";
import toast from "react-hot-toast";
import {
  Briefcase,
  Clock,
  CheckCircle,
  Play,
  History,
  ChevronRight,
  Search,
} from "lucide-react";

// Interface Application giữ nguyên
interface Application {
  id: number;
  jobId: number;
  jobTitle: string;
  companyName: string;
  studentId: number;
  studentName: string;
  cvUrl: string;
  status: string;
  appliedAt: string;
  recruiterNote?: string;
  matchScore?: number;
  aiEvaluation?: string;
}

export default function InterviewHubPage() {
  const router = useRouter();

  // State
  const [applications, setApplications] = useState<Application[]>([]);
  const [selectedApp, setSelectedApp] = useState<Application | null>(null);
  const [history, setHistory] = useState<InterviewSession[]>([]);
  const [loadingApps, setLoadingApps] = useState(true);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");

  // 1. Tải danh sách đơn ứng tuyển khi vào trang
  useEffect(() => {
    const fetchApps = async () => {
      try {
        setLoadingApps(true);
        const data = await getMyApplications();
        setApplications(data);

        // Tự động chọn job đầu tiên nếu có
        if (data && data.length > 0) {
          handleSelectApp(data[0]);
        }
      } catch (error) {
        console.error(error);
        toast.error("Lỗi tải danh sách đơn ứng tuyển");
      } finally {
        setLoadingApps(false);
      }
    };
    fetchApps();
  }, []);

  // 2. Hàm xử lý khi chọn 1 Application
  const handleSelectApp = async (app: Application) => {
    setSelectedApp(app);
    setLoadingHistory(true);
    try {
      const historyData = await interviewService.getHistory(app.jobId);
      console.log("historyData", historyData);

      // Sắp xếp mới nhất lên đầu
      if (Array.isArray(historyData)) {
        setHistory(
          // Thêm type any cho a, b để tránh lỗi TS nếu historyData chưa rõ type
          historyData.sort(
            (a: any, b: any) =>
              new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
          ),
        );
      } else {
        setHistory([]);
      }
    } catch (error) {
      console.error(error);
      setHistory([]);
    } finally {
      setLoadingHistory(false);
    }
  };

  // 3. Hàm chuyển trang sang phòng chat
  const handleStartInterview = () => {
    if (selectedApp) {
      router.push(`/interview/${selectedApp.jobId}`);
    }
  };

  // Filter list theo search
  const filteredApps = applications.filter(
    (app) =>
      (app.jobTitle &&
        app.jobTitle.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (app.companyName &&
        app.companyName.toLowerCase().includes(searchTerm.toLowerCase())),
  );

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-7xl mx-auto">
        <h1 className="text-2xl font-bold text-gray-800 mb-6 flex items-center gap-2">
          <Briefcase className="text-blue-600" />
          Trung tâm Luyện tập Phỏng vấn
        </h1>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 h-[calc(100vh-150px)]">
          {/* --- CỘT TRÁI: DANH SÁCH APPLICATION --- */}
          <div className="lg:col-span-4 bg-white rounded-xl shadow-sm border border-gray-200 flex flex-col overflow-hidden">
            {/* Search Box */}
            <div className="p-4 border-b border-gray-100">
              <div className="relative">
                <Search
                  className="absolute left-3 top-3 text-gray-400"
                  size={18}
                />
                <input
                  type="text"
                  placeholder="Tìm công việc..."
                  className="w-full pl-10 pr-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
            </div>

            {/* List */}
            <div className="flex-1 overflow-y-auto p-2 space-y-2">
              {loadingApps ? (
                <div className="text-center py-10 text-gray-500">
                  Đang tải...
                </div>
              ) : filteredApps.length === 0 ? (
                <div className="text-center py-10 text-gray-400 text-sm">
                  Chưa có đơn ứng tuyển nào.
                </div>
              ) : (
                filteredApps.map((app) => (
                  <div
                    key={app.id}
                    onClick={() => handleSelectApp(app)}
                    className={`p-4 rounded-lg cursor-pointer transition-all border ${
                      selectedApp?.id === app.id
                        ? "bg-blue-50 border-blue-500 shadow-sm"
                        : "bg-white border-transparent hover:bg-gray-50 hover:border-gray-200"
                    }`}
                  >
                    <div className="flex justify-between items-start mb-1">
                      <h3
                        className={`font-bold text-sm line-clamp-1 ${selectedApp?.id === app.id ? "text-blue-700" : "text-gray-800"}`}
                      >
                        {app.jobTitle}
                      </h3>
                      {selectedApp?.id === app.id && (
                        <ChevronRight size={16} className="text-blue-500" />
                      )}
                    </div>
                    <p className="text-xs text-gray-500 mb-2">
                      {app.companyName}
                    </p>
                    <span
                      className={`text-[10px] px-2 py-0.5 rounded-full font-medium ${
                        app.status === "PENDING"
                          ? "bg-yellow-100 text-yellow-700"
                          : app.status === "ACCEPTED"
                            ? "bg-green-100 text-green-700"
                            : "bg-gray-100 text-gray-600"
                      }`}
                    >
                      {app.status}
                    </span>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* --- CỘT PHẢI: CHI TIẾT & LỊCH SỬ --- */}
          <div className="lg:col-span-8 flex flex-col gap-6">
            {selectedApp ? (
              <>
                {/* 1. Header Job & Action Button */}
                <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200 flex flex-col md:flex-row justify-between items-center gap-4">
                  <div>
                    <h2 className="text-xl font-bold text-gray-900">
                      {selectedApp.jobTitle}
                    </h2>
                    <p className="text-gray-500">{selectedApp.companyName}</p>
                  </div>
                  <button
                    onClick={handleStartInterview}
                    className="flex items-center gap-2 bg-blue-600 text-white px-6 py-3 rounded-lg font-bold hover:bg-blue-700 transition shadow-lg shadow-blue-200 active:scale-95"
                  >
                    <Play size={20} fill="currentColor" />
                    Bắt đầu Mô phỏng mới
                  </button>
                </div>

                {/* 2. History List */}
                <div className="flex-1 bg-white rounded-xl shadow-sm border border-gray-200 flex flex-col overflow-hidden">
                  <div className="p-4 border-b border-gray-100 flex items-center gap-2">
                    <History className="text-gray-400" />
                    <h3 className="font-bold text-gray-700">
                      Lịch sử Luyện tập
                    </h3>
                  </div>

                  <div className="flex-1 overflow-y-auto p-0">
                    {loadingHistory ? (
                      <div className="text-center py-12 text-gray-400">
                        Đang tải lịch sử...
                      </div>
                    ) : history.length === 0 ? (
                      <div className="flex flex-col items-center justify-center h-full py-12 text-gray-400">
                        <div className="bg-gray-50 p-4 rounded-full mb-3">
                          <Briefcase size={32} className="text-gray-300" />
                        </div>
                        <p>Bạn chưa luyện tập phỏng vấn cho công việc này.</p>
                        <p className="text-sm">
                          Hãy ấn nút "Bắt đầu" để thử sức ngay!
                        </p>
                      </div>
                    ) : (
                      <table className="w-full text-left border-collapse">
                        <thead className="bg-gray-50 sticky top-0 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                          <tr>
                            <th className="px-6 py-3">Ngày thực hiện</th>
                            <th className="px-6 py-3 text-center">Điểm số</th>
                            <th className="px-6 py-3">Trạng thái</th>
                            <th className="px-6 py-3">Nhận xét nhanh</th>
                            <th className="px-6 py-3"></th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100 text-sm">
                          {history.map((session) => (
                            <tr
                              key={session.id}
                              className="hover:bg-gray-50 transition"
                            >
                              <td className="px-6 py-4 text-gray-600">
                                {new Date(session.createdAt).toLocaleDateString(
                                  "vi-VN",
                                )}{" "}
                                <br />
                                <span className="text-xs text-gray-400">
                                  {new Date(
                                    session.createdAt,
                                  ).toLocaleTimeString("vi-VN", {
                                    hour: "2-digit",
                                    minute: "2-digit",
                                  })}
                                </span>
                              </td>
                              <td className="px-6 py-4 text-center">
                                <span
                                  className={`inline-block w-10 h-10 leading-10 rounded-full font-bold text-xs ${
                                    !session.score
                                      ? "bg-gray-100 text-gray-400"
                                      : session.score >= 8
                                        ? "bg-green-100 text-green-700"
                                        : session.score >= 5
                                          ? "bg-yellow-100 text-yellow-700"
                                          : "bg-red-100 text-red-700"
                                  }`}
                                >
                                  {session.score ?? "-"}
                                </span>
                              </td>
                              <td className="px-6 py-4">
                                {session.status === "COMPLETED" ? (
                                  <span className="flex items-center gap-1 text-green-600 text-xs font-medium">
                                    <CheckCircle size={14} /> Hoàn thành
                                  </span>
                                ) : (
                                  <span className="flex items-center gap-1 text-blue-600 text-xs font-medium">
                                    <Clock size={14} /> Đang diễn ra
                                  </span>
                                )}
                              </td>
                              <td
                                className="px-6 py-4 max-w-xs truncate text-gray-500"
                                title={session.feedback || ""}
                              >
                                {session.feedback || "Chưa có nhận xét"}
                              </td>
                              <td className="px-6 py-4 text-right">
                                <button
                                  onClick={() => {
                                    if (!session.id) {
                                      toast.error(
                                        "Phiên phỏng vấn này bị lỗi ID",
                                      );
                                      return;
                                    }
                                    router.push(
                                      `/interview/${selectedApp.jobId}?sessionId=${session.id}`,
                                    );
                                  }}
                                  className="text-blue-600 hover:underline text-xs"
                                >
                                  Xem chi tiết
                                </button>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    )}
                  </div>
                </div>
              </>
            ) : (
              <div className="h-full flex flex-col items-center justify-center bg-white rounded-xl border border-dashed border-gray-300 text-gray-400">
                <Briefcase size={48} className="mb-4 opacity-20" />
                <p className="text-lg">
                  Chọn một công việc bên trái để xem chi tiết
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
