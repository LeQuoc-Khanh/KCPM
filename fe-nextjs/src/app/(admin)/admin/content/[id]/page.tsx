"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  MapPin,
  DollarSign,
  Clock,
  Building2,
  Briefcase,
  ArrowLeft,
  Calendar,
  ShieldCheck,
  Check,
  X,
} from "lucide-react";
import { recruitmentService } from "@/services/recruitmentService";
import api from "@/services/api";
import { JobPosting } from "@/types/recruitment";
import { useConfirm } from "@/context/ConfirmDialogContext";
import toast from "react-hot-toast";

export default function AdminJobDetailPage() {
  const params = useParams();
  const router = useRouter();
  const confirm = useConfirm();
  const id = Number(params.id);

  const [job, setJob] = useState<JobPosting | null>(null);
  const [loading, setLoading] = useState(true);
  const [isProcessing, setIsProcessing] = useState(false);

  const fetchDetail = async () => {
    try {
      if (!id) return;
      console.log("Fetching job detail for ID:", id);
      setLoading(true);
      const data = await recruitmentService.getJobDetail(id);
      setJob(data);
    } catch (error) {
      console.error(error);
      toast.error("Lỗi khi tải thông tin bài đăng");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDetail();
  }, [id]);

  const handleApprove = async () => {
    const ok = await confirm({
      title: "Duyệt bài đăng",
      message:
        "Bạn có chắc chắn muốn duyệt bài tuyển dụng này để hiển thị công khai?",
      confirmLabel: "Duyệt bài",
    });

    if (!ok) return;

    try {
      setIsProcessing(true);
      await api.put(`/admin/content/posts/${id}/approve`);
      toast.success("Đã duyệt bài viết!");
      await fetchDetail(); // Cập nhật lại state để ẩn nút và đổi badge
    } catch (err) {
      toast.error("Lỗi khi duyệt bài");
    } finally {
      setIsProcessing(false);
    }
  };

  const handleReject = async () => {
    const ok = await confirm({
      title: "Từ chối bài đăng",
      message:
        "Bài viết này sẽ bị từ chối và không được hiển thị. Bạn có chắc không?",
      isDanger: true,
      confirmLabel: "Từ chối",
    });

    if (!ok) return;

    try {
      setIsProcessing(true);
      await api.put(`/admin/content/posts/${id}/reject`, {});
      toast.success("Đã từ chối bài viết!");
      await fetchDetail();
    } catch (err) {
      toast.error("Lỗi khi từ chối bài viết!");
    } finally {
      setIsProcessing(false);
    }
  };

  if (loading)
    return (
      <div className="p-20 text-center animate-pulse text-gray-500 font-bold">
        Đang truy xuất dữ liệu hệ thống...
      </div>
    );
  if (!job)
    return (
      <div className="p-20 text-center text-red-500 font-bold">
        Bài đăng không tồn tại hoặc đã bị xóa.
      </div>
    );

  // Logic kiểm tra trạng thái để hiển thị UI
  const isPublished =
    job.status === ("PUBLISHED" as any) ||
    job.status.toString() === "PUBLISHED";
  const isPending =
    job.status === ("PENDING" as any) || job.status.toString() === "PENDING";
  const isRejected =
    job.status === ("REJECTED" as any) || job.status.toString() === "REJECTED";

  return (
    <div className="max-w-5xl mx-auto p-4 md:p-8 space-y-6">
      {/* Thanh công cụ Admin */}
      <div className="flex flex-col md:flex-row items-center justify-between bg-white p-4 rounded-2xl shadow-sm border border-gray-100 gap-4">
        <button
          onClick={() => router.back()}
          className="flex items-center text-gray-500 hover:text-indigo-600 font-bold transition-all group"
        >
          <ArrowLeft
            size={20}
            className="mr-2 group-hover:-translate-x-1 transition-transform"
          />
          Quay lại danh sách
        </button>

        <div className="flex flex-wrap items-center gap-3">
          <span className="flex items-center gap-1.5 px-3 py-1 bg-indigo-50 text-indigo-700 rounded-full text-xs font-black border border-indigo-100">
            <ShieldCheck size={14} /> CHẾ ĐỘ ADMIN
          </span>

          {/* Badge Trạng thái: Fix lỗi TypeScript bằng cách so sánh string chuẩn */}
          <span
            className={`px-4 py-1 rounded-full text-xs font-bold uppercase tracking-wider border ${
              isPublished
                ? "bg-green-50 text-green-700 border-green-200"
                : isPending
                  ? "bg-amber-50 text-amber-600 border-amber-200"
                  : "bg-gray-100 text-gray-600 border-gray-200"
            }`}
          >
            {isPublished
              ? "Đã duyệt"
              : isPending
                ? "Chờ duyệt"
                : isRejected
                  ? "Đã từ chối"
                  : job.status}
          </span>

          {/* NÚT PHÊ DUYỆT / TỪ CHỐI: Chỉ hiện khi bài đang Chờ duyệt */}
          {isPending && (
            <div className="flex gap-2 ml-2 border-l pl-3 border-gray-200">
              <button
                disabled={isProcessing}
                onClick={handleApprove}
                className="flex items-center gap-1.5 px-4 py-1.5 bg-emerald-600 text-white rounded-xl hover:bg-emerald-700 transition-all font-bold text-sm shadow-sm disabled:opacity-50"
              >
                <Check size={16} /> Duyệt
              </button>
              <button
                disabled={isProcessing}
                onClick={handleReject}
                className="flex items-center gap-1.5 px-4 py-1.5 bg-rose-50 text-rose-600 rounded-xl hover:bg-rose-100 transition-all font-bold text-sm border border-rose-100 disabled:opacity-50"
              >
                <X size={16} /> Từ chối
              </button>
            </div>
          )}
        </div>
      </div>

      <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
        {/* Banner Header */}
        <div className="h-40 bg-gradient-to-r from-slate-900 via-indigo-900 to-blue-900 flex items-end px-10 pb-6">
          <div className="flex items-center gap-6 translate-y-2">
            <div className="w-24 h-24 bg-white rounded-2xl shadow-2xl border-4 border-white flex items-center justify-center overflow-hidden p-2">
              {job.companyLogo ? (
                <img
                  src={job.companyLogo}
                  alt="logo"
                  className="w-full h-full object-contain"
                />
              ) : (
                <Building2 className="text-gray-300" size={40} />
              )}
            </div>
            <div>
              <h1 className="text-3xl font-black text-white leading-tight">
                {job.title}
              </h1>
              <p className="text-indigo-200 font-bold text-lg">
                {job.companyName}
              </p>
            </div>
          </div>
        </div>

        {/* Thông tin vắn tắt */}
        <div className="pt-16 pb-8 px-10 grid grid-cols-1 md:grid-cols-3 gap-6 bg-gray-50/50 border-b border-gray-100">
          <div className="flex items-center gap-4 p-4 bg-white rounded-2xl shadow-sm border border-gray-100">
            <div className="p-3 bg-emerald-50 text-emerald-600 rounded-xl">
              <DollarSign size={24} />
            </div>
            <div>
              <p className="text-[10px] text-gray-400 uppercase font-black tracking-widest">
                Lương
              </p>
              <p className="font-black text-gray-900">{job.salaryRange}</p>
            </div>
          </div>
          <div className="flex items-center gap-4 p-4 bg-white rounded-2xl shadow-sm border border-gray-100">
            <div className="p-3 bg-blue-50 text-blue-600 rounded-xl">
              <MapPin size={24} />
            </div>
            <div>
              <p className="text-[10px] text-gray-400 uppercase font-black tracking-widest">
                Địa điểm
              </p>
              <p
                className="font-black text-gray-900 truncate"
                title={job.location}
              >
                {job.location}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-4 p-4 bg-white rounded-2xl shadow-sm border border-gray-100">
            <div className="p-3 bg-orange-50 text-orange-600 rounded-xl">
              <Calendar size={24} />
            </div>
            <div>
              <p className="text-[10px] text-gray-400 uppercase font-black tracking-widest">
                Hạn nộp
              </p>
              <p className="font-black text-gray-900">
                {job.expiryDate
                  ? new Date(job.expiryDate).toLocaleDateString("vi-VN")
                  : "Vô thời hạn"}
              </p>
            </div>
          </div>
        </div>

        <div className="p-10 space-y-12">
          <section>
            <h3 className="text-xl font-black text-gray-900 mb-6 flex items-center gap-3">
              <span className="w-2 h-8 bg-indigo-600 rounded-full"></span>
              Chi tiết công việc
            </h3>
            <div className="text-gray-700 leading-relaxed whitespace-pre-line text-lg bg-gray-50/30 p-6 rounded-2xl border border-gray-50">
              {job.description}
            </div>
          </section>

          <section>
            <h3 className="text-xl font-black text-gray-900 mb-6 flex items-center gap-3">
              <span className="w-2 h-8 bg-indigo-600 rounded-full"></span>
              Yêu cầu & Quyền lợi
            </h3>
            <div className="text-gray-700 leading-relaxed whitespace-pre-line text-lg bg-gray-50/30 p-6 rounded-2xl border border-gray-50">
              {job.requirements}
            </div>
          </section>

          <div className="flex flex-wrap gap-8 pt-8 border-t border-gray-100">
            <div className="flex items-center gap-2 text-gray-500 font-bold">
              <Clock size={20} className="text-indigo-500" />
              <span>
                Đăng ngày:{" "}
                {job.createdAt
                  ? new Date(job.createdAt).toLocaleDateString("vi-VN")
                  : "---"}
              </span>
            </div>
            <div className="flex items-center gap-2 text-gray-500 font-bold">
              <Briefcase size={20} className="text-indigo-500" />
              <span>Loại hình: Toàn thời gian</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
