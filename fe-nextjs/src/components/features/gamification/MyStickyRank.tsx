import React from "react";
import { UserRankDetail } from "@/types/gamification";
import { Trophy } from "lucide-react";

interface Props {
  myRank: UserRankDetail | null;
  loading: boolean;
}

export default function MyStickyRank({ myRank, loading }: Props) {
  if (loading || !myRank) return null;

  return (
    <div className="fixed bottom-0 left-0 right-0 bg-white/95 backdrop-blur-md border-t border-blue-200 shadow-[0_-8px_30px_rgba(0,0,0,0.12)] z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3">
        <div className="flex items-center justify-between">
          {/* Left: Info */}
          <div className="flex items-center gap-4">
            <div className="relative">
              <img
                src={myRank.avatarUrl || "/default-avatar.png"}
                alt="Me"
                className="w-10 h-10 rounded-full border-2 border-white shadow-sm object-cover"
              />
              <div className="absolute -top-1 -right-2 bg-yellow-400 text-yellow-900 text-[10px] font-bold px-1.5 py-0.5 rounded-full border border-white shadow-sm flex items-center gap-0.5">
                <Trophy className="w-2 h-2" />
                <span>#{myRank.rank > 0 ? myRank.rank : "-"}</span>
              </div>
            </div>
            <div className="hidden sm:block">
              <p className="font-bold text-gray-800 text-sm">
                Thứ hạng của bạn
              </p>
              <p className="text-xs text-gray-500">
                Cố lên! Bạn đang làm rất tốt.
              </p>
            </div>
          </div>

          {/* Right: Points */}
          <div className="flex items-center gap-3">
            <div className="text-right">
              <p className="text-[10px] text-gray-500 uppercase font-bold tracking-wider">
                Tổng điểm
              </p>
              <p className="text-xl font-black text-blue-600 leading-none">
                {myRank.score?.toLocaleString()} điểm
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
