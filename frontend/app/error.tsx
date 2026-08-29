"use client";

import { useEffect } from "react";
import { Button } from "@/components/ui/button";
import { AlertCircle } from "lucide-react";

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="flex flex-col items-center justify-center min-h-[400px] h-full space-y-6 p-8 text-center">
      <div className="rounded-full bg-destructive/10 p-4">
        <AlertCircle className="w-12 h-12 text-destructive" />
      </div>
      <div className="space-y-2">
        <h1 className="text-3xl font-bold tracking-tight">Something went wrong!</h1>
        <p className="text-muted-foreground max-w-[400px]">
          We&apos;re sorry about this. Please try again or reload the page.
        </p>
      </div>
      <div className="flex gap-4">
        <Button onClick={() => reset()} variant="default" size="lg" className="rounded-full px-8">
          Try again
        </Button>
        <Button onClick={() => window.location.reload()} variant="outline" size="lg" className="rounded-full px-8">
          Reload page
        </Button>
      </div>
    </div>
  );
}
