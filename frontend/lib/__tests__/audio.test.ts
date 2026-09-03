import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { readAudioDurationMs } from "@/lib/audio";

describe("readAudioDurationMs", () => {
  const originalAudio = globalThis.Audio;
  let listeners: Record<string, (ev?: unknown) => void>;
  let duration: number;

  /** Audio giả không load thật — test điều khiển khi sự kiện metadata/error fire. */
  function installFakeAudio() {
    listeners = {};
    duration = NaN;

    const FakeAudio = vi.fn(function (this: {
      duration: number;
      preload: string;
      src: string;
      addEventListener: (name: string, cb: () => void) => void;
      removeEventListener: () => void;
    }) {
      // duration là getter live theo biến `duration` — test set qua setDuration().
      Object.defineProperty(this, "duration", {
        get: () => duration,
        configurable: true,
      });
      this.preload = "";
      this.src = "";
      this.addEventListener = (name, cb) => {
        listeners[name] = cb;
      };
      this.removeEventListener = () => {};
    }) as unknown as typeof Audio;

    globalThis.Audio = FakeAudio;
  }

  function setDuration(sec: number) {
    duration = sec;
    listeners["loadedmetadata"]?.();
  }

  function fail() {
    listeners["error"]?.();
  }

  beforeEach(() => {
    URL.createObjectURL = vi.fn(() => "blob:mock-audio");
    URL.revokeObjectURL = vi.fn();
    installFakeAudio();
  });

  afterEach(() => {
    globalThis.Audio = originalAudio;
  });

  it("resolves durationMs khi metadata sẵn sàng (giây → ms, làm tròn)", async () => {
    const prom = readAudioDurationMs(new File([], "song.mp3"));
    setDuration(3.5); // loadedmetadata với duration = 3.5s

    await expect(prom).resolves.toBe(3500);
  });

  it("rejects khi không parse được duration (NaN)", async () => {
    const prom = readAudioDurationMs(new File([], "broken.mp3"));
    setDuration(NaN); // metadata fire nhưng duration không hợp lệ

    await expect(prom).rejects.toThrow(/duration/i);
  });

  it("rejects khi media lỗi (không load được)", async () => {
    const prom = readAudioDurationMs(new File([], "corrupt.mp3"));
    fail(); // media error

    await expect(prom).rejects.toThrow(/load/i);
  });

  it("giải phóng object URL đã tạo", async () => {
    const prom = readAudioDurationMs(new File([], "song.mp3"));
    setDuration(10);

    await prom;
    expect(URL.revokeObjectURL).toHaveBeenCalledWith("blob:mock-audio");
    expect(URL.createObjectURL).toHaveBeenCalledWith(expect.any(File));
  });
});
