// src/services/recruitmentService.ts
import api from "./api";
import {
  JobPosting,
  JobCreateRequest,
  CandidateApplication,
  ApplicationStatus,
  AIAnalysisDetail,
  CompanyProfile, // Nhớ import thêm interface này
  DashboardStats,
} from "@/types/recruitment";

// Alias type cho khớp với page.tsx
export type RecruiterJob = JobPosting;
export type RecruiterApplication = CandidateApplication;

export const recruitmentService = {
  // ==========================================
  // 1. NHÓM JOB (TIN TUYỂN DỤNG) - RECRUITER
  // ==========================================
  getMyJobs: async (): Promise<JobPosting[]> => {
    const res = await api.get("/recruiter/jobs/me");
    return res.data;
  },

  createJob: async (data: JobCreateRequest): Promise<JobPosting> => {
    const res = await api.post("/recruiter/jobs", data);
    return res.data;
  },

  deleteJob: async (id: number): Promise<void> => {
    await api.delete(`/recruiter/jobs/${id}`);
  },

  searchMyJobs: async (keyword: string): Promise<any> => {
    const res = await api.get("/recruiter/jobs/search", {
      params: { keyword },
    });
    return res.data.data;
  },

  // ==========================================
  // 2. NHÓM CÔNG TY (COMPANY) - RECRUITER
  // ==========================================
  getMyCompany: async (): Promise<CompanyProfile> => {
    const res = await api.get("/recruiter/company/me");
    return res.data;
  },

  updateCompany: async (data: CompanyProfile): Promise<CompanyProfile> => {
    const res = await api.put("/recruiter/company/me", data);
    return res.data;
  },

  // ==========================================
  // 3. NHÓM DASHBOARD - RECRUITER
  // ==========================================
  getDashboardStats: async (): Promise<DashboardStats> => {
    const response = await api.get("/recruiter/dashboard/stats");
    return response.data;
  },

  getRecentApplications: async (): Promise<CandidateApplication[]> => {
    const response = await api.get("/recruiter/dashboard/recent-applications");
    return response.data;
  },

  // ==========================================
  // 4. NHÓM QUẢN LÝ ỨNG VIÊN (APPLICATIONS)
  // ==========================================

  // Endpoint: /api/applications/job/{jobId}
  getApplicationsByJob: async (
    jobId: number,
  ): Promise<CandidateApplication[]> => {
    const res = await api.get(`/applications/job/${jobId}`);
    return res.data;
  },

  // Endpoint: /api/applications/{id}/status
  updateApplicationStatus: async (
    appId: number,
    status: ApplicationStatus,
    note?: string,
  ) => {
    const res = await api.put(`/applications/${appId}/status`, null, {
      params: { newStatus: status, recruiterNote: note },
    });
    return res.data;
  },

  // Endpoint: /api/applications/{id}/analysis
  getApplicationAnalysis: async (
    applicationId: number,
  ): Promise<AIAnalysisDetail> => {
    const res = await api.post(`/applications/${applicationId}/analysis`);
    return res.data.data || res.data;
  },

  // ==========================================
  // 5. NHÓM PUBLIC / CANDIDATE (XEM JOB)
  // ==========================================

  // Xem chi tiết Job (Public - không cần login hoặc Candidate)
  getJobDetail: async (id: number): Promise<JobPosting> => {
    const response = await api.get(`/recruiter/jobs/public/${id}`);
    return response.data;
  },

  // Kiểm tra trạng thái ứng tuyển của Candidate với Job này
  checkApplicationStatus: async (jobId: number): Promise<string | null> => {
    try {
      const res = await api.get(`/applications/check/${jobId}`);
      return res.data.status;
    } catch (e) {
      return null;
    }
  },

  // Lấy danh sách đơn đã nộp của Candidate
  getMyApplications: async (): Promise<any[]> => {
    const res = await api.get("/applications/me");
    return res.data.data || res.data;
  },

  getApplicationDetail: async (id: number): Promise<any> => {
    const res = await api.get(`/applications/${id}`);
    return res.data; // Trả về JobApplicationResponse
  },

  // ==========================================
  // 6. NHÓM UPLOAD (Thêm mới)
  // ==========================================
  uploadImage: async (file: File): Promise<string> => {
    const formData = new FormData();
    formData.append("file", file);

    const res = await api.post("/recruiter/company/upload-image", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    // Server trả về Map<String, String> -> { "url": "..." }
    return res.data.url;
  },
};

// Export alias
export * as recruiterService from "./recruitmentService";
