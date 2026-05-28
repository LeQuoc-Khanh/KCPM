"use client";

import React, { useEffect, useState } from "react";
import {
  leaderboardService,
  LeaderboardLog,
} from "@/services/leaderboardService";
import { LeaderboardEntry } from "@/types/gamification";
import {
  Trophy,
  Users,
  Briefcase,
  Calendar,
  Search,
  Download,
  Medal,
  Crown,
  History,
  Clock,
  ArrowUpRight,
} from "lucide-react";

// Map tên hành động sang tiếng Việt cho dễ đọc
const ACTION_MAP: Record<string, string> = {
  LOGIN_DAILY: "Đăng nhập hàng ngày",
  APPLY: "Ứng tuyển việc làm",
  UPLOAD_CV: "Tải lên CV",
  INTERVIEW_PRACTICE: "Phỏng vấn AI",
  JOB_POST_APPROVED: "Đăng tin tuyển dụng",
  REVIEW_CV: "Duyệt hồ sơ",
  HIRED: "Tuyển dụng thành công",
};

export default function AdminLeaderboardPage() {
  const [activeTab, setActiveTab] = useState<"RANKING" | "LOGS">("RANKING");

  // State cho Ranking
  const [roleFilter, setRoleFilter] = useState<"CANDIDATE" | "RECRUITER">(
    "CANDIDATE",
  );
  const [periodFilter, setPeriodFilter] = useState<"WEEK" | "MONTH" | "YEAR">(
    "WEEK",
  );
  const [rankData, setRankData] = useState<LeaderboardEntry[]>([]);

  // State cho Logs
  const [logData, setLogData] = useState<LeaderboardLog[]>([]);

  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");

  useEffect(() => {
    fetchData();
  }, [activeTab, roleFilter, periodFilter]);

  const fetchData = async () => {
    setLoading(true);
    try {
      if (activeTab === "RANKING") {
        // Lấy Top 100 cho Admin xem thoải mái
        const res = await leaderboardService.getTop(
          roleFilter,
          periodFilter,
          100,
        );
        setRankData(res || []);
      } else {
        // Lấy 100 log gần nhất
        const res = await leaderboardService.getRecentLogs(100);
        setLogData(res || []);
      }
    } catch (error) {
      console.error("Lỗi tải dữ liệu:", error);
    } finally {
      setLoading(false);
    }
  };

  // Filter Client-side cho Ranking
  const filteredRank = rankData.filter((u) =>
    u.fullName?.toLowerCase().includes(searchTerm.toLowerCase()),
  );

  // Filter Client-side cho Logs
  const filteredLogs = logData.filter(
    (l) =>
      l.fullName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      l.actionType?.toLowerCase().includes(searchTerm.toLowerCase()),
  );

  const renderRankIcon = (rank: number) => {
    if (rank === 1)
      return <Crown className="w-5 h-5 text-yellow-500 fill-yellow-500" />;
    if (rank === 2)
      return <Medal className="w-5 h-5 text-gray-400 fill-gray-200" />;
    if (rank === 3)
      return <Medal className="w-5 h-5 text-orange-400 fill-orange-200" />;
    return <span className="font-bold text-gray-600 text-sm">#{rank}</span>;
  };

  return (
    <div className="p-6 space-y-6 bg-gray-50 min-h-screen">
      {/* HEADER & TABS */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-800 flex items-center gap-2">
            <Trophy className="text-yellow-600" />
            Quản trị Gamification
          </h1>
          <p className="text-gray-500 text-sm mt-1">
            Theo dõi xếp hạng và lịch sử điểm số toàn hệ thống.
          </p>
        </div>

        {/* Main Tab Switcher */}
        <div className="bg-white p-1 rounded-lg border border-gray-200 shadow-sm flex">
          <button
            onClick={() => setActiveTab("RANKING")}
            className={`px-4 py-2 rounded-md text-sm font-semibold flex items-center gap-2 transition-all ${
              activeTab === "RANKING"
                ? "bg-blue-50 text-blue-700"
                : "text-gray-600 hover:bg-gray-50"
            }`}
          >
            <Trophy size={16} /> Bảng Xếp Hạng
          </button>
          <div className="w-px bg-gray-200 my-2 mx-1"></div>
          <button
            onClick={() => setActiveTab("LOGS")}
            className={`px-4 py-2 rounded-md text-sm font-semibold flex items-center gap-2 transition-all ${
              activeTab === "LOGS"
                ? "bg-blue-50 text-blue-700"
                : "text-gray-600 hover:bg-gray-50"
            }`}
          >
            <History size={16} /> Nhật Ký Điểm
          </button>
        </div>
      </div>

      {/* TOOLBAR (Filter & Search) */}
      <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 flex flex-col md:flex-row justify-between gap-4">
        {/* Left Filters (Chỉ hiện khi ở Tab Ranking) */}
        {activeTab === "RANKING" ? (
          <div className="flex flex-wrap gap-3">
            <div className="flex bg-gray-100 p-1 rounded-lg">
              <button
                onClick={() => setRoleFilter("CANDIDATE")}
                className={`px-3 py-1.5 rounded-md text-xs font-semibold flex items-center gap-1 transition ${
                  roleFilter === "CANDIDATE"
                    ? "bg-white shadow text-gray-800"
                    : "text-gray-500"
                }`}
              >
                <Users size={14} /> Ứng viên
              </button>
              <button
                onClick={() => setRoleFilter("RECRUITER")}
                className={`px-3 py-1.5 rounded-md text-xs font-semibold flex items-center gap-1 transition ${
                  roleFilter === "RECRUITER"
                    ? "bg-white shadow text-gray-800"
                    : "text-gray-500"
                }`}
              >
                <Briefcase size={14} /> Nhà tuyển dụng
              </button>
            </div>

            <div className="flex items-center bg-white border border-gray-200 rounded-lg px-2">
              <Calendar size={14} className="text-gray-400 ml-2" />
              <select
                value={periodFilter}
                onChange={(e) => setPeriodFilter(e.target.value as any)}
                className="text-gray-700 text-sm px-2 py-2 outline-none cursor-pointer bg-transparent"
              >
                <option value="WEEK">Tuần Này</option>
                <option value="MONTH">Tháng Này</option>
                <option value="YEAR">Năm Này</option>
              </select>
            </div>
          </div>
        ) : (
          <div className="flex items-center text-sm text-gray-500 italic">
            <History size={16} className="mr-2" /> Đang hiển thị 100 giao dịch
            cộng điểm gần nhất.
          </div>
        )}

        {/* Right: Search & Export */}
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search
              className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
              size={16}
            />
            <input
              type="text"
              placeholder="Tìm kiếm..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-9 pr-4 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 w-48 lg:w-64"
            />
          </div>
          <button className="p-2 border border-gray-200 rounded-lg hover:bg-gray-50 text-gray-600">
            <Download size={18} />
          </button>
        </div>
      </div>

      {/* CONTENT TABLE */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden min-h-[400px]">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-500 uppercase font-semibold text-xs">
              {activeTab === "RANKING" ? (
                <tr>
                  <th className="px-6 py-4 text-center w-20">Hạng</th>
                  <th className="px-6 py-4 text-left">Thành viên</th>
                  <th className="px-6 py-4 text-center">Vai trò</th>
                  <th className="px-6 py-4 text-right">Tổng điểm</th>
                  <th className="px-6 py-4 text-center">Xu hướng</th>
                </tr>
              ) : (
                <tr>
                  <th className="px-6 py-4 text-left">Thời gian</th>
                  <th className="px-6 py-4 text-left">Thành viên</th>
                  <th className="px-6 py-4 text-left">Hành động</th>
                  <th className="px-6 py-4 text-left">Chi tiết (RefID)</th>
                  <th className="px-6 py-4 text-right">Điểm số</th>
                </tr>
              )}
            </thead>

            <tbody className="divide-y divide-gray-100">
              {loading ? (
                [...Array(5)].map((_, i) => (
                  <tr key={i} className="animate-pulse">
                    <td colSpan={5} className="px-6 py-4">
                      <div className="h-8 bg-gray-100 rounded w-full"></div>
                    </td>
                  </tr>
                ))
              ) : (
                <>
                  {/* TAB RANKING */}
                  {activeTab === "RANKING" &&
                    (filteredRank.length > 0 ? (
                      filteredRank.map((entry, index) => (
                        <tr
                          key={entry.userId}
                          className="hover:bg-blue-50/20 transition-colors"
                        >
                          <td className="px-6 py-4 text-center">
                            <div className="flex justify-center">
                              {renderRankIcon(index + 1)}
                            </div>
                          </td>
                          <td className="px-6 py-4">
                            <img
                              src={entry.avatarUrl || "/default-avatar.png"}
                              alt={entry.fullName}
                              className={`w-10 h-10 rounded-full object-cover border-2 border-gray-100`}
                            />
                            <div className="flex items-center gap-3">
                              <span className="font-semibold text-gray-700">
                                {entry.fullName}
                              </span>
                            </div>
                          </td>
                          <td className="px-6 py-4 text-center">
                            <span
                              className={`text-[10px] px-2 py-1 rounded-full font-bold ${roleFilter === "CANDIDATE" ? "bg-blue-100 text-blue-700" : "bg-purple-100 text-purple-700"}`}
                            >
                              {roleFilter}
                            </span>
                          </td>
                          <td className="px-6 py-4 text-right">
                            <span className="font-bold text-blue-600 bg-blue-50 px-3 py-1 rounded-full">
                              {/* FIX: Đổi từ totalPoints sang score để tránh lỗi undefined */}
                              {entry.score?.toLocaleString() || 0}
                            </span>
                          </td>
                          <td className="px-6 py-4 text-center text-gray-400">
                            {/* Placeholder cho trend */}-
                          </td>
                        </tr>
                      ))
                    ) : (
                      <tr>
                        <td
                          colSpan={5}
                          className="text-center py-12 text-gray-500"
                        >
                          Không có dữ liệu xếp hạng.
                        </td>
                      </tr>
                    ))}

                  {/* TAB LOGS */}
                  {activeTab === "LOGS" &&
                    (filteredLogs.length > 0 ? (
                      filteredLogs.map((log) => (
                        <tr
                          key={log.id}
                          className="hover:bg-gray-50 transition-colors"
                        >
                          <td className="px-6 py-4 text-gray-500 flex items-center gap-2">
                            <Clock size={14} />
                            {new Date(log.createdAt).toLocaleString("vi-VN")}
                          </td>
                          <td className="px-6 py-4 font-medium text-gray-800">
                            {log.fullName}
                          </td>
                          <td className="px-6 py-4">
                            <span className="bg-gray-100 text-gray-700 px-2 py-1 rounded text-xs font-medium border border-gray-200">
                              {ACTION_MAP[log.actionType] || log.actionType}
                            </span>
                          </td>
                          <td className="px-6 py-4 text-gray-400 font-mono text-xs">
                            {log.refId ? `#${log.refId}` : "-"}
                          </td>
                          <td className="px-6 py-4 text-right">
                            <span className="text-green-600 font-bold flex items-center justify-end gap-1">
                              <ArrowUpRight size={14} /> +{log.points}
                            </span>
                          </td>
                        </tr>
                      ))
                    ) : (
                      <tr>
                        <td
                          colSpan={5}
                          className="text-center py-12 text-gray-500"
                        >
                          Chưa có hoạt động nào được ghi nhận.
                        </td>
                      </tr>
                    ))}
                </>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
