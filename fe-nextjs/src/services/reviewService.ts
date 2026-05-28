import api from "./api"; // Axios instance của bạn

export interface Review {
  id: number;
  rating: number;
  comment: string;
  reviewerName: string;
  reviewerAvatar?: string;
  createdAt: string;
}

export const reviewService = {
  getReviews: async (companyId: number) => {
    const response = await api.get(`/reviews/company/${companyId}`);
    return response.data;
  },
  
  getAverageRating: async (companyId: number) => {
    const response = await api.get(`/reviews/company/${companyId}/average`);
    return response.data;
  },

  createReview: async (data: { companyId: number; rating: number; comment: string }) => {
    const response = await api.post("/reviews", data);
    return response.data;
  }
};