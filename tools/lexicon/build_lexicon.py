#!/usr/bin/env python3
"""Sinh vi_lexicon.bin từ vi_words.txt (word<TAB>freq). Chuẩn hóa NFC, lowercase."""
import argparse, struct, sys, unicodedata
from pathlib import Path

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--words", required=True)
    ap.add_argument("--out", required=True)
    a = ap.parse_args()
    words = {}
    for line in Path(a.words).read_text(encoding="utf-8").splitlines():
        if "\t" not in line:
            continue
        w, f = line.split("\t", 1)
        w = unicodedata.normalize("NFC", w.strip().lower())
        if not w or len(w) > 30:
            continue
        try:
            freq = int(f.strip())
        except ValueError:
            freq = 1
        words[w] = max(words.get(w, 0), freq)
    def strip(s):
        nfd = unicodedata.normalize("NFD", s)
        out = "".join(c for c in nfd if unicodedata.category(c) != "Mn")
        return out.replace("đ", "d").replace("Đ", "d").lower()
    items = sorted(words.items(), key=lambda kv: strip(kv[0]))
    out = Path(a.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    with open(out, "wb") as fh:
        fh.write(b"NEOLEX1\x00")
        fh.write(struct.pack(">I", 1))
        fh.write(struct.pack(">I", len(items)))
        for w, f in items:
            b = w.encode("utf-8")
            fh.write(struct.pack(">H", len(b)))
            fh.write(b)
            fh.write(struct.pack(">I", f))
    print(f"wrote {len(items)} words -> {out}")

if __name__ == "__main__":
    main()
