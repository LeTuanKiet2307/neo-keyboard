#!/usr/bin/env python3
"""Tổng hợp âm switch "linear" bằng toán học — không dùng sample/bản quyền bên thứ ba.

Linear (kiểu Gateron Yellow/Cream đã lube): không có click, không tactile bump. Âm chỉ đến từ
lúc đáy chạm (bottom-out) và lúc thả lên (top-out). Mục tiêu "dễ chịu":
- Press: tiếng "thock" tròn, trầm ~280-320Hz (loa điện thoại không phát nổi <250Hz), tắt nhanh; có một chút cộng hưởng nhựa ~1kHz rất
  nhỏ cho cảm giác "đặc". Transient được làm mềm (fade-in 2.5ms + nhiễu qua 2 tầng low-pass)
  để không có cạnh sắc gây "tạch".
- Release: cùng chất liệu nhưng nhẹ hơn nhiều (peak 0.32), cao hơn chút ~470Hz, rất ngắn.
- Toàn bộ qua low-pass 2 tầng ~3kHz để cắt sạch dải 3-8kHz (dải gây chói tai).
3 biến thể pitch mỗi loại để tránh lặp. Xuất WAV 44.1kHz mono 16-bit vào res/raw.
Chạy: python3 tools/sounds/build_cream_sounds.py
"""
from pathlib import Path
import math
import random
import struct
import wave

RATE = 44100
TARGET = Path(__file__).resolve().parents[2] / "android/app/src/main/res/raw"
TAU = 2 * math.pi


def onepole(samples, cutoff_hz):
    """Low-pass one-pole theo tần số cắt."""
    alpha = 1.0 - math.exp(-TAU * cutoff_hz / RATE)
    out, lp = [], 0.0
    for s in samples:
        lp += alpha * (s - lp)
        out.append(lp)
    return out


def lowpass(samples, cutoff_hz, stages=2):
    for _ in range(stages):
        samples = onepole(samples, cutoff_hz)
    return samples


def noise(n, seed):
    rng = random.Random(seed)
    return [rng.uniform(-1.0, 1.0) for _ in range(n)]


def normalize(samples, peak):
    m = max((abs(s) for s in samples), default=0.0)
    if m <= 0:
        return samples
    g = peak / m
    return [max(-1.0, min(1.0, s * g)) for s in samples]


def write_wav(name, samples):
    TARGET.mkdir(parents=True, exist_ok=True)
    path = TARGET / (name + ".wav")
    frames = b"".join(struct.pack("<h", round(s * 32767)) for s in samples)
    with wave.open(str(path), "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        out.writeframes(frames)
    print(f"  {path.name}: {len(samples)} mẫu, {path.stat().st_size} byte")


def smooth_env(t, attack, decay, dur, tail=0.012):
    """Fade-in dạng sin² (không có cạnh) + decay mũ + fade-out cuối."""
    a = 1.0 if t >= attack else math.sin(0.5 * math.pi * t / attack) ** 2
    end = min(1.0, max(0.0, (dur - t) / tail))
    return a * math.exp(-t / decay) * end


def press(f0, seed, dur=0.075):
    n = round(RATE * dur)
    # Nhiễu "chạm đáy": low-pass sâu (~900Hz) → tiếng bụp mềm, không xì.
    hit = lowpass(noise(n, seed), 1400, stages=3)
    hit = normalize(hit, 1.0)
    out = []
    for i in range(n):
        t = i / RATE
        body = math.sin(TAU * f0 * t) + 0.35 * math.sin(TAU * f0 * 2.02 * t + 0.4)
        plastic = 0.16 * math.sin(TAU * 1050 * t) * math.exp(-t / 0.005)
        thump = 0.55 * math.sin(TAU * f0 * 0.62 * t) * math.exp(-t / 0.026)
        s = (
            0.55 * body * smooth_env(t, 0.0025, 0.016, dur)
            + thump * smooth_env(t, 0.003, 1.0, dur)
            + plastic * smooth_env(t, 0.002, 1.0, dur)
            + 0.35 * hit[i] * smooth_env(t, 0.0015, 0.004, dur)
        )
        out.append(s)
    out = lowpass(out, 3000, stages=2)
    return normalize(out, 0.62)


def release(f0, seed, dur=0.045):
    n = round(RATE * dur)
    hit = normalize(lowpass(noise(n, seed), 1600, stages=3), 1.0)
    out = []
    for i in range(n):
        t = i / RATE
        body = math.sin(TAU * f0 * t) + 0.25 * math.sin(TAU * f0 * 1.9 * t)
        s = (
            0.6 * body * smooth_env(t, 0.002, 0.009, dur, tail=0.01)
            + 0.25 * hit[i] * smooth_env(t, 0.0015, 0.003, dur, tail=0.01)
        )
        out.append(s)
    out = lowpass(out, 3200, stages=2)
    return normalize(out, 0.32)


if __name__ == "__main__":
    print("Tổng hợp âm linear →", TARGET)
    for idx, factor in enumerate((1.0, 1.05, 0.95)):
        write_wav(f"cream_press_{idx}", press(300 * factor, seed=100 + idx))
    for idx, factor in enumerate((1.0, 1.06, 0.94)):
        write_wav(f"cream_release_{idx}", release(470 * factor, seed=200 + idx))
    print("Xong: 6 file WAV.")
