"use client";

import { useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useLogin } from "@/hooks/useAuth";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const loginMutation = useLogin();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    loginMutation.mutate({ email, password }, {
      onSuccess: (response) => {
        console.log("Login successful:", response);
        localStorage.setItem("token", response.token);
        alert("Login successful!");
      },
      onError: (error) => {
        console.error("Login failed:", error);
        alert(error.message || "Login failed. Please check your credentials.");
      }
    });
  };

  return (
    <Card className="border-none shadow-none bg-transparent">
      <CardHeader className="space-y-1 text-center">
        <CardTitle className="text-3xl font-bold tracking-tight">Login</CardTitle>
        <CardDescription className="text-zinc-500 dark:text-zinc-400">
          Enter your email and password to access your account
        </CardDescription>
      </CardHeader>
      <form onSubmit={handleSubmit}>
        <CardContent className="grid gap-4">
          <div className="grid gap-2">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              placeholder="name@example.com"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="bg-white dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800"
            />
          </div>
          <div className="grid gap-2">
            <div className="flex items-center justify-between">
              <Label htmlFor="password">Password</Label>
              <Link 
                href="/auth/forgot-password" 
                className="text-xs text-zinc-500 hover:text-zinc-950 dark:text-zinc-400 dark:hover:text-zinc-50 underline-offset-4 hover:underline"
              >
                Forgot password?
              </Link>
            </div>
            <Input
              id="password"
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="bg-white dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800"
            />
          </div>
        </CardContent>
        <CardFooter className="flex flex-col gap-4">
          <Button 
            className="w-full bg-green-500 hover:bg-green-600 text-black font-bold py-6 rounded-full transition-all"
            type="submit"
            disabled={loginMutation.isPending}
          >
            {loginMutation.isPending ? "Logging in..." : "Log In"}
          </Button>
          <div className="text-center text-sm text-zinc-500 dark:text-zinc-400">
            Don&apos;t have an account?{" "}
            <Link 
              href="/auth/register" 
              className="font-medium text-zinc-950 dark:text-zinc-50 hover:underline underline-offset-4"
            >
              Sign up free
            </Link>
          </div>
        </CardFooter>
      </form>
    </Card>
  );
}
