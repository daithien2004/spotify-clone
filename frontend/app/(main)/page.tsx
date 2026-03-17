import { Play } from 'lucide-react';

export default function Home() {
  const greeting = getGreeting();

  const dummyPlaylists = [
    { title: 'Liked Songs', color: 'from-indigo-600 to-indigo-400' },
    { title: 'Daily Mix 1', color: 'from-green-600 to-green-400' },
    { title: 'Discover Weekly', color: 'from-blue-600 to-blue-400' },
    { title: 'Top 50 - Global', color: 'from-purple-600 to-purple-400' },
    { title: 'Coding Focus', color: 'from-teal-600 to-teal-400' },
    { title: 'Chill Vibes', color: 'from-orange-600 to-orange-400' },
  ];

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-3xl font-bold tracking-tight text-white">{greeting}</h1>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {dummyPlaylists.map((playlist, idx) => (
          <div
            key={idx}
            className="group flex items-center gap-4 bg-zinc-800/50 hover:bg-zinc-700/50 transition duration-300 rounded-md overflow-hidden cursor-pointer relative"
          >
            <div className={`w-20 h-20 bg-gradient-to-br ${playlist.color} flex-shrink-0 shadow-lg`} />
            <div className="font-bold text-white flex-1 pr-14 truncate">
              {playlist.title}
            </div>
            {/* Play Button */}
            <button className="w-12 h-12 flex items-center justify-center rounded-full bg-green-500 text-black absolute right-4 opacity-0 group-hover:opacity-100 transition shadow-xl drop-shadow-md pb-0.5 pl-0.5 hover:scale-105">
              <Play className="w-6 h-6 fill-current" />
            </button>
          </div>
        ))}
      </div>

      <div className="mt-8">
        <h2 className="text-2xl font-bold text-white mb-6 hover:underline cursor-pointer">
          Made for you
        </h2>
        {/* Horizontal scrollable or grid items for albums */}
        <div className="flex gap-6 overflow-x-auto pb-4">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div
              key={i}
              className="bg-zinc-800/30 hover:bg-zinc-800 p-4 rounded-xl flex flex-col gap-4 cursor-pointer transition min-w-[180px] group"
            >
              <div className="w-full aspect-square bg-zinc-700 rounded-md relative shadow-md">
                <button className="w-12 h-12 flex items-center justify-center rounded-full bg-green-500 text-black absolute bottom-2 right-2 opacity-0 group-hover:opacity-100 transition shadow-xl pb-0.5 pl-0.5 hover:scale-105 z-10 translate-y-2 group-hover:translate-y-0">
                  <Play className="w-6 h-6 fill-current" />
                </button>
              </div>
              <div className="flex flex-col gap-1">
                <h3 className="font-bold text-white truncate">Daily Mix {i}</h3>
                <p className="text-sm text-zinc-400 line-clamp-2">Artists {i}, Band {i}, Singer {i} and more</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function getGreeting() {
  const currentHour = new Date().getHours();
  if (currentHour < 12) return 'Good morning';
  if (currentHour < 18) return 'Good afternoon';
  return 'Good evening';
}
