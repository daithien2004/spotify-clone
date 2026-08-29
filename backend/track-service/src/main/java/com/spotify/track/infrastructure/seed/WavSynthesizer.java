package com.spotify.track.infrastructure.seed;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Generates a tiny WAV (PCM 16-bit mono @ 22050 Hz) so dev playback has real
 * audio bytes without shipping media files. Pure JDK — used only by the seed
 * initializer; no streaming data ever depends on it in production.
 */
final class WavSynthesizer {

    static final int SAMPLE_RATE = 22050;

    private WavSynthesizer() {
    }

    static byte[] melody(int seed, int durationSeconds) {
        int samples = SAMPLE_RATE * durationSeconds;
        ByteArrayOutputStream body = new ByteArrayOutputStream(samples * 2);
        int[] notes = { 261, 293, 329, 349, 392, 440, 494, 523 }; // C major scale

        for (int i = 0; i < samples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double freq = notes[(i / (SAMPLE_RATE / 4) + seed) % notes.length];
            // Two-oscillator voice with slow decay → sounds gentle, not harsh
            double env = 1.0 - 0.25 * ((i / (SAMPLE_RATE / 4)) % 4) / 4.0;
            double value = 0.6 * Math.sin(2 * Math.PI * freq * t) * env
                         + 0.3 * Math.sin(2 * Math.PI * freq * 1.5 * t) * env;
            short sample = (short) (value * Short.MAX_VALUE);
            body.write(sample & 0xff);
            body.write((sample >> 8) & 0xff);
        }
        return wrapWav(body.toByteArray());
    }

    private static byte[] wrapWav(byte[] pcm) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int byteRate = SAMPLE_RATE * 2; // 16-bit mono → 2 bytes/sample
            writeAscii(out, "RIFF");
            writeIntLe(out, 36 + pcm.length);
            writeAscii(out, "WAVE");
            writeAscii(out, "fmt ");
            writeIntLe(out, 16);
            writeShortLe(out, 1);       // PCM
            writeShortLe(out, 1);       // mono
            writeIntLe(out, SAMPLE_RATE);
            writeIntLe(out, byteRate);
            writeShortLe(out, 2);       // block align
            writeShortLe(out, 16);      // bits per sample
            writeAscii(out, "data");
            writeIntLe(out, pcm.length);
            out.write(pcm);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("WAV synthesis failed", e);
        }
    }

    private static void writeAscii(ByteArrayOutputStream out, String s) {
        for (byte b : s.getBytes(java.nio.charset.StandardCharsets.US_ASCII)) {
            out.write(b);
        }
    }

    private static void writeIntLe(ByteArrayOutputStream out, int v) {
        out.write(v & 0xff);
        out.write((v >> 8) & 0xff);
        out.write((v >> 16) & 0xff);
        out.write((v >> 24) & 0xff);
    }

    private static void writeShortLe(ByteArrayOutputStream out, int v) {
        out.write(v & 0xff);
        out.write((v >> 8) & 0xff);
    }
}