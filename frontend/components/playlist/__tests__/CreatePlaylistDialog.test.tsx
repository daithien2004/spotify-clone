import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { CreatePlaylistDialog } from "@/components/playlist/CreatePlaylistDialog";

// === Mock dependency ngoại vi — chỉ tập trung test logic + render của dialog ===
const createMock = { mutate: vi.fn(), isPending: false, data: null, error: null };

vi.mock("@/hooks/useCreatePlaylist", () => ({
  useCreatePlaylist: () => createMock,
}));

vi.mock("sonner", () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

describe("CreatePlaylistDialog", () => {
  beforeEach(() => {
    createMock.mutate.mockReset();
    createMock.mutate.mockImplementation((_input: unknown, opts?: { onSuccess?: () => void }) => {
      opts?.onSuccess?.();
    });
    createMock.isPending = false;
    createMock.data = null;
  });

  it("mở dialog khi click trigger 'Create Playlist' (withLabel)", () => {
    render(<CreatePlaylistDialog withLabel />);

    fireEvent.click(screen.getByRole("button", { name: /Create Playlist/i }));

    expect(screen.getByLabelText("Name")).toBeInTheDocument();
    expect(screen.getByText("Create playlist")).toBeInTheDocument();
  });

  it("submit có title → gọi useCreatePlaylist().mutate với {title}", () => {
    render(<CreatePlaylistDialog withLabel />);
    fireEvent.click(screen.getByRole("button", { name: /Create Playlist/i }));

    fireEvent.change(screen.getByLabelText("Name"), {
      target: { value: "Chill Mix" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Create" }));

    expect(createMock.mutate).toHaveBeenCalledWith(
      { title: "Chill Mix" },
      expect.any(Object)
    );
  });

  it("không gọi mutate khi title trống/space", () => {
    render(<CreatePlaylistDialog withLabel />);
    fireEvent.click(screen.getByRole("button", { name: /Create Playlist/i }));

    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "   " } });
    const submit = screen.getByRole("button", { name: "Create" });
    expect(submit).toBeDisabled();
    fireEvent.click(submit);

    expect(createMock.mutate).not.toHaveBeenCalled();
  });
});
