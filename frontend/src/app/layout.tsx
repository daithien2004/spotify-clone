import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';
import { Sidebar } from '@/components/Sidebar';
import { Topbar } from '@/components/Topbar';

const inter = Inter({ subsets: ['latin'] });

export const metadata: Metadata = {
  title: 'Spotify Clone',
  description: 'Built with Next.js, Java Spring Boot, and Tailwind CSS',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className={`${inter.className} h-screen flex overflow-hidden bg-black text-white`}>
        <Sidebar />
        <div className="flex-1 flex flex-col bg-zinc-900 overflow-hidden relative">
          <Topbar />
          <main className="flex-1 overflow-y-auto w-full h-full relative p-6">
            <div className="absolute inset-0 bg-gradient-to-b from-indigo-900/30 to-zinc-900 pointer-events-none -z-10" />
            {children}
          </main>
        </div>
      </body>
    </html>
  );
}
