import { ChevronLeft, ChevronRight, User } from 'lucide-react';
import Link from 'next/link';

export function Topbar() {
    return (
        <div className="h-16 flex items-center justify-between px-6 bg-zinc-900/90 backdrop-blur-sm sticky top-0 z-10 w-full">
            <div className="flex items-center gap-4">
                <button className="bg-black/40 rounded-full p-2 text-zinc-400 cursor-not-allowed hidden md:block">
                    <ChevronLeft className="w-5 h-5" />
                </button>
                <button className="bg-black/40 rounded-full p-2 text-zinc-400 cursor-not-allowed hidden md:block">
                    <ChevronRight className="w-5 h-5" />
                </button>
            </div>

            <div className="flex items-center gap-4">
                <Link href="/signup" className="text-zinc-400 hover:text-white font-bold text-sm tracking-wide">
                    Sign up
                </Link>
                <Link href="/login" className="bg-white text-black font-bold py-3 px-8 rounded-full hover:scale-105 transition">
                    Log in
                </Link>
            </div>
        </div>
    );
}
