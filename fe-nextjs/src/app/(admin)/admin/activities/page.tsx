'use client';

import React, { useEffect, useState } from 'react';
import api from '@/services/api';

type RecentActivity = {
  refId: number;
  message: string;
  createdAt: string;
  timeAgo: string;
};

export default function AdminAllActivitiesPage() {
  const [items, setItems] = useState<RecentActivity[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchAll = async () => {
    setLoading(true);
    try {
      const res = await api.get('/admin/dashboard/all-activities');
      setItems(res.data?.data ?? []);
    } catch (err) {
      console.error('Lỗi tải tất cả hoạt động:', err);
      setItems([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAll();
  }, []);

  return (
    <div className="min-h-screen bg-gray-50 p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Tất cả hoạt động ứng tuyển</h1>
          <p className="text-gray-500">Danh sách toàn bộ hoạt động ứng tuyển trong hệ thống.</p>
        </div>

        <button
          type="button"
          onClick={fetchAll}
          className="bg-white border border-gray-300 px-4 py-2 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 shadow-sm"
        >
          Tải lại
        </button>
      </div>

      <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
        {loading ? (
          <div className="text-blue-600 text-lg font-semibold animate-pulse">
            Đang tải dữ liệu...
          </div>
        ) : items.length === 0 ? (
          <div className="text-sm text-gray-500">Chưa có hoạt động.</div>
        ) : (
          <div className="space-y-4">
            {items.map((a) => (
              <div
                key={a.refId}
                className="flex items-start gap-3 pb-3 border-b border-gray-50 last:border-0 last:pb-0"
              >
                <div className="w-2 h-2 mt-2 rounded-full bg-blue-500" />
                <div className="flex-1">
                  <p className="text-sm text-gray-800 font-medium">{a.message}</p>
                  <div className="flex items-center gap-2">
                    <p className="text-xs text-gray-500">{a.timeAgo}</p>
                    {a.createdAt ? (
                      <p className="text-xs text-gray-400">
                        • {new Date(a.createdAt).toLocaleString('vi-VN')}
                      </p>
                    ) : null}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
