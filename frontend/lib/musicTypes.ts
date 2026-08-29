/** Music domain types shared across Home / Search / Playlist screens. */

export interface TrackItem {
  id: string;
  title: string;
  artist: string;
  album: string;
  /** duration in seconds */
  durationSec: number;
  /** e.g. "Mar 3, 2026" */
  dateAdded: string;
  coverUrl?: string;
}

export interface Playlist {
  id: string;
  title: string;
  type: string; // "PUBLIC PLAYLIST" | "ALBUM" | "ARTIST"
  description: string;
  owner: string;
  coverUrl: string;
  /** tailwind gradient classes for the header backdrop */
  gradient: string;
  songs: TrackItem[];
  totalSongs: number; // Figma shows "34 songs" — may exceed rendered rows
  totalDurationLabel: string; // "2hr 01 min"
}