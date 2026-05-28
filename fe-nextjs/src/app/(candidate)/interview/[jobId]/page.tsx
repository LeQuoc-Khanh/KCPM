"use client";

import React, { useState, useEffect, useRef } from "react";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { interviewService } from "@/services/interviewService";
import { InterviewSession, MessageItem } from "@/types/interview";
import toast from "react-hot-toast";
import {
  Send,
  ArrowLeft, // Đã xóa Mic
  Briefcase,
  User,
  Bot,
  LogOut,
  CheckCircle,
  AlertCircle,
  AlertTriangle,
  X,
} from "lucide-react";

export default function InterviewRoomPage() {
  const params = useParams();
  const jobId = Number(params.jobId);
  const router = useRouter();
  const searchParams = useSearchParams();
  const sessionIdParam = searchParams.get("sessionId");
  const isReviewParam = searchParams.get("review") === "true";

  // State
  const [session, setSession] = useState<InterviewSession | null>(null);
  const [messages, setMessages] = useState<MessageItem[]>([]);
  const [inputStr, setInputStr] = useState("");
  const [loading, setLoading] = useState(true);
  const [isSending, setIsSending] = useState(false);
  const [showHistory, setShowHistory] = useState(false);

  // State quản lý Modal
  const [showConfirmModal, setShowConfirmModal] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  const hasInitialized = useRef(false);

  useEffect(() => {
    scrollToBottom();
  }, [messages, isSending, showHistory]);

  useEffect(() => {
    if (isReviewParam) {
      setShowHistory(true);
    }
  }, [isReviewParam]);

  // 1. INIT SESSION
  useEffect(() => {
    if (hasInitialized.current) return;

    const initSession = async () => {
      try {
        // Đánh dấu là đã bắt đầu chạy để chặn các lần sau
        hasInitialized.current = true;

        setLoading(true);

        if (sessionIdParam) {
          const sId = Number(sessionIdParam);
          if (isNaN(sId) || sId <= 0) {
            toast.error("Mã phiên không hợp lệ");
            return;
          }
          const data = await interviewService.getSessionResult(sId);
          setSession(data);
          if (!isReviewParam) toast.success("Đã tải kết quả");
          if (data.status === "COMPLETED") setShowHistory(true);
        } else {
          if (!jobId) {
            toast.error("Không tìm thấy Job ID");
            return;
          }
          const startData = await interviewService.startInterview(jobId);
          const sessionInfo = await interviewService.getSessionResult(
            startData.sessionId,
          );
          setSession(sessionInfo);
          setMessages([
            {
              sender: "AI",
              content:
                startData.greeting || `Chào bạn, mời bạn giới thiệu về mình.`,
            },
          ]);
          toast.success("Đã kết nối AI");
        }
      } catch (error: any) {
        console.error("Lỗi:", error);
        toast.error("Lỗi kết nối.");
        setTimeout(() => router.back(), 3000);
        hasInitialized.current = false;
      } finally {
        setLoading(false);
      }
    };

    if (jobId || sessionIdParam) {
      initSession();
    }
  }, [jobId, sessionIdParam, isReviewParam]);

  // 2. SEND MESSAGE
  const handleSend = async (e?: React.FormEvent) => {
    e?.preventDefault();
    if (!inputStr.trim() || !session || isSending) return;

    if (session.status === "COMPLETED") {
      toast.error("Phiên này đã kết thúc.");
      return;
    }

    const currentMsg = inputStr;
    setInputStr("");

    const newHistory = [
      ...messages,
      { sender: "USER", content: currentMsg } as MessageItem,
    ];
    setMessages(newHistory);
    setIsSending(true);

    try {
      const aiReplyText = await interviewService.sendMessage(
        session.id,
        currentMsg,
        messages,
      );
      setMessages((prev) => [...prev, { sender: "AI", content: aiReplyText }]);
    } catch (error) {
      console.error("Lỗi chat:", error);
      toast.error("Lỗi gửi tin nhắn");
      setInputStr(currentMsg);
    } finally {
      setIsSending(false);
    }
  };

  // 3. KÍCH HOẠT MODAL
  const onClickEndInterview = () => {
    if (!session || session.status === "COMPLETED") return;
    setShowConfirmModal(true);
  };

  // 4. KẾT THÚC THẬT SỰ
  const confirmEndInterview = async () => {
    setShowConfirmModal(false);

    try {
      const loadingToast = toast.loading("AI đang chấm điểm...");
      const resultSession = await interviewService.endInterview(
        session!.id,
        messages,
      );
      router.replace(
        `/interview/${jobId}?sessionId=${resultSession.id}&review=true`,
      );
      setSession(resultSession);
      setShowHistory(true);

      toast.dismiss(loadingToast);
      toast.success("Đã có kết quả!");
    } catch (error) {
      toast.dismiss();
      toast.error("Lỗi khi chấm điểm.");
    }
  };

  const messagesRef = useRef(messages);
  useEffect(() => {
    messagesRef.current = messages;
  }, [messages]);

  // [THÊM MỚI] Xử lý khi người dùng tắt tab hoặc F5
  useEffect(() => {
    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      // 1. Chỉ xử lý nếu phiên chưa kết thúc
      if (
        session &&
        session.status === "ONGOING" &&
        messagesRef.current.length > 0
      ) {
        // 2. Chuẩn bị payload
        const payload = JSON.stringify({
          history: messagesRef.current,
        });

        // 3. Sử dụng fetch với keepalive: true
        // Cơ chế này giúp request sống sót ngay cả khi tab đã đóng
        const endpoint = `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api"}/interview/${session.id}/end`;

        fetch(endpoint, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            // Thêm token nếu cần (thường lấy từ localStorage)
            Authorization: `Bearer ${localStorage.getItem("token") || ""}`,
          },
          body: payload,
          keepalive: true, // 🔥 QUAN TRỌNG NHẤT
        });
      }
    };

    window.addEventListener("beforeunload", handleBeforeUnload);

    return () => {
      window.removeEventListener("beforeunload", handleBeforeUnload);
    };
  }, [session]); // Chỉ phụ thuộc session ID

  // --- RENDER ---

  if (loading) {
    return (
      <div className="h-screen flex flex-col items-center justify-center bg-gray-50">
        <div className="w-16 h-16 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mb-4"></div>
        <h2 className="text-xl font-semibold text-gray-700">Đang kết nối...</h2>
      </div>
    );
  }

  // MODAL CONFIRM
  const ConfirmationModal = () => (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-fade-in">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-6 transform transition-all scale-100">
        <div className="flex justify-between items-start mb-4">
          <div className="p-3 bg-red-100 rounded-full">
            <AlertTriangle className="w-6 h-6 text-red-600" />
          </div>
          <button
            type="button"
            onClick={() => setShowConfirmModal(false)}
            className="text-gray-400 hover:text-gray-600 transition"
            title="Đóng"
            aria-label="Đóng modal"
          >
            <X size={20} />
          </button>
        </div>

        <h3 className="text-xl font-bold text-gray-900 mb-2">
          Kết thúc phỏng vấn?
        </h3>

        <p className="text-gray-600 mb-6 leading-relaxed">
          Bạn có chắc chắn muốn nộp bài và kết thúc phiên phỏng vấn này không?
          <br />
          Hành động này{" "}
          <span className="font-bold text-red-500">không thể hoàn tác</span> và
          nội dung chat sẽ được xóa sau khi chấm điểm.
        </p>

        <div className="flex gap-3 justify-end">
          <button
            onClick={() => setShowConfirmModal(false)}
            className="px-5 py-2.5 rounded-xl text-gray-700 font-medium hover:bg-gray-100 transition border border-gray-200"
          >
            Hủy bỏ
          </button>
          <button
            onClick={confirmEndInterview}
            className="px-5 py-2.5 rounded-xl bg-red-600 text-white font-medium hover:bg-red-700 transition shadow-lg shadow-red-200"
          >
            Đồng ý kết thúc
          </button>
        </div>
      </div>
    </div>
  );

  // UI KẾT QUẢ
  if (showHistory || session?.status === "COMPLETED") {
    return (
      <div className="min-h-screen bg-gray-50 p-6 flex items-center justify-center overflow-y-auto">
        <div className="max-w-3xl w-full bg-white rounded-3xl shadow-2xl p-8 md:p-12 border border-gray-100 relative overflow-hidden animate-fade-in-up">
          {/* Header Result */}
          <div className="text-center mb-10">
            <div className="inline-flex items-center justify-center w-20 h-20 bg-green-100 rounded-full mb-6 shadow-sm">
              <CheckCircle className="w-10 h-10 text-green-600" />
            </div>
            <h1 className="text-3xl font-extrabold text-gray-800 mb-2">
              Kết Quả Phỏng Vấn
            </h1>
            <div className="flex items-center justify-center gap-2 text-gray-500">
              <Briefcase size={16} />
              <span className="font-medium">{session?.jobTitle}</span>
              <span className="mx-2">•</span>
              <span>{session?.companyName}</span>
            </div>
          </div>

          {/* Score & Feedback */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-10">
            <div className="bg-gradient-to-br from-blue-50 to-indigo-50 p-6 rounded-2xl border border-blue-100 flex flex-col items-center justify-center text-center shadow-sm">
              <p className="text-sm font-bold text-blue-600 uppercase tracking-wider mb-2">
                Điểm Đánh Giá
              </p>
              <div className="text-6xl font-black text-blue-700 tracking-tighter">
                {session?.score ?? "?"}
                <span className="text-2xl text-blue-400 font-medium">/100</span>
              </div>
            </div>
            <div className="md:col-span-2 bg-white p-6 rounded-2xl border border-gray-200 shadow-sm hover:shadow-md transition">
              <div className="flex items-center gap-2 mb-3">
                <AlertCircle size={18} className="text-purple-600" />
                <p className="text-sm font-bold text-gray-700 uppercase">
                  Nhận xét từ AI
                </p>
              </div>
              <div className="text-gray-600 text-sm leading-relaxed max-h-60 overflow-y-auto pr-2 custom-scrollbar whitespace-pre-line">
                {session?.feedback || "Chưa có nhận xét."}
              </div>
            </div>
          </div>

          <div className="flex justify-center">
            <button
              onClick={() => router.push("/dashboard-candidate")}
              className="px-8 py-3.5 bg-gray-100 text-gray-700 font-bold rounded-xl hover:bg-gray-200 transition flex items-center justify-center gap-2"
            >
              <ArrowLeft size={18} /> Về trang chủ
            </button>
          </div>
        </div>
      </div>
    );
  }

  // UI CHAT (ĐÃ FIX)
  return (
    <div className="flex flex-col h-[100dvh] overflow-hidden bg-gray-100 relative">
      {showConfirmModal && <ConfirmationModal />}

      {/* HEADER */}
      <header className="bg-white px-6 py-4 flex justify-between items-center shadow-sm border-b border-gray-200 sticky top-0 z-20 shrink-0">
        <div className="flex items-center gap-4">
          <button
            onClick={() => router.back()}
            className="p-2 hover:bg-gray-100 rounded-full transition"
            title="Quay lại"
            aria-label="Quay lại"
          >
            <ArrowLeft size={20} className="text-gray-500" />
          </button>
          <div className="min-w-0">
            <h1 className="font-bold text-gray-800 flex items-center gap-2 truncate">
              <Briefcase size={16} className="text-blue-600 shrink-0" />
              <span className="truncate">
                {session?.jobTitle || "Phỏng vấn AI"}
              </span>
            </h1>
            <p className="text-xs text-gray-500 truncate">
              {session?.companyName}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3 shrink-0">
          <div className="hidden md:flex items-center gap-2 bg-green-50 text-green-700 px-3 py-1 rounded-full text-xs font-bold border border-green-100">
            <div className="w-2 h-2 rounded-full bg-green-50 animate-pulse"></div>{" "}
            Live
          </div>
          <button
            onClick={onClickEndInterview}
            className="flex items-center gap-2 bg-red-50 text-red-600 px-3 py-2 rounded-lg text-sm font-semibold hover:bg-red-100 transition border border-red-100"
            title="Kết thúc phỏng vấn"
            aria-label="Kết thúc phỏng vấn"
          >
            <LogOut size={16} />{" "}
            <span className="hidden sm:inline">Kết thúc</span>
          </button>
        </div>
      </header>

      {/* CHAT AREA */}
      <main className="flex-1 overflow-y-auto p-4 md:p-6 space-y-6 scroll-smooth bg-gray-50">
        {messages.map((msg, index) => {
          const isUser = msg.sender === "USER";
          return (
            <div
              key={index}
              className={`flex w-full ${isUser ? "justify-end" : "justify-start"}`}
            >
              <div
                className={`flex gap-3 max-w-[85%] md:max-w-[70%] ${isUser ? "flex-row-reverse" : "flex-row"}`}
              >
                <div
                  className={`w-8 h-8 md:w-10 md:h-10 rounded-full flex-shrink-0 flex items-center justify-center shadow-sm ${isUser ? "bg-blue-100" : "bg-gradient-to-br from-indigo-500 to-purple-600"}`}
                >
                  {isUser ? (
                    <User size={18} className="text-blue-600" />
                  ) : (
                    <Bot size={20} className="text-white" />
                  )}
                </div>
                <div
                  className={`flex flex-col ${isUser ? "items-end" : "items-start"}`}
                >
                  <div
                    className={`px-5 py-3.5 text-sm md:text-base leading-relaxed shadow-sm ${isUser ? "bg-blue-600 text-white rounded-2xl rounded-tr-none" : "bg-white text-gray-800 border border-gray-200 rounded-2xl rounded-tl-none"}`}
                  >
                    <p className="whitespace-pre-wrap break-words">
                      {msg.content}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          );
        })}
        {isSending && (
          <div className="flex w-full justify-start">
            <div className="flex gap-3 max-w-[70%]">
              <div className="w-8 h-8 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shadow-sm">
                <Bot size={20} className="text-white" />
              </div>
              <div className="bg-white border border-gray-200 px-4 py-3 rounded-2xl rounded-tl-none shadow-sm flex items-center gap-1.5 h-12">
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"></div>
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce delay-75"></div>
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce delay-150"></div>
              </div>
            </div>
          </div>
        )}
        <div ref={messagesEndRef} className="h-4" />
      </main>

      {/* FOOTER (ĐÃ XÓA MIC & CĂN CHỈNH) */}
      <div className="bg-white border-t border-gray-200 p-4 md:px-6 md:py-5 sticky bottom-0 z-20 shrink-0">
        <div className="max-w-4xl mx-auto">
          <form onSubmit={handleSend} className="relative w-full">
            <textarea
              value={inputStr}
              onChange={(e) => setInputStr(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.shiftKey) {
                  e.preventDefault();
                  handleSend();
                }
              }}
              disabled={isSending}
              placeholder="Nhập câu trả lời..."
              // Thay đổi pr-12 -> pr-14 để không bị nút Send che chữ
              className="w-full bg-gray-50 border border-gray-300 text-gray-800 rounded-2xl px-4 py-3 pr-14 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition resize-none min-h-[56px] max-h-32 shadow-sm"
              rows={1}
            />
            {/* Nút Send căn chỉnh absolute */}
            <button
              type="submit"
              disabled={!inputStr.trim() || isSending}
              title="Gửi tin nhắn"
              aria-label="Gửi tin nhắn"
              className="absolute right-3 bottom-3 p-2 bg-blue-600 text-white rounded-xl hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition shadow-md flex items-center justify-center mb-1"
            >
              <Send size={18} />
            </button>
          </form>
          <p className="text-center text-xs text-gray-400 mt-2 hidden sm:block">
            Trả lời tự nhiên. Nội dung chat sẽ không được lưu.
          </p>
        </div>
      </div>
    </div>
  );
}
