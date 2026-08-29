/**
 * English demo data — copy from Figma "Spotify Music UI Design (Community)"
 * (file QX9idJpprTfOqirvKN8jzr, screens 124:2941 / 178:3849 / 131:2938 / 325:7536).
 * Durations/artists follow the Figma track list (Julia Wolf, ayokay, Khalid…).
 */
import type { Playlist, TrackItem } from "./musicTypes";

export const NAV_USER = "davedirect3";

/* ---------------- Home sections ---------------- */

interface HomeSection {
  title: string;
  seeAll?: boolean;
  items: Array<{
    id: string;
    title: string;
    description: string;
    imageUrl: string;
    type: string;
    variant?: "track" | "artist";
  }>;
}

export const HOME_SECTIONS: HomeSection[] = [
  {
    title: "Your top mixes",
    seeAll: true,
    items: [
      {
        id: "mix-chill",
        title: "Chill Mix",
        description: "Julia Wolf, Khalid, ayokay and more",
        imageUrl: "/figma/chill-mix.png",
        type: "Playlist",
      },
      {
        id: "mix-pop",
        title: "Pop Mix",
        description: "Hey Violet, VÉRITÉ, Timeflies and more",
        imageUrl: "/figma/pop-mix.png",
        type: "Playlist",
      },
      {
        id: "mix-dm4",
        title: "Daily Mix 4",
        description: "Ayra Starr, Lil Kesh, Ed Sheeran and more",
        imageUrl: "/figma/daily-mix-4.png",
        type: "Playlist",
      },
      {
        id: "mix-dm1",
        title: "Daily Mix 1",
        description: "Ayra Starr, Lil Kesh, Ed Sheeran and more",
        imageUrl: "/figma/daily-mix-4.png",
        type: "Playlist",
      },
      {
        id: "mix-dm5",
        title: "Daily Mix 5",
        description: "FRENSHIP, Brooke Sierra, Julia Wolf and more",
        imageUrl: "/figma/pop-mix.png",
        type: "Playlist",
      },
      {
        id: "mix-folk",
        title: "Folk & Acoustic Mix",
        description: "Canyon City, Crooked Still, Gregory Alan…",
        imageUrl: "/figma/chill-mix.png",
        type: "Playlist",
      },
    ],
  },
  {
    title: "Recently played",
    items: [
      {
        id: "rec-happy",
        title: "Happy Hits!",
        description: "Hits to boost your mood and fill you wi…",
        imageUrl: "/figma/happy-hits.png",
        type: "Playlist",
      },
      {
        id: "rec-anime",
        title: "Anime Lofi & Ch…",
        description: "Experience the best Anime moments aga…",
        imageUrl: "/figma/happy-hits.png",
        type: "Playlist",
      },
      {
        id: "rec-liked",
        title: "Liked Songs",
        description: "607 liked songs",
        imageUrl: "/figma/liked-songs.jpg",
        type: "Playlist",
      },
      {
        id: "rec-lofi",
        title: "Lo-Fi Beats",
        description: "The whole day flows better with beats",
        imageUrl: "/figma/daily-mix-4.png",
        type: "Playlist",
      },
    ],
  },
  {
    title: "Uniquely yours",
    items: [
      {
        id: "uni-daily",
        title: "Daily Mix 2",
        description: "Dangrangto, The Wind and more",
        imageUrl: "/figma/pop-mix.png",
        type: "Playlist",
      },
      {
        id: "uni-daily3",
        title: "Daily Mix 3",
        description: "Ed Sheeran, Khalid and more",
        imageUrl: "/figma/chill-mix.png",
        type: "Playlist",
      },
    ],
  },
  {
    title: "Jump back in",
    items: [
      {
        id: "jb-chainsmokers",
        title: "The Chainsmokers",
        description: "Artist",
        imageUrl: "/figma/recent-search.png",
        type: "Artist",
        variant: "artist",
      },
      {
        id: "jb-sheeran",
        title: "Ed Sheeran",
        description: "Artist",
        imageUrl: "/figma/avatar-davedirect3.png",
        type: "Artist",
        variant: "artist",
      },
    ],
  },
  {
    title: "Made for you",
    items: [
      {
        id: "mf-pop",
        title: "Pop Mix",
        description: "Hey Violet, VÉRITÉ, Timeflies and more",
        imageUrl: "/figma/pop-mix.png",
        type: "Playlist",
      },
      {
        id: "mf-indie",
        title: "Indie Mix",
        description: "Joywave, The xx, The Neighbourhood and…",
        imageUrl: "/figma/daily-mix-4.png",
        type: "Playlist",
      },
    ],
  },
  {
    title: "Just the hits",
    items: [
      {
        id: "jh-no-1",
        title: "Just The Hits",
        description: "The biggest songs of right now",
        imageUrl: "/figma/pop-mix.png",
        type: "Playlist",
      },
    ],
  },
];

/* ---------------- Search ---------------- */

/* ---------------- Playlists ---------------- */

function track(
  id: string,
  title: string,
  artist: string,
  album: string,
  minutes: number,
  seconds: number,
  dateAdded = "Mar 3, 2026",
  coverUrl = ""
): TrackItem {
  return { id, title, artist, album, durationSec: minutes * 60 + seconds, dateAdded, coverUrl };
}

