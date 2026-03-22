"use client";

import { Search, Home as HomeIcon } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { 
  DropdownMenu, 
  DropdownMenuContent, 
  DropdownMenuItem, 
  DropdownMenuLabel, 
  DropdownMenuSeparator, 
  DropdownMenuTrigger 
} from "@/components/ui/dropdown-menu";
import Link from "next/link";
import { useAuthStore } from "@/hooks/useAuthStore";
import { useLogout } from "@/hooks/useAuth";
import { useState, useEffect } from "react";

export function TopNav() {
  const [mounted, setMounted] = useState(false);
  const user = useAuthStore((state) => state.user);
  const isAuth = useAuthStore((state) => state.isAuthenticated());
  const logout = useLogout();

  useEffect(() => {
    setMounted(true);
  }, []);

  return (
    <nav className="sticky top-0 z-10 flex h-16 items-center justify-between bg-background/80 px-6 backdrop-blur-md">
      {/* Left / Center Section */}
      <div className="flex items-center gap-2 lg:gap-8 flex-1">
        <div className="flex items-center gap-2">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-card text-foreground cursor-pointer" aria-label="Trang chủ">
            <HomeIcon className="h-6 w-6" />
          </div>
          <div className="relative group w-[300px] lg:w-[400px]">
            <Search className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-muted-foreground group-focus-within:text-foreground" />
            <Input
              placeholder="Bạn muốn phát nội dung gì?"
              aria-label="Tìm kiếm nội dung"
              className="h-12 w-full rounded-full border-none bg-muted pl-10 pr-10 text-foreground placeholder:text-muted-foreground focus-visible:ring-2 focus-visible:ring-ring/20"
            />
          </div>
        </div>
      </div>

      {/* Right Section: Auth */}
      <div className="flex items-center gap-4">
        {mounted && isAuth ? (
          <div className="flex items-center gap-4">
            <div className="hidden md:flex flex-col items-end">
              <span className="text-sm font-bold">{user?.displayName}</span>
              <span className="text-xs text-muted-foreground">{user?.email}</span>
            </div>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <div 
                  className="h-10 w-10 md:h-12 md:w-12 rounded-full overflow-hidden border-2 border-border cursor-pointer bg-card flex items-center justify-center hover:scale-105 transition-transform"
                >
                  {user?.avatarUrl ? (
                    <img src={user.avatarUrl} alt="Avatar" className="h-full w-full object-cover" />
                  ) : (
                    <span className="font-bold text-lg uppercase">{user?.displayName?.charAt(0) || "U"}</span>
                  )}
                </div>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56 bg-card border-border shadow-2xl rounded-xl p-2 font-medium">
                <DropdownMenuLabel className="text-muted-foreground pb-2 px-3 text-xs uppercase tracking-wider">Tài khoản</DropdownMenuLabel>
                <DropdownMenuItem className="cursor-pointer py-3 px-3 hover:bg-white/10 rounded-sm transition-colors text-foreground">
                  Hồ sơ
                </DropdownMenuItem>
                <DropdownMenuItem className="cursor-pointer py-3 px-3 hover:bg-white/10 rounded-sm transition-colors text-foreground">
                  Cài đặt
                </DropdownMenuItem>
                <DropdownMenuSeparator className="bg-border/50 my-1" />
                <DropdownMenuItem 
                  className="cursor-pointer py-3 px-3 hover:bg-white/10 rounded-sm transition-colors text-foreground focus:bg-white/10"
                  onClick={logout}
                >
                  Đăng xuất
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        ) : mounted ? (
          <>
            <Link href="/register">
              <span className="text-muted-foreground hover:text-foreground font-bold hover:scale-105 transition-all cursor-pointer">
                Đăng ký
              </span>
            </Link>
            <Link href="/login">
              <Button className="rounded-full bg-foreground text-background font-bold px-8 py-6 hover:scale-105 transition-transform">
                Đăng nhập
              </Button>
            </Link>
          </>
        ) : (
          <div className="w-48 h-12"></div> // Placeholder layout shift prevention
        )}
      </div>
    </nav>
  );
}
