"use client";

import React, { useEffect, useState } from "react";
import api from "@/services/api";
import { useRouter } from "next/navigation";
import {
  leaderboardService,
  LeaderboardLog,
} from "@/services/leaderboardService";
import { LeaderboardEntry } from "@/types/gamification";
import {
  Users,
  Briefcase,
  FileText,
  TrendingUp,
  Activity,
  Calendar,
} from "lucide-react";
import {
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  AreaChart,
  Area,
} from "recharts";
import { DashboardSummary, ApplicationsByDay } from "@/types/admin";

type RecentActivity = {
  refId: number;
  message: string;
  createdAt: string; // ISO
  timeAgo: string;
};

type JobPostingResponse = {
  id: number;
  title: string;
  description?: string;
  requirements?: string;
  salaryRange?: string;
  location?: string;
  expiryDate?: string;
  status?: string;
  recruiterId?: number;
  recruiterName?: string;
  createdAt?: string;
  updatedAt?: string;
};

// Hàm tiện ích format thời gian
function timeAgoVi(iso?: string) {
  if (!iso) return "";
  const t = new Date(iso).getTime();
  if (Number.isNaN(t)) return "";
  const diff = Date.now() - t;

  const sec = Math.floor(diff / 1000);
  if (sec < 60) return `${sec} giây trước`;
  const min = Math.floor(sec / 60);
  if (min < 60) return `${min} phút trước`;
  const h = Math.floor(min / 60);
  if (h < 24) return `${h} giờ trước`;
  const d = Math.floor(h / 24);
  return `${d} ngày trước`;
}

function actionLabel(actionType: string) {
  const map: Record<string, string> = {
    LOGIN_DAILY: "Đăng nhập",
    APPLY: "Ứng tuyển",
    UPLOAD_CV: "Tải CV",
    INTERVIEW_PRACTICE: "Phỏng vấn AI",
    JOB_POST_APPROVED: "Đăng tin",
    REVIEW_CV: "Duyệt hồ sơ",
    HIRED: "Tuyển dụng",
  };
  return map[actionType] || actionType;
}

function normalizePage<T>(raw: any) {
  const data = raw?.data ?? raw;
  if (Array.isArray(data)) return { content: data, totalElements: data.length };
  if (data?.content && Array.isArray(data.content)) return data;
  return { content: [], totalElements: 0 };
}

