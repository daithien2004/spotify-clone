import React from "react";

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-zinc-50 dark:bg-black px-4">
      <div className="w-full max-w-md space-y-8 animate-in fade-in duration-700">
        <div className="flex flex-col items-center justify-center">
            {/* You can add a logo here */}
            {/* <div className="h-12 w-12 bg-green-500 rounded-full mb-4 flex items-center justify-center"> */}
                {/* Spotify logo would go here */}
            {/* </div> */}
        </div>
        {children}
      </div>
    </div>
  );
}
