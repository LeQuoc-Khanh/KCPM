export interface Experience {
  // Các trường cho hiển thị Profile (Khớp với ExperienceDTO của BE)
  company?: string;
  role?: string;
  startDate?: string;
  endDate?: string;
  description?: string;

  // Các trường dự phòng cho AI (nếu dùng chung)
  totalYears?: number;
  level?: string;
  companies?: string[];
}

export interface CandidateProfile {
  id: number;
  // 4 trường mới thêm để fix lỗi
  userFullName?: string;
  fullName?: string;
  email?: string;
  avatarUrl?: string;

  websiteUrl?: string;
  cvFilePath?: string;
  skills: string[];
  experiences: Experience[];
  educations?: any[];
  aboutMe?: string;
  phoneNumber?: string;
  address?: string;
  linkedInUrl?: string;
}

export interface CandidateResponse {
  message: string;
  data: CandidateProfile;
}
