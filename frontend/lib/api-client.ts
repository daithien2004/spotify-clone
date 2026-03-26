import axios, {
  AxiosInstance,
  AxiosRequestConfig,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from "axios";
import { useAuthStore } from "@/hooks/useAuthStore";

export interface ApiResponse<T = unknown> {
  data: T;
  message?: string;
  status: number;
}

export interface ApiError {
  message: string;
  status: number;
  code?: string;
  errors?: Record<string, string[]>;
}

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:9000/api/v1";

const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 15_000,
  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
  },
  withCredentials: true, 
});

apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    config.headers["X-Request-ID"] = crypto.randomUUID();
    return config;
  },
  (error) => Promise.reject(error)
);

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) prom.reject(error);
    else prom.resolve(token);
  });
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (response: AxiosResponse) => response,

  async (error) => {
    const originalRequest = error.config as AxiosRequestConfig & {
      _retry?: boolean;
    };
    if (!error.response) {
      return Promise.reject(buildNetworkError(error));
    }

    const { status, data } = error.response;

    if (status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then(() => {
            return apiClient(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        await axios.post(
          `${BASE_URL}/auth/refresh`,
          {},
          { withCredentials: true }
        );
        
        processQueue(null);
        return apiClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError);
        useAuthStore.getState().clearAuth();
        
        if (typeof window !== "undefined" && window.location.pathname !== "/login") {
           window.location.href = "/login";
        }
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(buildApiError(status, data));
  }
);

function buildNetworkError(error: Error): ApiError {
  if (error.message === "Network Error") {
    return { message: "Không có kết nối mạng. Vui lòng kiểm tra lại.", status: 0 };
  }
  if (error.message.includes("timeout")) {
    return { message: "Request timeout. Vui lòng thử lại.", status: 408 };
  }
  return { message: error.message, status: 0 };
}

function buildApiError(status: number, data: any): ApiError {
  const base: ApiError = {
    message: data?.message || "Đã có lỗi xảy ra.",
    status,
    code: data?.code,
    errors: data?.errors,
  };

  const messages: Record<number, string> = {
    400: "Dữ liệu không hợp lệ.",
    403: "Bạn không có quyền thực hiện hành động này.",
    404: "Không tìm thấy tài nguyên.",
    409: "Xung đột dữ liệu.",
    422: "Dữ liệu không thể xử lý.",
    429: "Quá nhiều request. Vui lòng thử lại sau.",
    500: "Lỗi server. Vui lòng thử lại sau.",
    503: "Server đang bảo trì.",
  };

  if (!data?.message && messages[status]) {
    base.message = messages[status];
  }

  return base;
}

export const api = {
  get: <T>(url: string, config?: AxiosRequestConfig) =>
    apiClient.get<T>(url, config).then((r) => r.data),

  post: <T>(url: string, body?: unknown, config?: AxiosRequestConfig) =>
    apiClient.post<T>(url, body, config).then((r) => r.data),

  put: <T>(url: string, body?: unknown, config?: AxiosRequestConfig) =>
    apiClient.put<T>(url, body, config).then((r) => r.data),

  patch: <T>(url: string, body?: unknown, config?: AxiosRequestConfig) =>
    apiClient.patch<T>(url, body, config).then((r) => r.data),

  delete: <T>(url: string, config?: AxiosRequestConfig) =>
    apiClient.delete<T>(url, config).then((r) => r.data),
};

export default apiClient;
