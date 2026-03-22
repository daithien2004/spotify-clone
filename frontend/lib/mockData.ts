export interface SidebarItem {
  id: string;
  name: string;
  type: "Artist" | "Playlist" | "Album";
  imageUrl: string;
  subtitle?: string;
}

export interface MusicCard {
  id: string;
  title: string;
  description: string;
  imageUrl: string;
  type: "Playlist" | "Album" | "Artist";
}

export const LIBRARY_ITEMS: SidebarItem[] = [
  {
    id: "1",
    name: "Dangrangto",
    type: "Artist",
    imageUrl: "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?auto=format&fit=crop&w=500&q=80",
    subtitle: "Nghệ sĩ",
  },
  {
    id: "2",
    name: "Wren Evans",
    type: "Artist",
    imageUrl: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=500&q=80",
    subtitle: "Nghệ sĩ",
  },
  {
    id: "3",
    name: "RPT MCK",
    type: "Artist",
    imageUrl: "https://images.unsplash.com/photo-1496293455970-f8581aae0e3c?auto=format&fit=crop&w=500&q=80",
    subtitle: "Nghệ sĩ",
  },
];

export const DAILY_MIXES: MusicCard[] = [
  {
    id: "dm1",
    title: "Daily Mix 1",
    description: "Ngọt, ANH TRAI \"SAY HI\", Wren Evans và...",
    imageUrl: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=500&q=80",
    type: "Playlist",
  },
  {
    id: "dm2",
    title: "Daily Mix 2",
    description: "Dangrangto, The Wind, Wheelie và...",
    imageUrl: "https://images.unsplash.com/photo-1493225255756-d9584f8606e9?auto=format&fit=crop&w=500&q=80",
    type: "Playlist",
  },
  {
    id: "dm3",
    title: "Mời Ra Lò",
    description: "Nhún nhảy theo những giai điệu mới...",
    imageUrl: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=500&q=80",
    type: "Playlist",
  },
];

export const RECOMMENDED_FOR_TODAY: MusicCard[] = [
  {
    id: "rec1",
    title: "KHÔNG THỂ",
    description: "Được đề xuất cho hôm nay",
    imageUrl: "https://images.unsplash.com/photo-1496293455970-f8581aae0e3c?auto=format&fit=crop&w=500&q=80",
    type: "Album",
  },
  {
    id: "rec2",
    title: "Tâm sự",
    description: "Được đề xuất cho hôm nay",
    imageUrl: "https://images.unsplash.com/photo-1514525253361-b83f8096771c?auto=format&fit=crop&w=500&q=80",
    type: "Artist",
  },
  {
    id: "rec3",
    title: "Hẹn gặp lại",
    description: "Được đề xuất cho hôm nay",
    imageUrl: "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?auto=format&fit=crop&w=500&q=80",
    type: "Playlist",
  },
];
