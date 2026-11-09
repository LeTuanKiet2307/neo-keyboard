#!/usr/bin/env python3
"""Nhập pack âm switch cơ thật: trim silence, normalize, encode OGG q5, sinh pack.json + LICENSE (SPEC §4)."""
import argparse, hashlib, json, subprocess, sys
from pathlib import Path

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--id", required=True)
    ap.add_argument("--name", required=True)
    ap.add_argument("--source", required=True, help="thư mục chứa .wav/.flac: press*, release*, space*, enter*, backspace*")
    ap.add_argument("--license", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--check", action="store_true", help="chỉ kiểm tra, không ghi")
    a = ap.parse_args()
    src = Path(a.source)
    out = Path(a.out)
    press = sorted(src.glob("press*.*"))
    release = sorted(src.glob("release*.*"))
    if len(press) < 3 or len(release) < 2:
        print(f"thiếu biến thể: cần >=3 press, >=2 release (có {len(press)} press, {len(release)} release)", file=sys.stderr)
        return 1
    if a.check:
        print(f"OK: {a.id} press={len(press)} release={len(release)}")
        return 0
    out.mkdir(parents=True, exist_ok=True)
    pack = {"id": a.id, "name": a.name, "version": 1, "origin": "recorded", "license": a.license, "gain": 1.0}
    def enc(files, kind):
        names = []
        for i, f in enumerate(files, 1):
            dst = out / f"{kind}_{i:02d}.ogg"
            cmd = ["ffmpeg", "-y", "-v", "error", "-i", str(f), "-ac", "1", "-ar", "44100", "-c:a", "libvorbis", "-q:a", "5", str(dst)]
            subprocess.run(cmd, check=True)
            names.append(dst.name)
        return names
    pack["press"] = enc(press, "press")
    pack["release"] = enc(release, "release")
    for kind in ["space", "enter", "backspace"]:
        files = sorted(src.glob(f"{kind}*.*"))
        if files:
            pack.setdefault("special", {})[kind] = enc(files, kind)[0]
    (out / "pack.json").write_text(json.dumps(pack, ensure_ascii=False, indent=2), encoding="utf-8")
    h = hashlib.sha256()
    for f in list(press) + list(release):
        h.update(f.read_bytes())
    (out / "LICENSE.md").write_text(
        f"# {a.name} — bản thu\n- Giấy phép: {a.license}\n- SHA-256 nguồn: {h.hexdigest()}\n- Ngày nhập: 2026-11-09\n",
        encoding="utf-8")
    print(f"wrote {a.id} -> {out}")
    return 0

if __name__ == "__main__":
    sys.exit(main())
