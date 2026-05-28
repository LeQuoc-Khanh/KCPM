"use client";

import React, { useEffect, useState } from "react";
import { leaderboardService } from "@/services/leaderboardService";
import {
  LeaderboardEntry,
  UserRankDetail,
  Mission,
} from "@/types/gamification";
import MissionItem from "@/components/features/gamification/MissionItem";
import MyStickyRank from "@/components/features/gamification/MyStickyRank";
import { useAuth } from "@/context/Authcontext";
import { Trophy, Medal, Crown, Flame, Target } from "lucide-react";

export default function LeaderboardPage() {
  const { user } = useAuth();
  const [topUsers, setTopUsers] = useState<LeaderboardEntry[]>([]);
  const [myRank, setMyRank] = useState<UserRankDetail | null>(null);
  const [missions, setMissions] = useState<Mission[]>([]);
  const [period, setPeriod] = useState<"WEEK" | "MONTH">("WEEK");
  const [loading, setLoading] = useState(true);

  // Xác định role để gọi API (Gom VIP về gốc)
  const getRoleForApi = () => {
    if (!user?.userRole) return "CANDIDATE";
    return user.userRole.includes("RECRUITER") ? "RECRUITER" : "CANDIDATE";
  };

  const apiRole = getRoleForApi();

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [topData, missionData] = await Promise.all([
          leaderboardService.getTop(apiRole, period),
          leaderboardService.getMissions(apiRole, user?.id), // Truyền userId vào đây
        ]);
        setTopUsers(topData || []);
        setMissions(missionData || []);

        if (user?.id) {
          const myRankData = await leaderboardService.getMyRank(
            user.id,
            apiRole,
            period,
          );
          setMyRank(myRankData);
        }
      } catch (error) {
        console.error("Failed to fetch leaderboard", error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [user, period, apiRole]);

  const renderRankIcon = (rank: number) => {
    if (rank === 1)
      return <Crown className="w-6 h-6 text-yellow-500 fill-yellow-500" />;
    if (rank === 2)
      return <Medal className="w-6 h-6 text-gray-400 fill-gray-100" />;
    if (rank === 3)
      return <Medal className="w-6 h-6 text-orange-400 fill-orange-100" />;
    return (
      <span className="text-gray-400 font-bold text-sm w-6 text-center">
        #{rank}
      </span>
    );
  };

  return (
    <div className="min-h-screen bg-gray-50/50 pb-24">
      {/* HEADER BANNER */}
      <div className="bg-gradient-to-r from-blue-700 to-indigo-800 text-white pt-10 pb-16 px-4 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-64 h-64 bg-white/5 rounded-full blur-3xl -mr-16 -mt-16"></div>
        <div className="max-w-7xl mx-auto text-center relative z-10">
          <div className="inline-flex items-center gap-2 bg-white/10 backdrop-blur-sm px-4 py-1.5 rounded-full text-sm font-medium text-blue-100 mb-4 border border-white/10">
            <Flame className="w-4 h-4 text-orange-400 fill-orange-400" />
            Đua top nhận quyền lợi độc quyền
          </div>
          <h1 className="text-3xl md:text-4xl font-bold mb-3">
            Bảng Vinh Danh{" "}
            {apiRole === "CANDIDATE" ? "Ứng Viên" : "Nhà Tuyển Dụng"}
          </h1>
          <p className="text-blue-200 max-w-xl mx-auto mb-8">
            Tích điểm thông qua các hoạt động hàng ngày để khẳng định vị thế và
            nhận được sự chú ý từ cộng đồng.
          </p>

          {/* Period Toggle */}
          <div className="inline-flex bg-blue-900/40 p-1 rounded-xl backdrop-blur-md border border-white/10">
            <button
              onClick={() => setPeriod("WEEK")}
              className={`px-6 py-2 rounded-lg text-sm font-semibold transition-all ${
                period === "WEEK"
                  ? "bg-white text-blue-700 shadow-md"
                  : "text-blue-200 hover:text-white hover:bg-white/5"
              }`}
            >
              Tuần Này
            </button>
            <button
              onClick={() => setPeriod("MONTH")}
              className={`px-6 py-2 rounded-lg text-sm font-semibold transition-all ${
                period === "MONTH"
                  ? "bg-white text-blue-700 shadow-md"
                  : "text-blue-200 hover:text-white hover:bg-white/5"
              }`}
            >
              Tháng Này
            </button>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 -mt-8 relative z-20">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* LEFT: LEADERBOARD (70%) */}
          <div className="lg:col-span-8">
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
              <div className="p-5 border-b border-gray-50 flex justify-between items-center bg-white sticky top-0">
                <h2 className="font-bold text-gray-800 flex items-center gap-2">
                  <Trophy className="w-5 h-5 text-yellow-500" />
                  Xếp hạng Top 50
                </h2>
                <span className="text-xs font-medium text-gray-500 bg-gray-100 px-2.5 py-1 rounded-full">
                  Cập nhật realtime
                </span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-gray-50/50 text-gray-500 text-xs uppercase font-semibold tracking-wider">
                    <tr>
                      <th className="px-6 py-4 text-center w-20">Hạng</th>
                      <th className="px-6 py-4 text-left">Thành viên</th>
                      <th className="px-6 py-4 text-right">Điểm số</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-50">
                    {loading ? (
                      [...Array(6)].map((_, i) => (
                        <tr key={i} className="animate-pulse">
                          <td className="px-6 py-4">
                            <div className="h-8 w-8 bg-gray-100 rounded-full mx-auto"></div>
                          </td>
                          <td className="px-6 py-4">
                            <div className="h-10 w-48 bg-gray-100 rounded-lg"></div>
                          </td>
                          <td className="px-6 py-4">
                            <div className="h-6 w-16 bg-gray-100 rounded ml-auto"></div>
                          </td>
                        </tr>
                      ))
                    ) : topUsers.length > 0 ? (
                      topUsers.map((entry, index) => {
                        const isMe = entry.userId === user?.id;
                        return (
                          <tr
                            key={entry.userId}
                            className={`group transition-colors ${isMe ? "bg-blue-50/50" : "hover:bg-gray-50"}`}
                          >
                            <td className="px-6 py-4 text-center">
                              <div className="flex items-center justify-center">
                                {renderRankIcon(index + 1)}
                              </div>
                            </td>
                            <td className="px-6 py-4">
                              <div className="flex items-center gap-3">
                                <div className="relative">
                                  <img
                                    src={
                                      entry.avatarUrl || "/default-avatar.png"
                                    }
                                    alt={entry.fullName}
                                    className={`w-10 h-10 rounded-full object-cover border-2 ${isMe ? "border-blue-200" : "border-gray-100"}`}
                                  />
                                  {index < 3 && (
                                    <div className="absolute -top-1 -right-1 bg-yellow-400 rounded-full p-0.5 border border-white">
                                      <Crown className="w-2 h-2 text-white fill-white" />
                                    </div>
                                  )}
                                </div>
                                <div>
                                  <p
                                    className={`font-semibold text-sm ${isMe ? "text-blue-700" : "text-gray-900"}`}
                                  >
                                    {entry.fullName}{" "}
                                    {isMe && (
                                      <span className="text-[10px] bg-blue-100 text-blue-600 px-1.5 py-0.5 rounded ml-1">
                                        Bạn
                                      </span>
                                    )}
                                  </p>
                                  <p className="text-xs text-gray-500 mt-0.5">
                                    Thành viên tích cực
                                  </p>
                                </div>
                              </div>
                            </td>
                            <td className="px-6 py-4 text-right">
                              <span
                                className={`font-bold text-sm px-3 py-1 rounded-full ${
                                  isMe
                                    ? "bg-blue-600 text-white shadow-sm shadow-blue-200"
                                    : "bg-gray-100 text-gray-700"
                                }`}
                              >
                                {entry.score?.toLocaleString()} điểm
                              </span>
                            </td>
                          </tr>
                        );
                      })
                    ) : (
                      <tr>
                        <td colSpan={3} className="px-6 py-12 text-center">
                          <div className="flex flex-col items-center justify-center text-gray-400">
                            <Trophy className="w-12 h-12 mb-3 opacity-20" />
                            <p>Chưa có dữ liệu xếp hạng cho kỳ này.</p>
                          </div>
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* RIGHT: MISSIONS (30%) */}
          <div className="lg:col-span-4 space-y-6">
            {/* My Rank Card */}
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 relative overflow-hidden">
              <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-yellow-100 to-orange-100 rounded-full blur-2xl -mr-8 -mt-8 opacity-60"></div>
              <div className="relative z-10 text-center">
                <div className="inline-block p-1.5 rounded-full bg-white shadow-sm mb-3">
                  <img
                    src={user?.profileImageUrl || "/default-avatar.png"}
                    alt="Me"
                    className="w-16 h-16 rounded-full object-cover"
                  />
                </div>
                <h3 className="font-bold text-gray-800 text-lg">
                  {user?.fullName || "Khách"}
                </h3>
                <p className="text-xs text-gray-500 mb-5 uppercase tracking-wide font-semibold text-blue-600">
                  {apiRole === "CANDIDATE" ? "Ứng viên" : "Nhà tuyển dụng"}
                </p>

                <div className="grid grid-cols-2 gap-4 border-t border-gray-100 pt-4">
                  <div className="text-center">
                    <p className="text-xs text-gray-400 uppercase font-bold tracking-wider mb-1">
                      Hạng
                    </p>
                    <p className="text-2xl font-black text-gray-800">
                      #{myRank?.rank || "-"}
                    </p>
                  </div>
                  <div className="text-center border-l border-gray-100">
                    <p className="text-xs text-gray-400 uppercase font-bold tracking-wider mb-1">
                      Điểm
                    </p>
                    <p className="text-2xl font-black text-blue-600">
                      {myRank?.score?.toLocaleString()} điểm
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* Mission List */}
            <div className="bg-gray-50/50 rounded-2xl border border-gray-200/60 p-5">
              <h3 className="font-bold text-gray-800 mb-4 flex items-center gap-2 text-sm uppercase tracking-wider">
                <Target className="w-4 h-4 text-blue-600" />
                Nhiệm vụ khả dụng
              </h3>

              <div className="space-y-3">
                {missions.map((mission) => (
                  <MissionItem key={mission.code} mission={mission} />
                ))}
              </div>

              <div className="mt-4 text-center">
                <p className="text-xs text-gray-400 italic">
                  Hệ thống tự động cộng điểm sau khi hoàn thành.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <MyStickyRank myRank={myRank} loading={loading} />
    </div>
  );
}
