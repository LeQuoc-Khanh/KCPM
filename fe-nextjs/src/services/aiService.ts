import api from "@/services/api";

// Định nghĩa kiểu dữ liệu trả về từ Backend (khớp với ChatResponse DTO)
export interface ChatResponse {
  response: string;
}

export const aiService = {
  /**
   * Gửi câu hỏi đến AI Controller
   */
  askAI: async (message: string): Promise<string> => {
    try {
      // Gọi endpoint POST /api/chat/ask mà bạn đã tạo ở Backend
      const { data } = await api.post<ChatResponse>("/chat/ask", { message });
      return data.response;
    } catch (error) {
      console.error("AI Service Error:", error);
      return "Xin lỗi, tôi đang gặp sự cố kết nối. Vui lòng thử lại sau.";
    }
  },
};
