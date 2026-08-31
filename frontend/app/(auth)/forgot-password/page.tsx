"use client";

import { useState } from "react";
import Link from "next/link";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { useForgotPassword } from "@/hooks/useAuth";
import { validateEmail } from "@/lib/validation/auth";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const forgotMutation = useForgotPassword();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const err = validateEmail(email);
    if (err) return toast.error(err);
    forgotMutation.mutate(email, {
      onError: (error) =>
        toast.error("Failed to send reset email", {
          description: error.message || "Please try again.",
        }),
      onSuccess: () => setSent(true),
    });
  };

  return (
    <div className="flex flex-col items-center w-full max-w-[450px] mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-1000 transition-colors">
      <h1 className="text-4xl md:text-5xl font-bold text-center tracking-tighter text-foreground mb-2 leading-tight">
        Reset your password
      </h1>
      {sent ? (
        <div className="w-full space-y-4 text-center">
          <p className="text-muted-foreground">
            If {email} is registered, we sent a reset link. Check your inbox and pick up where you left off.
          </p>
          <Button asChild className="w-full bg-spotify-green hover:opacity-90 text-black font-bold h-12 rounded-full">
            <Link href="/login">Back to login</Link>
          </Button>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="w-full space-y-6">
          <div className="space-y-2">
            <Label htmlFor="email" className="text-sm font-bold text-foreground">Email</Label>
            <Input
              id="email" type="email" placeholder="name@example.com" required
              value={email} onChange={(e) => setEmail(e.target.value)}
              className="h-12 bg-background border-border hover:border-foreground focus:border-foreground text-foreground placeholder:text-muted-foreground rounded-[4px]"
            />
          </div>
          <Button className="w-full bg-spotify-green hover:opacity-90 text-black font-bold h-12 rounded-full" type="submit" disabled={forgotMutation.isPending}>
            {forgotMutation.isPending ? <Loader2 className="w-5 h-5 animate-spin" /> : "Send reset link"}
          </Button>
          <div className="text-center">
            <Link href="/login" className="text-sm text-foreground hover:text-spotify-green underline underline-offset-4 decoration-border">
              Back to login
            </Link>
          </div>
        </form>
      )}
    </div>
  );
}