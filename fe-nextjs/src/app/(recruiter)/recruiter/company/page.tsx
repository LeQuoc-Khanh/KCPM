"use client";

import React, { useState, useEffect, useRef } from "react";
import { 
  Edit, Save, X, Camera, Loader2, 
  Building2, Globe, Users, MapPin, 
  Mail, Phone, Calendar, Info 
} from "lucide-react";
import { recruitmentService } from "@/services/recruitmentService";
import { CompanyProfile } from "@/types/recruitment";
import { toast } from "react-hot-toast";
import { useConfirm } from "@/context/ConfirmDialogContext";
import CompanyReviews from "@/components/features/company/CompanyReviews";
import Link from "next/link";
import { Plus } from "lucide-react";

export default function CompanyPage() {
  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [companyData, setCompanyData] = useState<CompanyProfile | null>(null);
  const [editData, setEditData] = useState<CompanyProfile | null>(null);
  const confirm = useConfirm();
  
  const logoInputRef = useRef<HTMLInputElement>(null);
  const coverInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    fetchCompanyData();
  }, []);

  const fetchCompanyData = async () => {
    try {
      setLoading(true);
      const data = await recruitmentService.getMyCompany();
      const formattedData: CompanyProfile = {
        ...data,
        name: data?.name || "",
        description: data?.description || "",
        logoUrl: data?.logoUrl || "",
        coverImageUrl: data?.coverImageUrl || "",
      };
      setCompanyData(formattedData);
      setEditData(formattedData);
    } catch (error) {
      toast.error("Không thể tải thông tin công ty");
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    if (!editData) return;
    const ok = await confirm({
      title: "Cập nhật thông tin",
      message: "Những thay đổi này sẽ hiển thị công khai với tất cả ứng viên. Bạn chắc chắn chứ?",
      confirmLabel: "Cập nhật ngay",
    });

    if (!ok) return;
    try {
      const updated = await recruitmentService.updateCompany(editData);
      setCompanyData(updated);
      setIsEditing(false);
      toast.success("Thông tin công ty đã được cập nhật!");
    } catch (error) {
      toast.error("Lỗi cập nhật. Vui lòng thử lại sau.");
    }
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>, field: "logoUrl" | "coverImageUrl") => {
    const file = e.target.files?.[0];
    if (!file || !editData) return;
    if (file.size > 5 * 1024 * 1024) return toast.error("Ảnh quá lớn (Tối đa 5MB)");

    try {
      setUploading(true);
      const url = await recruitmentService.uploadImage(file);
      setEditData({ ...editData, [field]: url });
      toast.success("Đã tải ảnh lên!");
    } catch (error) {
      toast.error("Lỗi upload ảnh");
    } finally {
      setUploading(false);
      e.target.value = "";
    }
  };

  if (loading) return (
    <div className="flex flex-col items-center justify-center min-h-[400px] gap-3">
      <Loader2 className="animate-spin text-indigo-600" size={40} />
      <p className="text-gray-500 font-medium">Đang đồng bộ dữ liệu doanh nghiệp...</p>
    </div>
  );

  if (!editData) return <div className="p-8 text-center text-red-500">Dữ liệu không khả dụng.</div>;

  return (
    <div className="max-w-7xl mx-auto p-4 md:p-6 space-y-8 pb-20">
      {/* Input File Ẩn */}
      <input type="file" ref={coverInputRef} hidden accept="image/*" onChange={(e) => handleFileUpload(e, "coverImageUrl")} />
      <input type="file" ref={logoInputRef} hidden accept="image/*" onChange={(e) => handleFileUpload(e, "logoUrl")} />

      {/* Header Section */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
        <div>
          <h1 className="text-3xl font-extrabold text-gray-900 tracking-tight">Hồ sơ Doanh nghiệp</h1>
          <p className="text-gray-500 mt-1 flex items-center gap-2">
            <Info size={16} /> Quản lý cách thương hiệu của bạn hiển thị với ứng viên.
          </p>
        </div>
        <div className="flex gap-3 w-full md:w-auto">
          {!isEditing ? (
            <button
              onClick={() => setIsEditing(true)}
              className="flex-1 md:flex-none flex items-center justify-center bg-indigo-600 text-white px-6 py-2.5 rounded-xl hover:bg-indigo-700 transition-all shadow-md shadow-indigo-100 font-bold"
            >
              <Edit className="w-4 h-4 mr-2" /> Chỉnh sửa hồ sơ
            </button>
          ) : (
            <>
              <button
                onClick={handleSave}
                disabled={uploading}
                className="flex-1 md:flex-none flex items-center justify-center bg-emerald-600 text-white px-6 py-2.5 rounded-xl hover:bg-emerald-700 transition-all disabled:opacity-50 font-bold"
              >
                {uploading ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <Save className="w-4 h-4 mr-2" />}
                Lưu thay đổi
              </button>
              <button
                onClick={() => { setEditData(companyData); setIsEditing(false); }}
                className="flex-1 md:flex-none flex items-center justify-center bg-white text-gray-700 border border-gray-200 px-6 py-2.5 rounded-xl hover:bg-gray-50 transition-all font-bold"
              >
                <X className="w-4 h-4 mr-2" /> Hủy
              </button>
            </>
          )}
        </div>
      </div>

      {/* Main Profile Card */}
      <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
        {/* Cover Section */}
        <div className="relative h-64 bg-gradient-to-r from-slate-200 to-slate-300 group">
          {editData.coverImageUrl ? (
            <img src={editData.coverImageUrl} alt="Cover" className="w-full h-full object-cover" />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-gray-400">Chưa có ảnh bìa</div>
          )}
          {isEditing && (
            <div className="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all">
              <button
                onClick={() => coverInputRef.current?.click()}
                className="bg-white/20 backdrop-blur-lg border border-white/40 text-white px-5 py-2.5 rounded-2xl flex items-center hover:bg-white/30 transition-all"
              >
                <Camera className="w-5 h-5 mr-2" /> Thay đổi ảnh bìa
              </button>
            </div>
          )}
        </div>

        {/* Info Content */}
        <div className="px-8 pb-8">
          <div className="flex flex-col md:flex-row items-center md:items-end -mt-16 gap-6 mb-10">
            {/* Logo Area */}
            <div className="relative w-40 h-40 rounded-3xl bg-white border-4 border-white shadow-xl flex-shrink-0 group overflow-hidden">
              {editData.logoUrl ? (
                <img src={editData.logoUrl} alt="Logo" className="w-full h-full object-contain p-2" />
              ) : (
                <div className="w-full h-full flex items-center justify-center bg-gray-50 text-gray-300">
                   <Building2 size={60} />
                </div>
              )}
              {isEditing && (
                <div
                  className="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all cursor-pointer"
                  onClick={() => logoInputRef.current?.click()}
                >
                  <Camera className="w-8 h-8 text-white" />
                </div>
              )}
            </div>

            {/* Name & Title */}
            <div className="flex-1 text-center md:text-left space-y-2">
              {isEditing ? (
                <input
                  type="text"
                  value={editData.name}
                  onChange={(e) => setEditData({ ...editData, name: e.target.value })}
                  className="text-3xl font-black w-full border-b-2 border-indigo-500 focus:outline-none bg-transparent pb-1"
                  placeholder="Nhập tên công ty..."
                />
              ) : (
                <h2 className="text-4xl font-black text-gray-900">{companyData?.name || "Tên công ty"}</h2>
              )}
              <div className="flex flex-wrap justify-center md:justify-start gap-4 text-gray-500 font-medium">
                <span className="flex items-center gap-1.5"><Building2 size={18} /> {companyData?.industry || "Chưa cập nhật ngành nghề"}</span>
                <span className="flex items-center gap-1.5"><MapPin size={18} /> {companyData?.address?.split(',').pop() || "Địa điểm"}</span>
              </div>
            </div>
          </div>

          {/* Grid Details */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
            {/* Left Column: Bio & Reviews */}
            <div className="lg:col-span-8 space-y-10">
              <section className="space-y-4">
                <h3 className="text-xl font-bold text-gray-900 flex items-center gap-2">
                  <span className="w-1.5 h-6 bg-indigo-600 rounded-full"></span>
                  Giới thiệu doanh nghiệp
                </h3>
                {isEditing ? (
                  <textarea
                    value={editData.description}
                    onChange={(e) => setEditData({ ...editData, description: e.target.value })}
                    className="w-full border-2 border-gray-100 rounded-2xl p-4 text-gray-600 h-48 focus:border-indigo-500 focus:outline-none transition-all"
                    placeholder="Mô tả tầm nhìn, sứ mệnh và văn hóa công ty..."
                  />
                ) : (
                  <div className="bg-gray-50/50 rounded-2xl p-6 text-gray-700 leading-relaxed text-lg whitespace-pre-line border border-gray-100">
                    {companyData?.description || "Chưa có mô tả chi tiết."}
                  </div>
                )}
              </section>

              {/* Reviews Section */}
              <section className="pt-6">
                <div className="flex items-center justify-between mb-6">
                  <h3 className="text-xl font-bold text-gray-900">Đánh giá từ ứng viên</h3>
                </div>
                {companyData?.id && <CompanyReviews companyId={companyData.id} />}
              </section>
            </div>

            {/* Right Column: Metadata & Contact */}
            <div className="lg:col-span-4 space-y-6">
              <div className="bg-white rounded-2xl border border-gray-100 p-6 space-y-6 shadow-sm">
                <h4 className="font-bold text-gray-900 border-b pb-4">Thông tin chi tiết</h4>
                
                <div className="space-y-5">
                  {/* Quy mô */}
                  <div className="flex items-start gap-4">
                    <div className="p-2.5 bg-blue-50 text-blue-600 rounded-xl"><Users size={20} /></div>
                    <div className="flex-1">
                      <p className="text-xs text-gray-400 font-bold uppercase tracking-wider">Quy mô</p>
                      {isEditing ? (
                        <select
                          value={editData.size || ""}
                          onChange={(e) => setEditData({ ...editData, size: e.target.value })}
                          className="w-full border rounded-lg px-2 py-1 mt-1 text-sm outline-none border-gray-200"
                        >
                          <option value="">Chọn quy mô</option>
                          <option value="1-50">1-50 nhân viên</option>
                          <option value="51-200">51-200 nhân viên</option>
                          <option value="201-500">201-500 nhân viên</option>
                          <option value="500+">500+ nhân viên</option>
                        </select>
                      ) : (
                        <p className="text-gray-900 font-semibold">{companyData?.size || "Chưa cập nhật"}</p>
                      )}
                    </div>
                  </div>

                  {/* Website */}
                  <div className="flex items-start gap-4">
                    <div className="p-2.5 bg-emerald-50 text-emerald-600 rounded-xl"><Globe size={20} /></div>
                    <div className="flex-1 overflow-hidden">
                      <p className="text-xs text-gray-400 font-bold uppercase tracking-wider">Website</p>
                      {isEditing ? (
                        <input
                          type="text"
                          value={editData.website || ""}
                          onChange={(e) => setEditData({ ...editData, website: e.target.value })}
                          className="w-full border rounded-lg px-2 py-1 mt-1 text-sm outline-none border-gray-200"
                        />
                      ) : (
                        <a href={companyData?.website} target="_blank" className="text-indigo-600 font-semibold hover:underline block truncate">
                          {companyData?.website || "Chưa cập nhật"}
                        </a>
                      )}
                    </div>
                  </div>

                  {/* Email */}
                  <div className="flex items-start gap-4">
                    <div className="p-2.5 bg-purple-50 text-purple-600 rounded-xl"><Mail size={20} /></div>
                    <div className="flex-1 overflow-hidden">
                      <p className="text-xs text-gray-400 font-bold uppercase tracking-wider">Email liên hệ</p>
                      {isEditing ? (
                        <input
                          type="email"
                          value={editData.email || ""}
                          onChange={(e) => setEditData({ ...editData, email: e.target.value })}
                          className="w-full border rounded-lg px-2 py-1 mt-1 text-sm outline-none border-gray-200"
                        />
                      ) : (
                        <p className="text-gray-900 font-semibold truncate">{companyData?.email || "Chưa cập nhật"}</p>
                      )}
                    </div>
                  </div>

                  {/* Địa chỉ */}
                  <div className="flex items-start gap-4">
                    <div className="p-2.5 bg-orange-50 text-orange-600 rounded-xl"><MapPin size={20} /></div>
                    <div className="flex-1">
                      <p className="text-xs text-gray-400 font-bold uppercase tracking-wider">Trụ sở chính</p>
                      {isEditing ? (
                        <input
                          type="text"
                          value={editData.address || ""}
                          onChange={(e) => setEditData({ ...editData, address: e.target.value })}
                          className="w-full border rounded-lg px-2 py-1 mt-1 text-sm outline-none border-gray-200"
                        />
                      ) : (
                        <p className="text-gray-900 font-semibold leading-snug">{companyData?.address || "Chưa cập nhật"}</p>
                      )}
                    </div>
                  </div>
                </div>
              </div>

              {/* Recruitment Status Card */}
              <div className="bg-indigo-600 rounded-2xl p-6 text-white shadow-lg shadow-indigo-100">
                <div className="flex items-center gap-3 mb-4">
                  <div className="p-2 bg-white/20 rounded-lg"><Calendar size={20} /></div>
                  <h4 className="font-bold">Tin tuyển dụng</h4>
                </div>
                <p className="text-indigo-100 text-sm mb-4">Hồ sơ công ty hoàn thiện giúp tăng 40% tỷ lệ ứng tuyển từ ứng viên tiềm năng.</p>
                <Link
                  href="/recruiter/manage-jobs"
                  className="w-full bg-white text-indigo-600 font-bold py-2 rounded-xl hover:bg-indigo-50 transition-colors flex items-center justify-center"
                >
                  <Plus className="w-4 h-4 mr-2" /> Đăng tin mới
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}