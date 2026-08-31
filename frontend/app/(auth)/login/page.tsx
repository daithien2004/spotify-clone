"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useLogin, useVerify2faLogin } from "@/hooks/useAuth";
import { validateTotpCode } from "@/lib/validation/auth";
import { Loader2 } from "lucide-react";
import { SocialButton } from "@/components/auth/SocialButton";
import { toast } from "sonner";

const GATEWAY_URL = process.env.NEXT_PUBLIC_GATEWAY_URL || "http://localhost:9000";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [mfaToken, setMfaToken] = useState<string | null>(null);
  const [code, setCode] = useState("");
  const loginMutation = useLogin();
  const verify2faLogin = useVerify2faLogin();

  // Khi login trả mfaRequired → chuyển sang bước nhập 6 chữ số (giữ mfaToken trong memory — không persist)
  useEffect(() => {
    if (loginMutation.data?.mfaRequired && loginMutation.data.mfaToken) {
      setMfaToken(loginMutation.data.mfaToken);
    }
  }, [loginMutation.data]);

  const handleVerify2fa = (e: React.FormEvent) => {
    e.preventDefault();
    const err = validateTotpCode(code);
    if (err) return toast.error(err);
    if (!mfaToken) return toast.error("Session expired — log in again.");
    verify2faLogin.mutate(
      { mfaToken, code },
      {
        onError: (error) =>
          toast.error("Verification failed", {
            description: error.message || "Enter the 6-digit code from your app.",
          }),
      }
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    loginMutation.mutate(
      { email, password },
      {
        onError: (error) => {
          toast.error("Log in failed", {
            description: error.message || "Please check your credentials and try again.",
          });
        },
      }
    );
  };

  const handleGoogleLogin = () => {
    // Redirect to Gateway OAuth2 (Google) — toàn trang để nhận HttpOnly session cookie.
    window.location.href = `${GATEWAY_URL}/oauth2/authorization/google`;
  };

  if (mfaToken) {
    return (
      <div className="flex flex-col items-center w-full max-w-[450px] mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-1000 transition-colors">
        <h1 className="text-4xl md:text-5xl font-bold text-center tracking-tighter text-foreground mb-2 leading-tight">
          Two-factor authentication
        </h1>
        <p className="text-muted-foreground text-sm text-center">
          Enter the 6-digit code from your authenticator app.
        </p>
        <form onSubmit={handleVerify2fa} className="w-full space-y-6">
          <Input
            id="2fa-code"
            type="text"
            inputMode="numeric"
            maxLength={6}
            placeholder="123456"
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))}
            className="h-14 bg-background border-border text-foreground text-center text-2xl tracking-[0.5em] placeholder:text-muted-foreground rounded-[4px]"
          />
          <Button
            className="w-full bg-spotify-green hover:opacity-90 text-black font-bold h-12 rounded-full transition-transform active:scale-[0.98] disabled:opacity-70"
            type="submit"
            disabled={verify2faLogin.isPending}
          >
            {verify2faLogin.isPending ? <Loader2 className="w-5 h-5 animate-spin" /> : "Verify"}
          </Button>
        </form>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center w-full max-w-[450px] mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-1000 transition-colors">
      <h1 className="text-4xl md:text-5xl font-bold text-center tracking-tighter text-foreground mb-2 leading-tight">
        Welcome back
      </h1>

      <form onSubmit={handleSubmit} className="w-full space-y-6">
        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="email" className="text-sm font-bold text-foreground">
              Email or username
            </Label>
            <Input
              id="email"
              type="text"
              placeholder="Email or username"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="h-12 bg-background border-border hover:border-foreground focus:border-foreground focus:ring-1 focus:ring-ring text-foreground placeholder:text-muted-foreground transition-all rounded-[4px]"
            />
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label htmlFor="password" className="text-sm font-bold text-foreground">
                Password
              </Label>
            </div>
            <Input
              id="password"
              type="password"
              placeholder="Password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="h-12 bg-background border-border hover:border-foreground focus:border-foreground focus:ring-1 focus:ring-ring text-foreground placeholder:text-muted-foreground transition-all rounded-[4px]"
            />
          </div>
        </div>

        <Button
          className="w-full bg-spotify-green hover:opacity-90 text-black font-bold h-12 rounded-full transition-transform active:scale-[0.98] disabled:opacity-70"
          type="submit"
          disabled={loginMutation.isPending}
        >
          {loginMutation.isPending ? (
            <div className="flex items-center gap-2">
              <Loader2 className="w-5 h-5 animate-spin" />
              <span>Logging in...</span>
            </div>
          ) : "Log in"}
        </Button>

        <div className="text-center">
          <Link
            href="/forgot-password"
            className="text-sm text-foreground hover:text-spotify-green underline underline-offset-4 decoration-border hover:decoration-spotify-green transition-colors"
          >
            Forgot your password?
          </Link>
        </div>
      </form>

      <div className="w-full space-y-3">
        <SocialButton
          onClick={handleGoogleLogin}
          icon={
            <svg className="w-5 h-5" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z" />
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
            </svg>
          }
          text="Continue with Google"
        />
      </div>

      <div className="w-full border-muted text-center">
        <p className="text-muted-foreground text-base">
          Don&apos;t have an account?{" "}
          <Link
            href="/register"
            className="text-foreground hover:text-spotify-green font-bold underline underline-offset-4 decoration-border hover:decoration-spotify-green transition-colors"
          >
            Sign up for Spotify.
          </Link>
        </p>
      </div>
    </div>
  );
}