const CHILL_MIX_TRACKS: TrackItem[] = [
  track("c1", "Play It Safe", "Julia Wolf", "Girls In Purgatory (Full of Grace)", 2, 39, "Mar 3, 2026", "/figma/happy-hits.png"),
  track("c2", "In the Shape of a Dream", "ayokay", "In the Shape of a Dream", 2, 12, "Mar 3, 2026"),
  track("c3", "Free Spirit", "Khalid", "Free Spirit (Explicit)", 3, 2, "Mar 2, 2026"),
  track("c4", "Vacation", "Stockholm Black", "Vacation", 4, 25, "Mar 1, 2026"),
  track("c5", "Same Old", "Efraïm Leo", "Same Old", 2, 56, "Feb 28, 2026"),
  track("c6", "A Moment Apart", "ODESZA", "A Moment Apart", 3, 54, "Feb 27, 2026"),
  track("c7", "1993", "Ethan Gruska", "1993", 3, 13, "Feb 26, 2026"),
  track("c8", "Girl, I Know", "NEIL FRANCES", "Girl, I Know", 3, 14, "Feb 25, 2026"),
  track("c9", "Brightest Blue", "Ellie Goulding", "Brightest Blue", 3, 37, "Feb 24, 2026"),
];

const LIKED_TRACKS: TrackItem[] = [
  track("l1", "Here For Ya", "Adekunle Gold", "Here For Ya", 3, 9, "Mar 3, 2026"),
  track("l2", "Pillow", "Julia Wolf", "Girls In Purgatory", 2, 14, "Mar 3, 2026"),
  track("l3", "If I Were You", "Claud", "If I Were You", 3, 12, "Mar 3, 2026"),
  track("l4", "The Other Raver", "The Wldlfe", "The Other Raver", 3, 43, "Mar 3, 2026"),
  track("l5", "Goodpain", "Yoke Lore", "Absence", 3, 41, "Mar 3, 2026"),
  track("l6", "Memories (feat. Bella Shmurda)", "Ayra Starr", "19 & Dangerous", 3, 5, "Mar 3, 2026"),
];

const HAPPY_TRACKS: TrackItem[] = [
  track("h1", "Play It Safe", "Julia Wolf", "Girls In Purgatory (Full of Grace)", 2, 39),
  track("h2", "Ocean Front Apt.", "ayokay", "Digital Dreamscape", 2, 12),
  track("h3", "Free Spirit", "Khalid", "Free Spirit (Explicit)", 3, 2),
  track("h4", "Sunday Best", "Surfaces", "Where the Light Is", 2, 38),
  track("h5", "Circles", "Post Malone", "Hollywood's Bleeding", 3, 35),
];

export const PLAYLISTS: Record<string, Playlist> = {
  "chill-mix": {
    id: "chill-mix",
    title: "Chill Mix",
    type: "PUBLIC PLAYLIST",
    description: "Chill music to help you slow down.",
    owner: NAV_USER,
    coverUrl: "/figma/chill-mix.png",
    gradient: "from-genre-chill via-genre-chill/40",
    songs: CHILL_MIX_TRACKS,
    totalSongs: 34,
    totalDurationLabel: "2hr 01 min",
  },
  "pop-mix": {
    id: "pop-mix",
    title: "Pop Mix",
    type: "PUBLIC PLAYLIST",
    description: "All your favorite pop tracks, mixed for you.",
    owner: NAV_USER,
    coverUrl: "/figma/pop-mix.png",
    gradient: "from-genre-pop via-genre-pop/40",
    songs: CHILL_MIX_TRACKS.slice(0, 4),
    totalSongs: 34,
    totalDurationLabel: "2hr 01 min",
  },
  "happy-hits": {
    id: "happy-hits",
    title: "Happy Hits!",
    type: "PUBLIC PLAYLIST",
    description: "Hits to boost your mood and fill you with happiness!",
    owner: NAV_USER,
    coverUrl: "/figma/happy-hits.png",
    gradient: "from-genre-party via-genre-party/40",
    songs: HAPPY_TRACKS,
    totalSongs: 50,
    totalDurationLabel: "2hr 44 min",
  },
  "anime-lofi": {
    id: "anime-lofi",
    title: "Anime Lofi & Chill",
    type: "PUBLIC PLAYLIST",
    description: "Experience the best Anime moments again through Lofi.",
    owner: NAV_USER,
    coverUrl: "/figma/happy-hits.png",
    gradient: "from-genre-mood via-genre-mood/40",
    songs: CHILL_MIX_TRACKS.slice(0, 5),
    totalSongs: 80,
    totalDurationLabel: "4hr 12 min",
  },
  "liked-songs": {
    id: "liked-songs",
    title: "Liked Songs",
    type: "PLAYLIST",
    description: `${NAV_USER} · ${LIKED_TRACKS.length} liked songs`,
    owner: NAV_USER,
    coverUrl: "/figma/liked-songs.jpg",
    gradient: "from-genre-soul via-genre-soul/40",
    songs: LIKED_TRACKS,
    totalSongs: 607,
    totalDurationLabel: "1 day 2 hr",
  },
};

export const PLAYLIST_IDS = Object.keys(PLAYLISTS);

/* ---------------- Search index ---------------- */

/**
 * Track index cho Search — dedup track trùng id giữa các playlist.
 * Khi backend Search-service có: thay bằng api call /api/v1/search.
 */
export const TRACK_INDEX: TrackItem[] = Object.values(PLAYLISTS)
  .flatMap((p) => p.songs)
  .filter((song, i, songs) => songs.findIndex((s) => s.id === song.id) === i);