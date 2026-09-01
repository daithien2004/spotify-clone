import axios, {
  AxiosInstance,
  AxiosRequestConfig,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from "axios";
import { useAuthStore } from "@/hooks/useAuthStore";

/**
 * Envelope chuẩn của backend (common-lib ApiResponse): mọi success response
 * đều được GlobalResponseWrapper bọc thành { success, data, message, timestamp }.
 */
export interface ApiResponse<T = unknown> {
  success: boolean;
  data: T;
  message?: string;
  timestamp: string;
}

/** Lấy payload thật ra khỏi envelope — gọi sau khi api.* trả về toàn bộ body. */
export function unwrap<T>(envelope: ApiResponse<T>): T {
  return envelope.data;
}

/** Quy đường dẫn tương đối về absolute qua gateway.
 *  `audioUrl` từ backend (VD seed) đã kèm sẵn prefix `/api/v1/tracks/.../audio` —
 *  nếu gắn thêm `${BASE_URL}` (vốn đã gồm `/api/v1`) sẽ tạo URL kép
 *  `/api/v1/api/v1/...` → 401/format error (bug phát hiện bởi E2E main flow P4).
 *  Path đã kèm `/api/v1/` → xem như là API path đầy đủ, chỉ cần gắn origin
 *  (new URL với path bắt đầu bằng `/` sẽ thay thế path của base). Path relative
 *  khác vẫn gắn BASE_URL như trước. */
export function resolveApiUrl(path: string): string {
  if (/^https?:\/\//.test(path)) return path;
  if (path.startsWith("/api/v1/")) return new URL(path, BASE_URL).toString();
  if (path.startsWith("/")) return `${BASE_URL}${path}`;
  return `${BASE_URL}/${path}`;
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
    return { message: "No internet connection. Please check your connection.", status: 0 };
  }
  if (error.message.includes("timeout")) {
    return { message: "Request timed out. Please try again.", status: 408 };
  }
  return { message: error.message, status: 0 };
}

// Payload lỗi từ server không có schema cố định → narrow cast có cấu trúc.
type ErrorPayload = {
  message?: string;
  code?: ApiError["code"];
  errors?: ApiError["errors"];
};

function buildApiError(status: number, data: unknown): ApiError {
  const body = (data ?? {}) as ErrorPayload;
  const base: ApiError = {
    message: body.message ?? "Something went wrong.",
    status,
    code: body.code,
    errors: body.errors,
  };

  const messages: Record<number, string> = {
    400: "Invalid data.",
    403: "You don't have permission to do this.",
    404: "Resource not found.",
    409: "Data conflict.",
    422: "Data could not be processed.",
    429: "Too many requests. Please try again later.",
    500: "Server error. Please try again later.",
    503: "Server is under maintenance.",
  };

  if (!body.message && messages[status]) {
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
