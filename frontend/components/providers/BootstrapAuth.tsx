"use client";

import { useEffect } from "react";
import { useBootstrapAuth } from "@/hooks/useAuth";

export function BootstrapAuth({ children }: { children: React.ReactNode }) {
  const bootstrap = useBootstrapAuth();

  // Revalidate auth state sau hydrate để fix localStorage stale (JWT chết nhưng UI còn user)
  useEffect(() => {
    bootstrap();
  }, [bootstrap]);

  return <>{children}</>;
}