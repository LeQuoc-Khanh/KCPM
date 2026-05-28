"use client";

import { useState, useEffect } from "react";
import { X, Upload, FileText, CheckCircle, AlertCircle, Loader2 } from "lucide-react";
import * as candidateService from "@/services/candidateService";
import { useAuth } from "@/context/Authcontext";
import { CandidateProfile } from "@/types/candidate";
import toast from "react-hot-toast";

interface ApplyModalProps {
  isOpen: boolean;
  onClose: () => void;
  jobId: number;
  jobTitle: string;
  companyName: string;
  onSuccess?: () => void;
}

export default function ApplyModal({
    isOpen,
    onClose,
    jobId,
    jobTitle,
    companyName,
    onSuccess,
}: ApplyModalProps) {
  const { user } = useAuth();
  const [loading, setLoading] = useState(false);
  const [profile, setProfile] = useState<CandidateProfile | null>(null);
  
  // Form State
  const [coverLetter, setCoverLetter] = useState("");
  const [cvOption, setCvOption] = useState<"profile" | "upload">("profile");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  // Lấy thông tin profile để check xem đã có CV chưa
  useEffect(() => {
    if (isOpen && user) {
      fetchProfile();
    }
  }, [isOpen, user]);

  const fetchProfile = async () => {
    try {
      const data = await candidateService.getMyProfile();
      setProfile(data);
      // Nếu chưa có CV profile thì mặc định chọn upload
      if (!data.cvFilePath) {
        setCvOption("upload");
      }
    } catch (error) {
      console.error("Lỗi lấy profile:", error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      let finalCvUrl = "";

      // 1. Xử lý CV
      if (cvOption === "upload") {
        if (!selectedFile) {
          toast.error("Vui lòng chọn file CV để tải lên!");
          setLoading(false);
          return;
        }
        // Upload CV mới và lấy URL từ profile trả về (hoặc logic BE trả về URL)
        const updatedProfile = await candidateService.uploadCV(selectedFile);
        finalCvUrl = updatedProfile.cvFilePath || ""; 
      } else {
        // Dùng CV Profile
        if (!profile?.cvFilePath) {
          toast.error("Hồ sơ của bạn chưa có CV. Vui lòng upload CV mới.");
          setLoading(false);
          return;
        }
        // Backend tự lấy từ profile nếu cvUrl rỗng, nhưng gửi luôn cho chắc
        finalCvUrl = profile.cvFilePath;
      }

      // 2. Gửi request ứng tuyển
      await candidateService.applyJob({
        jobId,
        coverLetter,
        cvUrl: finalCvUrl,
      });

      toast.success("Ứng tuyển thành công!");

      if (onSuccess) {
        onSuccess();
      }

      onClose();
      
      // Reset form
      setCoverLetter("");
      setSelectedFile(null);
    } catch (error: any) {
      console.error("Lỗi ứng tuyển:", error);
      
      const message = error.response?.data?.message || "Có lỗi xảy ra khi ứng tuyển.";
      console.log("Toast message sẽ hiện:", message); 

      toast.error(message);
      
      if (error.response?.status === 400) {
        setTimeout(() => onClose(), 1500);
      }
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg overflow-hidden animate-in fade-in zoom-in duration-200">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-gray-100 bg-gray-50">
          <div>
            <h3 className="text-lg font-bold text-gray-900">Ứng tuyển công việc</h3>
            <p className="text-sm text-gray-500">
              {jobTitle} tại <span className="font-medium text-blue-600">{companyName}</span>
            </p>
          </div>
          <button onClick={onClose} className="p-2 text-gray-400 hover:text-gray-600 rounded-full hover:bg-gray-200">
            <X size={20} />
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          
          {/* Chọn CV */}
          <div className="space-y-3">
            <label className="text-sm font-semibold text-gray-700 block">CV của bạn</label>
            
            {/* Option 1: Dùng CV Profile */}
            <div 
              onClick={() => profile?.cvFilePath && setCvOption("profile")}
              className={`relative flex items-center p-4 border rounded-xl cursor-pointer transition-all ${
                cvOption === "profile" 
                  ? "border-blue-600 bg-blue-50/50 ring-1 ring-blue-600" 
                  : "border-gray-200 hover:border-gray-300"
              } ${!profile?.cvFilePath ? "opacity-60 cursor-not-allowed" : ""}`}
            >
              <div className="flex-1 flex items-center gap-3">
                <div className={`p-2 rounded-lg ${cvOption === "profile" ? "bg-blue-100 text-blue-600" : "bg-gray-100 text-gray-500"}`}>
                  <FileText size={20} />
                </div>
                <div>
                  <p className="font-medium text-gray-900">Sử dụng CV trong hồ sơ</p>
                  {profile?.cvFilePath ? (
                    <p className="text-xs text-blue-600 truncate max-w-[200px]">Đã có CV</p>
                  ) : (
                    <p className="text-xs text-red-500">Chưa có CV trong hồ sơ</p>
                  )}
                </div>
              </div>
              {cvOption === "profile" && <CheckCircle className="text-blue-600" size={20} />}
            </div>

            {/* Option 2: Upload mới */}
            <div 
              onClick={() => setCvOption("upload")}
              className={`relative flex items-center p-4 border rounded-xl cursor-pointer transition-all ${
                cvOption === "upload" 
                  ? "border-blue-600 bg-blue-50/50 ring-1 ring-blue-600" 
                  : "border-gray-200 hover:border-gray-300"
              }`}
            >
              <div className="flex-1 flex items-center gap-3">
                <div className={`p-2 rounded-lg ${cvOption === "upload" ? "bg-blue-100 text-blue-600" : "bg-gray-100 text-gray-500"}`}>
                  <Upload size={20} />
                </div>
                <div className="w-full">
                  <p className="font-medium text-gray-900">Tải lên CV mới</p>
                  {cvOption === "upload" && (
                    <input 
                      type="file" 
                      accept=".pdf,.doc,.docx"
                      className="mt-2 text-sm text-gray-500 file:mr-4 file:py-1 file:px-3 file:rounded-full file:border-0 file:text-xs file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
                      onChange={(e) => setSelectedFile(e.target.files?.[0] || null)}
                      onClick={(e) => e.stopPropagation()} // Tránh click input kích hoạt lại div
                    />
                  )}
                </div>
              </div>
              {cvOption === "upload" && <CheckCircle className="text-blue-600" size={20} />}
            </div>
          </div>

          {/* Cover Letter */}
          <div className="space-y-2">
            <label className="text-sm font-semibold text-gray-700 flex justify-between">
              Thư giới thiệu <span className="text-gray-400 font-normal">(Không bắt buộc)</span>
            </label>
            <textarea
              rows={4}
              placeholder="Viết đôi lời giới thiệu về bản thân và lý do bạn phù hợp..."
              className="w-full p-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none resize-none"
              value={coverLetter}
              onChange={(e) => setCoverLetter(e.target.value)}
            />
          </div>

          {/* Footer Actions */}
          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2.5 border border-gray-300 text-gray-700 font-medium rounded-lg hover:bg-gray-50 transition-colors"
              disabled={loading}
            >
              Hủy
            </button>
            <button
              type="submit"
              className="flex-1 flex items-center justify-center gap-2 px-4 py-2.5 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-all shadow-sm disabled:opacity-70 disabled:cursor-not-allowed"
              disabled={loading}
            >
              {loading ? (
                <>
                  <Loader2 className="animate-spin" size={18} /> Đang xử lý...
                </>
              ) : (
                "Xác nhận ứng tuyển"
              )}
            </button>
          </div>

        </form>
      </div>
    </div>
  );
}