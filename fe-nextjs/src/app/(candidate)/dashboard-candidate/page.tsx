"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  getRecentJobs,
  getBatchScores,
  getMyApplications,
} from "@/services/candidateService";
import ApplyModal from "@/components/features/jobs/ApplyModal";
import { useAuth } from "@/context/Authcontext"; // [THÊM] Import useAuth
import {
  Search,
  TrendingUp,
  Star,
  ArrowRight,
  Sparkles,
  FileText,
  Lock,
} from "lucide-react";
import PremiumFeatureLock from "@/components/common/PremiumFeatureLock";

export default function CandidateDashboard() {
  const { user } = useAuth(); // [THÊM] Lấy thông tin user từ Context

  // [THÊM] Định nghĩa biến isVip để code phía dưới không bị lỗi "isVip is not defined"
  const isVip = user?.userRole === "ADMIN" || user?.userRole?.includes("_VIP");

  const formatTimeAgo = (dateString: string) => {
    if (!dateString) return "Mới đăng";
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = Math.abs(now.getTime() - date.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays <= 1 ? "Vừa xong" : `${diffDays} ngày trước`;
  };

  const [jobs, setJobs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [appliedJobIds, setAppliedJobIds] = useState<number[]>([]);
  const [selectedJob, setSelectedJob] = useState<any>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const router = useRouter();

  const fetchAppliedStatus = async () => {
    try {
      const myApps = await getMyApplications();
      const ids = Array.isArray(myApps)
        ? myApps.map((app: any) => app.job?.id || app.jobId)
        : [];
      setAppliedJobIds(ids);
    } catch (error) {
      console.error("Lỗi lấy trạng thái ứng tuyển:", error);
    }
  };

  const handleSearch = () => {
    if (searchTerm.trim()) {
      router.push(`/jobs?keyword=${encodeURIComponent(searchTerm)}`);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") handleSearch();
  };

  const handleApply = (job: any) => {
    setSelectedJob(job);
  };

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const jobsData = await getRecentJobs();
        console.log("Fetched recent jobs:", jobsData); // Kiểm tra dữ liệu nhận được
        if (jobsData && jobsData.length > 0) {
          const jobIds = jobsData.map((job: any) => job.id);
          try {
            const scoresMap = await getBatchScores(jobIds);
            const mergedJobs = jobsData.map((job: any) => {
              const result = scoresMap[job.id] || {
                matchScore: 0,
                matchedSkills: [],
                missingSkills: [],
              };
              return {
                ...job,
                matchScore: result.matchScore,
                skillsFound: result.matchedSkills,
                skillsMissing: result.missingSkills,
              };
            });
            mergedJobs.sort((a: any, b: any) => b.matchScore - a.matchScore);
            setJobs(mergedJobs);
          } catch (err) {
            setJobs(
              jobsData.map((j: any) => ({
                ...j,
                matchScore: 0,
                skillsFound: [],
              })),
            );
          }
        } else {
          setJobs([]);
        }
      } catch (error) {
        console.error("Lỗi tải job:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
    fetchAppliedStatus();
  }, []);

  return (
    <div className="space-y-8 pb-10">
      {/* 1. SECTION: WELCOME & SEARCH */}
      <div className="bg-gradient-to-r from-blue-600 to-indigo-700 rounded-2xl p-8 text-white shadow-xl">
        <h1 className="text-3xl font-bold mb-2">
          Xin chào, {user?.fullName || "Ứng viên"}! 👋
        </h1>
        <p className="opacity-90 mb-6 text-blue-100">
          Hệ thống AI đã tìm thấy những cơ hội phù hợp nhất với hồ sơ của bạn
          hôm nay.
        </p>
        <div className="bg-white/10 backdrop-blur-md p-1.5 rounded-xl flex gap-2 max-w-2xl shadow-inner">
          <div className="flex-1 flex items-center bg-white rounded-lg px-4 py-2.5">
            <Search className="text-gray-400 mr-3" size={20} />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Tìm kiếm công việc, kỹ năng, công ty..."
              className="w-full bg-transparent outline-none text-gray-800 placeholder-gray-500"
            />
          </div>
          <button
            onClick={handleSearch}
            className="bg-orange-500 hover:bg-orange-600 text-white px-8 py-2.5 rounded-lg font-semibold transition-transform active:scale-95"
          >
            Tìm kiếm
          </button>
        </div>
      </div>

      {/* 2. SECTION: WIDGETS */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 flex flex-col justify-between hover:shadow-md transition">
          <div className="flex items-center justify-between mb-4">
            <div className="relative w-16 h-16">
              <span className="absolute inset-0 flex items-center justify-center text-blue-700 bg-blue-50 rounded-full">
                <FileText size={24} strokeWidth={2.5} />
              </span>
            </div>
            <div className="text-right">
              <p className="font-bold text-gray-800 text-lg">CV Của Bạn</p>
              <p className="text-gray-500 text-sm">Xem và cập nhật hồ sơ</p>
            </div>
          </div>
          <Link href="/upload-cv">
            <button className="w-full bg-gray-50 text-blue-600 py-2.5 rounded-lg text-sm font-semibold hover:bg-blue-50 border border-blue-100 transition">
              Cập nhật CV ngay
            </button>
          </Link>
        </div>

        {/* KHỐI AI CAREER COACH - ĐÃ SỬA LOGIC KHÓA */}
        <div className="md:col-span-2">
          {!isVip ? (
            <PremiumFeatureLock
              title="Mở khóa AI Career Coach"
              description="Nâng cấp VIP để nhận lời khuyên sự nghiệp và lộ trình thăng tiến từ AI."
            >
              <div className="bg-gray-200 p-6 rounded-xl shadow-md text-gray-500 flex flex-col justify-between relative overflow-hidden border border-gray-300 h-full">
                <div className="absolute top-0 right-0 w-32 h-32 bg-gray-400/10 rounded-full blur-2xl -mr-10 -mt-10"></div>
                <div className="relative z-10">
                  <div className="flex items-center gap-2 mb-2">
                    <TrendingUp className="text-gray-400" />
                    <h3 className="font-bold text-xl text-gray-600">
                      AI Career Coach
                    </h3>
                  </div>
                  <p className="text-gray-500 mb-4 max-w-lg">
                    Bạn muốn biết lộ trình thăng tiến cho vị trí Tech Lead? AI
                    có thể phân tích xu hướng thị trường.
                  </p>
                </div>
                <button className="w-fit bg-gray-300 text-gray-600 py-2 px-6 rounded-lg text-sm font-bold shadow-sm flex items-center gap-2 cursor-not-allowed">
                  <Lock size={16} /> Chat với AI Coach
                </button>
              </div>
            </PremiumFeatureLock>
          ) : (
            <div className="bg-gradient-to-br from-purple-600 to-indigo-600 p-6 rounded-xl shadow-md text-white flex flex-col justify-between relative overflow-hidden h-full">
              <div className="absolute top-0 right-0 w-32 h-32 bg-white/10 rounded-full blur-2xl -mr-10 -mt-10"></div>
              <div className="relative z-10">
                <div className="flex items-center gap-2 mb-2">
                  <TrendingUp className="text-yellow-300" />
                  <h3 className="font-bold text-xl">AI Career Coach</h3>
                </div>
                <p className="text-purple-100 mb-4 max-w-lg">
                  Bạn muốn biết lộ trình thăng tiến cho vị trí Tech Lead? AI có
                  thể đưa ra lời khuyên hữu ích.
                </p>
              </div>
              <Link href="/interview">
                <button className="w-fit bg-white text-purple-700 py-2 px-6 rounded-lg text-sm font-bold shadow-sm hover:bg-gray-50 transition relative z-10">
                  Chat với AI Coach
                </button>
              </Link>
            </div>
          )}
        </div>
      </div>

      {/* 3. SECTION: JOB LIST */}
      <div>
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-bold text-gray-800 flex items-center gap-2">
            <Star className="text-yellow-500 fill-yellow-500" size={24} />
            Danh Sách công việc mới nhất
          </h2>
          <Link
            href="/jobs?mode=all"
            className="text-sm text-blue-600 hover:underline font-medium flex items-center"
          >
            Xem tất cả <ArrowRight size={16} className="ml-1" />
          </Link>
        </div>

        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[1, 2, 3].map((i) => (
              <div
                key={i}
                className="h-48 bg-gray-100 rounded-xl animate-pulse"
              ></div>
            ))}
          </div>
        ) : jobs.length === 0 ? (
          <div className="text-center py-12 bg-white rounded-xl border border-gray-100">
            <p className="text-gray-500">Chưa tìm thấy công việc phù hợp.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
            {jobs.map((job: any) => (
              <div
                key={job.id}
                className="group bg-white rounded-xl border border-gray-100 shadow-sm hover:shadow-lg transition-all flex flex-col h-full"
              >
                <div className="flex justify-between items-start p-5 pb-2">
                  <div className="flex gap-4">
                    <div className="w-14 h-14 bg-white rounded-xl border border-gray-200 overflow-hidden flex-shrink-0 flex items-center justify-center relative">
                      {job.companyLogo ? (
                        <img
                          src={job.companyLogo}
                          alt={job.companyName || job.company}
                          className="w-full h-full object-contain p-1"
                          onError={(e) => {
                            // XỬ LÝ LỖI: Nếu ảnh chết (404), ẩn ảnh đi và hiện chữ cái fallback
                            e.currentTarget.style.display = "none";
                            const fallback = e.currentTarget.nextElementSibling;
                            if (fallback) {
                              fallback.classList.remove("hidden");
                              fallback.classList.add("flex");
                            }
                          }}
                        />
                      ) : null}

                      {/* FALLBACK: Chữ cái đầu tên công ty (Chỉ hiện khi không có Logo hoặc Logo lỗi) */}
                      <div
                        className={`${
                          job.companyLogo ? "hidden" : "flex"
                        } w-full h-full items-center justify-center bg-gray-50 text-blue-600 font-bold text-xl`}
                      >
                        {/* Ưu tiên lấy companyName, nếu không có thì lấy company (string cũ), nếu không thì hiện 'C' */}
                        {(job.companyName || job.company || "C")
                          .charAt(0)
                          .toUpperCase()}
                      </div>
                    </div>
                    <div>
                      <h3 className="font-bold text-lg text-gray-800 line-clamp-1">
                        {job.title}
                      </h3>
                      <p className="text-sm text-gray-500">{job.company}</p>
                    </div>
                  </div>
                  <div className="bg-blue-50 px-3 py-1 rounded-full border border-blue-100">
                    <span className="text-sm font-bold text-blue-700 flex items-center gap-1">
                      <Sparkles size={12} /> {job.matchScore}%
                    </span>
                  </div>
                </div>

                <div className="px-5 py-2 flex-1 flex flex-col gap-4">
                  <div className="flex flex-wrap gap-2 text-xs">
                    <span className="bg-gray-50 px-2 py-1 rounded border">
                      {job.location}
                    </span>
                    <span className="bg-green-50 text-green-700 px-2 py-1 rounded border font-medium">
                      {job.salary}
                    </span>
                  </div>
                  <p className="text-sm text-gray-600 line-clamp-2">
                    {job.description}
                  </p>
                </div>

                <div className="p-5 pt-2 mt-auto border-t border-gray-50 flex gap-3">
                  <button
                      onClick={() => router.push(`/jobs/${job.id}`)}
                      className="py-2.5 rounded-lg bg-gray-100 text-gray-800 font-semibold text-sm hover:bg-gray-200 flex items-center justify-center gap-2"                    >
                      <FileText className="h-4 w-4" />
                      Xem chi tiết
                    </button>
                  <Link href={`/cv-analysis/${job.id}`} className="flex-1">
                    <button className="w-full py-2.5 rounded-lg bg-purple-50 text-purple-700 font-semibold text-sm hover:bg-purple-100 flex items-center justify-center gap-2">
                      <Sparkles size={16} /> AI Phân tích
                    </button>
                  </Link>
                  <button
                    onClick={() => handleApply(job)}
                    disabled={appliedJobIds.includes(job.id)}
                    className={`flex-1 py-2.5 rounded-lg font-semibold text-sm transition-all ${
                      appliedJobIds.includes(job.id)
                        ? "bg-green-50 text-green-700 cursor-not-allowed"
                        : "bg-blue-600 text-white hover:bg-blue-700"
                    }`}
                  >
                    {appliedJobIds.includes(job.id)
                      ? "Đã ứng tuyển"
                      : "Ứng tuyển ngay"}
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {selectedJob && (
        <ApplyModal
          isOpen={!!selectedJob}
          onClose={() => setSelectedJob(null)}
          jobId={selectedJob.id}
          jobTitle={selectedJob.title}
          // [FIX 2] Sửa selectedJob.company thành selectedJob.companyName
          companyName={selectedJob.companyName}
          onSuccess={() => {
            setAppliedJobIds((prev) => [...prev, selectedJob.id]);
            setSelectedJob(null);
          }}
        />
      )}
    </div>
  );
}
