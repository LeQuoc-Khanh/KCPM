"use client";

import { useState, useRef } from "react";
import { uploadCV } from "@/services/candidateService";
import toast from "react-hot-toast";

interface CVUploadFormProps {
  currentCvUrl?: string;
  onUploadSuccess: () => void;
}

export default function CVUploadForm({
  currentCvUrl,
  onUploadSuccess,
}: CVUploadFormProps) {
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Kiểm tra dung lượng file (5MB)
    if (file.size > 5 * 1024 * 1024) {
      toast.error("File quá lớn! Vui lòng chọn file dưới 5MB.");
      return;
    }

    const loadingToastId = toast.loading("Đang tải lên CV...");

    try {
      setUploading(true);
      await uploadCV(file);
      
      toast.dismiss(loadingToastId);
      toast.success("Đăng tải CV thành công!");
      
      onUploadSuccess();
    } catch (error) {
      console.error(error);
      
      toast.dismiss(loadingToastId);
      toast.error("Có lỗi xảy ra khi upload CV. Vui lòng thử lại!");
    } finally {
      setUploading(false);
      // Reset input để có thể chọn lại cùng 1 file nếu cần
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const triggerSelectFile = () => {
    fileInputRef.current?.click();
  };

  return (
    <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200 h-full flex flex-col">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-lg font-bold text-gray-800">CV Của Bạn</h2>
        <input
          type="file"
          ref={fileInputRef}
          onChange={handleFileChange}
          accept=".pdf,.doc,.docx"
          className="hidden"
          title="Tải lên CV của bạn"
          aria-label="Tải lên CV của bạn"
        />
      </div>

      <div className="flex-1 flex flex-col justify-center items-center w-full">
        {uploading ? (
          <div className="text-center py-10">
            <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600 mx-auto mb-3"></div>
            <p className="text-sm text-gray-500">
              Đang phân tích & lưu trữ CV...
            </p>
          </div>
        ) : currentCvUrl ? (
          <div className="w-full flex flex-col">
            <div className="w-full bg-gray-100 rounded-lg border border-gray-300 overflow-hidden mb-4 relative h-[500px]">
              <iframe
                src={`https://docs.google.com/gview?url=${currentCvUrl}&embedded=true`}
                className="w-full h-full absolute inset-0"
                title="Xem trước CV"
              ></iframe>
            </div>

            <div className="flex justify-between items-center">
              <a
                href={currentCvUrl}
                target="_blank"
                rel="noreferrer"
                className="text-blue-600 hover:underline text-sm font-medium"
              >
                Xem file gốc ↗
              </a>
              <button
                onClick={triggerSelectFile}
                className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 text-sm font-semibold transition"
              >
                Cập nhật CV mới
              </button>
            </div>
          </div>
        ) : (
          <div className="text-center py-12 px-4 border-2 border-dashed border-gray-300 rounded-xl w-full h-[300px] flex flex-col justify-center items-center">
            <div className="bg-blue-50 w-16 h-16 rounded-full flex items-center justify-center mb-4">
              <span className="text-3xl">📄</span>
            </div>
            <h3 className="text-gray-800 font-semibold mb-2">
              Bạn chưa đăng tải CV
            </h3>
            <p className="text-gray-500 text-sm mb-6 max-w-xs mx-auto">
              Hãy upload CV để hệ thống AI có thể phân tích và gợi ý việc làm
              phù hợp nhất cho bạn.
            </p>
            <button
              onClick={triggerSelectFile}
              className="px-6 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium shadow-md transition transform hover:scale-105"
            >
              Upload CV Ngay
            </button>
          </div>
        )}
      </div>
    </div>
  );
}