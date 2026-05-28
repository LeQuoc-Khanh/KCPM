// src/types/recruitment.ts

export enum JobStatus {
  PUBLISHED = "PUBLISHED",
  CLOSED = "CLOSED",
  DRAFT = "DRAFT",
  OPEN = "OPEN",
}

// Enum chuẩn khớp với Backend
export enum ApplicationStatus {
  PUBLISHED = "PUBLISHED", // Đang tuyển
  APPLIED = "APPLIED", // Mới ứng tuyển
  PENDING = "PENDING", // Chờ xử lý (mặc định)
  SCREENING = "SCREENING", // Đang sàng lọc
  INTERVIEW = "INTERVIEW", // Đã duyệt phỏng vấn (Thay cho APPROVED)
  OFFERED = "OFFERED", // Đã gửi offer
  REJECTED = "REJECTED", // Từ chối
  HIRED = "HIRED", // Đã tuyển
}

export interface JobPosting {
  id: number;
  title: string;
  location: string;
  salaryRange: string;
  expiryDate: string;
  status: JobStatus;
  applicationCount?: number;
  description?: string;
  requirements?: string;
  benefits?: string;
  createdAt?: string;
  extractedSkills?: string[];

  // Thông tin cơ sở
  companyId?: number;
  companyName?: string;
  companyLogo?: string;
  companyWebsite?: string;
  companyDescription?: string;
  companyAddress?: string;
}

export interface CandidateApplication {
  id: number;
  studentName?: string;
  candidateName?: string; // Backend có thể trả về trường này
  email?: string;
  phone?: string;
  matchScore: number;
  status: ApplicationStatus;
  cvUrl: string;
  jobTitle?: string;
  appliedAt?: string;

  aiEvaluation?: string;
  matchedSkillsList?: string;
  missingSkillsList?: string;

  recruiterNote?: string;
}

export interface JobCreateRequest {
  title: string;
  description: string;
  requirements: string;
  location: string;
  salaryRange: string;
  expiryDate: string;
}

export interface AIAnalysisDetail {
  id?: number;
  email?: string;
  phone?: string;
  matchPercentage?: number;
  matchScore?: number;
  evaluation?: string;
  aiEvaluation?: string;
  learningPath?: string;
  careerAdvice?: string;
  matchedSkillsList?: string[];
  missingSkillsList?: string[];
  otherHardSkillsList?: string[];
  otherSoftSkillsList?: string[];
  recommendedSkillsList?: string[];
  matchedSkillsCount?: number;
  missingSkillsCount?: number;
  otherHardSkillsCount?: number;
  otherSoftSkillsCount?: number;
  recommendedSkillsCount?: number;
  candidateName?: string;
  studentName?: string;
  jobTitle?: string;
  status?: ApplicationStatus; // <--- Quan trọng: Để fix lỗi appDetail.status
  cvUrl?: string; // <--- Quan trọng: Để fix lỗi appDetail.cvUrl
  appliedAt?: string; // <--- Quan trọng: Để fix lỗi appDetail.appliedAt
  recruiterNote?: string;
}

export interface CompanyProfile {
  id: number;
  name: string;
  email?: string;
  phone?: string;
  logoUrl?: string; // Thay đổi từ 'logo' thành 'logoUrl'
  coverImageUrl?: string; // Thêm mới trường này
  website?: string;
  description?: string;
  address?: string;
  industry?: string;
  size?: string;
  foundedYear?: string; // Đồng bộ kiểu String với Backend DTO
}

export interface DashboardStats {
  totalActiveJobs: number;
  totalCandidates: number;
  newCandidatesToday: number;
  pipelineStats: { [key: string]: number };
}
