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
  },
} as const;
