export const queryKeys = {
  auth: {
    all: ["auth"] as const,
    user: () => [...queryKeys.auth.all, "user"] as const,
  },
  tracks: {
    all: ["tracks"] as const,
    detail: (id: string) => [...queryKeys.tracks.all, id] as const,
    list: (params?: Record<string, unknown>) => [...queryKeys.tracks.all, "list", params] as const,
  },
  playlists: {
    all: ["playlists"] as const,
    detail: (id: string) => [...queryKeys.playlists.all, id] as const,
    tracks: (id: string) => [...queryKeys.playlists.all, id, "tracks"] as const,
    list: () => [...queryKeys.playlists.all, "list"] as const,
  },
  home: {
    all: ["home"] as const,
    sections: () => [...queryKeys.home.all, "sections"] as const,
  },
  search: {
    all: ["search"] as const,
    tracks: (q: string) => [...queryKeys.search.all, "tracks", q] as const,
  },
} as const;
