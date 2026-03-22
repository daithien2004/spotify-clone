"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ChevronLeft } from "lucide-react";

export default function ForgotPasswordPage() {
  return (
    <div className="flex flex-col items-center w-full max-w-[450px] mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-1000">
      <div className="w-full">
        <Link 
          href="/login" 
          className="inline-flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors group"
        >
          <ChevronLeft className="w-4 h-4 transition-transform group-hover:-translate-x-1" />
          <span>Quay lại đăng nhập</span>
        </Link>
      </div>

      <div className="space-y-2 text-center">
        <h1 className="text-4xl md:text-5xl font-bold tracking-tighter text-foreground">
          Khôi phục mật khẩu
        </h1>
        <p className="text-muted-foreground">
          Nhập email của bạn để nhận hướng dẫn khôi phục mật khẩu.
        </p>
      </div>

      <form className="w-full space-y-6">
        <div className="space-y-2">
          <Label htmlFor="email" className="text-sm font-bold text-foreground">
            Địa chỉ email
          </Label>
          <Input
            id="email"
            type="email"
            placeholder="name@example.com"
            required
            className="h-12 bg-background border-border hover:border-foreground focus:border-foreground focus:ring-1 focus:ring-ring text-foreground placeholder:text-muted-foreground transition-all rounded-[4px]"
          />
        </div>

        <Button
          className="w-full bg-spotify-green hover:opacity-90 text-black font-bold h-12 rounded-full transition-transform active:scale-[0.98]"
          type="submit"
        >
          Gửi yêu cầu
        </Button>
      </form>
    </div>
  );
}
