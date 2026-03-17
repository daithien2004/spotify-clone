import { Home, Search, Library, PlusSquare, Heart } from 'lucide-react';
import Link from 'next/link';

export function Sidebar() {
    return (
        <div className="w-64 bg-black h-full flex flex-col hidden md:flex">
            {/* Top Nav */}
            <div className="p-6">
                <div className="text-white font-bold text-2xl flex items-center gap-2 mb-8">
                    <div className="w-8 h-8 bg-green-500 rounded-full flex items-center justify-center">
                        <span className="text-black text-sm">S</span>
                    </div>
                    Spotify
                </div>

                <div className="flex flex-col gap-4 text-zinc-400 font-semibold">
                    <Link href="/" className="flex items-center gap-4 hover:text-white transition group">
                        <Home className="w-6 h-6" />
                        Home
                    </Link>
                    <Link href="/search" className="flex items-center gap-4 hover:text-white transition">
                        <Search className="w-6 h-6" />
                        Search
                    </Link>
                    <Link href="/library" className="flex items-center gap-4 hover:text-white transition">
                        <Library className="w-6 h-6" />
                        Your Library
                    </Link>
                </div>
            </div>

            {/* Playlists / Actions */}
            <div className="px-6 pt-2 pb-6">
                <div className="flex flex-col gap-4 text-zinc-400 font-semibold">
                    <button className="flex items-center gap-4 hover:text-white transition w-full text-left">
                        <div className="bg-zinc-300 text-black p-1 rounded-sm">
                            <PlusSquare className="w-5 h-5" />
                        </div>
                        Create Playlist
                    </button>
                    <button className="flex items-center gap-4 hover:text-white transition w-full text-left">
                        <div className="bg-gradient-to-br from-indigo-500 to-indigo-300 text-white p-1 rounded-sm">
                            <Heart className="w-5 h-5 fill-current" />
                        </div>
                        Liked Songs
                    </button>
                </div>
            </div>

            <div className="border-t border-zinc-800 mx-6"></div>

            {/* Playlist List dummy */}
            <div className="flex-1 overflow-y-auto p-6 flex flex-col gap-3 text-zinc-400 text-sm">
                <p className="hover:text-white cursor-pointer truncate">Chill Vibes</p>
                <p className="hover:text-white cursor-pointer truncate">Top Hits 2024</p>
                <p className="hover:text-white cursor-pointer truncate">Coding Focus</p>
            </div>
        </div>
    );
}
