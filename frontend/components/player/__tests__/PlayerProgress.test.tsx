import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { PlayerProgress } from "@/components/player/PlayerProgress";

describe("PlayerProgress", () => {
  it("renders current and total time labels", () => {
    render(
      <PlayerProgress progress={50} currentTime="1:52" totalTime="3:45" />
    );
    expect(screen.getByText("1:52")).toBeInTheDocument();
    expect(screen.getByText("3:45")).toBeInTheDocument();
  });

  it("defaults to 0:00 when times are not provided", () => {
    render(<PlayerProgress progress={0} />);
    // Cả thời gian hiện tại  và tổng đều mặc định "0:00" → 2 phần tử.
    expect(screen.getAllByText("0:00")).toHaveLength(2);
  });

  it("exposes an accessible slider for track duration", () => {
    render(<PlayerProgress progress={30} />);
    expect(
      screen.getByRole("slider", { name: "Song duration" })
    ).toBeInTheDocument();
  });
});