"use client";
import { useState, useEffect } from "react";
import Link from "next/link";
import {
  Plus,
  Users,
  MapPin,
  Calendar,
  DollarSign,
  Trash2,
  Loader2, // Import thêm icon Loader
} from "lucide-react";
import { recruitmentService } from "@/services/recruitmentService";
import { JobPosting, JobStatus, JobCreateRequest } from "@/types/recruitment";
import toast from "react-hot-toast";
import { useConfirm } from "@/context/ConfirmDialogContext";

const getStatusLabel = (status: JobStatus | string) => {
  switch (status) {
    case JobStatus.PUBLISHED:
    case "PUBLISHED":
    case "OPEN": // Xử lý cả trường hợp legacy "OPEN"
      return "Đang tuyển";
    case JobStatus.DRAFT:
    case "DRAFT":
      return "Bản nháp";
    case JobStatus.CLOSED:
    case "CLOSED":
      return "Đã đóng";
    case "PENDING":
      return "Chờ duyệt";
    default:
      return status;
  }
};

const getStatusBadgeColor = (status: JobStatus | string) => {
  switch (status) {
    case JobStatus.PUBLISHED:
    case "PUBLISHED":
    case "OPEN":
      return "bg-green-100 text-green-700 border border-green-200";
    case JobStatus.DRAFT:
    case "DRAFT":
      return "bg-yellow-100 text-yellow-800 border border-yellow-200";
    case JobStatus.CLOSED:
    case "CLOSED":
      return "bg-gray-100 text-gray-600 border border-gray-200";
    case "PENDING":
      return "bg-blue-100 text-blue-700 border border-blue-200";
    default:
      return "bg-gray-100 text-gray-600 border border-gray-200";
  }
};

