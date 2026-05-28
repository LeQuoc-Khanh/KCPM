"use client";

import { useEffect, useState } from 'react';
import { Bell, CheckCheck, Trash2 } from 'lucide-react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import toast, { Toaster } from 'react-hot-toast';
import { useAuth } from '@/context/Authcontext';
import api from '@/services/api';
import Link from 'next/link';

export default function NotificationBell() {
  const { user } = useAuth();
  const [notifications, setNotifications] = useState<any[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

useEffect(() => {
    if (!user) return;
    fetchNotifications();

    // Dùng biến môi trường
    const socketUrl = process.env.NEXT_PUBLIC_API_URL 
        ? `${process.env.NEXT_PUBLIC_API_URL.replace('/api', '')}/ws` 
        : 'http://localhost:8080/ws';

    const socket = new SockJS(socketUrl);
    const stompClient = new Client({
      webSocketFactory: () => socket,
      // Cấu hình reconnect tự động
      reconnectDelay: 5000, 
      onConnect: () => {
        // Subscribe đến kênh thông báo của người dùng
        stompClient.subscribe(`/user/${user.id}/queue/notifications`, (message) => {
          const newNotif = JSON.parse(message.body);
          setNotifications(prev => [newNotif, ...prev]);
          setUnreadCount(prev => prev + 1);
          playNotificationSound();
          
          toast((t) => (
            <div onClick={() => { toast.dismiss(t.id); window.location.href = newNotif.link; }}>
              <b>{newNotif.title}</b>
              <p className="text-sm">{newNotif.message}</p>
            </div>
          ), { duration: 5000, position: 'bottom-right' });
        });
      },
      connectHeaders: {
         Authorization: `Bearer ${localStorage.getItem('token')}` 
      }
    });

    stompClient.activate();

    return () => {
      stompClient.deactivate();
    };
  }, [user]);

  const getViewAllLink = () => {
    if (!user) return '/login';
    
    // Kiểm tra role để trả về route tương ứng
    switch (user.userRole) { 
        case 'RECRUITER':
            return '/recruiter/notifications';
        case 'ADMIN':
            return '/admin/notifications';
        case 'CANDIDATE':
        default:
            return '/notifications'; // Candidate thường ở root
    }
  };

  const fetchNotifications = async () => {
    try {
      const res = await api.get('/notifications');
      setNotifications(res.data);
      setUnreadCount(res.data.filter((n: any) => !n.read).length);
    } catch (e) {
      console.error(e);
    }
  };

  const handleRead = async (notif: any) => {
    if (!notif.read) {
        await api.put(`/notifications/${notif.id}/read`);
        // Cập nhật state cục bộ
        setNotifications(prev => prev.map(n => 
            n.id === notif.id ? { ...n, read: true } : n
        ));
        setUnreadCount(prev => Math.max(0, prev - 1));
    }
    setIsOpen(false);
  };

  const handleMarkAllRead = async () => {
    if (unreadCount === 0) return;
    try {
        await api.put('/notifications/read-all');
        
        // Cập nhật UI ngay lập tức
        setNotifications(prev => prev.map(n => ({ ...n, read: true })));
        setUnreadCount(0);
        toast.success("Đã đánh dấu tất cả là đã đọc");
    } catch (error) {
        console.error("Lỗi khi đánh dấu đã đọc", error);
    }
  };

  const handleDelete = async (e: React.MouseEvent, id: number) => {
    // Chặn sự kiện click lan ra ngoài (để không bị trigger handleRead/Link)
    e.preventDefault(); 
    e.stopPropagation();

    try {
        await api.delete(`/notifications/${id}`);
        
        // Cập nhật state: Lọc bỏ thông báo vừa xóa
        setNotifications(prev => {
            const target = prev.find(n => n.id === id);
            // Nếu xóa thông báo chưa đọc thì giảm count
            if (target && !target.read) {
                setUnreadCount(count => Math.max(0, count - 1));
            }
            return prev.filter(n => n.id !== id);
        });
        toast.success("Đã xóa thông báo");
    } catch (error) {
        toast.error("Lỗi khi xóa thông báo");
    }
  };

  // chưa phát triển phần âm thanh
  const playNotificationSound = () => {
      const audio = new Audio('/notification.mp3'); 
      audio.play().catch(e => console.log("Audio play blocked"));
  };

  return (
    <div className="relative">
      <button onClick={() => setIsOpen(!isOpen)} className="relative p-2 hover:bg-gray-100 rounded-full">
        <Bell size={24} className="text-gray-600" />
        {unreadCount > 0 && (
          <span className="absolute top-0 right-0 inline-flex items-center justify-center px-2 py-1 text-xs font-bold leading-none text-red-100 transform translate-x-1/4 -translate-y-1/4 bg-red-600 rounded-full">
            {unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-96 bg-white rounded-lg shadow-xl overflow-hidden z-50 border border-gray-100 ring-1 ring-black ring-opacity-5">
          <div className="flex items-center justify-between px-4 py-3 border-b bg-gray-50">
            <span className="text-sm font-bold text-gray-800">Thông báo</span>
            {unreadCount > 0 && (
                <button 
                    onClick={handleMarkAllRead}
                    className="text-xs flex items-center text-blue-600 hover:text-blue-800 font-medium transition-colors"
                >
                    <CheckCheck size={14} className="mr-1" /> Đọc tất cả
                </button>
            )}
          </div>

          <div className="max-h-[400px] overflow-y-auto custom-scrollbar">
            {notifications.length === 0 ? (
              <div className="px-4 py-8 text-sm text-gray-500 text-center flex flex-col items-center">
                <Bell size={32} className="text-gray-300 mb-2" />
                Không có thông báo mới
              </div>
            ) : (
              notifications.map((notif) => (
                <div 
                  key={notif.id} 
                  className={`group relative block border-b last:border-0 transition-colors ${
                      !notif.read ? 'bg-blue-50/60' : 'bg-white'
                  }`}
                >
                    {/* Link bao phủ nội dung */}
                    <Link 
                        href={notif.link || '#'} 
                        onClick={() => handleRead(notif)}
                        className="block px-4 py-3 hover:bg-gray-50 pr-10" // Thêm pr-10 để tránh đè nút xóa
                    >
                        <div className="flex justify-between items-start gap-2">
                            <p className={`text-sm ${!notif.read ? 'font-semibold text-gray-900' : 'text-gray-700'}`}>
                                {notif.title}
                            </p>
                            {!notif.read && <span className="w-2 h-2 bg-blue-600 rounded-full mt-1.5 flex-shrink-0"></span>}
                        </div>
                        <p className="text-xs text-gray-600 mt-1 line-clamp-2">{notif.message}</p>
                        <p className="text-[10px] text-gray-400 mt-1">
                            {new Date(notif.createdAt).toLocaleString('vi-VN')}
                        </p>
                    </Link>

                    <button
                        onClick={(e) => handleDelete(e, notif.id)}
                        className="absolute top-3 right-3 p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-full opacity-0 group-hover:opacity-100 transition-all duration-200"
                        title="Xóa thông báo"
                    >
                        <Trash2 size={14} />
                    </button>
                </div>
              ))
            )}
          </div>
          
          <div className="p-2 border-t bg-gray-50 text-center">
              <Link 
                  href={getViewAllLink()} 
                  onClick={() => setIsOpen(false)}
                  className="text-xs text-gray-600 hover:text-blue-600 font-medium block w-full py-1"
              >
                  Xem tất cả lịch sử
              </Link>
          </div>
        </div>
      )}
    </div>
  );
}