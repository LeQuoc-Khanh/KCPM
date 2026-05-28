// src/types/gamification.ts

export interface LeaderboardEntry {
  userId: number;
  fullName: string;
  avatarUrl: string;
  rank: number;
  score: number;
  trend?: "UP" | "DOWN" | "STABLE";
}

export interface UserRankDetail {
  userId: number;
  fullName: string;
  avatarUrl: string;
  rank: number;
  score: number;
  period: string;
}

export interface Mission {
  code: string;
  name: string;
  description: string;
  points: number;
  dailyLimit: number;
  completedCount: number;
  isFinished?: boolean;
}

export interface LeaderboardResponse<T> {
  success: boolean;
  data: T;
  meta?: any;
}