export default function AdminDashboard() {
  const router = useRouter();

  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [chartData, setChartData] = useState<ApplicationsByDay[]>([]);
  const [recentActivities, setRecentActivities] = useState<RecentActivity[]>(
    [],
  );

  // State cho Gamification
  const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>([]);
  const [pointsLogs, setPointsLogs] = useState<LeaderboardLog[]>([]);

  const [loading, setLoading] = useState(true);

  // Leaderboard filters
  const [lbPeriod, setLbPeriod] = useState<"WEEK" | "MONTH" | "YEAR">("WEEK");
  const [lbRole, setLbRole] = useState<"CANDIDATE" | "RECRUITER">("CANDIDATE");
  const [lbLoading, setLbLoading] = useState(false);
  const [logLoading, setLogLoading] = useState(false);

  // Quick action count
  const [pendingPostsCount, setPendingPostsCount] = useState(0);
  const [pendingReportsCount, setPendingReportsCount] = useState(0);

  const fetchDashboard = async () => {
    setLoading(true);
    try {
      const [summaryRes, chartRes, recentRes, pendingRes, reportSummaryRes] =
        await Promise.all([
          api.get("/admin/dashboard/summary"),
          api.get("/admin/dashboard/applications-chart", {
            params: { days: 7 },
          }),
          api.get("/admin/dashboard/recent-activities", {
            params: { limit: 5 },
          }),
          api.get("/admin/content/pending", { params: { page: 0, size: 1 } }),
          api.get("/admin/violation-reports/summary"),
        ]);

      setSummary(summaryRes.data?.data ?? null);
      setChartData(chartRes.data?.data ?? []);
      setRecentActivities(recentRes.data?.data ?? []);
      setPendingReportsCount(reportSummaryRes.data?.pendingCount ?? 0);

      const pendingPage = normalizePage<JobPostingResponse>(pendingRes.data);
      setPendingPostsCount(pendingPage.totalElements ?? 0);

      // Gọi dữ liệu Gamification
      await Promise.all([fetchLeaderboard(), fetchPointLogs()]);
    } catch (err) {
      console.error("Lỗi tải dữ liệu Dashboard:", err);
      setSummary(null);
      setChartData([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchLeaderboard = async (period = lbPeriod, role = lbRole) => {
    setLbLoading(true);
    try {
      // Gọi API lấy Top 10 cho Dashboard
      const data = await leaderboardService.getTop(role, period);
      setLeaderboard(data || []);
    } catch (e) {
      console.error("Lỗi tải leaderboard:", e);
      setLeaderboard([]);
    } finally {
      setLbLoading(false);
    }
  };

  const fetchPointLogs = async () => {
    setLogLoading(true);
    try {
      const data = await leaderboardService.getRecentLogs(5);
      setPointsLogs(data || []);
    } catch (e) {
      console.error("Lỗi tải logs:", e);
      setPointsLogs([]);
    } finally {
      setLogLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboard();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!loading) fetchLeaderboard(lbPeriod, lbRole);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [lbPeriod, lbRole]);

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center bg-gray-50">
        <div className="text-blue-600 text-xl font-semibold animate-pulse">
          Đang tải dữ liệu hệ thống...
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6 space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">
            Tổng quan hệ thống
          </h1>
          <p className="text-gray-500">Chào mừng trở lại, Administrator.</p>
        </div>
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={fetchDashboard}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 shadow-sm flex items-center gap-2"
          >
            ↻ Tải lại
          </button>
          <button
            type="button"
            className="bg-white border border-gray-300 px-4 py-2 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 shadow-sm flex items-center gap-2"
          >
            <Calendar className="w-4 h-4" />
            Hôm nay: {new Date().toLocaleDateString("vi-VN")}
          </button>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          title="Ứng viên"
          value={summary?.totalCandidates ?? 0}
          icon={<Users className="w-6 h-6 text-blue-600" />}
          // trend="+12% so với tháng trước"
          color="bg-blue-50"
        />
        <StatCard
          title="Nhà tuyển dụng"
          value={summary?.totalRecruiters ?? 0}
          icon={<Briefcase className="w-6 h-6 text-purple-600" />}
          // trend="+5% so với tháng trước"
          color="bg-purple-50"
        />
        <StatCard
          title="Tin tuyển dụng"
          value={summary?.totalActiveJobs ?? 0}
          icon={<FileText className="w-6 h-6 text-green-600" />}
          // trend="Tin đang mở"
          color="bg-green-50"
        />
        <StatCard
          title="Lượt ứng tuyển"
          value={summary?.totalApplications ?? 0}
          icon={<TrendingUp className="w-6 h-6 text-orange-600" />}
          // trend="7 ngày gần đây"
          color="bg-orange-50"
        />
      </div>

      {/* Leaderboard + Points Log */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Leaderboard */}
        <div className="lg:col-span-2 bg-white p-6 rounded-xl shadow-sm border border-gray-100 flex flex-col">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-bold text-gray-800">
              Bảng xếp hạng (Top 10)
            </h3>
            <button
              onClick={() => router.push("/admin/leaderboard")}
              className="text-sm text-blue-600 hover:underline"
            >
              Xem chi tiết
            </button>
          </div>

          <div className="flex items-center justify-between gap-3 mb-4 flex-wrap">
            {/* Role switch */}
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setLbRole("CANDIDATE")}
                className={`px-3 py-1.5 rounded-lg text-sm border ${
                  lbRole === "CANDIDATE"
                    ? "bg-blue-50 text-blue-700 border-blue-100"
                    : "bg-white text-gray-700 border-gray-200 hover:bg-gray-50"
                }`}
              >
                Ứng viên
              </button>
              <button
                type="button"
                onClick={() => setLbRole("RECRUITER")}
                className={`px-3 py-1.5 rounded-lg text-sm border ${
                  lbRole === "RECRUITER"
                    ? "bg-blue-50 text-blue-700 border-blue-100"
                    : "bg-white text-gray-700 border-gray-200 hover:bg-gray-50"
                }`}
              >
                Nhà tuyển dụng
              </button>
            </div>

            {/* Period tabs */}
            <div className="flex items-center gap-2">
              {(["WEEK", "MONTH", "YEAR"] as const).map((p) => (
                <button
                  key={p}
                  type="button"
                  onClick={() => setLbPeriod(p)}
                  className={`px-3 py-1.5 rounded-lg text-sm border ${
                    lbPeriod === p
                      ? "bg-gray-900 text-white border-gray-900"
                      : "bg-white text-gray-700 border-gray-200 hover:bg-gray-50"
                  }`}
                >
                  {p === "WEEK" ? "Tuần" : p === "MONTH" ? "Tháng" : "Năm"}
                </button>
              ))}
            </div>
          </div>

          <div className="flex-1 min-h-0">
            {lbLoading ? (
              <div className="text-sm text-gray-500 text-center py-4">
                Đang tải bảng xếp hạng...
              </div>
            ) : leaderboard.length === 0 ? (
              <div className="text-sm text-gray-500 text-center py-4">
                Chưa có dữ liệu bảng xếp hạng.
              </div>
            ) : (
              <div className="max-h-[360px] overflow-y-auto pr-2 space-y-3 scrollbar-thin">
                {leaderboard.map((x, idx) => (
                  <div
                    key={x.userId}
                    className="flex items-center justify-between p-3 rounded-lg border border-gray-100 hover:bg-gray-50"
                  >
                    <div className="flex items-center gap-3">
                      <div
                        className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm
                        ${
                          idx === 0
                            ? "bg-yellow-100 text-yellow-700"
                            : idx === 1
                              ? "bg-gray-200 text-gray-700"
                              : idx === 2
                                ? "bg-orange-100 text-orange-800"
                                : "bg-blue-50 text-blue-700"
                        }`}
                      >
                        {idx + 1}
                      </div>
                      <div>
                        <div className="text-sm font-semibold text-gray-900">
                          {x.fullName}
                        </div>
                        <div className="text-xs text-gray-500">
                          ID: {x.userId}
                        </div>
                      </div>
                    </div>

                    {/* --- SỬA CHỖ NÀY: Thêm fallback hiển thị điểm --- */}
                    <div className="text-sm font-bold text-gray-900">
                      {/* Ưu tiên totalPoints, nếu null thì hiện 0 */}
                      {x.score?.toLocaleString() ?? 0} điểm
                    </div>
                    {/* ----------------------------------------------- */}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Points Logs */}
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 flex flex-col">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-bold text-gray-800">Điểm gần đây</h3>
          </div>

          <div className="flex-1 min-h-[260px]">
            {logLoading ? (
              <div className="text-sm text-gray-500 text-center py-4">
                Đang tải lịch sử...
              </div>
            ) : pointsLogs.length === 0 ? (
              <div className="text-sm text-gray-500 text-center py-4">
                Chưa có log cộng điểm.
              </div>
            ) : (
              <div className="space-y-4">
                {pointsLogs.map((l, idx) => (
                  <div
                    key={`${l.userId}-${l.createdAt}-${idx}`}
                    className="flex items-start gap-3 pb-3 border-b border-gray-50 last:border-0 last:pb-0"
                  >
                    <div className="w-2 h-2 mt-2 rounded-full bg-blue-500" />
                    <div className="min-w-0">
                      <p className="text-sm text-gray-800 font-medium break-words">
                        {l.fullName}{" "}
                        <span className="font-bold text-blue-600">
                          +{l.points}
                        </span>{" "}
                      </p>
                      <p className="text-xs text-gray-500">
                        {actionLabel(l.actionType)} • {timeAgoVi(l.createdAt)}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Charts + Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Chart */}
        <div className="lg:col-span-2 bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-bold text-gray-800 flex items-center gap-2">
              <Activity className="w-5 h-5 text-blue-500" />
              Biểu đồ Ứng tuyển (7 ngày qua)
            </h3>
          </div>

          <div className="h-80 w-full min-w-0">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="colorCount" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.8} />
                    <stop offset="95%" stopColor="#3B82F6" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid
                  strokeDasharray="3 3"
                  vertical={false}
                  stroke="#E5E7EB"
                />
                <XAxis
                  dataKey="date"
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: "#6B7280" }}
                />
                <YAxis
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: "#6B7280" }}
                />
                <Tooltip
                  contentStyle={{
                    backgroundColor: "#fff",
                    borderRadius: "8px",
                    border: "none",
                    boxShadow: "0 4px 6px -1px rgb(0 0 0 / 0.1)",
                  }}
                />
                <Area
                  type="monotone"
                  dataKey="count"
                  stroke="#3B82F6"
                  fillOpacity={1}
                  fill="url(#colorCount)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Recent activities */}
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 flex flex-col">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-bold text-gray-800">
              Hoạt động ứng tuyển
            </h3>
          </div>

          <div className="flex-1 min-h-[220px]">
            {recentActivities.length === 0 ? (
              <div className="text-sm text-gray-500">
                Chưa có hoạt động gần đây.
              </div>
            ) : (
              <div className="space-y-4">
                {recentActivities.map((a) => (
                  <div
                    key={a.refId}
                    className="flex items-start gap-3 pb-3 border-b border-gray-50 last:border-0 last:pb-0"
                  >
                    <div className="w-2 h-2 mt-2 rounded-full bg-blue-500" />
                    <div>
                      <p className="text-sm text-gray-800 font-medium">
                        {a.message}
                      </p>
                      <p className="text-xs text-gray-500">{a.timeAgo}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <button
            type="button"
            className="w-full text-center text-sm text-blue-600 font-medium mt-4 hover:underline pt-4 border-t border-gray-100"
            onClick={() => router.push("/admin/activities")}
          >
            Xem tất cả hoạt động
          </button>
        </div>
      </div>

      {/* Quick actions */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-100">
          <h3 className="font-bold text-gray-800">Quản lý nhanh</h3>
        </div>
        <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-4">
          <ActionButton
            label="Phê duyệt bài đăng"
            count={pendingPostsCount}
            color="text-yellow-600 bg-yellow-50"
            onClick={() => router.push("/admin/content")}
          />
          <ActionButton
            label="Báo cáo vi phạm"
            count={pendingReportsCount}
            color="text-red-600 bg-red-50"
            onClick={() =>
              router.push("/admin/violation-reports?status=PENDING")
            }
          />
        </div>
      </div>
    </div>
  );
}

function StatCard({ title, value, icon, trend, color }: any) {
  return (
    <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
      <div className="flex justify-between items-start mb-4">
        <div>
          <p className="text-sm font-medium text-gray-500">{title}</p>
          <h3 className="text-3xl font-bold text-gray-800 mt-1">{value}</h3>
        </div>
        <div className={`p-3 rounded-lg ${color}`}>{icon}</div>
      </div>
      <p className="text-xs text-gray-500 flex items-center gap-1">
        {String(trend).includes("+") ? (
          <span className="text-green-600 font-medium bg-green-50 px-1 rounded">
            {trend}
          </span>
        ) : (
          <span className="text-gray-600">{trend}</span>
        )}
      </p>
    </div>
  );
}

function ActionButton({ label, count, color, onClick }: any) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex items-center justify-between p-4 rounded-lg border border-transparent hover:border-gray-200 transition-all ${color}`}
    >
      <span className="font-medium">{label}</span>
      {count > 0 && (
        <span className="bg-white px-2 py-1 rounded-full text-xs font-bold shadow-sm">
          {count}
        </span>
      )}
    </button>
  );
}
