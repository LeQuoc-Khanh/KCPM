import axiosInstance from './api';
import { AuthResponseData } from '@/types/auth'; 

export const paymentService = {
  upgradeToVip: async (): Promise<AuthResponseData> => {
    const response = await axiosInstance.post<AuthResponseData>('/payment/vip-upgrade');
    return response.data;
  },
};