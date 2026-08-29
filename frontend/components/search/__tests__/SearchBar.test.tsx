import { describe, it, expect, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { SearchBar } from "@/components/search/SearchBar";
import { usePlayerStore } from "@/hooks/usePlayerStore";

function typeInSearch(value: string) {
  const input = screen.getByRole("combobox");
  fireEvent.change(input, { target: { value } });
  return input;
}

function resetPlayer() {
  localStorage.clear();
  usePlayerStore.setState({
    isPlaying: false,
    currentTrack: null,
    volume: 0.7,
    progress: 0,
  });
}

describe("SearchBar", () => {
  beforeEach(() => {
    resetPlayer();
  });

  it("renders an accessible combobox with expanded state", () => {
    render(<SearchBar />);
    const input = screen.getByRole("combobox");
    expect(input).toBeInTheDocument();
    expect(input).toHaveAttribute("aria-expanded", "false");
  });

  it("shows suggestions and song results when typing", () => {
    render(<SearchBar />);
    typeInSearch("ocean");
    expect(screen.getByLabelText("Tìm kiếm Ocean Front Apt.")).toBeInTheDocument();
    expect(
      screen.getByLabelText("Phát bài hát Ocean Front Apt. — ayokay")
    ).toBeInTheDocument();
    expect(screen.getByRole("combobox")).toHaveAttribute("aria-expanded", "true");
  });

  it("picking a suggestion fills the query and keeps results open", () => {
    render(<SearchBar />);
    typeInSearch("khalid");
    fireEvent.click(screen.getByLabelText("Tìm kiếm Khalid"));

    const input = screen.getByRole("combobox") as HTMLInputElement;
    expect(input.value).toBe("Khalid");
    expect(input).toHaveAttribute("aria-expanded", "true");
  });

  it("selecting a song plays it and closes the dropdown", () => {
    render(<SearchBar />);
    typeInSearch("ocean");
    fireEvent.click(screen.getByLabelText("Phát bài hát Ocean Front Apt. — ayokay"));

    expect(usePlayerStore.getState().isPlaying).toBe(true);
    expect(usePlayerStore.getState().currentTrack?.title).toBe("Ocean Front Apt.");
    expect(screen.getByRole("combobox")).toHaveAttribute("aria-expanded", "false");
  });

  it("shows no-results state when nothing matches", () => {
    render(<SearchBar />);
    typeInSearch("zzzz-no-match");
    expect(screen.getByText(/no results/i)).toBeInTheDocument();
  });

  it("closes on Escape", () => {
    render(<SearchBar />);
    const input = typeInSearch("free");
    fireEvent.keyDown(input, { key: "Escape" });
    expect(input).toHaveAttribute("aria-expanded", "false");
  });

  it("closes when clicking outside", () => {
    render(<SearchBar />);
    typeInSearch("free");
    fireEvent.mouseDown(document.body);
    expect(screen.getByRole("combobox")).toHaveAttribute("aria-expanded", "false");
  });

  it("hides results when query is cleared", () => {
    render(<SearchBar />);
    const input = typeInSearch("free");
    fireEvent.change(input, { target: { value: "" } });
    expect(screen.queryByLabelText("Tìm kiếm Free Spirit")).not.toBeInTheDocument();
  });
});