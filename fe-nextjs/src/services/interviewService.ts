// src/services/interviewService.ts
import api from "./api";
import {
  StartInterviewResponse,
  MessageItem,
  InterviewSession,
} from "@/types/interview";

export const interviewService = {
  // 1. Bắt đầu phỏng vấn
  // Backend trả về: { data: { sessionId: 123, greeting: "Chào bạn..." } }
  startInterview: async (jobId: number): Promise<StartInterviewResponse> => {
    const response = await api.post("/interview/start", { jobId });
    return response.data.data; // Lấy phần data trong MessageResponse
  },

  // 2. Chat (Gửi tin nhắn mới + Lịch sử cũ)
  sendMessage: async (
    sessionId: number,
    message: string,
    history: MessageItem[],
  ): Promise<string> => {
    // Lưu ý: 'history' là danh sách tin nhắn TRƯỚC KHI user nhập câu mới
    const response = await api.post(`/interview/${sessionId}/chat`, {
      message,
      history,
    });
    return response.data.data; // Backend trả về chuỗi text câu trả lời của AI
  },

  // 3. Kết thúc (Gửi toàn bộ lịch sử để chấm điểm)
  endInterview: async (
    sessionId: number,
    fullHistory: MessageItem[],
  ): Promise<InterviewSession> => {
    const response = await api.post(`/interview/${sessionId}/end`, {
      history: fullHistory,
    });
    return response.data.data; // Trả về Session đã có điểm & feedback
  },

  // 4. Lấy kết quả (Chỉ xem điểm, không xem chat)
  getSessionResult: async (sessionId: number): Promise<InterviewSession> => {
    const response = await api.get(`/interview/${sessionId}`);
    return response.data.data;
  },

  // 5. Lấy lịch sử danh sách
  getHistory: async (jobId: number) => {
    const response = await api.get(`/interview/history?jobId=${jobId}`);
    return response.data.data;
  },
};
