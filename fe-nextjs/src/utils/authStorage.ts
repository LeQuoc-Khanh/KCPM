import Cookies from 'js-cookie';

const TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';
const USER_ROLE_KEY = 'userRole'; // Lưu thêm role để middleware dễ xử lý

export const setToken = (token: string) => {
  // Expires 1 ngày, secure true nếu chạy https
  Cookies.set(TOKEN_KEY, token, { expires: 1, path: '/' });
};

export const getToken = () => {
  return Cookies.get(TOKEN_KEY);
};

export const removeToken = () => {
  Cookies.remove(TOKEN_KEY, { path: '/' });
  Cookies.remove(REFRESH_TOKEN_KEY, { path: '/' });
  Cookies.remove(USER_ROLE_KEY, { path: '/' });
};

export const setRefreshToken = (token: string) => {
  Cookies.set(REFRESH_TOKEN_KEY, token, { expires: 7, path: '/' });
};

export const getRefreshToken = () => {
  return Cookies.get(REFRESH_TOKEN_KEY);
};

export const setUserRole = (role: string) => {
  if (typeof window !== 'undefined') {
    localStorage.setItem(USER_ROLE_KEY, role);
  }
  // Quan trọng: Lưu vào Cookie để Middleware đọc được
  Cookies.set(USER_ROLE_KEY, role, { expires: 1, path: '/' });
};

export const getUserRole = () => {
  if (typeof window !== 'undefined') {
    return localStorage.getItem(USER_ROLE_KEY);
  }
  return Cookies.get(USER_ROLE_KEY);
};