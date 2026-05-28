'use client';

import React, { useEffect, useState } from 'react';
import { Save, Lock, Bell, Globe, Mail } from 'lucide-react';
import api from '@/services/api';

type MaintenancePayload = {
  enabled: boolean;
  message?: string;
};

export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState<'GENERAL' | 'SECURITY' | 'NOTIFICATIONS'>('GENERAL');
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);

  // Form State
  const [config, setConfig] = useState({
    siteName: 'CareerMate',
    supportEmail: 'support@careermate.com',

    maintenanceMode: false,
    maintenanceMessage: 'Hệ thống đang bảo trì, vui lòng thử lại sau.',

    emailNotifications: true,
    newJobApproval: true,
  });

  const fetchMaintenance = async () => {
    try {
      const res = await api.get('/admin/settings/maintenance');
      const data = res.data?.data;

      setConfig((prev) => ({
        ...prev,
        maintenanceMode: Boolean(data?.enabled),
        maintenanceMessage: data?.message || prev.maintenanceMessage,
      }));
    } catch (e) {
      console.error('Lỗi tải maintenance:', e);
      // Không throw để UI vẫn render
    }
  };

  useEffect(() => {
    // Load cấu hình từ BE (hiện tại mình chỉ implement maintenance để chạy)
    (async () => {
      setInitialLoading(true);
      await fetchMaintenance();
      setInitialLoading(false);
    })();
  }, []);

  const handleSave = async () => {
    setLoading(true);
    try {
      // ✅ Lưu maintenance về BE
      const payload: MaintenancePayload = {
        enabled: config.maintenanceMode,
        message: config.maintenanceMessage,
      };

      await api.put('/admin/settings/maintenance', payload);

      // ✅ set cookie để middleware đọc được (chạy ngay lập tức)
      document.cookie = `maintenanceEnabled=${payload.enabled ? "true" : "false"}; path=/;`;
      document.cookie = `maintenanceMessage=${encodeURIComponent(payload.message || "")}; path=/;`;

      alert('Đã lưu cài đặt thành công!');
    } catch (e: any) {
      console.error('Lỗi lưu settings:', e);
      const msg =
        e?.response?.data?.message ||
        e?.message ||
        'Lưu thất bại, vui lòng thử lại.';
      alert(msg);
    } finally {
      setLoading(false);
    }
  };

  if (initialLoading) {
    return <div className="max-w-4xl mx-auto p-6">Đang tải cài đặt...</div>;
  }

  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Cài đặt hệ thống</h1>

      <div className="flex flex-col md:flex-row gap-6">
        {/* Sidebar Menu Cài đặt */}
        <div className="w-full md:w-64 flex-shrink-0">
          <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
            <button
              type="button"
              onClick={() => setActiveTab('GENERAL')}
              className={`w-full flex items-center px-4 py-3 text-sm font-medium transition ${
                activeTab === 'GENERAL'
                  ? 'bg-blue-50 text-blue-700 border-l-4 border-blue-600'
                  : 'text-gray-600 hover:bg-gray-50'
              }`}
            >
              <Globe className="w-5 h-5 mr-3" /> Bảo trì
            </button>
            {/* <button
              type="button"
              onClick={() => setActiveTab('SECURITY')}
              className={`w-full flex items-center px-4 py-3 text-sm font-medium transition ${
                activeTab === 'SECURITY'
                  ? 'bg-blue-50 text-blue-700 border-l-4 border-blue-600'
                  : 'text-gray-600 hover:bg-gray-50'
              }`}
            >
              <Lock className="w-5 h-5 mr-3" /> Bảo mật & Quyền
            </button> */}
            {/* <button
              type="button"
              onClick={() => setActiveTab('NOTIFICATIONS')}
              className={`w-full flex items-center px-4 py-3 text-sm font-medium transition ${
                activeTab === 'NOTIFICATIONS'
                  ? 'bg-blue-50 text-blue-700 border-l-4 border-blue-600'
                  : 'text-gray-600 hover:bg-gray-50'
              }`}
            >
              <Bell className="w-5 h-5 mr-3" /> Thông báo
            </button>*/}
          </div>
        </div> 

        {/* Content Area */}
        <div className="flex-1">
          <div className="bg-white rounded-xl shadow-sm border p-6">
            {/* TAB: GENERAL */}
            {activeTab === 'GENERAL' && (
              <div className="space-y-6 animate-fadeIn">
                <h3 className="text-lg font-bold text-gray-800 border-b pb-2">
                  Bảo trì hệ thống
                </h3>

                {/* ✅ Maintenance block + message */}
                <div className="p-4 bg-yellow-50 rounded-lg border border-yellow-100 space-y-3">
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="font-medium text-yellow-800 block">
                        Chế độ bảo trì
                      </span>
                      <span className="text-xs text-yellow-600">
                        Chỉ Admin mới có thể truy cập hệ thống khi bật.
                      </span>
                    </div>

                    <label className="relative inline-flex items-center cursor-pointer">
                      <input
                        type="checkbox"
                        className="sr-only peer"
                        checked={config.maintenanceMode}
                        onChange={(e) =>
                          setConfig({
                            ...config,
                            maintenanceMode: e.target.checked,
                          })
                        }
                      />
                      <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-yellow-500"></div>
                    </label>
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-yellow-800 mb-1">
                      Thông báo bảo trì (trả về cho người dùng)
                    </label>
                    <textarea
                      rows={2}
                      value={config.maintenanceMessage}
                      onChange={(e) =>
                        setConfig({
                          ...config,
                          maintenanceMessage: e.target.value,
                        })
                      }
                      className="w-full px-3 py-2 border rounded-lg outline-none text-sm bg-white"
                      placeholder="Ví dụ: Hệ thống đang bảo trì, vui lòng thử lại sau."
                    />
                  </div>
                </div>
              </div>
            )}

            {/* TAB: SECURITY
            {activeTab === 'SECURITY' && (
              <div className="space-y-6 animate-fadeIn">
                <h3 className="text-lg font-bold text-gray-800 border-b pb-2">
                  Cấu hình Bảo mật
                </h3>

                <div className="flex items-center justify-between">
                  <div>
                    <span className="font-medium text-gray-800 block">
                      Kiểm duyệt tin tuyển dụng
                    </span>
                    <span className="text-sm text-gray-500">
                      Yêu cầu Admin duyệt trước khi tin được hiển thị công khai.
                    </span>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      className="sr-only peer"
                      checked={config.newJobApproval}
                      onChange={(e) =>
                        setConfig({ ...config, newJobApproval: e.target.checked })
                      }
                    />
                    <div className="w-11 h-6 bg-gray-200 rounded-full peer peer-checked:after:translate-x-full after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                  </label>
                </div>
              </div>
            )} */}

            {/* TAB: NOTIFICATIONS
            {activeTab === 'NOTIFICATIONS' && (
              <div className="space-y-6 animate-fadeIn">
                <h3 className="text-lg font-bold text-gray-800 border-b pb-2">
                  Cài đặt Thông báo
                </h3>
                <div className="flex items-center justify-between">
                  <div>
                    <span className="font-medium text-gray-800 block">
                      Email thông báo hệ thống
                    </span>
                    <span className="text-sm text-gray-500">
                      Nhận email khi có người dùng mới hoặc báo cáo vi phạm.
                    </span>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      className="sr-only peer"
                      checked={config.emailNotifications}
                      onChange={(e) =>
                        setConfig({
                          ...config,
                          emailNotifications: e.target.checked,
                        })
                      }
                    />
                    <div className="w-11 h-6 bg-gray-200 rounded-full peer peer-checked:after:translate-x-full after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                  </label>
                </div>
              </div>
            )} */}

            {/* Save Button */}
            <div className="mt-8 pt-6 border-t flex justify-end">
              <button
                type="button"
                onClick={handleSave}
                disabled={loading}
                className="flex items-center bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg font-medium transition disabled:opacity-50"
              >
                {loading ? (
                  'Đang lưu...'
                ) : (
                  <>
                    <Save className="w-4 h-4 mr-2" /> Lưu thay đổi
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
