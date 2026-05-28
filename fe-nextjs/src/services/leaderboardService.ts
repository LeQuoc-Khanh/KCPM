import api from "./api";
import {
  LeaderboardEntry,
  UserRankDetail,
  Mission,
  LeaderboardResponse,
} from "@/types/gamification";

export interface LeaderboardLog {
  id: number;
  userId: number;
  fullName: string;
  role: string;
  actionType: string;
  points: number;
  createdAt: string;
  refId?: number;
}

export const leaderboardService = {
  // Lấy Top Xếp hạng (Hỗ trợ thêm YEAR)
  getTop: async (
    role: "CANDIDATE" | "RECRUITER",
    period: "WEEK" | "MONTH" | "YEAR",
    limit = 50,
  ) => {
    const response = await api.get<LeaderboardResponse<LeaderboardEntry[]>>(
      "/leaderboard",
      {
        params: { role, period, limit },
      },
    );
    return response.data.data;
  },

  // Lấy lịch sử cộng điểm (Logs) - Cho phép lấy nhiều dòng hơn
  getRecentLogs: async (limit: number = 50) => {
    const response = await api.get<{
      success: boolean;
      data: LeaderboardLog[];
    }>("/leaderboard/logs", {
      params: { limit },
    });
    return response.data.data;
  },

  // Lấy hạng của tôi
  getMyRank: async (
    userId: number,
    role: "CANDIDATE" | "RECRUITER",
    period: "WEEK" | "MONTH" = "WEEK",
  ) => {
    const response = await api.get<LeaderboardResponse<UserRankDetail>>(
      "/leaderboard/me",
      {
        params: { userId, role, period },
      },
    );
    return response.data.data;
  },

  // Lấy danh sách nhiệm vụ
  getMissions: async (role: "CANDIDATE" | "RECRUITER", userId?: number) => {
    // Thêm userId?: number
    const response = await api.get<LeaderboardResponse<Mission[]>>(
      "/leaderboard/missions",
      {
        params: { role, userId }, // Truyền userId vào params
      },
    );
    return response.data.data;
  },
};
