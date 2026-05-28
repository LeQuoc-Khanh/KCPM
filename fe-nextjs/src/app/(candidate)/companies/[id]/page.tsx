"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { MapPin, Globe, Users, ArrowLeft, Building, Mail, Phone, Briefcase } from "lucide-react";
import CompanyReviews from "@/components/features/company/CompanyReviews";
import { companyService, Company } from "@/services/companyService";
import toast from "react-hot-toast";

export default function CompanyDetailPage() {
  const params = useParams();
  const router = useRouter();
  const id = Number(params.id);
  
  const [company, setCompany] = useState<Company | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (id) {
      fetchCompanyDetail();
    }
  }, [id]);

  const fetchCompanyDetail = async () => {
    try {
      setIsLoading(true);
      const data = await companyService.getById(id);
      setCompany(data); 
    } catch (error) {
      toast.error("Không tìm thấy thông tin công ty");
      router.push("/jobs");
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) return <div className="p-10 text-center flex flex-col items-center gap-2">
    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
    <span className="text-gray-500">Đang tải thông tin công ty...</span>
  </div>;

  if (!company) return <div className="p-10 text-center text-red-500 font-medium">Không tìm thấy thông tin công ty.</div>;

  return (
    <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6">
      {/* Nút quay lại gọn gàng */}
      <button 
        onClick={() => router.back()} 
        className="group flex items-center text-gray-500 mb-6 hover:text-indigo-600 transition-colors font-medium"
      >
        <ArrowLeft size={18} className="mr-2 group-hover:-translate-x-1 transition-transform"/> Quay lại danh sách
      </button>

      {/* Header Banner - Tăng chiều cao banner cho thoáng */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden mb-8">
        <div className="h-48 bg-gradient-to-r from-indigo-600 to-blue-500 relative mb-5">
          {company.coverImageUrl && (
            <img src={company.coverImageUrl} className="w-full h-full object-cover" alt="banner" />
          )}
        </div>
        <div className="px-8 pb-8 relative -mt-16">
          <div className="flex flex-col md:flex-row items-center md:items-end -mt-16 gap-6">
            <div className="w-32 h-32 bg-white rounded-2xl shadow-lg border-4 border-white overflow-hidden flex-shrink-0">
              {company.logoUrl ? (
                <img src={company.logoUrl} alt={company.name} className="w-full h-full object-contain p-2"/>
              ) : (
                <div className="w-full h-full flex items-center justify-center bg-gray-50">
                   <Building className="text-gray-300" size={48} />
                </div>
              )}
            </div>
            <div className="flex-1 text-center md:text-left mb-2">
              <h1 className="text-3xl font-extrabold text-gray-900 mb-2">{company.name}</h1>
              <div className="flex flex-wrap justify-center md:justify-start gap-y-2 gap-x-6 text-gray-600">
                <span className="flex items-center gap-1.5"><MapPin size={18} className="text-gray-400"/> {company.address}</span>
                <span className="flex items-center gap-1.5"><Briefcase size={18} className="text-gray-400"/> {company.industry || "Công nghệ"}</span>
                {company.website && (
                  <a href={company.website} target="_blank" className="flex items-center gap-1.5 text-indigo-600 hover:text-indigo-700 font-medium">
                    <Globe size={18}/> Website công ty
                  </a>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Main Grid: 2 Cột */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Cột trái: Nội dung chính (Chiếm 8 phần) */}
        <div className="lg:col-span-8 space-y-8">
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
            <h3 className="text-xl font-bold text-gray-900 mb-4 flex items-center gap-2">
              <span className="w-1.5 h-6 bg-indigo-600 rounded-full"></span>
              Giới thiệu công ty
            </h3>
            <div className="text-gray-600 leading-relaxed whitespace-pre-line text-lg">
              {company.description || "Công ty hiện chưa cập nhật mô tả chi tiết."}
            </div>
          </div>

          {/* Phần Đánh giá đưa vào đây để có không gian rộng hơn */}
          <CompanyReviews companyId={id} />
        </div>

        {/* Cột phải: Thông tin nhanh (Chiếm 4 phần) */}
        <div className="lg:col-span-4 space-y-6">
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 sticky top-24">
            <h3 className="text-lg font-bold text-gray-900 mb-6 border-b pb-4">Thông tin liên hệ</h3>
            <div className="space-y-5">
              <div className="flex items-start gap-4">
                <div className="p-2.5 bg-gray-50 text-gray-500 rounded-xl">
                  <Users size={20} />
                </div>
                <div>
                  <p className="text-sm text-gray-400 font-medium uppercase tracking-wider">Quy mô</p>
                  <p className="text-gray-900 font-semibold">{company.size || "Đang cập nhật"} nhân viên</p>
                </div>
              </div>

              <div className="flex items-start gap-4">
                <div className="p-2.5 bg-gray-50 text-gray-500 rounded-xl">
                  <Mail size={20} />
                </div>
                <div>
                  <p className="text-sm text-gray-400 font-medium uppercase tracking-wider">Email</p>
                  <p className="text-gray-900 font-semibold break-all">{company.email || "contact@company.com"}</p>
                </div>
              </div>

              <div className="flex items-start gap-4">
                <div className="p-2.5 bg-gray-50 text-gray-500 rounded-xl">
                  <Phone size={20} />
                </div>
                <div>
                  <p className="text-sm text-gray-400 font-medium uppercase tracking-wider">Điện thoại</p>
                  <p className="text-gray-900 font-semibold">{company.phone || "Chưa cập nhật"}</p>
                </div>
              </div>
            </div>

          </div>
        </div>
      </div>
    </div>
  );
}