"use client";

import { useState, Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useResetPassword } from "@/hooks/useAuth";
import { validatePassword, validateConfirmPassword } from "@/lib/validation/auth";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";

function ResetPasswordForm() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token") ?? "";
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [done, setDone] = useState(false);
  const resetMutation = useResetPassword();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const pwErr = validatePassword(password);
    if (pwErr) return toast.error(pwErr);
    const cfErr = validateConfirmPassword(password, confirm);
    if (cfErr) return toast.error(cfErr);
    if (!token) return toast.error("This link is invalid or expired. Request a new one.");
    resetMutation.mutate(
      { token, newPassword: password },
      {
        onSuccess: () => setDone(true),
        onError: (error) =>
          toast.error("Reset failed", {
            description: error.message || "This link may have expired. Request a new one.",
          }),
      }
    );
  };

  if (done) {
    return (
      <div className="w-full text-center space-y-4">
        <h1 className="text-3xl font-bold tracking-tight text-foreground">Password updated</h1>
        <p className="text-muted-foreground">Your password has been reset. Log in with your new password.</p>
        <Button asChild className="w-full bg-spotify-green hover:opacity-90 text-black font-bold h-12 rounded-full">
          <Link href="/login">Log in</Link>
        </Button>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center w-full max-w-[450px] mx-auto space-y-8">
      <h1 className="text-4xl md:text-5xl font-bold text-center tracking-tighter text-foreground">
        Choose a new password
      </h1>
      <form onSubmit={handleSubmit} className="w-full space-y-6">
        <div className="space-y-2">
          <Label htmlFor="newPassword" className="text-sm font-bold text-foreground">New password</Label>
          <Input
            id="newPassword" type="password" placeholder="At least 8 characters" required
            value={password} onChange={(e) => setPassword(e.target.value)}
            className="h-12 bg-background border-border text-foreground placeholder:text-muted-foreground rounded-[4px]"
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="confirmPassword" className="text-sm font-bold text-foreground">Confirm password</Label>
          <Input
            id="confirmPassword" type="password" placeholder="Repeat password" required
            value={confirm} onChange={(e) => setConfirm(e.target.value)}
            className="h-12 bg-background border-border text-foreground placeholder:text-muted-foreground rounded-[4px]"
          />
        </div>
        <Button type="submit" disabled={resetMutation.isPending}
          className="w-full bg-spotify-green hover:opacity-90 text-black font-bold h-12 rounded-full">
          {resetMutation.isPending ? <Loader2 className="w-5 h-5 animate-spin" /> : "Reset password"}
        </Button>
      </form>
    </div>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense
      fallback={
        <div className="flex justify-center">
          <div className="h-12 w-12 animate-spin rounded-full border-4 border-spotify-green border-t-transparent" />
        </div>
      }
    >
      <ResetPasswordForm />
    </Suspense>
  );
}