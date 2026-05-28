"use client";

import { useEffect, useState } from 'react';
import api from '@/services/api';
import { useRouter } from 'next/navigation';
import { Bell, CheckCheck, Trash2, Clock, ChevronRight } from 'lucide-react';
import toast from 'react-hot-toast';

export default function NotificationList() {
  const [notifications, setNotifications] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<'ALL' | 'UNREAD'>('ALL');
  const router = useRouter();

  useEffect(() => {
    fetchNotifications();
  }, []);

  const fetchNotifications = async () => {
    try {
      setLoading(true);
      const res = await api.get('/notifications');
      setNotifications(res.data);
    } catch (error) {
      console.error(error);
      toast.error("Không thể tải thông báo");
    } finally {
      setLoading(false);
    }
  };

  const handleMarkAsRead = async (id: number, link?: string) => {
    try {
      await api.put(`/notifications/${id}/read`);
      setNotifications(prev => prev.map(n => 
        n.id === id ? { ...n, read: true } : n
      ));
      if (link) router.push(link);
    } catch (error) {
      console.error(error);
    }
  };

  const handleMarkAllRead = async () => {
    const hasUnread = notifications.some(n => !n.read);
    if (!hasUnread) return toast("Tất cả đã được đọc rồi!");

    try {
      await api.put('/notifications/read-all');
      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
      toast.success("Đã đánh dấu tất cả là đã đọc");
    } catch (error) {
      toast.error("Lỗi kết nối");
    }
  };

  const handleDelete = async (e: React.MouseEvent, id: number) => {
    e.stopPropagation();
    if (!confirm("Bạn có chắc muốn xóa thông báo này?")) return;

    try {
      await api.delete(`/notifications/${id}`);
      setNotifications(prev => prev.filter(n => n.id !== id));
      toast.success("Đã xóa");
    } catch (error) {
      toast.error("Không thể xóa");
    }
  };

  const filteredNotifications = filter === 'ALL' 
    ? notifications 
    : notifications.filter(n => !n.read);

  const formatTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleString('vi-VN', {
      hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric'
    });
  };

  return (
    <div className="bg-white rounded-lg shadow-sm p-4 md:p-6 min-h-[500px]">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-center gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-800 flex items-center gap-2">
            <Bell className="text-blue-600" /> Thông báo
          </h1>
          <p className="text-gray-500 text-sm mt-1">
             Quản lý tất cả thông báo của bạn
          </p>
        </div>
        
        <div className="flex items-center gap-3">
           <div className="flex bg-gray-100 rounded-lg p-1">
              <button 
                  onClick={() => setFilter('ALL')}
                  className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all ${
                      filter === 'ALL' ? 'bg-white shadow-sm text-gray-900' : 'text-gray-500 hover:text-gray-700'
                  }`}
              >
                  Tất cả
              </button>
              <button 
                  onClick={() => setFilter('UNREAD')}
                  className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all ${
                      filter === 'UNREAD' ? 'bg-white shadow-sm text-gray-900' : 'text-gray-500 hover:text-gray-700'
                  }`}
              >
                  Chưa đọc
              </button>
          </div>
          <button 
            onClick={handleMarkAllRead}
            className="flex items-center gap-2 px-3 py-2 text-xs font-medium text-blue-600 bg-blue-50 hover:bg-blue-100 rounded-lg transition-colors"
          >
            <CheckCheck size={14} /> Đọc tất cả
          </button>
        </div>
      </div>

      {/* List */}
      <div className="space-y-3">
        {loading ? (
           [1, 2, 3].map(i => <div key={i} className="h-20 bg-gray-50 rounded-lg animate-pulse"></div>)
        ) : filteredNotifications.length === 0 ? (
          <div className="text-center py-16 border border-dashed border-gray-200 rounded-lg">
              <Bell size={40} className="mx-auto text-gray-300 mb-3" />
              <p className="text-gray-500">Không có thông báo nào.</p>
          </div>
        ) : (
          filteredNotifications.map((notif) => (
            <div 
              key={notif.id}
              onClick={() => handleMarkAsRead(notif.id, notif.link)}
              className={`group relative flex items-start p-4 rounded-xl border transition-all cursor-pointer hover:shadow-md ${
                notif.read ? 'bg-white border-gray-100' : 'bg-blue-50/40 border-blue-100'
              }`}
            >
              <div className={`mt-1 p-2 rounded-full flex-shrink-0 ${
                  notif.read ? 'bg-gray-100 text-gray-400' : 'bg-blue-100 text-blue-600'
              }`}>
                  {notif.read ? <CheckCheck size={18} /> : <Bell size={18} />}
              </div>

              <div className="ml-4 flex-1 pr-8">
                  <h3 className={`text-base ${notif.read ? 'font-medium text-gray-800' : 'font-bold text-gray-900'}`}>
                      {notif.title}
                  </h3>
                  <p className="text-gray-600 text-sm mt-1 line-clamp-2">{notif.message}</p>
                  <div className="flex items-center gap-3 mt-2">
                      <span className="flex items-center text-xs text-gray-400">
                          <Clock size={12} className="mr-1" /> {formatTime(notif.createdAt)}
                      </span>
                  </div>
              </div>

              <div className="absolute top-4 right-4 flex items-center gap-2 opacity-100 md:opacity-0 md:group-hover:opacity-100 transition-opacity">
                  <button 
                      onClick={(e) => handleDelete(e, notif.id)}
                      className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-full transition-colors"
                  >
                      <Trash2 size={16} />
                  </button>
              </div>
              {!notif.read && <span className="absolute top-4 right-4 w-2 h-2 bg-blue-600 rounded-full md:hidden"></span>}
            </div>
          ))
        )}
      </div>
    </div>
  );
}