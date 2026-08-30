import { describe, it, expect, beforeEach, vi } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { SearchBar } from "@/components/search/SearchBar";
import { usePlayerStore } from "@/hooks/usePlayerStore";
import { SearchApiService } from "@/services/api/searchService";

vi.mock("@/services/api/searchService", () => ({
  SearchApiService: { search: vi.fn() },
}));

const TRACKS = [
  { id: "t1", title: "Free Spirit", artist: "Khalid", album: "Free Spirit (Explicit)", durationMs: 182000 },
  { id: "t2", title: "Ocean Front Apt.", artist: "ayokay", album: "Digital Dreamscape", durationMs: 132000, artworkUrl: "/figma/chill-mix.png" },
];

function mockApi(query: string) {
  const q = query.trim().toLowerCase();
  const hits = TRACKS.filter(
    (t) =>
      t.title.toLowerCase().includes(q) ||
      t.artist.toLowerCase().includes(q) ||
      t.album.toLowerCase().includes(q)
  );
  vi.mocked(SearchApiService.search).mockImplementation(async () =>
    query.trim() ? hits : []
  );
}

function renderSearchBar() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  render(
    <QueryClientProvider client={queryClient}>
      <SearchBar />
    </QueryClientProvider>
  );
}

function resetPlayer() {
  localStorage.clear();
  usePlayerStore.setState({
    isPlaying: false,
    currentTrack: null,
    volume: 0.7,
    progress: 0,
    queue: [],
    queueIndex: -1,
  });
}

describe("SearchBar", () => {
  beforeEach(() => {
    resetPlayer();
    vi.mocked(SearchApiService.search).mockReset();
  });

  it("renders an accessible combobox with expanded state", () => {
    renderSearchBar();
    const input = screen.getByRole("combobox");
    expect(input).toBeInTheDocument();
    expect(input).toHaveAttribute("aria-expanded", "false");
  });

  it("queries the API and shows suggestions + song results while typing", async () => {
    mockApi("free");
    renderSearchBar();
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "free" } });

    await waitFor(() => {
      expect(SearchApiService.search).toHaveBeenCalledWith("free");
    });
    expect(screen.getByLabelText("Tìm kiếm Free Spirit")).toBeInTheDocument();
    expect(
      screen.getByLabelText("Phát bài hát Free Spirit — Khalid")
    ).toBeInTheDocument();
  });

  it("does not fire a request for a blank query", async () => {
    renderSearchBar();
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "   " } });
    await waitFor(() => {
      expect(screen.getByRole("combobox")).toHaveAttribute("aria-expanded", "false");
    });
    expect(SearchApiService.search).not.toHaveBeenCalled();
  });

  it("selecting a song plays it and closes the dropdown", async () => {
    mockApi("ocean");
    renderSearchBar();
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "ocean" } });

    await waitFor(() => {
      expect(screen.getByLabelText("Phát bài hát Ocean Front Apt. — ayokay")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByLabelText("Phát bài hát Ocean Front Apt. — ayokay"));

    expect(usePlayerStore.getState().isPlaying).toBe(true);
    expect(usePlayerStore.getState().currentTrack?.title).toBe("Ocean Front Apt.");
    expect(usePlayerStore.getState().currentTrack?.imageUrl).toBe("/figma/chill-mix.png");
    expect(screen.getByRole("combobox")).toHaveAttribute("aria-expanded", "false");
  });

  it("shows no-results state when the API returns nothing", async () => {
    vi.mocked(SearchApiService.search).mockResolvedValue([]);
    renderSearchBar();
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "zzzz" } });

    await waitFor(() => {
      expect(screen.getByText(/No results for/i)).toBeInTheDocument();
    });
  });

  it("closes on Escape", async () => {
    mockApi("free");
    renderSearchBar();
    const input = screen.getByRole("combobox");
    fireEvent.change(input, { target: { value: "free" } });
    await waitFor(() => {
      expect(screen.getByLabelText("Tìm kiếm Free Spirit")).toBeInTheDocument();
    });
    fireEvent.keyDown(input, { key: "Escape" });
    expect(input).toHaveAttribute("aria-expanded", "false");
  });

  it("closes when clicking outside", async () => {
    mockApi("free");
    renderSearchBar();
    const input = screen.getByRole("combobox");
    fireEvent.change(input, { target: { value: "free" } });
    await waitFor(() => {
      expect(screen.getByLabelText("Tìm kiếm Free Spirit")).toBeInTheDocument();
    });
    fireEvent.mouseDown(document.body);
    expect(input).toHaveAttribute("aria-expanded", "false");
  });
});