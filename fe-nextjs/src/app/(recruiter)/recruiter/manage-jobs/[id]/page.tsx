"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { recruitmentService } from "@/services/recruitmentService";
import { JobPosting, JobStatus } from "@/types/recruitment";
import { 
  ArrowLeft, 
  MapPin, 
  DollarSign, 
  Calendar, 
  Clock, 
  Briefcase, 
  Edit, 
  Users, 
  Building 
} from "lucide-react";
import toast from "react-hot-toast";

export default function JobDetailPage() {
  const params = useParams();
  const router = useRouter();
  const jobId = Number(params.id);

  const [job, setJob] = useState<JobPosting | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (jobId) {
      loadJobDetail();
    }
  }, [jobId]);

  const loadJobDetail = async () => {
    try {
      setIsLoading(true);
      const data = await recruitmentService.getJobDetail(jobId);
      setJob(data);
    } catch (error) {
      toast.error("Không thể tải thông tin công việc");
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "OPEN":
      case "PUBLISHED":
        return <span className="bg-green-100 text-green-700 px-3 py-1 rounded-full text-sm font-medium">Đang tuyển</span>;
      case "CLOSED":
        return <span className="bg-red-100 text-red-700 px-3 py-1 rounded-full text-sm font-medium">Đã đóng</span>;
      case "DRAFT":
        return <span className="bg-gray-100 text-gray-700 px-3 py-1 rounded-full text-sm font-medium">Nháp</span>;
      default:
        return <span className="bg-blue-100 text-blue-700 px-3 py-1 rounded-full text-sm font-medium">{status}</span>;
    }
  };

  if (isLoading) return <div className="p-6 text-center text-gray-500">Đang tải dữ liệu...</div>;
  if (!job) return <div className="p-6 text-center text-red-500">Không tìm thấy công việc này.</div>;

  return (
    <div className="space-y-6 max-w-5xl mx-auto pb-10">
      {/* Header & Navigation */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <button
          onClick={() => router.back()}
          className="flex items-center text-gray-500 hover:text-gray-800 w-fit"
        >
          <ArrowLeft size={18} className="mr-1" /> Quay lại danh sách
        </button>
        
        {/* <div className="flex gap-3">
          
          <button className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 font-medium shadow-sm">
            <Edit size={18} /> Chỉnh sửa tin
          </button>
        </div> */}
      </div>

      {/* Main Content Card */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        {/* Banner / Header Info */}
        <div className="p-6 border-b border-gray-100 bg-gray-50/50">
          <div className="flex flex-col md:flex-row justify-between items-start gap-4">
            <div>
              <div className="flex items-center gap-3 mb-2">
                {getStatusBadge(job.status)}
                <span className="text-sm text-gray-500 flex items-center gap-1">
                  <Clock size={14} /> Đăng ngày: {new Date(job.createdAt || "").toLocaleDateString('vi-VN')}
                </span>
              </div>
              <h1 className="text-3xl font-bold text-gray-900 mb-2">{job.title}</h1>
              <div className="flex items-center gap-2 text-gray-600">
                <Building size={18} className="text-gray-400"/>
                <span className="font-medium">{job.companyName || "Công ty chưa cập nhật"}</span>
              </div>
            </div>
            
            <div className="flex flex-col items-end gap-2">
               <div className="text-2xl font-bold text-indigo-600">{job.salaryRange}</div>
               <div className="text-sm text-gray-500">Mức lương dự kiến</div>
            </div>
          </div>
        </div>

        <div className="p-6 grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Left Column: Main Details */}
          <div className="lg:col-span-2 space-y-8">
            
            {/* Mô tả công việc */}
            <section>
              <h3 className="text-lg font-bold text-gray-900 mb-3 flex items-center gap-2">
                <Briefcase size={20} className="text-indigo-500" /> Mô tả công việc
              </h3>
              <div className="text-gray-700 leading-relaxed whitespace-pre-line bg-gray-50 p-4 rounded-lg">
                {job.description || "Chưa có mô tả chi tiết."}
              </div>
            </section>

            {/* Yêu cầu */}
            <section>
              <h3 className="text-lg font-bold text-gray-900 mb-3 flex items-center gap-2">
                <Users size={20} className="text-indigo-500" /> Yêu cầu ứng viên
              </h3>
              <div className="text-gray-700 leading-relaxed whitespace-pre-line bg-gray-50 p-4 rounded-lg">
                {job.requirements || "Chưa có yêu cầu chi tiết."}
              </div>
            </section>

            {/*
            { Quyền lợi}
            <section>
              <h3 className="text-lg font-bold text-gray-900 mb-3 flex items-center gap-2">
                <DollarSign size={20} className="text-indigo-500" /> Quyền lợi
              </h3>
              <div className="text-gray-700 leading-relaxed whitespace-pre-line bg-gray-50 p-4 rounded-lg">
                {job.benefits || "Chưa có thông tin quyền lợi."}
              </div>
            </section>
            */}

            {/* Kỹ năng (Tags) */}
            {job.extractedSkills && job.extractedSkills.length > 0 && (
              <section>
                <h3 className="text-lg font-bold text-gray-900 mb-3">Kỹ năng yêu cầu</h3>
                <div className="flex flex-wrap gap-2">
                  {job.extractedSkills.map((skill, index) => (
                    <span 
                      key={index} 
                      className="px-3 py-1 bg-blue-50 text-blue-700 rounded-full text-sm font-medium border border-blue-100"
                    >
                      {skill}
                    </span>
                  ))}
                </div>
              </section>
            )}
          </div>

          {/* Right Column: Summary Info */}
          <div className="space-y-6">
            <div className="bg-gray-50 rounded-xl p-5 border border-gray-100 sticky top-4">
              <h3 className="font-semibold text-gray-900 mb-4 pb-2 border-b">Thông tin chung</h3>
              
              <ul className="space-y-4">
                <li className="flex items-start gap-3">
                  <div className="mt-1 p-2 bg-white rounded-full shadow-sm text-indigo-600">
                    <MapPin size={18} />
                  </div>
                  <div>
                    <span className="block text-sm text-gray-500">Địa điểm làm việc</span>
                    <span className="font-medium text-gray-900">{job.location}</span>
                  </div>
                </li>

                <li className="flex items-start gap-3">
                  <div className="mt-1 p-2 bg-white rounded-full shadow-sm text-indigo-600">
                    <Calendar size={18} />
                  </div>
                  <div>
                    <span className="block text-sm text-gray-500">Hạn nộp hồ sơ</span>
                    <span className="font-medium text-gray-900">
                      {new Date(job.expiryDate).toLocaleDateString('vi-VN')}
                    </span>
                  </div>
                </li>

                <li className="flex items-start gap-3">
                  <div className="mt-1 p-2 bg-white rounded-full shadow-sm text-indigo-600">
                    <Users size={18} />
                  </div>
                  <div>
                    <span className="block text-sm text-gray-500">Số lượng tuyển</span>
                    <span className="font-medium text-gray-900">Không giới hạn</span>
                  </div>
                </li>
              </ul>

              <div className="mt-6 pt-4 border-t border-gray-200">
                <div className="text-sm text-gray-500 mb-2">Thống kê ứng tuyển</div>
                <div className="flex items-center justify-between">
                  <span className="font-medium">Tổng hồ sơ:</span>
                  <span className="bg-indigo-100 text-indigo-700 px-2 py-0.5 rounded text-sm font-bold">
                    {job.applicationCount || 0}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}