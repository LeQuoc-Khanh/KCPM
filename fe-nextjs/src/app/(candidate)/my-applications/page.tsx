"use client";
import { useEffect, useState } from "react";
import {
  getMyApplications,
  cancelApplication,
} from "@/services/candidateService";
import { Briefcase, CheckCircle, Clock, XCircle, Trash2 } from "lucide-react";
import Link from "next/link";
import toast from "react-hot-toast";

export default function MyApplicationsPage() {
  const [applications, setApplications] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchApps = async () => {
      try {
        const data = await getMyApplications();
        setApplications(data);
      } catch (error) {
        console.error("Failed to load applications", error);
      } finally {
        setLoading(false);
      }
    };
    fetchApps();
  }, []);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "PENDING":
        return (
          <span className="flex items-center text-yellow-600 bg-yellow-50 px-3 py-1 rounded-full text-sm">
            <Clock size={14} className="mr-1" /> Đang chờ duyệt
          </span>
        );
      case "APPROVED":
        return (
          <span className="flex items-center text-green-600 bg-green-50 px-3 py-1 rounded-full text-sm">
            <CheckCircle size={14} className="mr-1" /> Đã duyệt hồ sơ
          </span>
        );
      case "REJECTED":
        return (
          <span className="flex items-center text-red-600 bg-red-50 px-3 py-1 rounded-full text-sm">
            <XCircle size={14} className="mr-1" /> Từ chối
          </span>
        );
      default:
        return (
          <span className="text-gray-500 bg-gray-100 px-3 py-1 rounded-full text-sm">
            {status}
          </span>
        );
    }
  };

  const handleDelete = async (appId: number) => {
    if (!confirm("Bạn có chắc chắn muốn hủy đơn ứng tuyển này không?")) return;

    try {
      await cancelApplication(appId);

      setApplications((prev) => prev.filter((app) => app.id !== appId));

      toast.success("Đã hủy đơn ứng tuyển thành công");
    } catch (error: any) {
      console.error("Lỗi khi hủy đơn:", error);
      toast.error(error.response?.data?.message || "Không thể hủy đơn lúc này");
    }
  };

  return (
    <div className="max-w-5xl mx-auto py-8 px-4">
      <h1 className="text-2xl font-bold mb-6 flex items-center gap-2">
        <Briefcase className="text-blue-600" /> Lịch sử ứng tuyển
      </h1>

      {loading ? (
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="h-24 bg-gray-100 rounded-xl animate-pulse"
            />
          ))}
        </div>
      ) : applications.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-xl shadow-sm border">
          <p className="text-gray-500 mb-4">
            Bạn chưa ứng tuyển công việc nào.
          </p>
          <Link
            href="/dashboard-candidate"
            className="text-blue-600 hover:underline"
          >
            Tìm việc ngay
          </Link>
        </div>
      ) : (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
          <table className="w-full text-left">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="p-4 font-semibold text-gray-600">Công việc</th>
                <th className="p-4 font-semibold text-gray-600">Công ty</th>
                <th className="p-4 font-semibold text-gray-600">Ngày nộp</th>
                <th className="p-4 font-semibold text-gray-600">Trạng thái</th>
                <th className="p-4 font-semibold text-gray-600">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {applications.map((app: any) => (
                <tr key={app.id} className="hover:bg-gray-50 transition">
                  <td className="p-4 font-medium text-blue-600">
                    {app.jobTitle}
                  </td>
                  <td className="p-4 text-gray-700">{app.companyName}</td>
                  <td className="p-4 text-gray-500 text-sm">
                    {new Date(app.appliedAt).toLocaleDateString("vi-VN")}
                  </td>
                  <td className="p-4">{getStatusBadge(app.status)}</td>

                  <td className="p-4">
                    {app.status === "PENDING" ? (
                      <button
                        onClick={() => handleDelete(app.id)}
                        className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-full transition-colors"
                        title="Hủy ứng tuyển"
                      >
                        <Trash2 size={18} />
                      </button>
                    ) : (
                      <span className="text-xs text-gray-400 italic">
                        Không thể hủy
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
