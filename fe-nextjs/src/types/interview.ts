// src/types/interview.ts

export interface MessageItem {
  sender: "USER" | "AI";
  content: string;
  sentAt?: string; // Optional, dùng để hiển thị giờ nếu thích
}

// Response khi gọi API /start
export interface StartInterviewResponse {
  sessionId: number;
  greeting: string;
}

// DTO gửi lên khi Chat
export interface ChatPayload {
  message: string;
  history: MessageItem[];
}

// DTO gửi lên khi End
export interface EndPayload {
  history: MessageItem[];
}

// Session (Chỉ còn thông tin chung và kết quả)
export interface InterviewSession {
  id: number;
  status: string;
  score: number | null;
  feedback: string | null;
  jobId: number;
  jobTitle: string;
  companyName: string;
  createdAt: string;
}
