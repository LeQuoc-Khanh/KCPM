"use client";

import React, { useEffect, useMemo, useState } from "react";
import {
  ArrowUpRight,
  Clock,
  History,
  RefreshCw,
  Search,
} from "lucide-react";
import {
  leaderboardService,
  LeaderboardLog,
} from "@/services/leaderboardService";

const ACTION_MAP: Record<string, string> = {
  LOGIN_DAILY: "Đăng nhập hằng ngày",
  APPLY: "Ứng tuyển việc làm",
  UPLOAD_CV: "Tải lên CV",
  INTERVIEW_PRACTICE: "Phỏng vấn AI",
  JOB_POST_APPROVED: "Đăng tin tuyển dụng",
  REVIEW_CV: "Duyệt hồ sơ",
  HIRED: "Tuyển dụng thành công",
};

export default function AdminPointLogsPage() {
  const [logs, setLogs] = useState<LeaderboardLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [limit, setLimit] = useState(100);

  const fetchLogs = async (nextLimit = limit) => {
    setLoading(true);
    try {
      const data = await leaderboardService.getRecentLogs(nextLimit);
      setLogs(data || []);
    } catch (error) {
      console.error("Lỗi tải lịch sử điểm:", error);
      setLogs([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const filteredLogs = useMemo(() => {
    const keyword = searchTerm.trim().toLowerCase();
    if (!keyword) return logs;

    return logs.filter((log) => {
      const actionLabel = ACTION_MAP[log.actionType] || log.actionType;
      return (
        log.fullName?.toLowerCase().includes(keyword) ||
        log.role?.toLowerCase().includes(keyword) ||
        log.actionType?.toLowerCase().includes(keyword) ||
        actionLabel.toLowerCase().includes(keyword) ||
        String(log.refId ?? "").includes(keyword)
      );
    });
  }, [logs, searchTerm]);

  const handleLimitChange = (value: number) => {
    setLimit(value);
    fetchLogs(value);
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6 space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800 flex items-center gap-2">
            <History className="text-blue-600" />
            Nhật ký điểm
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Theo dõi các giao dịch cộng điểm gần đây trong hệ thống.
          </p>
        </div>

        <button
          type="button"
          onClick={() => fetchLogs()}
          className="inline-flex items-center justify-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50"
        >
          <RefreshCw size={16} />
          Tải lại
        </button>
      </div>

      <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div className="relative w-full md:max-w-sm">
          <Search
            className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
            size={16}
          />
          <input
            type="text"
            value={searchTerm}
            onChange={(event) => setSearchTerm(event.target.value)}
            placeholder="Tìm theo tên, vai trò, hành động..."
            className="w-full rounded-lg border border-gray-200 py-2 pl-9 pr-4 text-sm text-gray-700 outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div className="flex items-center gap-2 text-sm text-gray-600">
          <span>Hiển thị</span>
          <select
            value={limit}
            onChange={(event) => handleLimitChange(Number(event.target.value))}
            className="rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-700 outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value={50}>50 log</option>
            <option value={100}>100 log</option>
            <option value={200}>200 log</option>
          </select>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-500 uppercase font-semibold text-xs">
              <tr>
                <th className="px-6 py-4 text-left">Thời gian</th>
                <th className="px-6 py-4 text-left">Thành viên</th>
                <th className="px-6 py-4 text-left">Vai trò</th>
                <th className="px-6 py-4 text-left">Hành động</th>
                <th className="px-6 py-4 text-left">Ref ID</th>
                <th className="px-6 py-4 text-right">Điểm</th>
              </tr>
            </thead>

            <tbody className="divide-y divide-gray-100">
              {loading ? (
                [...Array(5)].map((_, index) => (
                  <tr key={index} className="animate-pulse">
                    <td colSpan={6} className="px-6 py-4">
                      <div className="h-8 w-full rounded bg-gray-100" />
                    </td>
                  </tr>
                ))
              ) : filteredLogs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-12 text-gray-500">
                    Chưa có log cộng điểm phù hợp.
                  </td>
                </tr>
              ) : (
                filteredLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 text-gray-500">
                      <span className="inline-flex items-center gap-2">
                        <Clock size={14} />
                        {new Date(log.createdAt).toLocaleString("vi-VN")}
                      </span>
                    </td>
                    <td className="px-6 py-4 font-medium text-gray-800">
                      {log.fullName || `User #${log.userId}`}
                    </td>
                    <td className="px-6 py-4">
                      <span className="rounded-full bg-blue-50 px-2.5 py-1 text-xs font-semibold text-blue-700">
                        {log.role || "-"}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span className="rounded border border-gray-200 bg-gray-100 px-2 py-1 text-xs font-medium text-gray-700">
                        {ACTION_MAP[log.actionType] || log.actionType}
                      </span>
                    </td>
                    <td className="px-6 py-4 font-mono text-xs text-gray-400">
                      {log.refId ? `#${log.refId}` : "-"}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <span className="inline-flex items-center justify-end gap-1 font-bold text-green-600">
                        <ArrowUpRight size={14} />
                        +{log.points}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
