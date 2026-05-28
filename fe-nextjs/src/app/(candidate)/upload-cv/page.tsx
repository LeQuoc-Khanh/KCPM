"use client";

import { useEffect, useState } from "react";
import Link from "next/link"; // 👈 1. Import Link
import { getMyProfile } from "@/services/candidateService";
import { FileText, ArrowLeft } from "lucide-react"; // 👈 2. Import icon ArrowLeft

import CVUploadForm from "@/components/features/cv/CVUploadForm";

export default function ProfilePage() {
  const [loading, setLoading] = useState(true);
  const [cvUrl, setCvUrl] = useState<string | undefined>(undefined);

  const fetchProfile = async () => {
    try {
      const data = await getMyProfile();
      if (data) {
        setCvUrl(data.cvFilePath);
      }
    } catch (error) {
      console.error("Lỗi tải profile:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
  }, []);

  if (loading)
    return (
      <div className="p-10 text-center text-gray-500">Đang tải dữ liệu...</div>
    );

  return (
    <div className="max-w-4xl mx-auto py-8 px-4">
      {/* 👇 3. THÊM NÚT QUAY LẠI Ở ĐÂY */}
      <div className="mb-6">
        <Link
          href="/dashboard-candidate"
          className="inline-flex items-center gap-2 text-gray-600 hover:text-blue-600 font-medium transition-colors"
        >
          <ArrowLeft size={20} />
          Quay lại tìm việc
        </Link>
      </div>
      {/* 👆 KẾT THÚC NÚT QUAY LẠI */}

      {/* HEADER */}
      <div className="mb-6 border-b pb-4">
        <h1 className="text-2xl font-bold flex items-center gap-2 text-gray-800">
          <FileText className="text-blue-600" /> Quản lý CV
        </h1>
        <p className="text-gray-500 text-sm mt-1">
          Xem và cập nhật CV của bạn để hệ thống AI phân tích mức độ phù hợp
          công việc.
        </p>
      </div>

      {/* KHUNG UPLOAD & XEM CV */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 min-h-[400px]">
        <CVUploadForm
          currentCvUrl={cvUrl}
          onUploadSuccess={() => fetchProfile()}
        />
      </div>
    </div>
  );
}
