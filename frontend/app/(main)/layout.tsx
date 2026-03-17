import { Sidebar } from '@/components/Sidebar';
import { Topbar } from '@/components/Topbar';

export default function MainLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="h-screen flex overflow-hidden bg-black text-white">
      <Sidebar />
      <div className="flex-1 flex flex-col bg-zinc-900 overflow-hidden relative">
        <Topbar />
        <main className="flex-1 overflow-y-auto w-full h-full relative p-6">
          <div className="absolute inset-0 bg-gradient-to-b from-indigo-900/30 to-zinc-900 pointer-events-none -z-10" />
          {children}
        </main>
      </div>
    </div>
  );
}
