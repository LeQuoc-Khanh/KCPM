import React from "react";
import { Mission } from "@/types/gamification";
import { CheckCircle2, Zap, Trophy } from "lucide-react"; // Thêm icon Trophy nếu muốn

interface Props {
  mission: Mission;
}

export default function MissionItem({ mission }: Props) {
  // Tính phần trăm hoàn thành
  const progress = Math.min(
    (mission.completedCount / mission.dailyLimit) * 100,
    100,
  );
  const isDone = mission.completedCount >= mission.dailyLimit;

  return (
    <div
      className={`relative flex items-start justify-between p-4 mb-3 border rounded-xl shadow-sm transition-all group overflow-hidden ${isDone ? "bg-blue-50 border-blue-200" : "bg-white border-gray-100 hover:shadow-md"}`}
    >
      {/* (Tùy chọn) Thanh tiến độ chạy ngầm dưới nền */}
      <div
        className="absolute bottom-0 left-0 h-1 bg-blue-500 transition-all duration-500 opacity-20"
        style={{ width: `${progress}%` }}
      />

      <div className="flex-1 pr-4 z-10">
        <div className="flex items-center gap-2 mb-1">
          {isDone ? (
            <Trophy className="w-4 h-4 text-yellow-500" />
          ) : (
            <Zap className="w-4 h-4 text-orange-500 fill-orange-500" />
          )}

          <h4
            className={`font-bold text-sm transition-colors ${isDone ? "text-blue-700" : "text-gray-800 group-hover:text-blue-600"}`}
          >
            {mission.name}
          </h4>
        </div>

        <p className="text-xs text-gray-500 leading-relaxed mb-2">
          {mission.description}
        </p>

        {/* Hiển thị Tiến độ: 1/5 */}
        <div className="flex items-center gap-2 mt-2">
          <div className="flex-1 h-2 bg-gray-200 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-500 ${isDone ? "bg-green-500" : "bg-blue-600"}`}
              style={{ width: `${progress}%` }}
            />
          </div>
          <div className="text-[10px] font-bold text-gray-600 min-w-[30px] text-right">
            {mission.completedCount}/{mission.dailyLimit}
          </div>
        </div>
      </div>

      <div
        className={`flex flex-col items-center justify-center min-w-[60px] h-[60px] rounded-lg border ${isDone ? "bg-white border-blue-200" : "bg-gradient-to-br from-blue-50 to-blue-100 border-blue-200"}`}
      >
        <span
          className={`text-lg font-black ${isDone ? "text-gray-400" : "text-blue-600"}`}
        >
          +{mission.points}
        </span>
        <span className="text-[9px] text-blue-500 font-bold uppercase tracking-wider">
          Điểm
        </span>
      </div>
    </div>
  );
}
