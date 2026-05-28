"use client";

import React, { useState, useEffect } from "react";
import api from "@/services/api";
import { Check, X, FileText, Clock, Eye, Building2 } from "lucide-react"; // Thêm Eye, Building2
import { useConfirm } from "@/context/ConfirmDialogContext";
import Link from "next/link"; // Thêm Link để điều hướng
import toast from "react-hot-toast";

interface Article {
  id: number;
  title: string;
  authorName?: string;
  summary: string;
  status:
    | "PENDING"
    | "PUBLISHED"
    | "REJECTED"
    | "DRAFT"
    | "HIDDEN"
    | "CLOSED"
    | "DELETED";
  createdAt: string;
  companyLogo?: string; // Thêm logo để hiển thị cho chuyên nghiệp
}

const formatDateTime = (iso: string) => {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;

  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(d);
};

export default function ContentManagementPage() {
  const confirm = useConfirm();
  const [activeTab, setActiveTab] = useState<"PENDING" | "ALL">("PENDING");
  const [articles, setArticles] = useState<Article[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchArticles = async () => {
    setLoading(true);
    try {
      const endpoint =
        activeTab === "PENDING"
          ? "/admin/content/pending"
          : "/admin/content/posts?status=ALL";

      const res = await api.get(endpoint);
      const content = res.data?.content ?? [];

      const mapped: Article[] = content.map((j: any) => ({
        id: j.id,
        title: j.title,
        authorName: j.companyName ?? "Công ty ẩn danh",
        summary: j.description ?? j.summary ?? "",
        status: j.status,
        createdAt: j.createdAt,
        companyLogo: j.companyLogo,
      }));

      setArticles(mapped);
    } catch (error) {
      console.error("Lỗi tải bài viết:", error);
      setArticles([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchArticles();
  }, [activeTab]);

  const handleApprove = async (id: number) => {
    const ok = await confirm({
      title: "Duyệt bài đăng",
      message:
        "Bạn có chắc chắn muốn duyệt bài tuyển dụng này để hiển thị công khai?",
      confirmLabel: "Duyệt bài",
    });

    if (!ok) return;
    try {
      await api.put(`/admin/content/posts/${id}/approve`);
      toast.success("Đã duyệt bài viết!");
      fetchArticles(); // Tải lại danh sách
    } catch (err) {
      toast.error("Lỗi khi duyệt bài");
    }
  };

  const handleReject = async (id: number) => {
    const ok = await confirm({
      title: "Từ chối bài đăng",
      message:
        "Bài viết này sẽ bị từ chối và không được hiển thị. Bạn có chắc không?",
      isDanger: true,
      confirmLabel: "Từ chối",
    });

    if (!ok) return;
    try {
      await api.put(`/admin/content/posts/${id}/reject`, {});
      toast.success("Đã từ chối bài viết!");
      fetchArticles(); // Tải lại danh sách
    } catch (err) {
      toast.error("Lỗi khi từ chối bài viết!");
    }
  };

  return (
    <div className="max-w-6xl mx-auto space-y-6 p-4">
      <div className="flex justify-between items-center bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
        <div>
          <h1 className="text-2xl font-black text-gray-900">
            Quản lý Nội dung
          </h1>
          <p className="text-sm text-gray-500">
            Kiểm duyệt và quản lý các bài đăng tuyển dụng trên hệ thống.
          </p>
        </div>
        <div className="flex space-x-1 bg-gray-100 p-1 rounded-xl border">
          <button
            onClick={() => setActiveTab("PENDING")}
            className={`px-5 py-2 text-sm font-bold rounded-lg transition-all ${
              activeTab === "PENDING"
                ? "bg-white text-blue-600 shadow-sm"
                : "text-gray-500 hover:text-gray-700"
            }`}
          >
            Chờ duyệt ({articles.filter((a) => a.status === "PENDING").length})
          </button>
          <button
            onClick={() => setActiveTab("ALL")}
            className={`px-5 py-2 text-sm font-bold rounded-lg transition-all ${
              activeTab === "ALL"
                ? "bg-white text-blue-600 shadow-sm"
                : "text-gray-500 hover:text-gray-700"
            }`}
          >
            Tất cả
          </button>
        </div>
      </div>

      <div className="grid gap-4">
        {loading ? (
          <div className="py-20 text-center text-gray-400 animate-pulse font-medium">
            Đang lấy danh sách bài đăng...
          </div>
        ) : articles.length === 0 ? (
          <div className="text-center py-20 bg-white rounded-2xl border-2 border-dashed border-gray-200">
            <FileText className="w-16 h-16 text-gray-200 mx-auto mb-4" />
            <p className="text-gray-500 font-bold">
              Không có bài viết nào cần xử lý.
            </p>
          </div>
        ) : (
          articles.map((item) => (
            <div
              key={item.id}
              className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 hover:border-blue-200 hover:shadow-md transition-all flex flex-col md:flex-row justify-between items-start md:items-center gap-4 group"
            >
              <div className="flex gap-4 flex-1">
                {/* Logo công ty thật */}
                <div className="w-14 h-14 bg-gray-50 rounded-xl border border-gray-100 flex items-center justify-center overflow-hidden flex-shrink-0">
                  {item.companyLogo ? (
                    <img
                      src={item.companyLogo}
                      alt="logo"
                      className="w-full h-full object-contain"
                    />
                  ) : (
                    <Building2 className="text-gray-300" size={24} />
                  )}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-3 mb-1">
                    {item.status === "PENDING" ? (
                      <span className="bg-amber-50 text-amber-600 text-[10px] px-2 py-0.5 rounded-full font-black border border-amber-100 uppercase tracking-wider">
                        Chờ duyệt
                      </span>
                    ) : (
                      <span className="bg-green-50 text-green-600 text-[10px] px-2 py-0.5 rounded-full font-black border border-green-100 uppercase tracking-wider">
                        {item.status}
                      </span>
                    )}
                    <span className="text-[11px] text-gray-400 font-medium flex items-center gap-1">
                      <Clock size={12} /> {formatDateTime(item.createdAt)}
                    </span>
                  </div>
                  <Link href={`/admin/content/${item.id}`}>
                    <h3 className="text-lg font-bold text-gray-900 group-hover:text-blue-600 transition truncate cursor-pointer">
                      {item.title}
                    </h3>
                  </Link>
                  <p className="text-sm text-gray-500 font-bold flex items-center gap-1 mt-0.5">
                    {item.authorName}
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2 w-full md:w-auto border-t md:border-t-0 pt-4 md:pt-0">
                {/* NÚT XEM CHI TIẾT */}
                <Link
                  href={`/admin/content/${item.id}`}
                  className="flex-1 md:flex-none flex items-center justify-center gap-2 px-4 py-2 bg-gray-50 text-gray-600 rounded-xl hover:bg-blue-50 hover:text-blue-600 transition-all font-bold border border-transparent hover:border-blue-100"
                  title="Xem chi tiết nội dung"
                >
                  <Eye size={18} />
                  <span className="md:hidden lg:inline text-sm">
                    Xem chi tiết
                  </span>
                </Link>

                {activeTab === "PENDING" && (
                  <>
                    <button
                      onClick={() => handleApprove(item.id)}
                      className="p-2.5 bg-emerald-50 text-emerald-600 rounded-xl hover:bg-emerald-600 hover:text-white transition-all border border-emerald-100 shadow-sm"
                      title="Phê duyệt bài đăng"
                    >
                      <Check className="w-5 h-5" />
                    </button>
                    <button
                      onClick={() => handleReject(item.id)}
                      className="p-2.5 bg-rose-50 text-rose-600 rounded-xl hover:bg-rose-600 hover:text-white transition-all border border-rose-100 shadow-sm"
                      title="Từ chối bài đăng"
                    >
                      <X className="w-5 h-5" />
                    </button>
                  </>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
