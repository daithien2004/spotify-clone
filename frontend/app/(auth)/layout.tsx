import React from "react";

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-start bg-black text-white px-4 pt-8 md:pt-12">
      <div className="w-full max-w-[734px] flex flex-col items-center">
        {/* Spotify Logo Header */}
        <div className="mb-8 md:mb-12">
          <svg viewBox="0 0 167.5 167.5" className="h-[40px] w-[40px] text-white fill-current">
            <path d="M83.7 0C37.5 0 0 37.5 0 83.7c0 46.3 37.5 83.7 83.7 83.7 46.3 0 83.7-37.5 83.7-83.7C167.5 37.5 130 0 83.7 0zm38.4 120.7c-1.5 2.5-4.8 3.3-7.3 1.8-16-9.8-36.2-12-59.9-6.6-2.8.6-5.7-1.2-6.3-4-.6-2.8 1.2-5.7 4-6.3 26-5.9 48.4-3.4 66.3 7.5 2.5 1.6 3.3 4.8 1.9 7.6zm10.2-22.8c-1.9 3.1-5.9 4-9 2.1-18.3-11.2-46.2-14.5-67.8-7.9-3.5 1.1-7.1-1-8.2-4.5-1.1-3.5 1-7.1 4.5-8.2 25.1-7.6 55.9-3.9 77.2 9.2 3.1 1.9 4 5.9 3.3 9.3zm.9-23.9c-22-13-58.3-14.3-79.5-7.8-3.4 1-6.9-1-7.9-4.4-1-3.4 1-6.9 4.4-7.9 24.1-7.3 64.3-5.7 89.8 9.5 3 1.8 4 5.7 2.2 8.7-1.8 3.1-5.8 4.1-9 1.9z" />
          </svg>
        </div>

        <div className="w-full max-w-[450px] space-y-6">
          {children}
        </div>
      </div>
    </div>
  );
}
