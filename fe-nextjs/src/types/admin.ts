export interface DashboardSummary {
  totalCandidates: number;
  totalRecruiters: number;
  totalActiveJobs: number;
  totalApplications: number;
  revenue?: number; // Nếu có tính năng thanh toán
}

export interface ApplicationsByDay {
  date: string;
  count: number;
}

// Giả sử API trả về thêm danh sách hoạt động gần đây
export interface RecentActivity {
  id: number;
  type: 'APPLICATION' | 'USER' | 'JOB';
  content: string;
  time: string;
  status: 'SUCCESS' | 'WARNING' | 'INFO';
}