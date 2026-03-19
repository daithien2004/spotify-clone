import { useMutation } from "@tanstack/react-query";
import { AuthService, LoginRequest, RegisterRequest } from "@/services/api/authService";

export const useLogin = () => {
  return useMutation({
    mutationFn: (data: LoginRequest) => AuthService.login(data),
  });
};

export const useRegister = () => {
  return useMutation({
    mutationFn: (data: RegisterRequest) => AuthService.register(data),
  });
};
