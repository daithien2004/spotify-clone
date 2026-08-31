"use client";

import { useEffect, useState, Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { AuthService } from "@/services/api/authService";
import { useAuthStore } from "@/hooks/useAuthStore";

type Status = "verifying" | "success" | "error";

function VerifyEmailContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token") ?? "";
  const [status, setStatus] = useState<Status>("verifying");
  const user = useAuthStore((s) => s.user);

  useEffect(() => {
    let cancelled = false;
    if (!token) {
      setStatus("error");
      return;
    }
    AuthService.verifyEmail(token)
      .then(() => { if (!cancelled) setStatus("success"); })
      .catch(() => { if (!cancelled) setStatus("error"); });
    return () => { cancelled = true; };
  }, [token]);

  if (status === "verifying") {
    return (
      <div className="flex flex-col items-center gap-4">
        <div className="h-12 w-12 animate-spin rounded-full border-4 border-spotify-green border-t-transparent" />
        <p className="text-foreground">Verifying your email…</p>
      </div>
    );
  }
  if (status === "success") {
    return (
      <div className="space-y-4 text-center">
        <h1 className="text-3xl font-bold text-foreground">Email verified</h1>
        <p className="text-muted-foreground">Your account email has been confirmed.</p>
        <Button asChild className="w-full bg-spotify-green hover:opacity-90 text-black font-bold h-12 rounded-full">
          <Link href={user ? "/" : "/login"}>{user ? "Back to home" : "Log in"}</Link>
        </Button>
      </div>
    );
  }
  return (
    <div className="space-y-4 text-center">
      <h1 className="text-3xl font-bold text-foreground">Verification link invalid</h1>
      <p className="text-muted-foreground">This link may have expired. Request a new verification email.</p>
      <Button asChild className="w-full bg-spotify-green hover:opacity-90 text-black font-bold h-12 rounded-full">
        <Link href="/account">Go to account</Link>
      </Button>
    </div>
  );
}

export default function VerifyEmailPage() {
  return (
    <Suspense
      fallback={
        <div className="flex justify-center">
          <div className="h-12 w-12 animate-spin rounded-full border-4 border-spotify-green border-t-transparent" />
        </div>
      }
    >
      <VerifyEmailContent />
    </Suspense>
  );
}