#!/usr/bin/env python3
"""Tổng hợp âm switch "cream" bằng toán học — không dùng sample/bản quyền bên thứ ba.

Cream = trầm, tròn, "thock" đục chứ không click sắc. Press: transient nhiễu đã low-pass +
thân sine tắt dần ~180-260Hz + lớp thud trầm. Release: "clack" nhẹ hơn ~350-500Hz + ping lò xo
~2kHz rất nhỏ. 3 biến thể pitch mỗi loại để tránh lặp. Xuất WAV 44.1kHz mono 16-bit vào res/raw.
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


def lowpass_noise(n, seed, alpha):
    """Nhiễu trắng qua one-pole low-pass: alpha nhỏ → tiếng đục/tối hơn (cream)."""
    rng = random.Random(seed)
    out = []
    lp = 0.0
    for _ in range(n):
        lp += alpha * (rng.uniform(-1.0, 1.0) - lp)
        out.append(lp)
    return out


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


def press(f0, seed, dur=0.09):
    n = round(RATE * dur)
    noise = lowpass_noise(n, seed, alpha=0.18)
    out = []
    for i in range(n):
        t = i / RATE
        attack = min(1.0, t / 0.0015)
        release = min(1.0, (dur - t) / 0.015)
        env = math.exp(-t / 0.022)
        body = math.sin(TAU * f0 * t) + 0.45 * math.sin(TAU * f0 * 0.5 * t + 0.3)
        thud = 0.6 * math.sin(TAU * f0 * 0.5 * t) * math.exp(-t / 0.03)
        transient = noise[i] * 2.4 * math.exp(-t / 0.0025)
        out.append((0.5 * body * env + 0.4 * thud + 0.5 * transient) * attack * release)
    return normalize(out, 0.85)


def release(f0, seed, dur=0.06):
    n = round(RATE * dur)
    noise = lowpass_noise(n, seed, alpha=0.3)
    out = []
    for i in range(n):
        t = i / RATE
        attack = min(1.0, t / 0.001)
        end = min(1.0, (dur - t) / 0.012)
        env = math.exp(-t / 0.013)
        body = math.sin(TAU * f0 * t) + 0.3 * math.sin(TAU * f0 * 1.5 * t)
        ping = 0.12 * math.sin(TAU * 2100 * t) * math.exp(-t / 0.006)
        transient = noise[i] * 1.6 * math.exp(-t / 0.0018)
        out.append((0.5 * body * env + ping + 0.4 * transient) * attack * end)
    return normalize(out, 0.6)


if __name__ == "__main__":
    print("Tổng hợp âm cream →", TARGET)
    for idx, factor in enumerate((1.0, 1.06, 0.94)):
        write_wav(f"cream_press_{idx}", press(200 * factor, seed=100 + idx))
    for idx, factor in enumerate((1.0, 1.07, 0.93)):
        write_wav(f"cream_release_{idx}", release(420 * factor, seed=200 + idx))
    print("Xong: 6 file WAV.")