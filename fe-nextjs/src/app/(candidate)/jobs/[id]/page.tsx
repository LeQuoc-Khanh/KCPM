"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import {
  MapPin,
  DollarSign,
  Clock,
  Building,
  Globe,
  CheckCircle,
  Briefcase,
  ArrowLeft,
  Star, 
  ExternalLink 
} from "lucide-react";
import { recruitmentService } from "@/services/recruitmentService";
import { JobPosting } from "@/types/recruitment";
import ApplyModal from "@/components/features/jobs/ApplyModal";
import toast from "react-hot-toast";

export default function JobDetailPage() {
  const params = useParams();
  const jobId = Number(params.id);

  const [job, setJob] = useState<JobPosting | null>(null);
  const [loading, setLoading] = useState(true);
  const [isApplyModalOpen, setIsApplyModalOpen] = useState(false);
  const [applicationStatus, setApplicationStatus] = useState<string | null>(
    null,
  );

  useEffect(() => {
    const fetchJobDetail = async () => {
      try {
        if (!jobId) return;
        // Gọi API lấy chi tiết và kiểm tra trạng thái ứng tuyển
        const [jobData, statusData] = await Promise.all([
          recruitmentService.getJobDetail(jobId),
          recruitmentService.checkApplicationStatus(jobId),
        ]);
        setJob(jobData);
        setApplicationStatus(statusData);
      } catch (error) {
        console.error("Failed to load job detail", error);
        toast.error("Không thể tải thông tin công việc");
      } finally {
        setLoading(false);
      }
    };
    fetchJobDetail();
  }, [jobId]);

  if (loading)
    return (
      <div className="min-h-screen flex items-center justify-center">
        Đang tải thông tin...
      </div>
    );
  if (!job)
    return (
      <div className="min-h-screen flex items-center justify-center">
        Không tìm thấy công việc này.
      </div>
    );

  const isExpired = job.expiryDate
    ? new Date(job.expiryDate) < new Date()
    : false;

  return (
    <div className="bg-gray-50 min-h-screen pb-12">
      {/* Header Banner */}
      <div className="bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <Link
            href="/jobs"
            className="inline-flex items-center text-gray-500 hover:text-blue-600 mb-6 transition"
          >
            <ArrowLeft size={16} className="mr-2" /> Quay lại danh sách
          </Link>

          <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
            <div className="flex items-start gap-4">
              <div className="w-20 h-20 bg-gray-100 rounded-lg flex items-center justify-center border border-gray-200 shadow-sm overflow-hidden">
                {job.companyLogo ? (
                  <img
                    src={job.companyLogo}
                    alt={job.companyName}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <Building size={32} className="text-gray-400" />
                )}
              </div>
              <div>
                <h1 className="text-3xl font-bold text-gray-900 mb-2">
                  {job.title}
                </h1>
                <div className="flex flex-wrap items-center gap-4 text-gray-600">
                  <span className="flex items-center gap-1 font-medium text-blue-700">
                    <Building size={16} />{" "}
                    {job.companyName || "Công ty ẩn danh"}
                  </span>
                  <span className="flex items-center gap-1">
                    <MapPin size={16} /> {job.location}
                  </span>
                  <span className="flex items-center gap-1">
                    <Clock size={16} /> Đăng{" "}
                    {job.createdAt
                      ? new Date(job.createdAt).toLocaleDateString("vi-VN")
                      : "---"}
                  </span>
                </div>
              </div>
            </div>

            <div className="flex flex-col items-end gap-2">
              {applicationStatus ? (
                <button
                  disabled
                  className="px-8 py-3 bg-green-100 text-green-700 font-bold rounded-lg flex items-center gap-2 cursor-not-allowed"
                >
                  <CheckCircle size={20} /> Đã ứng tuyển
                </button>
              ) : isExpired ? (
                <button
                  disabled
                  className="px-8 py-3 bg-gray-200 text-gray-500 font-bold rounded-lg cursor-not-allowed"
                >
                  Đã hết hạn
                </button>
              ) : (
                <button
                  onClick={() => setIsApplyModalOpen(true)}
                  className="px-8 py-3 bg-blue-600 text-white font-bold rounded-lg hover:bg-blue-700 transition shadow-lg hover:shadow-xl transform hover:-translate-y-0.5"
                >
                  Ứng tuyển ngay
                </button>
              )}
              <span className="text-sm text-gray-500">
                Hạn nộp:{" "}
                {job.expiryDate
                  ? new Date(job.expiryDate).toLocaleDateString("vi-VN")
                  : "Vô thời hạn"}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-6">
            {/* Thông tin lương & địa điểm */}
            <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="flex items-center gap-3 p-3 bg-blue-50 rounded-lg">
                <div className="p-2 bg-blue-100 text-blue-600 rounded-full">
                  <DollarSign size={20} />
                </div>
                <div>
                  <p className="text-xs text-gray-500 uppercase font-semibold">
                    Mức lương
                  </p>
                  <p className="font-bold text-gray-900">{job.salaryRange}</p>
                </div>
              </div>
              <div className="flex items-center gap-3 p-3 bg-purple-50 rounded-lg">
                <div className="p-2 bg-purple-100 text-purple-600 rounded-full">
                  <Briefcase size={20} />
                </div>
                <div>
                  <p className="text-xs text-gray-500 uppercase font-semibold">
                    Hình thức
                  </p>
                  <p className="font-bold text-gray-900">Toàn thời gian</p>
                </div>
              </div>
              <div className="flex items-center gap-3 p-3 bg-orange-50 rounded-lg">
                <div className="p-2 bg-orange-100 text-orange-600 rounded-full">
                  <MapPin size={20} />
                </div>
                <div>
                  <p className="text-xs text-gray-500 uppercase font-semibold">
                    Địa điểm
                  </p>
                  <p
                    className="font-bold text-gray-900 "
                    title={job.location}
                  >
                    {job.location}
                  </p>
                </div>
              </div>
            </div>

            <div className="bg-white p-8 rounded-xl shadow-sm border border-gray-100">
              <h2 className="text-xl font-bold text-gray-900 mb-4 border-l-4 border-blue-600 pl-3">
                Mô tả công việc
              </h2>
              <div className="prose max-w-none text-gray-600 whitespace-pre-line mb-8">
                {job.description}
              </div>

              <h2 className="text-xl font-bold text-gray-900 mb-4 border-l-4 border-blue-600 pl-3">
                Yêu cầu ứng viên
              </h2>
              <div className="prose max-w-none text-gray-600 whitespace-pre-line mb-8">
                {job.requirements}
              </div>
            </div>
          </div>

          {/* Sidebar thông tin công ty */}
          <div className="space-y-6">
            <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 sticky top-4">
              <h3 className="text-lg font-bold text-gray-900 mb-4">
                Thông tin công ty
              </h3>
              <div className="flex items-center gap-4 mb-6">
                <div className="w-16 h-16 bg-gray-50 rounded border flex items-center justify-center">
                  {job.companyLogo ? (
                    <img
                      src={job.companyLogo}
                      alt={job.companyName}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <Building size={24} className="text-gray-400" />
                  )}
                </div>
                <div>
                  <h4 className="font-bold text-gray-900 line-clamp-2">
                    {job.companyName}
                  </h4>
                </div>
              </div>

              <div className="space-y-4 text-sm">
                {job.companyWebsite && (
                  <div className="flex items-start gap-3">
                    <Globe size={18} className="text-gray-400 mt-0.5" />
                    <a
                      href={job.companyWebsite}
                      target="_blank"
                      rel="noreferrer"
                      className="text-gray-600 hover:text-blue-600 break-all"
                    >
                      {job.companyWebsite}
                    </a>
                  </div>
                )}
              </div>

              {/* MỚI: Link tới trang chi tiết công ty & Đánh giá */}
              <div className="pt-4 border-t border-gray-100 mt-4">
                {job.companyId ? (
                  <Link
                    href={`/companies/${job.companyId}`}
                    className="flex items-center justify-center gap-2 w-full py-2.5 bg-indigo-50 text-indigo-700 rounded-lg hover:bg-indigo-100 font-medium transition-colors border border-indigo-100"
                  >
                    <Star size={18} className="text-indigo-500" /> 
                    <span>Xem đánh giá & Chi tiết</span>
                    <ExternalLink size={14} className="ml-auto opacity-50" />
                  </Link>
                ) : (
                  <div className="text-center text-gray-400 text-sm py-2">
                    Thông tin công ty đang cập nhật
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Modal ứng tuyển thực sự */}
      <ApplyModal
        isOpen={isApplyModalOpen}
        onClose={() => setIsApplyModalOpen(false)}
        jobId={job.id}
        jobTitle={job.title}
        companyName={job.companyName || "Công ty ẩn danh"}
        onSuccess={() => setApplicationStatus("applied")}
      />
    </div>
  );
}