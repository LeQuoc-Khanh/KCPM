import api from "./api";

// Định nghĩa interface cho dữ liệu Công ty
export interface Company {
  id: number;
  name: string;
  description: string;
  logoUrl: string;
  coverImageUrl?: string;
  website: string;
  size: string;
  address: string;
  industry?: string;
  email?: string;
  phone?: string;
}

export const companyService = {
  // Lấy thông tin chi tiết công ty theo ID
  getById: async (id: number): Promise<Company> => {
    const response = await api.get(`/companies/${id}`);
    return response.data;
  }
};