/**
 * Đọc duration của file audio phía client trước khi upload.
 *
 * Dùng <audio> element (preload=metadata) thay vì AudioContext.decodeAudioData vì:
 * - <audio> không cần giữ context/giải phóng tài nguyên, auto gọn với lifecycle element.
 * - Hỗ trợ mọi codec trình duyệt play được mà không cần library thêm (ràng buộc dự án).
 * Backend POST /tracks bắt buộc durationMs — đây là nguồn cho field đó.
 */
export function readAudioDurationMs(file: File): Promise<number> {
  return new Promise((resolve, reject) => {
    const objectUrl = URL.createObjectURL(file);
    const audio = new Audio();
    audio.preload = "metadata";

    const cleanup = () => {
      // objectURL giữ blob trong memory — phải revoke để tránh leak.
      URL.revokeObjectURL(objectUrl);
      audio.removeEventListener("loadedmetadata", onLoaded);
      audio.removeEventListener("error", onError);
    };

    const onLoaded = () => {
      const ms = Math.round(audio.duration * 1000);
      if (Number.isNaN(ms)) {
        cleanup();
        reject(new Error("Could not read audio duration from this file."));
        return;
      }
      cleanup();
      resolve(ms);
    };

    const onError = () => {
      cleanup();
      reject(new Error("Could not load the audio file."));
    };

    // Đăng ký listener TRƯỚC khi set src — tránh bỏ lỡ sự kiện nếu metadata tải tức thì.
    audio.addEventListener("loadedmetadata", onLoaded);
    audio.addEventListener("error", onError);
    audio.src = objectUrl;
  });
}
