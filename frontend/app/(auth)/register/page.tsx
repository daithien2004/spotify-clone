"use client";

import { useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { useRegister } from "@/hooks/useAuth";

export default function RegisterPage() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const registerMutation = useRegister();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    registerMutation.mutate({ name, email, password }, {
      onSuccess: (response) => {
        console.log("Registration successful:", response);
        alert("Registration successful! You can now log in.");
      },
      onError: (error) => {
        console.error("Registration failed:", error);
        alert(error.message || "Registration failed. Please try again.");
      }
    });
  };

  return (
    <Card className="border-none shadow-none bg-transparent">
      <CardHeader className="space-y-1 text-center">
        <CardTitle className="text-3xl font-bold tracking-tight">Sign up</CardTitle>
        <CardDescription className="text-zinc-500 dark:text-zinc-400">
          Create an account to start listening
        </CardDescription>
      </CardHeader>
      <form onSubmit={handleSubmit}>
        <CardContent className="grid gap-4">
          <div className="grid gap-2">
            <Label htmlFor="name">Full Name</Label>
            <Input
              id="name"
              type="text"
              placeholder="John Doe"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="bg-white dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800"
            />
          </div>
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
            <Label htmlFor="password">Password</Label>
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
            disabled={registerMutation.isPending}
          >
            {registerMutation.isPending ? "Creating account..." : "Sign Up"}
          </Button>
          <div className="text-center text-sm text-zinc-500 dark:text-zinc-400">
            Already have an account?{" "}
            <Link 
              href="/auth/login" 
              className="font-medium text-zinc-950 dark:text-zinc-50 hover:underline underline-offset-4"
            >
              Log in here
            </Link>
          </div>
        </CardFooter>
      </form>
    </Card>
  );
}
