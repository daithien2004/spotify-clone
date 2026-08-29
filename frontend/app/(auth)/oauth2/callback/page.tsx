"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/hooks/useAuthStore";
import { AuthService } from "@/services/api/authService";
import { toast } from "sonner";

// Backend đã set HttpOnly cookie sau Google Login → chỉ cần fetch /auth/me rồi redirect.
export default function OAuth2CallbackPage() {
  const router = useRouter();
  const setAuth = useAuthStore((s) => s.setAuth);

  useEffect(() => {
    const onboardOAuth2User = async () => {
      try {
        const response = await AuthService.me();

        if (response.success && response.data) {
          setAuth(response.data);
          toast.success("Signed in with Google!");
          router.push("/");
        } else {
          throw new Error("Could not fetch user info");
        }
      } catch (error) {
        console.error("OAuth2 Callback Error:", error);
        toast.error("Something went wrong while signing in with Google.");
        router.push("/login");
      }
    };

    onboardOAuth2User();
  }, [router, setAuth]);

  return (
    <div className="flex h-screen w-screen flex-col items-center justify-center bg-black text-white">
      <div className="h-12 w-12 animate-spin rounded-full border-4 border-spotify-green border-t-transparent"></div>
      <p className="mt-4 text-lg font-medium">Completing your sign in...</p>
    </div>
  );
}