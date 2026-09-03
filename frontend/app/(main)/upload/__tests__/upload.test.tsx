import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import UploadPage from "@/app/(main)/upload/page";
import type { TrackResponse } from "@/lib/adapters";

vi.mock("@/services/api/trackService", () => ({
  TrackService: {
    createTrack: vi.fn(),
    uploadAudio: vi.fn(),
  },
}));

vi.mock("@/lib/audio", () => ({
  readAudioDurationMs: vi.fn().mockResolvedValue(180_000),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

const TRACK: TrackResponse = {
  id: "new-track-1",
  title: "New Track",
  artist: "Artist",
  album: "Album",
  durationMs: 180000,
  artworkUrl: null,
  audioUrl: null,
  createdAt: "2026-09-03T00:00:00Z",
  updatedAt: "2026-09-03T00:00:00Z",
};

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <UploadPage />
    </QueryClientProvider>
  );
}

async function fillValidForm() {
  const file = new File(["data"], "song.mp3", { type: "audio/mpeg" });
  fireEvent.change(screen.getByLabelText(/audio file/i), {
    target: { files: [file] },
  });
  fireEvent.change(screen.getByLabelText(/^title$/i), {
    target: { value: "New Track" },
  });
  fireEvent.change(screen.getByLabelText(/^artist$/i), {
    target: { value: "Artist" },
  });
}

describe("UploadPage", () => {
  beforeEach(() => vi.clearAllMocks());

  it("renders upload form with file picker and required fields", () => {
    renderPage();
    expect(screen.getByRole("heading", { name: /upload a track/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/audio file/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^title$/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^artist$/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/album/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /upload/i })).toBeInTheDocument();
  });

  it("submit button is disabled when no file selected", () => {
    renderPage();
    expect(screen.getByRole("button", { name: /upload/i })).toBeDisabled();
  });

  it("enables submit after selecting file and filling required fields", () => {
    renderPage();
    fillValidForm();
    expect(screen.getByRole("button", { name: /upload/i })).toBeEnabled();
  });

  it("submits metadata create + audio upload in sequence", async () => {
    const { TrackService } = await import("@/services/api/trackService");
    vi.mocked(TrackService.createTrack).mockResolvedValue(TRACK);
    vi.mocked(TrackService.uploadAudio).mockResolvedValue();

    renderPage();
    fillValidForm();
    fireEvent.click(screen.getByRole("button", { name: /upload/i }));

    await waitFor(() => {
      expect(TrackService.createTrack).toHaveBeenCalledWith({
        title: "New Track",
        artist: "Artist",
        album: undefined,
        durationMs: 180000,
      });
    });
    expect(TrackService.uploadAudio).toHaveBeenCalledWith(
      "new-track-1",
      expect.any(File)
    );
  });
});
