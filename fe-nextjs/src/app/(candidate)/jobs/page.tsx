"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/context/Authcontext";
import * as candidateService from "@/services/candidateService";
import { useRouter, useSearchParams } from "next/navigation";
import ApplyModal from "@/components/features/jobs/ApplyModal";
import {
  Search,
  MapPin,
  DollarSign,
  Briefcase,
  ArrowRight,
  Sparkles,
  AlertCircle,
  Building2,
  List,
  FileText,
  ListChecks,
} from "lucide-react";
import Link from "next/link";

export default function JobsPage() {
  const { user } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();

  // Mặc định là 'all' (Tất cả việc làm)
  const [activeTab, setActiveTab] = useState<"all" | "matching">("all");

  const [jobs, setJobs] = useState<any[]>([]);
  const [filteredJobs, setFilteredJobs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedJob, setSelectedJob] = useState<{
    id: number;
    title: string;
    company: string;
  } | null>(null);
  const [appliedJobIds, setAppliedJobIds] = useState<number[]>([]);

  // Logic bắt URL
  useEffect(() => {
    const mode = searchParams.get("mode");
    if (mode === "matching") {
      setActiveTab("matching");
    } else {
      setActiveTab("all");
    }
  }, [searchParams]);

  useEffect(() => {
    if (user?.id) {
      fetchJobs();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, activeTab]);

  useEffect(() => {
    if (searchTerm.trim() === "") {
      setFilteredJobs(jobs);
    } else {
      const lowerTerm = searchTerm.toLowerCase();
      const filtered = jobs.filter(
        (job) =>
          job.title?.toLowerCase().includes(lowerTerm) ||
          job.company?.toLowerCase().includes(lowerTerm),
      );
      setFilteredJobs(filtered);
    }
  }, [searchTerm, jobs]);

  const fetchJobs = async () => {
    try {
      setLoading(true);
      let data = [];

      if (activeTab === "matching") {
        // Tab phụ: API matching đã có sẵn điểm
        data = await candidateService.getMatchingJobs();
      } else {
        // Tab chính: Lấy tất cả job
        if (typeof candidateService.getAllJobs === "function") {
          data = await candidateService.getAllJobs();
        } else {
          // Fallback nếu chưa có getAllJobs
          data = await candidateService.getRecentJobs();
        }
      }

      // [UPDATE] TÍNH ĐIỂM CHO TAB 'ALL'
      // Nếu đang ở tab 'all' và có dữ liệu, gọi thêm API tính điểm để hiển thị % phù hợp
      if (activeTab === "all" && Array.isArray(data) && data.length > 0) {
        try {
          // Lấy danh sách ID
          const jobIds = data.map((j: any) => j.id);

          // Kiểm tra hàm getBatchScores có tồn tại không
          if (typeof candidateService.getBatchScores === "function") {
            const scoresMap = await candidateService.getBatchScores(jobIds);

            // Merge điểm vào danh sách job
            data = data.map((job: any) => {
              const scoreData = scoresMap[job.id];

              // Xử lý an toàn: API có thể trả về số (80) hoặc object ({matchScore: 80})
              let matchScore = 0;
              let skillsFound = [];

              if (typeof scoreData === "number") {
                matchScore = scoreData;
              } else if (scoreData && typeof scoreData === "object") {
                matchScore = scoreData.matchScore || 0;
                skillsFound = scoreData.matchedSkills || [];
              }

              return {
                ...job,
                matchScore,
                // Nếu API batch trả về skillsFound thì dùng, không thì giữ nguyên
                skillsFound:
                  skillsFound.length > 0 ? skillsFound : job.skillsFound,
              };
            });
          }
        } catch (err) {
          console.error("Lỗi tính điểm batch (không ảnh hưởng hiển thị):", err);
          // Vẫn hiển thị job bình thường dù lỗi tính điểm
        }
      }

      const safeData = Array.isArray(data) ? data : [];

      // Sắp xếp:
      // - Matching: Điểm cao lên đầu
      // - All: Có thể giữ nguyên (thường là mới nhất) hoặc cũng đưa điểm cao lên đầu
      if (activeTab === "matching") {
        safeData.sort(
          (a: any, b: any) => (b.matchScore || 0) - (a.matchScore || 0),
        );
      }

      setJobs(safeData);
      setFilteredJobs(safeData);
    } catch (error) {
      console.error("Lỗi tải việc làm:", error);
      setJobs([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchAppliedStatus = async () => {
    if (!user?.id) return;
    try {
      const myApps = await candidateService.getMyApplications();
      // Map ra danh sách ID từ response
      const ids = Array.isArray(myApps)
        ? myApps.map((app: any) => app.job?.id || app.jobId || app.id)
        : [];
      setAppliedJobIds(ids);
    } catch (error) {
      console.error("Lỗi lấy trạng thái ứng tuyển", error);
    }
  };

  // Gọi hàm này trong useEffect, cùng lúc với fetchJobs
  useEffect(() => {
    if (user?.id) {
      fetchJobs();
      fetchAppliedStatus();
    }
  }, [user, activeTab]);

/*************  ✨ Windsurf Command ⭐  *************/
/**
 * Set selected job to open ApplyModal
 * @param {any} job - Job object to be opened in ApplyModal
 */
/*******  db8d8496-d194-432f-8530-195bc9a5b4b4  *******/
  const handleApply = (job: any) => {
    setSelectedJob({
      id: job.id,
      title: job.title,
      company: job.company || "Công ty ẩn danh",
    });
  };

  const handleTabChange = (tab: "all" | "matching") => {
    setActiveTab(tab);
    setSearchTerm("");
    router.replace(tab === "all" ? "/jobs" : `/jobs?mode=${tab}`);
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header Section */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">
            {activeTab === "all" ? "Tất cả việc làm" : "Việc làm phù hợp"}
          </h1>
          <p className="mt-2 text-gray-600">
            {activeTab === "all"
              ? "Khám phá tất cả các cơ hội nghề nghiệp đang mở tuyển."
              : "Danh sách các công việc được AI đề xuất dựa trên hồ sơ của bạn."}
          </p>
        </div>

        {/* TABS SWITCHER */}
        <div className="flex gap-4 mb-6 border-b border-gray-200">
          <button
            onClick={() => handleTabChange("all")}
            className={`pb-3 px-1 flex items-center gap-2 font-medium text-sm transition-colors border-b-2 ${
              activeTab === "all"
                ? "border-blue-600 text-blue-600"
                : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
            }`}
          >
            <List className="h-4 w-4" />
            Tất cả việc làm
          </button>

          <button
            onClick={() => handleTabChange("matching")}
            className={`pb-3 px-1 flex items-center gap-2 font-medium text-sm transition-colors border-b-2 ${
              activeTab === "matching"
                ? "border-blue-600 text-blue-600"
                : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
            }`}
          >
            <Sparkles className="h-4 w-4" />
            Gợi ý cho bạn (AI)
          </button>
        </div>

        {/* SearchContent Section */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-8">
          <div className="relative">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 h-5 w-5" />
            <input
              type="text"
              placeholder="Tìm kiếm theo tên công việc, công ty..."
              className="w-full pl-12 pr-4 py-3 bg-gray-50 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition-all"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>

        {/* Job List Section */}
        {loading ? (
          <div className="grid grid-cols-1 gap-6">
            {[1, 2, 3].map((i) => (
              <div
                key={i}
                className="bg-white p-6 rounded-xl shadow-sm animate-pulse h-48"
              ></div>
            ))}
          </div>
        ) : filteredJobs.length > 0 ? (
          <div className="grid grid-cols-1 gap-6">
            {filteredJobs.map((job) => (
              <div
                key={job.id}
                className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 hover:shadow-md transition-shadow"
              >
                <div className="flex flex-col md:flex-row justify-between gap-6">
                  {/* Job Info */}
                  <div className="flex-1">
                    <div className="flex items-start justify-between mb-2">
                      <div>
                        <Link href={`/jobs/${job.id}`}>
                          <h3 className="text-xl font-bold text-gray-900 hover:text-blue-600 transition-colors cursor-pointer">
                            {job.title}
                          </h3>
                        </Link>
                        <div className="flex items-center gap-2 text-gray-600 mt-1">
                          <Building2 className="h-4 w-4" />
                          <span className="font-medium">{job.company}</span>
                        </div>
                      </div>

                      {/* [ĐÃ SỬA] Luôn hiện Match Score Badge */}
                      <div className="flex flex-col items-end">
                        <div
                          className={`flex items-center gap-1.5 px-3 py-1 rounded-full text-sm font-semibold border ${
                            (job.matchScore || 0) >= 50
                              ? "bg-green-50 text-green-700 border-green-100" // Cao: Xanh lá
                              : (job.matchScore || 0) > 0
                                ? "bg-blue-50 text-blue-700 border-blue-100" // Có điểm: Xanh dương
                                : "bg-gray-100 text-gray-500 border-gray-200" // 0 điểm: Xám
                          }`}
                        >
                          <Sparkles
                            className={`h-4 w-4 ${
                              (job.matchScore || 0) >= 50
                                ? "fill-green-700"
                                : ""
                            }`}
                          />
                          {job.matchScore || 0}% Phù hợp
                        </div>
                      </div>
                    </div>

                    <div className="flex flex-wrap gap-4 mt-4 text-sm text-gray-600">
                      <div className="flex items-center gap-1.5 bg-gray-50 px-3 py-1.5 rounded-lg border border-gray-100">
                        <MapPin className="h-4 w-4 text-gray-500" />
                        {job.location || "Remote"}
                      </div>
                      <div className="flex items-center gap-1.5 bg-gray-50 px-3 py-1.5 rounded-lg border border-gray-100">
                        <DollarSign className="h-4 w-4 text-gray-500" />
                        {job.salary || "Thỏa thuận"}
                      </div>
                      <div className="flex items-center gap-1.5 bg-gray-50 px-3 py-1.5 rounded-lg border border-gray-100">
                        <Briefcase className="h-4 w-4 text-gray-500" />
                        {job.jobType || "Toàn thời gian"}
                      </div>
                    </div>

                    {/* MÔ TẢ & YÊU CẦU */}
                    <div className="space-y-3 mt-5 border-t border-gray-100 pt-4">
                      {job.description && (
                        <div className="flex gap-3 items-start">
                          <FileText
                            size={16}
                            className="mt-0.5 text-blue-500 shrink-0"
                          />
                          <div>
                            <span className="text-xs font-bold text-gray-500 uppercase block mb-1">
                              Mô tả:
                            </span>
                            <p className="text-sm text-gray-700 line-clamp-2 leading-relaxed">
                              {job.description}
                            </p>
                          </div>
                        </div>
                      )}

                      {job.requirements && (
                        <div className="flex gap-3 items-start">
                          <ListChecks
                            size={16}
                            className="mt-0.5 text-orange-500 shrink-0"
                          />
                          <div>
                            <span className="text-xs font-bold text-gray-500 uppercase block mb-1">
                              Yêu cầu:
                            </span>
                            <p className="text-sm text-gray-700 line-clamp-2 leading-relaxed">
                              {job.requirements}
                            </p>
                          </div>
                        </div>
                      )}
                    </div>

                    {/* Matching Skills - Chỉ hiện nếu có dữ liệu */}
                    {job.skillsFound && job.skillsFound.length > 0 && (
                      <div className="mt-5">
                        <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">
                          Kỹ năng phù hợp:
                        </span>
                        <div className="flex flex-wrap gap-2 mt-2">
                          {job.skillsFound
                            .slice(0, 5)
                            .map((skill: string, idx: number) => (
                              <span
                                key={idx}
                                className="px-2.5 py-1 bg-blue-50 text-blue-700 text-xs rounded-md font-medium border border-blue-100"
                              >
                                {skill}
                              </span>
                            ))}
                          {job.skillsFound.length > 5 && (
                            <span className="px-2.5 py-1 bg-gray-100 text-gray-600 text-xs rounded-md border border-gray-200">
                              +{job.skillsFound.length - 5}
                            </span>
                          )}
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex flex-col gap-3 min-w-[200px] border-l border-gray-100 pl-6 md:justify-center">
                    <button
                      onClick={() => router.push(`/jobs/${job.id}`)}
                      className="w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-blue-50 text-blue-700 rounded-lg font-medium hover:bg-blue-100 transition-colors border border-blue-100">
                      <FileText className="h-4 w-4" />
                      Xem chi tiết
                    </button>
                    <button
                      onClick={() => router.push(`/cv-analysis/${job.id}`)}
                      className="w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-purple-50 text-purple-700 rounded-lg font-medium hover:bg-purple-100 transition-colors border border-purple-100">
                      <Sparkles className="h-4 w-4" />
                      AI Phân tích
                    </button>

                    <button
                      onClick={() => router.push(`/interview/${job.id}`)}
                      className="w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-white text-gray-700 rounded-lg font-medium hover:bg-gray-50 border border-gray-200 transition-colors"
                    >
                      <Building2 className="h-4 w-4" />
                      Phỏng vấn thử
                    </button>

                    <button
                      onClick={() => handleApply(job)}
                      disabled={appliedJobIds.includes(job.id)}
                      className={`w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg font-medium transition-all shadow-sm
                        ${
                          appliedJobIds.includes(job.id)
                            ? "bg-green-50 text-green-700 border border-green-200 cursor-not-allowed" // Style Đã nộp
                            : "bg-blue-600 text-white hover:bg-blue-700 shadow-sm shadow-blue-200" // Style Chưa nộp
                        }`}
                    >
                      {appliedJobIds.includes(job.id) ? (
                        <>
                          <ListChecks className="h-4 w-4" /> Đã ứng tuyển
                        </>
                      ) : (
                        <>
                          Ứng tuyển ngay <ArrowRight className="h-4 w-4" />
                        </>
                      )}
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          /* Empty State */
          <div className="text-center py-16 bg-white rounded-xl shadow-sm border border-gray-200">
            <div className="bg-gray-100 p-4 rounded-full inline-flex mb-4">
              <AlertCircle className="h-8 w-8 text-gray-400" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900">
              {activeTab === "matching"
                ? "Không tìm thấy công việc phù hợp (Match > 50%)"
                : "Không tìm thấy công việc nào"}
            </h3>
            <p className="text-gray-500 mt-2 max-w-md mx-auto">
              {activeTab === "matching"
                ? "Hãy thử cập nhật hồ sơ kỹ năng của bạn hoặc chuyển sang tab 'Tất cả việc làm' để xem thêm cơ hội."
                : "Hiện tại hệ thống chưa có công việc nào đang mở."}
            </p>
            {activeTab === "matching" && (
              <div className="flex gap-3 justify-center mt-6">
                <button
                  onClick={() => handleTabChange("all")}
                  className="px-6 py-2.5 bg-white text-blue-600 border border-blue-600 rounded-lg font-medium hover:bg-blue-50 transition-colors"
                >
                  Xem tất cả việc làm
                </button>
                <Link
                  href="/profile"
                  className="px-6 py-2.5 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors"
                >
                  Cập nhật hồ sơ
                </Link>
              </div>
            )}
          </div>
        )}
      </div>
      {selectedJob && (
        <ApplyModal
          isOpen={!!selectedJob}
          onClose={() => setSelectedJob(null)}
          jobId={selectedJob.id}
          jobTitle={selectedJob.title}
          companyName={selectedJob.company}
          onSuccess={() => {
            fetchAppliedStatus(); // Load lại danh sách appliedIds để nút chuyển sang màu xanh ngay lập tức
            setSelectedJob(null); // Đóng modal
          }}
        />
      )}
    </div>
  );
}