export default function ManageJobsPage() {
  const [jobs, setJobs] = useState<JobPosting[]>([]);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  // --- MỚI 1: State để theo dõi quá trình submit ---
  const [isSubmitting, setIsSubmitting] = useState(false);

  const confirm = useConfirm();

  const [newJob, setNewJob] = useState({
    title: "",
    location: "",
    salaryRange: "",
    expiryDate: "",
    description: "",
    requirements: "",
  });

  useEffect(() => {
    loadJobs();
  }, []);

  const loadJobs = async () => {
    try {
      const data = await recruitmentService.getMyJobs();
      setJobs(data);
    } catch (error) {
      toast.error("Lỗi tải danh sách việc làm");
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateJob = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      if (!newJob.expiryDate) {
        toast.error("Vui lòng chọn hạn nộp hồ sơ");
        return;
      }

      // --- MỚI 2: Bắt đầu loading ---
      setIsSubmitting(true);

      const payload: JobCreateRequest = {
        ...newJob,
        expiryDate: newJob.expiryDate,
      };

      await recruitmentService.createJob(payload);

      toast.success("Đăng tin thành công!");
      setShowCreateModal(false);
      loadJobs();

      setNewJob({
        title: "",
        location: "",
        salaryRange: "",
        expiryDate: "",
        description: "",
        requirements: "",
      });
    } catch (error: any) {
      console.error("Lỗi API:", error);
      if (error.response?.data) {
        const serverData = error.response.data;
        if (serverData.message) {
          toast.error(serverData.message);
        } else if (serverData.errors && typeof serverData.errors === "object") {
          const firstErrorKey = Object.keys(serverData.errors)[0];
          const errorMessage = serverData.errors[firstErrorKey];
          toast.error(`Lỗi: ${errorMessage}`);
        } else {
          toast.error("Lỗi dữ liệu không hợp lệ (400)");
        }
      } else {
        toast.error("Lỗi kết nối đến máy chủ");
      }
    } finally {
      // --- MỚI 3: Kết thúc loading (dù thành công hay thất bại) ---
      setIsSubmitting(false);
    }
  };

  const handleDeleteJob = async (id: number) => {
    const isConfirmed = await confirm({
      title: "Xóa tin tuyển dụng",
      message:
        "Tin tuyển dụng này sẽ bị xóa vĩnh viễn và không thể khôi phục. Bạn có chắc chắn không?",
      isDanger: true,
      confirmLabel: "Xóa ngay",
    });

    if (!isConfirmed) return;
    try {
      await recruitmentService.deleteJob(id);
      toast.success("Đã xóa tin tuyển dụng");
      loadJobs();
    } catch (error) {
      toast.error("Không thể xóa tin này");
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-800">Quản lý tuyển dụng</h1>
        <button
          onClick={() => setShowCreateModal(true)}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg flex items-center gap-2 hover:bg-blue-700 transition"
        >
          <Plus size={20} /> Đăng tin mới
        </button>
      </div>

      {/* Danh sách Jobs */}
      {isLoading ? (
        <div className="text-center py-10 flex flex-col items-center justify-center text-gray-500">
          <Loader2 className="animate-spin mb-2" size={32} />
          <span>Đang tải danh sách...</span>
        </div>
      ) : (
        <div className="grid gap-4">
          {jobs.length === 0 ? (
            <p className="text-gray-500 text-center">
              Chưa có tin tuyển dụng nào.
            </p>
          ) : (
            jobs.map((job) => (
              <div
                key={job.id}
                className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm hover:shadow-md transition"
              >
                <div className="flex justify-between items-start">
                  <div>
                    <h3 className="text-xl font-bold text-gray-900 mb-1">
                      <Link
                        href={`/recruiter/manage-jobs/${job.id}`}
                        className="hover:text-blue-600"
                      >
                        {job.title}
                      </Link>
                    </h3>
                    <div className="flex gap-4 text-sm text-gray-500 mb-3">
                      <span className="flex items-center gap-1">
                        <MapPin size={16} /> {job.location}
                      </span>
                      <span className="flex items-center gap-1">
                        <DollarSign size={16} /> {job.salaryRange}
                      </span>
                      <span className="flex items-center gap-1">
                        <Calendar size={16} />
                        {job.expiryDate
                          ? new Date(job.expiryDate).toLocaleDateString("vi-VN")
                          : "N/A"}
                      </span>
                    </div>
                  </div>
                  <div
                    className={`px-3 py-1 rounded-full text-xs font-semibold border ${getStatusBadgeColor(
                      job.status,
                    )}`}
                  >
                    {getStatusLabel(job.status)}
                  </div>
                </div>

                <div className="border-t pt-4 flex justify-between items-center">
                  <Link
                    href={`/recruiter/manage-jobs/${job.id}`}
                    className="flex items-center gap-2 text-blue-600 font-medium hover:underline"
                  >
                    <Users size={18} />
                    Đã có {job.applicationCount || 0} hồ sơ ứng tuyển
                  </Link>
                  <button
                    onClick={() => handleDeleteJob(job.id)}
                    className="text-gray-400 hover:text-red-500 text-sm flex items-center gap-1"
                  >
                    <Trash2 size={16} /> Xóa tin
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {/* Modal Tạo Job */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
            <h2 className="text-xl font-bold mb-4">Đăng tin tuyển dụng mới</h2>
            <form onSubmit={handleCreateJob} className="space-y-4">
              {/* ... (Phần input form giữ nguyên như cũ) ... */}

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Tiêu đề
                  </label>
                  <input
                    placeholder="VD: Java Developer"
                    className="border p-2 rounded w-full"
                    required
                    value={newJob.title}
                    onChange={(e) =>
                      setNewJob({ ...newJob, title: e.target.value })
                    }
                    disabled={isSubmitting} // Disable khi đang gửi
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Địa điểm
                  </label>
                  <input
                    placeholder="VD: Hà Nội"
                    className="border p-2 rounded w-full"
                    required
                    value={newJob.location}
                    onChange={(e) =>
                      setNewJob({ ...newJob, location: e.target.value })
                    }
                    disabled={isSubmitting}
                  />
                </div>
              </div>

              {/* ... (Các input khác tương tự, bạn nên thêm disabled={isSubmitting}) ... */}

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Mức lương
                  </label>
                  <input
                    placeholder="VD: 10-15 triệu"
                    className="border p-2 rounded w-full"
                    value={newJob.salaryRange}
                    onChange={(e) =>
                      setNewJob({ ...newJob, salaryRange: e.target.value })
                    }
                    disabled={isSubmitting}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Hạn nộp
                  </label>
                  <input
                    type="date"
                    className="border p-2 rounded w-full"
                    required
                    value={newJob.expiryDate}
                    onChange={(e) =>
                      setNewJob({ ...newJob, expiryDate: e.target.value })
                    }
                    disabled={isSubmitting}
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Mô tả công việc
                </label>
                <textarea
                  placeholder="Mô tả chi tiết..."
                  className="border p-2 rounded w-full h-24"
                  required
                  value={newJob.description}
                  onChange={(e) =>
                    setNewJob({ ...newJob, description: e.target.value })
                  }
                  disabled={isSubmitting}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Yêu cầu ứng viên
                </label>
                <textarea
                  placeholder="Yêu cầu kỹ năng, kinh nghiệm..."
                  className="border p-2 rounded w-full h-24"
                  required
                  value={newJob.requirements}
                  onChange={(e) =>
                    setNewJob({ ...newJob, requirements: e.target.value })
                  }
                  disabled={isSubmitting}
                />
              </div>

              {/* --- MỚI 4: Button Footer --- */}
              <div className="flex justify-end gap-3 pt-4 border-t mt-4">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded transition"
                  disabled={isSubmitting} // Không cho hủy ngang khi đang gửi
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className={`px-4 py-2 rounded transition flex items-center gap-2 text-white
                    ${
                      isSubmitting
                        ? "bg-blue-400 cursor-not-allowed"
                        : "bg-blue-600 hover:bg-blue-700"
                    }`}
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 className="animate-spin" size={18} />
                      Đang xử lý...
                    </>
                  ) : (
                    "Đăng tin"
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
