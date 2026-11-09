# Neo Keyboard — Đặc tả v2

**Phiên bản:** 2.0 (dự thảo để triển khai)  
**Ngày:** 09/11/2026  
**Thay thế:** `copper-cliff-salmon.md` — Kế hoạch MVP 0.1.0  
**Mục tiêu:** `0.2.0` — bàn phím dùng hằng ngày, không phải demo  
**Máy chuẩn:** vivo Y22s, Android 14 (API 34), 720×1612, `adb` serial `10ACA819JQ001H7`  
**Build:** Kotlin 2.1.21 · AGP 8.10.1 · Gradle 8.14.3 · Compose BOM 2025.05.01 · minSdk 29 / target 35

---

## 0. Tóm tắt điều hành

Bản 0.1.0 đã chứng minh được kiến trúc IME + Telex, nhưng chưa phải bàn phím dùng được. Đặc tả v2 thay sáu quyết định sai hoặc thiếu của bản cũ:

| # | Vấn đề | Cách xử lý trong v2 | Mức |
|---|---|---|---|
| 1 | Âm "cream" do code tổng hợp, nghe như tiếng bíp | Thay bằng **bản thu switch cơ thật**, có giấy phép, pipeline nhập và gói âm chuẩn | P0 |
| 2 | Dải gợi ý từ chưa từng hoạt động | **Engine gợi ý thật** + dải gợi ý luôn hiện phía trên phím | P0 |
| 3 | Phím "3D" thực chất là ô phẳng có bóng | **Keycap 3 lớp** có thành bên, mặt vát, hành trình xuống, bóng siết | P0 |
| 4 | Màu không bám System Zero | **Theme Bridge** một chiều từ System Zero sang Neo Keyboard, đồng bộ tức thì | P0 |
| 5 | Giữ xóa không xóa, xóa từng ký tự bất tiện | **Repeat + xóa từ + xóa grapheme**, giữ đúng hành vi Telex khi tap | P0 |
| 6 | Không có copy/paste | **Bộ nhớ tạm trong IME**: dán, sao chép lựa chọn, lịch sử có kiểm soát | P0 |

**Điều kiện tiên quyết trung thực:** đồng bộ màu *chuẩn* chỉ khả thi nếu System Zero xuất bảng màu của nó ra ngoài. Material You của Android không phải bảng màu System Zero (System Zero dựng palette OKLCH từ ảnh nền riêng). Vì vậy v2 yêu cầu một `ContentProvider` nhỏ trong System Zero. Nếu không được phép sửa System Zero, mục 7 chỉ có thể dừng ở mức "bám Material You hệ thống" và sẽ lệch màu — đặc tả ghi rõ đây là giới hạn, không phải lỗi của Neo Keyboard.

**Nguyên tắc xuyên suốt:**

- Không vá cảm giác bằng hiệu ứng giả. Âm phải là bản thu; gợi ý phải có dữ liệu; 3D phải có hình học thật.
- Offline hoàn toàn. Không thêm `INTERNET`. Không gửi nội dung gõ, clipboard hay lịch sử học đi đâu.
- Mọi giá trị màu/kích thước/thời lượng mới nằm trong `ui/design/Zero*`, không hardcode trong màn hình.
- Mỗi mốc phải build xanh, test xanh, lint 0 lỗi **và** chạy trên máy thật. "Build xanh" không phải bằng chứng hoàn thành.

---

## 1. Hiện trạng và khoảng cách

### 1.1. Đã có, giữ lại

- `NeoImeService` chạy được sau khi gắn ba `ViewTree*Owner` lên decor của cửa sổ IME.
- `TelexEngine` thuần Kotlin, 11 test pass, xử lý replay/đặt dấu/escape.
- Kiến trúc tách lớp: service → controller → Telex → `InputConnection`; UI Compose đọc state.
- Màn Cài đặt, luồng bật/chọn IME, âm lượng, rung, popup, layout QWERTY + ký hiệu.
- Design token `Zero*` copy có chọn lọc từ System Zero; font Nunito; không xin `INTERNET`.

### 1.2. Lỗi và thiếu — bằng chứng trong code

| Triệu chứng | Bằng chứng | Hệ quả |
|---|---|---|
| Âm không thật | `CreamSoundPlayer` nạp `R.raw.cream_press_*` / `cream_release_*`; `tools/sounds/build_cream_sounds.py` tổng hợp sine + noise; file 90 ms / 60 ms | Nghe như tiếng bíp, không có cảm giác switch cơ |
| Không có gợi ý | `NoOpSuggestions` luôn trả `emptyList()`; `CandidateStrip` `if (candidates.isEmpty()) return` | Dải gợi ý không bao giờ xuất hiện |
| Phím không 3D | `NeoKey` chỉ có một keycap + `padding(bottom = wall)`; `animateDpAsState` đọc trong `padding` → layout lại mỗi frame; không vẽ thành bên | Trông như ô phẳng, không có chiều sâu, có thể giật khi nhấn |
| Màu lệch System Zero | `NeoTheme` chỉ có `dynamicColorScheme()` hoặc `ZeroPalette.scheme(customSeed)`; chỉ nghe `ACTION_WALLPAPER_CHANGED` của hệ thống | System Zero đổi ảnh nền/palette thì bàn phím không biết |
| Đè xóa không xóa | `KeyboardController.backspace()` chỉ gọi một lần; `NeoKey` không có long-press/repeat | Giữ backspace không có tác dụng |
| Không copy/paste | Không có `ClipboardManager` / `ClipData` trong source | Không dán được, không sao chép được |
| Dữ liệu nhạy cảm có thể backup | `AndroidManifest.xml` đang `allowBackup="true"` | Lịch sử clipboard/từ điển có thể lên cloud nếu thêm sau này |
| Release ký debug key | `build.gradle.kts` đặt `signingConfig = signingConfigs.getByName("debug")` | Không thể phát hành nghiêm túc |

### 1.3. Không đập đi làm lại

Giữ nguyên: `TelexEngine`, `NeoImeService` host, `KeyboardSettings` → `StateFlow`, `ZeroPalette`/token, `KeyHaptics`, `KeyPopup`, `EnableFlow`. Các phần dưới đây là **thay hoặc bổ sung**, không viết lại từ đầu.

---

## 2. Phạm vi, ưu tiên, định nghĩa hoàn thành

### 2.1. P0 — bắt buộc cho 0.2.0

1. Gói âm switch cơ thật + `KeySoundPlayer` thay `CreamSoundPlayer`.
2. Dải gợi ý luôn hiện + engine gợi ý tiếng Việt (completion + next-word) + học từ người dùng.
3. Keycap 3D thật + gesture down/up/cancel + long-press backspace.
4. Theme Bridge System Zero → Neo Keyboard, có fallback.
5. Xóa: tap đúng Telex, giữ để lặp, vuốt để xóa từ, an toàn grapheme.
6. Clipboard: panel dán, sao chép lựa chọn, lịch sử, privacy.

### 2.2. P1 — nên có trong 0.2.x

- Hủy phím khi vuốt ra ngoài (đã gộp vào P0 vì liên quan gesture).
- Auto-cap đầu câu; double-space → `. `.
- Phím tắt cứng Ctrl+C/V/X/A/Backspace.
- Accessibility: TalkBack đọc và kích hoạt được phím, candidate, clipboard.
- Landscape gọn; font scale 200% không cắt chữ.
- Release signing thật; `allowBackup=false` hoặc backup rules loại dữ liệu nhạy cảm.
- Xử lý lỗi: provider chết, pack âm hỏng, DB lỗi, `InputConnection` null.

### 2.3. Không làm trong v2

- Không làm bàn phím đa ngôn ngữ đầy đủ (chỉ subtype `vi` + `en` hiện có).
- Không emoji picker, voice input, gesture typing, theme shop, cloud sync.
- Không thu thập dữ liệu, không analytics, không `INTERNET`.
- Không fork Gboard; không copy mã nguồn Gboard.
- Không thay đổi System Zero ngoài `ThemeBridgeProvider` tối thiểu.

### 2.4. Định nghĩa hoàn thành (Definition of Done)

Một mốc chỉ được coi là xong khi:

- [ ] `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` → BUILD SUCCESSFUL, lint 0 errors.
- [ ] Unit test mới cho phần vừa làm pass; không sửa test cũ để cho xanh.
- [ ] Cài APK debug lên vivo Y22s, mở bàn phím trong ít nhất 3 app thật.
- [ ] Không có `FATAL EXCEPTION` của `app.neokeyboard` trong `logcat -b crash`.
- [ ] Các tiêu chí nghiệm thu riêng của mốc (mục 4.7, 5.8, 6.10, 7.10, 8.9, 9.8) đạt.
- [ ] Ảnh chụp màn hình trước/sau lưu ở `/tmp/opencode/neo/` (hoặc thư mục dự án) để đối chiếu.

---

## 3. Kiến trúc v2

### 3.1. Sơ đồ luồng

```
┌──────────────────────────── Compose UI ────────────────────────────┐
│ KeyboardScreen                                                     │
│  ├─ CandidateStrip (luôn hiện) ── clipboard button                 │
│  ├─ KeyRow / NeoKey (3 lớp, gesture down/up/cancel)                │
│  └─ ClipboardPanel (thay nội dung bàn phím khi mở)                 │
└───────────────┬────────────────────────────────────────────────────┘
                │ onKeyDown / onKeyUp / onKeyCancel
┌───────────────▼────────────────────────────────────────────────────┐
│ KeyboardController                                                 │
│  ├─ TelexEngine                 (giữ nguyên)                       │
│  ├─ TextEditor (InputConnection)                                   │
│  ├─ BackspaceRepeater           (coroutine, long-press)            │
│  ├─ SuggestionCoordinator ── LexiconSuggestionEngine               │
│  │                              ├─ BundledLexicon (assets)         │
│  │                              └─ UserLexiconStore (SQLite)       │
│  ├─ ClipboardRepository         (SQLite + ClipboardManager)        │
│  ├─ KeySoundPlayer ── SoundPackRepository (assets)                 │
│  └─ KeyHaptics                  (giữ nguyên, thêm repeat tick)     │
└───────────────┬────────────────────────────────────────────────────┘
                │
┌───────────────▼────────────────────────────────────────────────────┐
│ NeoTheme ← ThemeBridgeClient ← ContentProvider (System Zero :theme)│
└────────────────────────────────────────────────────────────────────┘
```

### 3.2. Luồng một phím (chuẩn mới)

1. **Down:** `NeoKey` báo `onKeyDown(key)`.
   - Phát âm press + rung.
   - Hiện popup (nếu bật).
   - Nếu là `BACKSPACE`: xóa một đơn vị ngay, khởi động bộ lặp sau 400 ms.
2. **Move:** ra ngoài biên → đánh dấu `canceled`, ẩn popup, dừng lặp; nếu là backspace và kéo trái ≥ 24 dp → xóa từ một lần.
3. **Up trong biên:** `onKeyUp(key, inside = true)`.
   - Phím thường: thực hiện ký tự/space/enter/shift.
   - Backspace: dừng lặp (lần xóa đầu đã xảy ra lúc down).
   - Phát âm release.
4. **Up ngoài biên / cancel:** `onKeyUp(key, inside = false)`, không thực hiện phím, vẫn phát release.

### 3.3. File mới / sửa chính

| File | Việc |
|---|---|
| `sound/KeySoundPlayer.kt` | Thay `CreamSoundPlayer`; SoundPool + pack + round-robin + jitter |
| `sound/SoundPackRepository.kt` | Đọc `assets/sounds/index.json`, manifest, validate |
| `suggest/LexiconSuggestionEngine.kt` | Engine completion + next-word |
| `suggest/BundledLexicon.kt` | Đọc `vi_lexicon.bin`, prefix search |
| `suggest/UserLexiconStore.kt` | SQLite `words`, `bigrams`; học từ |
| `suggest/Suggestion.kt` | Model `Suggestion`/`Kind` |
| `ime/TextEditor.kt` | Bọc `InputConnection`: composing, grapheme delete, word delete, selection |
| `ime/BackspaceRepeater.kt` | Coroutine lặp xóa, tốc độ tăng dần |
| `clipboard/ClipboardRepository.kt` | SQLite clip + capture + retention |
| `clipboard/ClipboardPanel.kt` | UI panel trong IME |
| `theme/ThemeBridgeClient.kt` | Query/observe provider System Zero, cache |
| `theme/ThemeSnapshot.kt` | Parse JSON → `ColorScheme`/extras |
| `ui/NeoTheme.kt` | Thêm `ColorSource.SYSTEM_ZERO`, nhận snapshot |
| `ui/keyboard/NeoKey.kt` | Keycap 3 lớp, gesture mới, performance |
| `ui/keyboard/CandidateStrip.kt` | Luôn hiện, scroll, clipboard button |
| `ui/keyboard/KeyboardScreen.kt` | Thêm panel clipboard, state |
| `ui/settings/SettingsScreen.kt` | Section mới: âm, gợi ý, clipboard, theme status |
| `data/KeyboardSettings.kt` | Key mới + default |
| `ui/design/ZeroTokens.kt` | Token keycap/suggestion/clipboard |
| `AndroidManifest.xml` | `allowBackup=false`, không thêm INTERNET; không cần quyền cho provider theme |
| `app/build.gradle.kts` | `noCompress += "ogg"`, test deps, release signing |
| `tools/sounds/import_pack.py` | Pipeline nhập âm thật |
| `tools/lexicon/build_lexicon.py` | Sinh `vi_lexicon.bin` |
| `docs/SPEC-v2.md` | Tài liệu này |

---

## 4. Âm thanh cơ học thật (P0)

### 4.1. Quyết định: không tổng hợp

Bản 0.1.0 dùng `tools/sounds/build_cream_sounds.py` để tạo sine/noise. Đó là lý do nghe giả. v2 **cấm** sinh âm bằng code trong đường build. Mọi file phát ra phải là bản thu thật từ switch cơ (hoặc bản thu do người dùng tự thực hiện), đã qua xử lý cắt/normalize/encode.

Được phép:

- Tải bản thu có giấy phép cho phép phân phối lại (CC0, CC-BY, MIT, Apache-2.0, hoặc giấy phép ghi rõ cho phép dùng trong app).
- Tự thu âm bằng điện thoại/mic rời.
- Dùng pack có sẵn nếu `LICENSE` của pack cho phép.

Không được phép:

- Chạy script tổng hợp sóng để tạo file `.wav`/`.ogg` trong `assets`.
- Đổi tên file tổng hợp cũ thành tên pack mới.
- Dùng file có nguồn không rõ giấy phép.
- Lấy mẫu từ video/YouTube mà không có giấy phép.

### 4.2. Nguồn và giấy phép

Mỗi pack phải có `LICENSE.md` trong thư mục pack, ghi tối thiểu:

```markdown
# Cherry MX Blue — bản thu

- Nguồn: https://example.org/...
- Tác giả: ...
- Giấy phép: CC0-1.0 (hoặc CC-BY-4.0, kèm điều kiện ghi công)
- Ngày tải: 2026-11-09
- Đã xử lý: cắt silence, chuẩn hóa peak -1 dBFS, encode OGG q5
- SHA-256 của file gốc: ...
```

Quy trình tìm nguồn:

1. Tìm trên Freesound / Wikimedia Commons / archive.org / repo sound pack có giấy phép rõ ràng.
2. Ưu tiên bản thu cùng một switch cho cả press và release (không ghép switch khác nhau trong cùng pack).
3. Tải file gốc WAV/FLAC, lưu ở `tools/sounds/sources/<pack>/` (không đưa vào APK).
4. Ghi lại URL, tác giả, giấy phép, hash **trước khi** xử lý.
5. Nếu không tìm được bản thu có giấy phép phù hợp: **dừng lại và báo người dùng**, đề nghị tự thu âm. Không quay lại tổng hợp.

### 4.3. Định dạng gói âm

```
android/app/src/main/assets/sounds/
├── index.json
├── cherry_mx_blue/
│   ├── pack.json
│   ├── LICENSE.md
│   ├── press_01.ogg
│   ├── press_02.ogg
│   ├── press_03.ogg
│   ├── press_04.ogg
│   ├── release_01.ogg
│   ├── release_02.ogg
│   ├── release_03.ogg
│   ├── space_01.ogg        (tùy chọn)
│   ├── enter_01.ogg        (tùy chọn)
│   └── backspace_01.ogg    (tùy chọn)
└── cream_linear/
    └── ...
```

`index.json`:

```json
{
  "version": 1,
  "default": "cherry_mx_blue",
  "packs": ["cherry_mx_blue", "cream_linear"]
}
```

`pack.json`:

```json
{
  "id": "cherry_mx_blue",
  "name": "Cherry MX Blue",
  "version": 1,
  "origin": "recorded",
  "license": "CC0-1.0",
  "source": "https://example.org/source",
  "author": "…",
  "gain": 0.9,
  "press": ["press_01.ogg", "press_02.ogg", "press_03.ogg", "press_04.ogg"],
  "release": ["release_01.ogg", "release_02.ogg", "release_03.ogg"],
  "special": {
    "space": "space_01.ogg",
    "enter": "enter_01.ogg",
    "backspace": "backspace_01.ogg"
  }
}
```

Yêu cầu tối thiểu:

- ≥ 3 file press **khác nhau** (không phải cùng một file đổi pitch).
- ≥ 2 file release **khác nhau**.
- Mỗi file dài 15–150 ms; press thường 30–90 ms, release 15–60 ms.
- Mono, 44.1 kHz, OGG Vorbis q5; peak ≤ −1 dBFS; không clip.
- Độ to giữa các pack lệch ≤ 1.5 dB RMS (đo trên cùng một cửa sổ 20 ms).
- `origin` phải là `"recorded"` hoặc `"field-recording"`; `"synthesized"` bị test chặn.

### 4.4. Pipeline nhập

`tools/sounds/import_pack.py`:

```bash
python3 tools/sounds/import_pack.py \
  --id cherry_mx_blue \
  --name "Cherry MX Blue" \
  --source tools/sounds/sources/cherry_mx_blue \
  --license CC0-1.0 \
  --out android/app/src/main/assets/sounds/cherry_mx_blue
```

Các bước:

1. Đọc tất cả `.wav`/`.flac` trong thư mục nguồn; phân loại theo tên `press*`, `release*`, `space*`, `enter*`, `backspace*`.
2. Trim silence: ngưỡng −45 dBFS, giữ 2 ms trước attack và 10 ms sau decay.
3. Chuẩn hóa peak về −1 dBFS; không nén/limiter mạnh để giữ transient.
4. Encode OGG q5 bằng `ffmpeg` (`-ac 1 -ar 44100 -c:a libvorbis -q:a 5`).
5. Sinh `pack.json` + `LICENSE.md`; tính SHA-256 file gốc và file đã xử lý.
6. `--check` chỉ kiểm tra, không ghi; dùng trong CI/test.
7. Nếu thiếu số biến thể tối thiểu → exit code khác 0, không ghi pack.

`build.gradle.kts` phải có:

```kotlin
androidResources {
    noCompress += "ogg"
}
```

để `SoundPool.load(AssetFileDescriptor, …)` đọc được file nén.

### 4.5. Phát âm

`KeySoundPlayer` thay `CreamSoundPlayer`:

- `SoundPool(maxStreams = 12, USAGE_ASSISTANCE_SONIFICATION, CONTENT_TYPE_SONIFICATION)`.
- Nạp **tất cả** file của pack đang chọn trong `onCreate`/khi đổi pack; không nạp trong lúc gõ.
- `playPress(keyKind)`: chọn ngẫu nhiên trong pool press; `rate = 0.97..1.03`; volume = `soundVolume * pack.gain * 0.95..1.05`.
- `playRelease()`: pool release; volume thấp hơn press ~15%.
- Space/Enter/Backspace: dùng file `special` nếu có; nếu không, dùng press với `rate = 0.92` và volume +10%.
- Round-robin có random start; tránh phát cùng một file hai lần liên tiếp.
- Bỏ qua nếu chưa load xong (không block phím), nếu `soundEnabled=false`, volume ≤ 0, ringer silent, hoặc `Settings.System.SOUND_EFFECTS_ENABLED=0`.
- `release()` giải phóng pool khi service chết hoặc đổi pack.
- Log debug `sound/latency` từ lúc `onKeyDown` đến lúc gọi `SoundPool.play`; mục tiêu < 5 ms trên máy chuẩn.

Không dùng `MediaPlayer` (khởi tạo chậm), không dùng `AudioTrack` tự viết (phức tạp), không phát qua `ToneGenerator` (âm giả).

### 4.6. Cài đặt

Thay mục "Âm phím cream" bằng:

- `Âm phím` (bật/tắt).
- `Bộ âm`: danh sách pack đọc từ `index.json`, radio chọn; hiện tên + giấy phép.
- `Âm lượng`: slider 0..1.
- `Nghe thử`: phát 3 press + 2 release, cách nhau 120 ms.
- `Giấy phép âm thanh`: mở màn danh sách pack + `LICENSE.md`.

Nếu pack đã chọn không còn trong assets: fallback pack default, hiện cảnh báo trong Cài đặt, **không** ghi đè lựa chọn của người dùng.

### 4.7. Kiểm thử và nghiệm thu

**Unit / build:**

- `SoundPackTest`: parse `index.json`/`pack.json`; đủ số file; đuôi `.ogg`; `origin != synthesized`; `LICENSE.md` tồn tại; không có file `.wav` trong assets.
- Gradle task `verifySoundPacks` chạy `import_pack.py --check`; fail nếu thiếu file/giấy phép.
- Test `KeySoundPlayer` không crash khi pack hỏng/thiếu; fallback default.

**Máy thật:**

- [ ] Gõ 30 phím liên tiếp: không nghe lặp máy móc rõ rệt; press/release khác nhau.
- [ ] So sánh với bản 0.1.0: bản mới có transient rõ, không còn tiếng sine.
- [ ] Chế độ im lặng: không phát.
- [ ] Đổi pack trong Cài đặt: có hiệu lực ngay, không cần khởi động lại IME.
- [ ] Tai nghe Bluetooth: vẫn phát, không làm gián đoạn nhạc (usage sonification).
- [ ] Độ trễ chủ quan < 30 ms; log `sound/latency` < 5 ms.

**Xóa cũ:**

- Xóa `res/raw/cream_*.wav`, `CreamSoundPlayer.kt`, `tools/sounds/build_cream_sounds.py` (hoặc chuyển vào `tools/sounds/legacy/` chỉ để tham khảo, không build).
- Không để bất kỳ script tổng hợp nào trong đường build.

---

## 5. Dải gợi ý và engine gợi ý (P0)

### 5.1. Vị trí và kích thước

**Quyết định: dải gợi ý luôn nằm phía trên các hàng phím.**

- Portrait: cao `44 dp`, full width, nằm trên hàng `qwerty`.
- Landscape: cao `36 dp`, tối đa 3 gợi ý, vẫn ở trên.
- Multi-window/height thấp: thu gọn chiều cao, **không** đổi xuống dưới phím.
- Bàn phím nổi (P2): dải neo trên mặt bàn phím nổi.

**Vì sao không đặt dưới phím:** không có ràng buộc kỹ thuật nào của IME buộc phải đặt dưới. Input view có thể cao tùy ý; dải trên là nơi ngón tay ít che nhất và khớp thói quen bàn phím. Đặt dưới hàng phím cuối sẽ sát gesture navigation, dễ chạm nhầm và trông như một hàng phím phụ. Nếu sau này có yêu cầu rõ, thêm setting `suggestionPlacement = TOP | BOTTOM` nhưng **mặc định TOP**; không tự đổi vì lý do chiều cao.

Dải **luôn chiếm chỗ**, kể cả khi không có gợi ý, để bàn phím không nhảy chiều cao khi gợi ý xuất hiện/biến mất. Khi rỗng, dải chỉ có nền + nút clipboard bên phải. Ngoại lệ: field mật khẩu hoặc `IME_FLAG_NO_SUGGESTIONS` → ẩn cả dải để tiết kiệm chỗ và tránh lộ gợi ý.

### 5.2. Giao diện dải

- Nền: `surfaceContainerLow` (cùng nền bàn phím), không viền; phân tách bằng khoảng trống 4 dp phía trên hàng phím.
- Candidate: `titleMedium`, `onSurface`; mỗi mục cao 44 dp (portrait) / 36 dp (landscape), padding ngang 12 dp, bo `ZeroRadius.Field` khi nhấn, state layer `ZeroIndication`.
- Tối đa 4 gợi ý (portrait) / 3 (landscape); nếu dài hơn, cuộn ngang. Không cắt chữ giữa từ; mỗi mục `maxLines=1`.
- Thứ tự: gợi ý tốt nhất bên trái. Không tự động chọn/tô đậm gợi ý đầu để tránh người dùng tưởng đã được thay thế.
- Nút clipboard bên phải: 44×44 dp (portrait) / 36×36 dp (landscape), icon 24 dp, `contentDescription="Bộ nhớ tạm"`. Ẩn ở field mật khẩu hoặc khi clipboard tắt.
- Khi không có gợi ý: khoảng trống bên trái + nút clipboard. Không hiện chữ "Không có gợi ý".
- Khi field mật khẩu: ẩn dải hoàn toàn; bàn phím cao lên tương ứng (chấp nhận layout đổi ở field mật khẩu).

### 5.3. Mô hình dữ liệu

```kotlin
enum class SuggestionKind { TYPED, COMPLETION, CORRECTION, NEXT_WORD }

data class Suggestion(
    val text: String,
    val kind: SuggestionKind,
    val score: Float,
)
```

`SuggestionEngine` mới:

```kotlin
interface SuggestionEngine {
    /** Chuỗi đang composing đổi. Trả tối đa [limit] gợi ý. */
    fun onComposingChanged(composing: String, limit: Int = 4): List<Suggestion>

    /** Một từ vừa commit (đã qua CommitProcessor). */
    fun onWordCommitted(word: String)

    /** Vừa chèn khoảng trắng: gợi ý từ tiếp theo. */
    fun onSpace(limit: Int = 4): List<Suggestion>

    /** Con trỏ nhảy / đổi field / xóa hết. */
    fun reset()
}
```

Quy tắc:

- `onComposingChanged("")` → `emptyList()`.
- `onComposingChanged("ngu")` → completion từ lexicon, tối đa 4, không trùng.
- `onSpace()` → next-word từ bigram người dùng + danh sách khởi động; nếu không có dữ liệu → `emptyList()`.
- Không trả gợi ý rỗng, không trả chuỗi có khoảng trắng, không trả từ dài > 30 ký tự.
- Chuẩn hóa NFC; so sánh không phân biệt hoa/thường; giữ nguyên dấu tiếng Việt.

### 5.4. Lexicon đóng gói

- Nguồn: wordlist tiếng Việt có giấy phép phân phối lại rõ ràng (MIT/CC0/Apache-2.0/CC-BY kèm ghi công). Ghi nguồn + giấy phép trong `assets/lexicon/LICENSE.md`.
- Kích thước mục tiêu: 20.000–50.000 từ thông dụng; APK tăng ≤ 1.5 MB sau nén.
- Định dạng nhị phân `vi_lexicon.bin`:

```
magic   "NEOLEX1"  (8 byte)
version uint32     = 1
count   uint32
repeat count:
  len   uint16     (độ dài UTF-8)
  word  byte[len]  (UTF-8, NFC, lowercase)
  freq  uint32     (tần suất tương đối)
```

- Sắp xếp theo thứ tự từ điển UTF-8; tìm prefix bằng binary search + duyệt tiếp.
- `BundledLexicon` nạp một lần ở background (`Dispatchers.Default`), publish `StateFlow<Boolean>`; trước khi xong, engine trả `emptyList()` (không block gõ).
- Nếu file thiếu/hỏng: log, dùng danh sách từ khởi động tối thiểu (100 từ) hoặc tắt gợi ý; không crash.

Công cụ `tools/lexicon/build_lexicon.py`:

```bash
python3 tools/lexicon/build_lexicon.py \
  --words tools/lexicon/vi_words.txt \
  --out android/app/src/main/assets/lexicon/vi_lexicon.bin
```

`vi_words.txt`: mỗi dòng `word<TAB>freq`; script chuẩn hóa NFC, lowercase, lọc 1..30 ký tự, bỏ trùng, sắp xếp, ghi header.

### 5.5. Xếp hạng

**Completion (P0):**

1. Nếu `composing` khớp chính xác một từ: đưa từ đó lên đầu (kind `TYPED`), sau đó các từ cùng prefix.
2. Prefix match, sắp xếp theo `freq * userBoost * recencyBoost`.
3. `userBoost`: từ có trong `UserLexiconStore` × 3; `recencyBoost`: dùng trong 7 ngày × 1.5.
4. Tối đa 4; bỏ trùng; bỏ từ có cùng text với `composing` nếu đã có ở vị trí 0.

**Next-word (P0):**

1. Bigram `(lastWord, next)` từ `UserLexiconStore`, sắp xếp theo count.
2. Nếu chưa đủ 3: thêm từ khởi động theo ngữ cảnh đơn giản (`là`, `của`, `và`, `có`, `không`, `được`, `cho`, `một`, `những`, `này`, `thì`, `mà`, `để`, `với`, `khi`).
3. Tối đa 3; không lặp `lastWord`.

**Correction (P1, không bắt buộc 0.2.0):**

- Chỉ khi không có completion nào; edit distance ≤ 1 (Damerau) với từ dài ≥ 4; score thấp hơn completion.
- Không tự sửa; chỉ hiện như candidate. `CommitProcessor` vẫn identity trong 0.2.0.

### 5.6. Tích hợp controller

- Khi gõ chữ Telex:
  ```kotlin
  val composed = telex.append(c)
  ic.setComposingText(composed, 1)
  ui.composing = composed
  ui.candidates = suggestions.onComposingChanged(composed)
  ```
- Khi space:
  ```kotlin
  commitComposing()                 // commit từ + học
  ic.commitText(" ", 1)
  ui.candidates = suggestions.onSpace()
  ```
- Khi chọn candidate:
  ```kotlin
  ic.commitText("${candidate.text} ", 1)  // thay vùng composing
  suggestions.onWordCommitted(candidate.text)
  resetWord()
  ui.candidates = suggestions.onSpace()   // gợi ý từ kế tiếp
  ```
- Khi backspace trong composing: cập nhật `ui.candidates = suggestions.onComposingChanged(composed)`; nếu composing rỗng → `emptyList()`.
- Khi con trỏ nhảy khỏi vùng composing: `finishComposingText()`, `resetWord()`, `ui.candidates = emptyList()`.
- Khi đổi field: `resetWord()`, `ui.candidates = emptyList()`, `suggestions.reset()`.
- Nếu `ic == null` hoặc field mật khẩu: không gọi engine, không hiện dải.

`SuggestionCoordinator` giữ `lastCommittedWord`; chỉ học khi `!password && !noPersonalizedLearning && suggestionsEnabled`.

### 5.7. Học từ người dùng và privacy

- Bảng `words(word TEXT PRIMARY KEY, freq INTEGER NOT NULL, last_used INTEGER NOT NULL)`.
- Bảng `bigrams(prev TEXT, next TEXT, count INTEGER NOT NULL, last_used INTEGER NOT NULL, PRIMARY KEY(prev, next))`.
- Học mỗi khi commit một từ (không học khi: mật khẩu, email/URI, số/điện thoại, `IME_FLAG_NO_PERSONALIZED_LEARNING`, hoặc field có `inputType` nhạy cảm).
- Ghi theo lô: giữ trong memory, flush mỗi 20 từ hoặc 2 giây; tránh ghi SQLite mỗi phím.
- Giới hạn: 10.000 từ, 50.000 bigram; xóa bớt mục cũ/ít dùng khi vượt.
- Cài đặt: `Gợi ý từ` (bật/tắt), `Học từ đã gõ` (bật/tắt), `Xóa dữ liệu học` (có xác nhận).
- Không log nội dung từ; chỉ log số lượng/độ trễ.

### 5.8. Kiểm thử và nghiệm thu

**Unit:**

- `LexiconTest`: parse binary; prefix `ngu` trả `người`, `ngủ`, `ngu`...; Unicode NFC; không trả từ ngoài prefix.
- `SuggestionEngineTest`: xếp hạng; user boost; next-word bigram; max 4; không trùng; rỗng khi không có dữ liệu.
- `SuggestionCoordinatorTest` (fake `InputConnection`): gõ `ngu` → candidate; chọn `người` → commit `"người "`; space → next-word; backspace → cập nhật; password → không gọi engine.
- `UserLexiconStoreTest`: học, đếm, giới hạn, xóa; bigram.
- `KeyboardSettingsTest`: default mới; pack/theme fallback.

**Máy thật:**

- [ ] Mở Settings search, gõ `ngu`: dải hiện ≥ 3 gợi ý; chạm `người` → field có `người `.
- [ ] Gõ một câu 5 từ: sau mỗi space có next-word (sau khi đã học) hoặc ít nhất 1 gợi ý khởi động.
- [ ] Gõ Telex `tieesng`: dải hiện `tiếng`; chạm → `tiếng `.
- [ ] Xóa lùi trong composing: dải cập nhật, không treo.
- [ ] Field mật khẩu: dải ẩn, không học, không lưu clipboard.
- [ ] Field số/điện thoại: dải ẩn.
- [ ] Đổi field: dải reset, không lộ gợi ý của field trước.
- [ ] Không có gợi ý: dải vẫn giữ chiều cao, bàn phím không nhảy.

### 5.9. Hiệu năng

- `onComposingChanged` phải < 2 ms trên vivo Y22s (đo bằng `System.nanoTime` trong debug; log nếu vượt).
- Nạp lexicon < 150 ms ở background; không block lần mở bàn phím đầu tiên.
- Không cấp phát danh sách mới mỗi frame; chỉ khi composing/key đổi.

---

## 6. Keycap 3D thật (P0)

### 6.1. Vấn đề hiện tại

`NeoKey` hiện tại:

- Một `Box` đế + một `Box` keycap, keycap chỉ `padding(bottom = wall)`.
- `animateDpAsState` cho `wall`, đọc trong `padding` → **layout lại từng frame** cho từng phím.
- Không có thành bên; bóng tiếp xúc duy nhất nằm dưới keycap, nên mắt chỉ thấy một ô phẳng nổi.
- `zeroPressRadius` animate shape mỗi frame → cấp phát shape/recomposition.

Kết quả: "nổi nhẹ" nhưng không có chiều sâu, và có thể giật khi gõ nhanh.

### 6.2. Cấu trúc ba lớp

Mỗi phím gồm:

```
┌──────────────────────────────┐  ← cell (touch target)
│  ┌────────────────────────┐  │
│  │ 1. Socket (đế lõm)     │  │  nền tối, bo 16, inset 1
│  │  ┌──────────────────┐  │  │
│  │  │ 2. Side wall      │  │  │  thành bên, cao 4 dp ở đáy
│  │  │  ┌────────────┐  │  │  │
│  │  │  │ 3. Cap top │  │  │  │  mặt trên, vát nhẹ, nhãn
│  │  │  └────────────┘  │  │  │
│  │  └──────────────────┘  │  │
│  └────────────────────────┘  │
└──────────────────────────────┘
```

Thứ tự vẽ: socket → bóng → side wall → cap. Cap có `padding(start=1dp, end=1dp, top=1dp, bottom=4dp)` để lộ thành bên 4 dp ở đáy và 1 dp ở cạnh trên/hai bên. Touch target vẫn là toàn cell.

### 6.3. Token mới trong `ZeroTokens.kt`

| Token | Giá trị | Ghi chú |
|---|---|---|
| `ZeroSize.KeyHeightPortrait` | `52.dp` | hiện tại |
| `ZeroSize.KeyHeightLandscape` | `40.dp` | hiện tại |
| `ZeroSize.KeyGap` | `4.dp` | dùng `ZeroSpace.Inline` |
| `ZeroSize.KeyWall` | `4.dp` | thành bên khi nghỉ |
| `ZeroSize.KeyWallPressed` | `1.dp` | thành bên khi nhấn |
| `ZeroSize.KeyTravel` | `3.dp` | hành trình cap |
| `ZeroSize.KeySocketInset` | `1.dp` | mép đế |
| `ZeroSize.KeyShadowBlur` | `5.dp` | bóng tiếp xúc |
| `ZeroSize.KeyShadowOffset` | `2.dp` | đổ xuống |
| `ZeroAlpha.KeyShadow` | `0.22f` | nhẹ hơn `ContactShadow` 0.7 |
| `ZeroDuration.KeyTravel` | `90` ms | hoặc spring |
| `ZeroSize.SuggestionHeight` | `44.dp` | dải gợi ý |
| `ZeroSize.SuggestionHeightLandscape` | `36.dp` | |
| `ZeroSize.ClipboardRow` | `56.dp` | panel clipboard |
| `ZeroRadius.Key` | `ZeroRadius.Row` (16) | mặt cap |
| `ZeroRadius.KeyPressed` | `ZeroRadius.Field` (12) | không dùng `Pressed` 8 |

Không thêm giá trị lẻ 6/10/14/18/24/36; dùng ladder hiện có.

### 6.4. Màu và vát

Lấy từ `MaterialTheme.colorScheme` (đã đồng bộ System Zero):

- Socket: `surfaceContainerLow` làm tối 4% (lerp với đen 0.04).
- Side wall: `surfaceContainerHigh` làm tối 8%.
- Cap top: gradient dọc:
  - 0%: `surfaceContainerHighest` làm sáng 4%;
  - 45%: `surfaceContainerHighest`;
  - 100%: `surfaceContainerHighest` làm tối 6%.
- Viền sáng trên: `Color.White.copy(alpha=0.06f)`, cao 1 dp, bo theo cap.
- Bóng trong đáy cap: `Color.Black.copy(alpha=0.08f)`, cao 2 dp.
- Nhấn:
  - Cap: gradient từ `primaryContainer` sáng 4% → tối 6%.
  - Side wall: `primaryContainer` tối 12%.
  - Nhãn: `onPrimaryContainer`.
- Phím chức năng (shift/backspace/enter): cap `surfaceContainerHigh`; nhấn `secondaryContainer`; nhãn `onSecondaryContainer`.
- Phím space: nhãn "space" alpha 0.55; cap như phím thường.

Mọi phép sáng/tối nằm trong `ui/design/KeyColors.kt` (hoặc `ZeroKeyColors`), không viết trong `NeoKey`.

### 6.5. Chuyển động

- Dùng `Animatable(0f)` cho tiến trình nhấn `0..1`; đọc **chỉ trong `graphicsLayer`**:
  ```kotlin
  val travelPx = with(LocalDensity.current) { ZeroSize.KeyTravel.toPx() }
  Modifier.graphicsLayer { translationY = progress * travelPx }
  ```
- Không đọc animated Dp trong `padding`, `size`, `height`, `offset` layout.
- Spring: `dampingRatio = 0.75f`, `stiffness = 900f`; hoặc tween `90 ms` với `ZeroEasing.EffectsFast`.
- Bóng: một layer riêng, cùng kích thước cap; `alpha = 1f - 0.35f * progress`; không di chuyển theo cap; cache bitmap.
- Bo góc: snap 16 → 12 ngay khi down, 12 → 16 khi up; **không** animate shape mỗi frame. Nếu muốn mềm, dùng `graphicsLayer` scale 0.995 thay vì đổi shape.
- Màu: snap khi down/up; không animate màu để tránh recomposition 30 phím.
- Tôn trọng `LocalZeroMotion`: khi motion tắt, `progress` snap 0/1, giữ thay đổi màu; không travel.
- Popup: hiện khi down, ẩn khi up/cancel; không animate chiều cao.

### 6.6. Gesture semantics

Thay `detectTapGestures` bằng `awaitEachGesture`:

```kotlin
awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    pressed = true
    canceled = false
    onKeyDown(key)

    val longPress = if (key.special == Special.BACKSPACE) {
        launch { delay(400); if (!canceled) onBackspaceRepeatStart() }
    } else null

    var upInside = false
    var wordDeleteConsumed = false
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == down.id } ?: break
        val inside = change.position in bounds
        if (!inside && !canceled) {
            canceled = true
            longPress?.cancel()
            onKeyCancel(key)
        }
        if (!wordDeleteConsumed && key.special == Special.BACKSPACE &&
            change.position.x < down.position.x - 24.dp.toPx()
        ) {
            wordDeleteConsumed = true
            longPress?.cancel()
            onBackspaceWordDelete()
        }
        if (!change.pressed) {
            upInside = inside
            break
        }
    }
    longPress?.cancel()
    pressed = false
    onKeyUp(key, upInside && !canceled)
}
```

Quy tắc:

- Phím thường: chỉ thực hiện khi up **trong** biên và chưa cancel.
- Backspace: xóa lần đầu ngay khi down; lặp sau 400 ms; dừng khi up/cancel; vuốt trái ≥ 24 dp → xóa từ một lần, dừng lặp.
- Popup: chỉ hiện khi down và `key.showPopup`; ẩn khi ra ngoài/up.
- Âm release: luôn phát khi up/cancel (ngón tay nhấc lên), kể cả khi phím bị hủy.
- Không dùng `clickable` (cần tách down/up và cancel).

### 6.7. Popup

Giữ `KeyPopup` hiện tại (Popup window, không bị cắt). Thêm:

- Popup backspace không hiện.
- Popup shift/enter không hiện.
- Popup phím chữ hiện ký tự sau khi shift.
- Popup dùng `surfaceContainerHighest` + bóng nhẹ; không đổi màu theo pressed.
- Khoảng cách popup → key: 6 dp; canh giữa; nếu sát mép trái/phải, dịch vào trong.

### 6.8. Hiệu năng

- Mỗi phím chỉ recompose khi `pressed`/`ui.shifted`/`ui.page` đổi; không recompose toàn bàn phím khi một phím nhấn.
- `KeyLayouts.rows(page)` cache trong `remember(page)`.
- Không dùng `Modifier.shadow` (vẽ bóng mỗi frame). Chỉ dùng bóng cache bitmap.
- Mục tiêu: 95th percentile frame < 16.7 ms khi gõ 20 phím trên vivo Y22s; `dumpsys gfxinfo app.neokeyboard framestats`.
- Không cấp phát `Path`/`Brush` mỗi frame; cache theo màu/shape.

### 6.9. Accessibility

- `semantics { role = Role.Button; contentDescription = ...; onClick { onKeyUp(key, true); true } }` để TalkBack kích hoạt được.
- `contentDescription` tiếng Việt: "Xóa", "Enter", "Shift", "Bàn phím số", "Bàn phím chữ", "Dấu cách".
- Candidate: `contentDescription = "Gợi ý: $text"`.
- Clipboard button: "Bộ nhớ tạm".
- Key height ≥ 48 dp; width chấp nhận ~32 dp (đặc thù bàn phím), có rung phản hồi.
- Font scale 200%: nhãn không cắt; dùng `maxLines=1`, `softWrap=false`, `TextAutoSize` nếu có; giảm cỡ chữ trong giới hạn.

### 6.10. Kiểm thử và nghiệm thu

**Code review:**

- [ ] Không còn `animateDpAsState` đọc trong `padding`/`size`/`height` của `NeoKey`.
- [ ] Có đủ 3 lớp socket/side/cap; side wall cao 4 dp ở nghỉ.
- [ ] Bóng dùng bitmap cache, không `Modifier.shadow`.
- [ ] `graphicsLayer` là nơi duy nhất đọc travel.

**Máy thật:**

- [ ] Ảnh nghỉ: thấy thành bên 4 dp, mặt cap sáng hơn đế, có viền sáng trên.
- [ ] Ảnh nhấn: cap tụt ≥ 3 dp, thành bên còn ≤ 1 dp, bóng siết, màu `primaryContainer`.
- [ ] Gõ 20 phím nhanh: không giật, không nháy layout, không mất phím.
- [ ] Vuốt ra ngoài: phím không gõ, popup ẩn, release vẫn kêu.
- [ ] Backspace giữ: xóa lặp; vuốt trái: xóa từ.
- [ ] Landscape: key 40 dp, thành bên 4 dp, không cắt nhãn.
- [ ] TalkBack: đọc và kích hoạt được ít nhất 5 phím + candidate + clipboard.

---

## 7. Đồng bộ màu System Zero (P0)

### 7.1. Vì sao Material You không đủ

System Zero không dùng `dynamicColorScheme()` của Android. Nó dựng palette OKLCH từ **ảnh nền do người dùng nhập**, với `PaletteStyle` (Dịu/Rực/Trầm), seed chọn tay, và chế độ sáng/tối `SYSTEM/LIGHT/DARK/WALLPAPER`. Android chỉ biết palette Material You từ ảnh nền hệ thống, nên hai bên lệch nhau ngay cả khi cùng ảnh nền.

Muốn "bắt màu cho chuẩn", Neo Keyboard phải nhận **đúng các role màu** System Zero đang dùng, không tự dựng lại từ seed (dễ lệch thuật toán), không dùng Material You.

### 7.2. Kiến trúc bridge

```
System Zero main process
  MainActivity / ZeroTheme
    └─ ThemeBridgePublisher ── ghi filesDir/theme-bridge.json (AtomicFile)
                              └─ notifyChange(content://app.systemzero.theme/snapshot)

System Zero :theme process (riêng, nhẹ)
  ThemeBridgeProvider (read-only)
    └─ đọc theme-bridge.json

Neo Keyboard
  ThemeBridgeClient
    ├─ query provider khi mở bàn phím
    ├─ ContentObserver khi đang chạy
    └─ cache JSON trong SharedPreferences
  NeoTheme → ColorScheme từ snapshot
```

**Tại sao provider ở process riêng:** nếu query provider khởi động tiến trình chính của System Zero, `SystemZeroApp.onCreate()` sẽ schedule worker/alarm, tốn pin và có thể chạy nền ngoài ý muốn. Provider đặt `android:process=":theme"`; `SystemZeroApp.onCreate()` kiểm tra process name và bỏ qua khởi tạo nặng cho `:theme`. Query chỉ đọc file JSON ~4 KB.

### 7.3. Hợp đồng phía System Zero

**Manifest:**

```xml
<provider
    android:name=".system.ThemeBridgeProvider"
    android:authorities="app.systemzero.theme"
    android:exported="true"
    android:grantUriPermissions="false"
    android:process=":theme" />
```

**Không đặt `readPermission`** cho provider này: dữ liệu chỉ là bảng màu, không phải nội dung người dùng, và provider chỉ đọc. Một custom normal permission sẽ có rủi ro thứ tự cài đặt (Neo Keyboard cài trước System Zero thì permission chưa tồn tại, có thể không được cấp). Nếu sau này muốn hạn chế, thêm normal permission **và** test kỹ thứ tự cài/gỡ; đừng đổi sang `signature` vì hai app đang ký key khác nhau.

**`SystemZeroApp.onCreate()`:**

```kotlin
override fun onCreate() {
    super.onCreate()
    if (processName.endsWith(":theme")) return   // provider process: không schedule worker/alarm
    SummarizeWorker.schedule(this)
    EmbeddingWorker.enqueue(this)
    Notifications.createChannels(this)
    AlarmScheduler.scheduleRollover(this)
}
```

**`ThemeBridgeProvider`:**

- URI: `content://app.systemzero.theme/snapshot`.
- `query()` trả một dòng cột `json` (TEXT) + các cột debug `contract`, `revision`, `generated_at`, `dark`.
- `insert/update/delete` ném `UnsupportedOperationException`.
- `onCreate()` chỉ mở file, không đụng DB/Room/network.
- Nếu file chưa có: trả cursor rỗng (0 dòng), không crash.
- `getType()`: `vnd.android.cursor.item/vnd.app.systemzero.theme`.

**Tuân thủ System Zero:** khi sửa System Zero, làm theo `AGENTS.md` của repo đó — thêm **một dòng** vào `docs/history/CHANGELOG.md`, chạy đủ test/lint, không đổi dữ liệu/tool/prompt. Đây là tính năng người dùng yêu cầu rõ trong chat nên được phép theo §1 (chế độ bảo trì), nhưng phải giữ thay đổi ở mức tối thiểu: một provider + một publisher + guard process.

**`ThemeBridgePublisher`:**

- Gọi từ `MainActivity` trong `LaunchedEffect(mode, seed, style, wallpaperColors, dark)`, debounce 100 ms để không ghi liên tục khi ảnh nền vừa phân tích xong.
- Tính cả hai bảng màu:
  ```kotlin
  val light = palette(seed, dark = false, style, colors)
  val dark  = palette(seed, dark = true,  style, colors)
  ```
  dùng đúng nhánh của `ZeroTheme`:
  ```kotlin
  if (style == PaletteStyle.SOFT && colors == null)
      ZeroPalette.scheme(seed, dark)
  else
      ZeroPalette.scheme(seed, dark, style, colors)
  ```
- Ghi JSON qua `AtomicFile` tại `filesDir/theme-bridge.json`.
- `revision` tăng đơn điệu, lưu trong `SharedPreferences("theme_bridge")` hoặc đọc từ file cũ; không dùng `System.currentTimeMillis()` làm revision.
- Sau khi ghi: `contentResolver.notifyChange(uri, null)`.
- Ghi trên `Dispatchers.IO`; không block UI.
- Nếu ghi lỗi: log, giữ file cũ; không crash app.

**Kích hoạt ghi:**

- App khởi động xong, state palette có sẵn.
- Đổi `appearanceMode`, `paletteSeed`, `paletteStyle`.
- Nhập/xóa ảnh nền; phân tích ảnh xong (`wallpaperColors` đổi).
- Đổi ảnh nền preset.
- Không cần ghi khi chỉ đổi sáng/tối hệ thống vì snapshot có cả hai bảng; Neo Keyboard tự chọn.

### 7.4. JSON schema

```json
{
  "contract": 1,
  "revision": 42,
  "generatedAt": 1762646400000,
  "systemZeroVersion": "1.4.0",
  "mode": "SYSTEM",
  "seed": -9671572,
  "style": "SOFT",
  "hasWallpaper": true,
  "wallpaperLight": false,
  "currentDark": true,
  "light": {
    "primary": -16777216,
    "onPrimary": -1,
    "primaryContainer": -1,
    "onPrimaryContainer": -16777216,
    "inversePrimary": -1,
    "secondary": -1,
    "onSecondary": -1,
    "secondaryContainer": -1,
    "onSecondaryContainer": -1,
    "tertiary": -1,
    "onTertiary": -1,
    "tertiaryContainer": -1,
    "onTertiaryContainer": -1,
    "background": -1,
    "onBackground": -1,
    "surface": -1,
    "onSurface": -1,
    "surfaceVariant": -1,
    "onSurfaceVariant": -1,
    "surfaceTint": -1,
    "inverseSurface": -1,
    "inverseOnSurface": -1,
    "error": -1,
    "onError": -1,
    "errorContainer": -1,
    "onErrorContainer": -1,
    "outline": -1,
    "outlineVariant": -1,
    "scrim": -16777216,
    "surfaceBright": -1,
    "surfaceDim": -1,
    "surfaceContainerLowest": -1,
    "surfaceContainerLow": -1,
    "surfaceContainer": -1,
    "surfaceContainerHigh": -1,
    "surfaceContainerHighest": -1
  },
  "dark": { "...": "cùng 36 role" },
  "extrasLight": {
    "success": -1, "onSuccess": -1, "successContainer": -1, "onSuccessContainer": -1
  },
  "extrasDark": { "...": "cùng 4 role" }
}
```

Yêu cầu:

- Đủ **36 role** `ColorScheme` ở cả `light` và `dark`; thiếu role nào thì Neo Keyboard fallback role đó từ `lightColorScheme()`/`darkColorScheme()` và log warning.
- `contract` là số nguyên; Neo Keyboard chỉ chấp nhận `1`; cao hơn → fallback, log.
- `revision` tăng; Neo Keyboard bỏ qua snapshot có revision ≤ revision đã cache.
- `mode` là `SYSTEM|LIGHT|DARK|WALLPAPER`.
- `wallpaperLight` có thể `null` nếu không có ảnh nền.
- `currentDark` chỉ để debug; Neo Keyboard tự tính.

### 7.5. Phía Neo Keyboard

**Manifest:**

```xml
<!-- Không cần quyền: provider chỉ đọc bảng màu, không phải dữ liệu người dùng. -->
```

**`ThemeBridgeClient`:**

- URI: `content://app.systemzero.theme/snapshot`.
- `init`: đọc cache từ `SharedPreferences("theme_bridge")` (JSON + revision), parse đồng bộ (~1 ms) để tránh nháy màu; state `Ready(cached)` hoặc `Unavailable`.
- `start(scope)`:
  - Query provider trên `Dispatchers.IO`.
  - Đăng ký `ContentObserver` trên URI.
  - Khi observer bắn: debounce 50 ms, query lại.
- `refresh()`: query; nếu thành công và revision mới hơn → cập nhật cache + `StateFlow`; nếu lỗi → giữ cache, đánh dấu `stale=true`.
- `stop()`: unregister observer, cancel scope.
- Lỗi phải bắt: `SecurityException`, `IllegalArgumentException`, `NullPointerException`, `RemoteException` (qua `Exception`), `JSONException`.
- Không query trên main thread.
- Query lại mỗi lần `onStartInputView` (debounce 1 s) để bắt thay đổi khi System Zero bị kill.

**`ThemeSnapshot`:**

```kotlin
data class ThemeSnapshot(
    val contract: Int,
    val revision: Long,
    val mode: String,
    val wallpaperLight: Boolean?,
    val light: Map<String, Int>,
    val dark: Map<String, Int>,
    val extrasLight: Map<String, Int>,
    val extrasDark: Map<String, Int>,
) {
    fun effectiveDark(systemDark: Boolean): Boolean = when (mode) {
        "SYSTEM" -> systemDark
        "LIGHT" -> false
        "DARK" -> true
        "WALLPAPER" -> wallpaperLight?.not() ?: systemDark
        else -> systemDark
    }

    fun scheme(dark: Boolean): ColorScheme = ...
    fun extras(dark: Boolean): ZeroExtraColors = ...
}
```

- Map role → `Color(argb)`; dùng `darkColorScheme()`/`lightColorScheme()` làm base rồi `copy` từng role.
- Không chạy lại contrast guard; màu phải giống System Zero tuyệt đối.
- `extras` map vào `LocalZeroColors`.

**`NeoTheme`:**

```kotlin
enum class ColorSource { SYSTEM_ZERO, SYSTEM, CUSTOM }

@Composable
fun NeoTheme(
    mode: AppearanceMode,
    colorSource: ColorSource,
    seed: Int,
    systemZero: ThemeSnapshot?,
    paletteRevision: Int,
    ...
) {
    val systemDark = isSystemInDarkTheme()
    val dark = if (colorSource == ColorSource.SYSTEM_ZERO && systemZero != null)
        systemZero.effectiveDark(systemDark)
    else when (mode) { ... }

    val scheme = remember(dark, colorSource, seed, systemZero?.revision, paletteRevision) {
        when {
            colorSource == ColorSource.SYSTEM_ZERO && systemZero != null -> systemZero.scheme(dark)
            colorSource == ColorSource.SYSTEM && Build.VERSION.SDK_INT >= 31 ->
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            else -> ZeroPalette.scheme(seed, dark)
        }
    }
    val extras = if (colorSource == ColorSource.SYSTEM_ZERO && systemZero != null)
        systemZero.extras(dark) else ZeroPalette.extras(dark)
    ...
}
```

- `ColorSource.SYSTEM_ZERO` là default mới.
- Khi chọn `SYSTEM_ZERO`, `AppearanceMode` bị khóa theo snapshot; UI disable các chip Sáng/Tối/Tự động và hiện "Theo System Zero".
- Khi `SYSTEM_ZERO` không khả dụng: xử lý như `ColorSource.SYSTEM` — dynamic color trên API 31+, `ZeroPalette(DefaultSeed, systemDark)` dưới API 31; **không** ghi đè lựa chọn; khi provider xuất hiện lại, tự dùng.
- `paletteRevision` vẫn tăng khi `ACTION_WALLPAPER_CHANGED`, `onConfigurationChanged`, observer theme.

### 7.6. Chọn sáng/tối

Neo Keyboard phải tái tạo đúng `isZeroDark(mode, wallpaperLight)` của System Zero:

| `mode` | `dark` |
|---|---|
| `SYSTEM` | `isSystemInDarkTheme()` |
| `LIGHT` | `false` |
| `DARK` | `true` |
| `WALLPAPER` | `wallpaperLight?.not() ?: isSystemInDarkTheme()` |

- Dùng `systemDark` của Neo Keyboard (cùng hệ thống) khi mode SYSTEM.
- Không dùng `currentDark` trong snapshot để chọn, vì nó có thể cũ khi System Zero đã chết và người dùng đổi dark mode.
- Snapshot có cả `light`/`dark` nên không cần System Zero chạy lại.

### 7.7. Fallback và trạng thái

`ThemeBridgeState`:

```kotlin
sealed interface ThemeBridgeState {
    data object Loading : ThemeBridgeState
    data class Ready(val snapshot: ThemeSnapshot, val stale: Boolean = false) : ThemeBridgeState
    data object Unavailable : ThemeBridgeState
    data class Error(val reason: String) : ThemeBridgeState
}
```

- `Unavailable`: System Zero chưa cài/chưa có provider/file rỗng.
- `Error`: query lỗi, JSON hỏng, contract lạ.
- `Ready(stale=true)`: dùng cache cũ, provider không trả lời.
- Cài đặt hiển thị:
  - "Đã kết nối System Zero · revision 42 · tối" (xanh).
  - "Không tìm thấy System Zero — đang dùng màu hệ thống" (vàng).
  - "Đang dùng bản màu gần nhất (không kết nối được)" (vàng).
- Không hiện lỗi kỹ thuật trên bàn phím; chỉ trong Cài đặt.

### 7.8. Cài đặt giao diện

- Nguồn màu: 3 chip `System Zero` / `Hệ thống` / `Tuỳ chỉnh`.
- Khi `System Zero`:
  - Card trạng thái + revision + sáng/tối.
  - Nút "Mở System Zero" (mở app) nếu cài.
  - Chế độ sáng tối: disable, label "Theo System Zero".
- Khi `Hệ thống`: chip Tự động/Sáng/Tối như cũ.
- Khi `Tuỳ chỉnh`: swatch + hue slider như cũ.
- Không hiển thị seed của System Zero như thể người dùng chỉnh được; chỉ hiển thị để debug nếu cần.

### 7.9. Hiệu năng và privacy

- Query provider ~4 KB JSON; không đọc ảnh nền, không phân tích màu trong Neo Keyboard.
- Observer chỉ hoạt động khi IME service sống; unregister khi chết.
- Không log toàn bộ JSON; chỉ log contract/revision/lỗi.
- Màu không phải dữ liệu nhạy cảm, nhưng vẫn không gửi ra ngoài.
- Provider chỉ đọc; không cho ghi từ app khác.

### 7.10. Kiểm thử và nghiệm thu

**System Zero:**

- `ThemeBridgeProviderTest`: ghi snapshot mẫu, query trả đúng JSON/revision; provider process không schedule worker (test process name guard ở mức unit/log).
- `ThemeSnapshotTest`: cả 36 role; light/dark khác nhau; `revision` tăng; JSON round-trip.
- `ZeroPaletteTest` (đã có) vẫn pass.

**Neo Keyboard:**

- `ThemeSnapshotTest`: parse fixture; `effectiveDark` cho 4 mode; map đủ role; contrast `onSurface/surface` ≥ 7:1, `onSurfaceVariant/surfaceContainer` ≥ 4.5:1 (kiểm tra fixture, không sửa màu).
- `ThemeBridgeClientTest` (fake resolver): cache, revision cũ bị bỏ, JSON hỏng → `Error`, không crash.
- `NeoThemeTest` (nếu có Robolectric): `SYSTEM_ZERO` dùng đúng primary của fixture.

**Máy thật:**

- [ ] System Zero đổi ảnh nền đỏ → mở bàn phím: keycap/nền/dải gợi ý cùng tông đỏ, không còn xanh olive mặc định.
- [ ] Đổi sang ảnh xanh khi bàn phím đang mở (chia đôi màn hình hoặc mở lại): cập nhật ≤ 1 s.
- [ ] System Zero `Dịu` → `Rực`: bàn phím đổi độ rực theo.
- [ ] System Zero `SYSTEM` + đổi dark mode hệ thống: bàn phím đổi sáng/tối đúng.
- [ ] System Zero `WALLPAPER` + ảnh sáng/tối: bàn phím theo ảnh.
- [ ] Gỡ System Zero: bàn phím fallback màu hệ thống, Cài đặt báo "Không tìm thấy".
- [ ] Cài lại System Zero: mở bàn phím, tự kết nối lại.
- [ ] So sánh 5 role (primary, surface, surfaceContainerLow, surfaceContainerHighest, onSurface) bằng mắt/ảnh: khớp System Zero.

### 7.11. Nếu không được sửa System Zero

Nếu người dùng không đồng ý thêm provider:

- Neo Keyboard chỉ có thể dùng `dynamicColorScheme()` (Material You) hoặc seed tự chỉnh.
- Không thể khớp palette OKLCH từ ảnh nền riêng của System Zero.
- Đặc tả ghi rõ trong Cài đặt: "Đồng bộ chính xác cần System Zero 1.4+".
- Không giả vờ đồng bộ bằng cách đọc `SharedPreferences` (Android chặn) hoặc `sharedUserId` (deprecated, cần cùng chữ ký).

---

## 8. Xóa và backspace (P0)

### 8.1. Hành vi hiện tại

```kotlin
private fun backspace() {
    if (!telex.isEmpty) {
        val composed = telex.backspace()
        if (composed.isEmpty()) {
            ic?.commitText("", 1)
            ui.composing = ""
        } else {
            ic?.setComposingText(composed, 1)
            ui.composing = composed
        }
        ui.candidates = suggestions.onComposingChanged(composed)
    } else {
        ic?.deleteSurroundingText(1, 0)
    }
}
```

- Chỉ xóa một lần mỗi lần chạm.
- `deleteSurroundingText(1, 0)` xóa 1 **UTF-16 code unit**, có thể tách emoji/grapheme.
- Không có long-press/repeat.
- Khi composing, xóa dấu là đúng Telex, nhưng giữ phím không có tác dụng.

### 8.2. Tap backspace

Thứ tự ưu tiên:

1. Nếu có selection → xóa selection (`ic.commitText("", 1)`), reset Telex.
2. Nếu đang composing → `telex.backspace()`; cập nhật composing/candidates.
   - Nếu composing rỗng: `ic.setComposingText("", 1)` rồi `ic.finishComposingText()`; reset word.
3. Nếu không composing → xóa **một grapheme cluster** trước con trỏ.

Không dùng `deleteSurroundingText(1, 0)` trực tiếp nữa.

### 8.3. Giữ backspace

- Down: xóa một đơn vị ngay (tap semantics).
- Sau `400 ms`: bắt đầu lặp.
- Nhịp: 70 ms/lần; sau mỗi 5 lần giảm 5 ms, tối thiểu 35 ms.
- Mỗi lần lặp:
  - Nếu composing: `telex.backspace()`; khi composing rỗng, chuyển sang xóa grapheme.
  - Nếu không composing: xóa một grapheme; nếu đang ở đầu từ, lần tiếp theo xóa từ.
- Phát âm press (volume 0.7) + rung tick mỗi lần lặp; không phát release cho đến khi nhấc tay.
- Dừng khi up/cancel; hủy coroutine.
- Không lặp nếu field mật khẩu? Vẫn lặp xóa bình thường; chỉ tắt học/gợi ý.

### 8.4. Vuốt trái để xóa từ

- Trong lúc giữ backspace, nếu ngón tay di chuyển sang trái ≥ 24 dp:
  - Hủy bộ lặp.
  - Xóa một từ trước con trỏ (word delete).
  - Rung mạnh hơn một nhịp; phát âm special backspace.
  - Đánh dấu `wordDeleteConsumed` để không lặp lại trong cùng gesture.
- Nếu tiếp tục giữ và vuốt trái lần nữa? Chỉ một lần mỗi gesture; người dùng nhấc và lặp lại.

### 8.5. Xóa grapheme cluster

`TextEditor.deleteGrapheme()`:

1. `val before = ic.getTextBeforeCursor(64, 0)?.toString()`.
2. Nếu `before` null/rỗng → `ic.deleteSurroundingTextInCodePoints(1, 0)` (API 24+); nếu vẫn false → `sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)`.
3. Dùng `java.text.BreakIterator.getCharacterInstance()` trên `before`, lấy cluster cuối; `length = before.length - boundary`.
4. `ic.deleteSurroundingText(length, 0)`; nếu false → fallback code points/key event.
5. Không chuẩn hóa `before` (không đổi văn bản của host).

Test: `é` NFC (1 code unit), `e\u0301` NFD (2), emoji ZWJ family, cờ 🇻🇳, chữ Việt có dấu, ký tự CJK.

### 8.6. Xóa từ

`TextEditor.deleteWord()`:

1. Nếu composing: `ic.commitText("", 1)` để xóa cả vùng composing; reset Telex.
2. Nếu selection: xóa selection.
3. Lấy tối đa 64 ký tự trước con trỏ; bỏ khoảng trắng cuối; quét ngược đến khoảng trắng/dấu câu (`.,;:!?()[]{}"“”‘’` và newline); xóa span đó.
4. Nếu span rỗng: xóa một grapheme.
5. Fallback: gửi `Ctrl+Backspace`? Không; dùng `deleteSurroundingText` với span đã tính.

### 8.7. Selection và con trỏ

- Nếu `ic.getSelectedText(0) != null`: backspace xóa selection, không đụng Telex.
- Nếu con trỏ nhảy trong lúc composing: `finishComposingText()`, reset Telex/candidates (đã có).
- Nếu `onUpdateSelection` báo candidatesEnd khác: reset.
- Không xóa khi `ic == null`.

### 8.8. Fallback

- `deleteSurroundingText` trả false → thử `deleteSurroundingTextInCodePoints(1, 0)`.
- Vẫn false → `sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)`.
- Không crash nếu host không hỗ trợ.

### 8.9. Kiểm thử và nghiệm thu

**Unit (fake `InputConnection`):**

- Tap khi composing `tiếng` → `tiến`/`tiếng` theo Telex replay; không xóa ký tự host.
- Tap khi composing rỗng → xóa 1 grapheme.
- Giữ 1 s → ≥ 8 lần xóa; dừng khi up.
- Vuốt trái → xóa từ.
- Selection → xóa selection.
- Emoji ZWJ/flag → xóa nguyên cluster, không để rác.
- `deleteSurroundingText` false → fallback code points/key event.
- Password field → vẫn xóa, không học.

**Máy thật:**

- [ ] Giữ backspace 1 s trong Settings search: xóa liên tục, không dừng.
- [ ] Gõ `tieesng` rồi tap backspace: dấu/ky tự cuối được hoàn nguyên đúng Telex.
- [ ] Giữ backspace đến khi hết từ: chuyển sang xóa ký tự trước đó, không kẹt.
- [ ] Vuốt trái trên backspace: xóa cả từ.
- [ ] Xóa emoji trong Chrome: không để lại nửa emoji.
- [ ] Không có crash khi host trả `null` cho `getTextBeforeCursor`.

---

## 9. Clipboard và copy/paste (P0)

### 9.1. Điểm vào

- Nút clipboard ở cuối dải gợi ý, 44×44 dp (portrait) / 36×36 dp (landscape), luôn hiện ở field thường.
- Ẩn ở field mật khẩu (`password`, `visiblePassword`, `webPassword`, số + password) và khi `clipboardEnabled=false`.
- Mở panel thay nội dung bàn phím, không mở cửa sổ mới, không làm mất focus field.

### 9.2. Panel

- Header 48 dp: nút đóng (←), tiêu đề "Bộ nhớ tạm", nút "Dán" (đọc clipboard hệ thống), nút "Sao chép lựa chọn" (disabled nếu không có selection).
- Danh sách `LazyColumn`, row 56 dp:
  - Preview tối đa 2 dòng, 200 ký tự, `onSurface`.
  - Thời gian tương đối ("2 phút trước"), `onSurfaceVariant`.
  - Icon ghim nếu pinned.
  - Menu row: Ghim/Bỏ ghim, Xóa.
- Empty state: "Chưa có nội dung. Sao chép văn bản để dùng lại." + nút "Dán từ hệ thống".
- Footer: "Lưu 30 ngày · tối đa 50 mục" + "Xóa tất cả" (xác nhận inline, không dialog).
- Panel cao bằng nội dung bàn phím; danh sách cuộn; không thay đổi chiều cao IME.
- Đóng panel: nút ←; không dùng back hệ thống (host có thể ẩn bàn phím).

### 9.3. Dán

- Chạm row → `ic.commitText(text, 1)`:
  - Nếu có selection: thay selection.
  - Nếu đang composing: `finishComposingText()` trước khi mở panel (xem 9.5), nên không mất chữ.
- Dán xong: đóng panel, quay lại bàn phím, reset Telex/candidates, phát haptic nhẹ.
- "Dán" header: đọc `ClipboardManager.primaryClip` tại thời điểm bấm; nếu có text → dán + thêm vào history (dedupe).
- Không dán ảnh/URI trong v2; chỉ text.

### 9.4. Sao chép lựa chọn

- `val selected = ic.getSelectedText(0)?.toString()`.
- Nếu null/rỗng: nút disabled.
- Nếu có: `ClipboardManager.setPrimaryClip(ClipData.newPlainText("Neo Keyboard", selected))`; thêm vào history; hiện "Đã sao chép" trong header 1.5 s.
- Không dùng `performContextMenuAction(android.R.id.copy)` để tránh phụ thuộc host; đọc trực tiếp.
- Không sao chép ở field mật khẩu (panel đã ẩn).
- Android 13+: hệ thống có thể hiện clipboard overlay; chấp nhận.

### 9.5. Lịch sử

- Nguồn:
  1. Khi panel mở: đọc primary clip, thêm nếu là text.
  2. `OnPrimaryClipChangedListener` chỉ đăng ký khi IME visible, field không mật khẩu, `clipboardEnabled=true`.
  3. Khi người dùng bấm "Sao chép lựa chọn".
- Chuẩn hóa:
  - Bỏ clip rỗng/toàn khoảng trắng.
  - Dedupe theo hash SHA-256 của text; nếu trùng clip gần nhất, chỉ cập nhật `created_at`, không thêm row.
  - Giới hạn mỗi clip 100.000 ký tự; cắt và ghi chú "…" nếu dài hơn? Tốt hơn: không lưu clip > 100 KB, chỉ dán trực tiếp.
  - Bỏ qua `ClipDescription.extras` có `EXTRA_IS_SENSITIVE=true` (Android 13+).
- Trước khi mở panel: `finishComposingText()` + reset Telex để văn bản host ổn định.
- Khi đổi field/password: đóng panel, hủy listener.

### 9.6. Lưu trữ và retention

SQLite (`clipboard.db`, version 1):

```sql
CREATE TABLE clips (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  text TEXT NOT NULL,
  hash TEXT NOT NULL UNIQUE,
  created_at INTEGER NOT NULL,
  pinned INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX clips_created ON clips(created_at DESC);
```

- Tối đa 50 mục không ghim; pinned tối đa 10.
- Xóa mục không ghim cũ hơn 30 ngày khi mở panel.
- "Xóa tất cả" xóa cả pinned sau xác nhận.
- Không backup: `allowBackup=false` hoặc `dataExtractionRules` loại `clipboard.db` + `user_lexicon.db` + prefs.
- Không log text.

### 9.7. Privacy

- Không có `INTERNET`; clipboard chỉ ở máy.
- Không đọc clipboard khi bàn phím không hiện.
- Không lưu ở field mật khẩu.
- Không lưu clip có cờ sensitive.
- Không hiển thị clipboard history trong màn Cài đặt (chỉ nút xóa).
- Nếu người dùng tắt `Bộ nhớ tạm`: xóa listener, ẩn nút, không xóa dữ liệu cũ trừ khi bấm "Xóa lịch sử".

### 9.8. Kiểm thử và nghiệm thu

**Unit:**

- `ClipboardRepositoryTest`: add/dedupe/max 50/retention 30 ngày/pinned/xóa tất cả; text > 100 KB không lưu.
- `ClipboardCoordinatorTest` (fake `InputConnection` + fake `ClipboardManager`): dán thay selection; dán khi composing đã finish; copy selection; password → không mở/không lưu.
- `ClipboardPanelStateTest`: empty state, confirm xóa, đóng panel.

**Máy thật:**

- [ ] Sao chép một đoạn trong Chrome → mở bàn phím → panel → chạm clip → dán đúng.
- [ ] Chọn text trong Settings search → panel → "Sao chép lựa chọn" → dán vào field khác.
- [ ] Copy 2 lần cùng text: history chỉ 1 mục, thời gian cập nhật.
- [ ] Field mật khẩu: không thấy nút clipboard; không có clip mới.
- [ ] Xóa tất cả: danh sách rỗng, không crash.
- [ ] Panel mở/đóng không làm mất chữ đang gõ.
- [ ] Android 13+: clip sensitive không vào history.

---

## 10. Các hành vi cơ bản còn thiếu (P1)

### 10.1. Hủy phím khi vuốt ra ngoài

Đã gộp vào 6.6. Test: nhấn `a`, vuốt ra ngoài, nhấc tay → không có `a`; âm release vẫn kêu; popup ẩn.

### 10.2. Auto-cap đầu câu

- Bật mặc định; tắt trong password/email/URI/number.
- Dùng `ic.getCursorCapsMode(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)`; nếu trả `CAP_MODE_SENTENCES`/`CAP_MODE_WORDS` → shift ONCE.
- Sau `.` `!` `?` + space/newline → shift ONCE.
- Không auto-cap nếu người dùng đang giữ shift LOCK.
- Cài đặt: `Tự viết hoa đầu câu`.

### 10.3. Double-space → dấu chấm

- Nếu space vừa bấm cách space trước < 300 ms và không có composing:
  - Xóa space vừa commit (`deleteSurroundingText(1,0)`).
  - Commit `". "`.
  - Tắt trong password/email/URI/number.
- Cài đặt: `Hai lần space = dấu chấm`.

### 10.4. Phím tắt cứng

- `Ctrl+C/V/X/A`: gửi `performContextMenuAction` hoặc `InputConnection` tương ứng; nếu host không hỗ trợ, fallback `sendDownUpKeyEvents` với meta.
- `Ctrl+Backspace`: xóa từ.
- `Shift+Enter`: xuống dòng (nếu editor cho phép).
- Không can thiệp phím tắt hệ thống khác.

### 10.5. Accessibility

- Xem 6.9.
- Thêm `testTag` cho `keyboard`, `candidate-strip`, `clipboard-panel`, `clipboard-row`, `backspace` để test UI sau này.
- TalkBack không đọc nội dung clipboard (privacy).

### 10.6. Landscape

- Key height 40 dp; gap 4 dp; suggestion 36 dp; clipboard row 48 dp.
- Không cắt nhãn; `qwerty` 10 phím vẫn đủ.
- Panel clipboard cuộn được; header gọn.

### 10.7. Release signing

- Tạo keystore ngoài repo: `~/.local/share/neo-keyboard/release.jks`.
- `keystore.properties` (không commit):
  ```
  storeFile=~/.local/share/neo-keyboard/release.jks
  storePassword=...
  keyAlias=neo-keyboard
  keyPassword=...
  ```
- `build.gradle.kts`:
  ```kotlin
  val keystore = rootProject.file("keystore.properties")
  signingConfigs {
      if (keystore.exists()) create("release") { ... }
  }
  buildTypes.release {
      signingConfig = signingConfigs.findByName("release")
          ?: error("Thiếu keystore.properties — không ký release bằng debug key")
  }
  ```
- `allowBackup=false` hoặc backup rules loại DB/prefs nhạy cảm.
- Version `0.2.0` / code `2`.

### 10.8. Xử lý lỗi

- Provider theme chết: fallback, không crash.
- Pack âm hỏng: fallback default, log.
- Lexicon hỏng: tắt gợi ý, log.
- SQLite lỗi: dùng in-memory, không crash.
- `InputConnection` null: bỏ qua thao tác.
- `SoundPool` load fail: im lặng, không block.
- Không hiện stack trace cho người dùng; log tag `NeoKeyboard`.

---

## 11. Cài đặt và dữ liệu

### 11.1. Bản đồ cài đặt

| Nhóm | Mục | Kiểu | Default |
|---|---|---|---|
| Bàn phím | Bật/chọn IME | flow | — |
| Giao diện | Nguồn màu | `SYSTEM_ZERO/SYSTEM/CUSTOM` | `SYSTEM_ZERO` |
| Giao diện | Chế độ sáng tối | `SYSTEM/LIGHT/DARK` | `SYSTEM` |
| Giao diện | Màu chủ đạo | Int | `ZeroPalette.DefaultSeed` |
| Âm thanh | Âm phím | Boolean | true |
| Âm thanh | Bộ âm | String | `index.default` |
| Âm thanh | Âm lượng | Float 0..1 | 0.7 |
| Rung | Rung khi nhấn | Boolean | true |
| Rung | Mức rung | `LIGHT/MEDIUM/STRONG` | `MEDIUM` |
| Gõ tiếng Việt | Telex | Boolean | true |
| Gõ tiếng Việt | Popup xem trước | Boolean | true |
| Gõ tiếng Việt | Tự viết hoa đầu câu | Boolean | true |
| Gõ tiếng Việt | Hai space = dấu chấm | Boolean | true |
| Gợi ý | Gợi ý từ | Boolean | true |
| Gợi ý | Học từ đã gõ | Boolean | true |
| Gợi ý | Số gợi ý | Int 3..5 | 4 |
| Bộ nhớ tạm | Bật | Boolean | true |
| Bộ nhớ tạm | Lưu lịch sử | Int ngày | 30 |
| Giới thiệu | Phiên bản/giấy phép | — | — |

### 11.2. SharedPreferences keys

Giữ tên `neo_keyboard`; thêm:

```
color_source        = SYSTEM_ZERO | SYSTEM | CUSTOM
sound_pack          = cherry_mx_blue
suggestions_enabled = true
learn_words         = true
suggestion_count    = 4
clipboard_enabled   = true
clipboard_days      = 30
auto_cap            = true
double_space_period = true
```

Không đổi key cũ (`mode`, `custom_seed`, `sound_enabled`, `sound_volume`, `haptics_enabled`, `haptic_strength`, `telex_enabled`, `popup_enabled`) để giữ cài đặt người dùng.

### 11.3. Database

`user_lexicon.db`:

```sql
CREATE TABLE words (
  word TEXT PRIMARY KEY,
  freq INTEGER NOT NULL,
  last_used INTEGER NOT NULL
);
CREATE TABLE bigrams (
  prev TEXT NOT NULL,
  next TEXT NOT NULL,
  count INTEGER NOT NULL,
  last_used INTEGER NOT NULL,
  PRIMARY KEY(prev, next)
);
```

`clipboard.db`:

```sql
CREATE TABLE clips (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  text TEXT NOT NULL,
  hash TEXT NOT NULL UNIQUE,
  created_at INTEGER NOT NULL,
  pinned INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX clips_created ON clips(created_at DESC);
```

- Dùng `SQLiteOpenHelper`, WAL, không Room/KSP (giữ build nhẹ).
- Mở DB lazily ở background; không mở trong `Application.onCreate`.
- Version 1; migration chỉ thêm cột/bảng, không destructive.

### 11.4. Migration dữ liệu cũ

- Không có DB cũ; chỉ SharedPreferences. Key mới có default.
- Nếu `color_source` chưa có: default `SYSTEM_ZERO`; nếu người dùng đã chọn `CUSTOM` trước đó, key `color_source` đã có từ 0.1.0 → giữ.
- Nếu `sound_pack` chưa có: default từ `index.json`.
- Không xóa lịch sử/cài đặt khi nâng cấp.

---

## 12. Kiểm thử và nghiệm thu tổng thể

### 12.1. Unit test mục tiêu

| Test | Số case tối thiểu |
|---|---|
| `TelexEngineTest` (giữ) | 11 |
| `LexiconTest` | 6 |
| `SuggestionEngineTest` | 12 |
| `SuggestionCoordinatorTest` | 8 |
| `UserLexiconStoreTest` | 6 |
| `BackspaceTest` | 10 |
| `ClipboardRepositoryTest` | 8 |
| `ClipboardCoordinatorTest` | 6 |
| `SoundPackTest` | 5 |
| `ThemeSnapshotTest` | 6 |
| `ThemeBridgeClientTest` | 6 |
| `KeyboardSettingsTest` | 4 |
| `ZeroPaletteTest` | 4 |

Tổng mục tiêu: ≥ 80 case mới, không tính 11 Telex cũ. Không sửa test cũ để cho xanh.

### 12.2. Build/lint

```bash
export JAVA_HOME=~/.local/share/mise/installs/java/temurin-21
cd Neo_keyboard/android
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

- BUILD SUCCESSFUL.
- `lint-results-debug.txt`: 0 errors; warnings không tăng quá 21 (hoặc giải thích từng warning mới).
- `testDebugUnitTest` không có failure/error.
- `assembleRelease` chỉ chạy khi có keystore; không ký debug.

### 12.3. Checklist máy thật

**Cơ bản:**

- [ ] Cài APK debug, chọn Neo Keyboard làm IME mặc định.
- [ ] Mở Settings search, Messages, Chrome; gõ được, không crash.
- [ ] Xoay ngang/dọc; bàn phím không cắt.
- [ ] Field mật khẩu: không gợi ý, không clipboard, không học.
- [ ] Field số/điện thoại: layout số, không Telex.
- [ ] Field email/URI: không Telex.
- [ ] Chế độ tối/sáng: màu đúng.
- [ ] Chế độ im lặng: không âm.
- [ ] Rung: tắt/bật, 3 mức.

**Tính năng mới:**

- [ ] Âm thật: nghe thử, so sánh 0.1.0.
- [ ] Gợi ý: gõ `ngu`, chọn `người`; next-word sau space.
- [ ] 3D: ảnh nghỉ/nhấn; không giật.
- [ ] Theme: đổi ảnh nền System Zero, màu cập nhật.
- [ ] Backspace: giữ 1 s, vuốt trái, emoji.
- [ ] Clipboard: copy/paste, lịch sử, xóa, password.

### 12.4. Hiệu năng

- `dumpsys gfxinfo app.neokeyboard framestats` khi gõ 20 phím: 95th percentile < 16.7 ms.
- Không có frame `Janky` liên tiếp > 3.
- Bộ nhớ IME < 120 MB (ước tính; đo bằng `dumpsys meminfo app.neokeyboard`).
- Nạp lexicon < 150 ms; không block first key.
- Provider query: warm < 20 ms; cold start process `:theme` < 150 ms (log).

### 12.5. Regression

- 11 Telex test cũ pass.
- Không còn crash `ViewTreeLifecycleOwner`.
- Không thay đổi hành vi commit/enter/space ngoài spec.
- Không thêm `INTERNET`.
- Không log nội dung gõ/clipboard.

---

## 13. Lộ trình

### M1 — Âm thật + keycap 3D + backspace (P0, 4–5 ngày)

1. Tìm/thu pack âm; viết `import_pack.py`; thêm assets; `KeySoundPlayer`.
2. Viết lại `NeoKey` 3 lớp + gesture down/up/cancel; `BackspaceRepeater`; `TextEditor.deleteGrapheme/deleteWord`.
3. Test unit âm/backspace; test máy thật.

**Xong khi:** âm thật, phím có thành bên, giữ backspace xóa, không giật.

### M2 — Gợi ý + dải gợi ý (P0, 5–7 ngày)

1. Wordlist + `build_lexicon.py` + `BundledLexicon`.
2. `LexiconSuggestionEngine` + `UserLexiconStore`.
3. `CandidateStrip` luôn hiện; controller integration; next-word.
4. Test unit + máy thật.

**Xong khi:** gõ `ngu` có gợi ý; chọn được; next-word; password ẩn.

### M3 — Clipboard (P0, 2–3 ngày)

1. `ClipboardRepository` + DB.
2. `ClipboardPanel` + nút trên dải.
3. Copy selection/paste/listener/privacy.
4. Test unit + máy thật.

**Xong khi:** copy/paste hoạt động; password ẩn; lịch sử đúng.

### M4 — Theme Bridge (P0, 3–4 ngày, gồm System Zero)

1. System Zero: provider `:theme`, publisher, snapshot light/dark, process guard.
2. Neo Keyboard: client, cache, observer, `ColorSource.SYSTEM_ZERO`, settings status.
3. Test provider + máy thật.

**Xong khi:** đổi ảnh nền System Zero → bàn phím đổi màu ≤ 1 s; fallback đúng.

### M5 — P1 cơ bản + hoàn thiện (2–3 ngày)

1. Auto-cap, double-space, hardware shortcuts.
2. Accessibility, landscape, font scale.
3. Release signing, `allowBackup=false`, version 0.2.0.
4. Kiểm hiệu năng, sửa warning.

**Xong khi:** checklist P1 đạt; release APK ký thật; lint 0 errors.

### Ước lượng tổng

- Một người làm liên tục: **16–22 ngày** (chưa tính thời gian tìm/thu âm và duyệt giấy phép).
- Nếu tìm được pack âm có sẵn giấy phép: giảm 1–2 ngày.
- Nếu phải tự thu âm: cộng 1–2 ngày.

### Thứ tự phụ thuộc

- M1 và M2 độc lập; có thể làm song song nếu có hai người.
- M3 phụ thuộc M2 (dùng chung dải/nút) nhưng có thể làm sau M1.
- M4 phụ thuộc System Zero; làm song song với M2/M3.
- M5 sau cùng.

---

## 14. Rủi ro và quyết định

### 14.1. Bảng rủi ro

| Rủi ro | Mức | Giảm thiểu |
|---|---|---|
| Không tìm được âm thật có giấy phép | Cao | Ưu tiên CC0; nếu không, tự thu; **không** quay lại tổng hợp |
| SoundPool trễ trên OEM | Trung bình | Preload, sonification usage, đo log; fallback im lặng |
| Wordlist không có giấy phép rõ | Cao | Chỉ dùng MIT/CC0/Apache/CC-BY; ghi LICENSE; nếu không rõ thì không dùng |
| Gợi ý làm chậm gõ | Trung bình | Binary search, < 2 ms; nếu vượt, debounce 16 ms |
| Theme provider khởi động System Zero nặng | Cao | Process `:theme` + guard `Application.onCreate` |
| System Zero đổi code palette làm lệch | Trung bình | Snapshot role, không tự dựng lại; contract version |
| Bàn phím quá cao vì dải gợi ý | Trung bình | 44 dp; landscape 36 dp; ẩn ở password |
| Compose jank khi 30 phím | Cao | graphicsLayer, bóng cache, không layout animation |
| Clipboard bị OS chặn listener | Trung bình | Vẫn có "Dán từ hệ thống"; đọc khi mở panel |
| Xóa grapheme khác nhau giữa app | Trung bình | BreakIterator + fallback code points/key event |
| Release key bị mất | Cao | Lưu keystore ngoài repo + backup; không commit |
| System Zero đang bảo trì, sửa provider ảnh hưởng | Trung bình | Chỉ thêm provider/publisher, không đổi dữ liệu/tool/prompt; test đầy đủ |

### 14.2. Quyết định đã chốt

1. **Âm thật, không tổng hợp.** Không có ngoại lệ.
2. **Dải gợi ý trên phím, luôn giữ chỗ.** Không đặt dưới phím.
3. **Keycap 3 lớp, hành trình 3 dp, thành bên 4→1 dp.** Không dùng layout animation.
4. **Theme Bridge qua provider, snapshot cả light/dark.** Không đọc trộm prefs, không `sharedUserId`.
5. **Backspace tap giữ Telex; giữ để lặp; vuốt trái xóa từ.**
6. **Clipboard là panel trong IME, không cửa sổ rời.**
7. **Không thêm INTERNET; không analytics.**
8. **Mọi thay đổi System Zero nằm ngoài phạm vi Neo Keyboard, nhưng là dependency bắt buộc của mục 7.**

### 14.3. Câu hỏi mở

- Người dùng có đồng ý thêm `ThemeBridgeProvider` vào System Zero không? Nếu không, mục 7 dừng ở fallback Material You.
- Người dùng có sẵn sàng tự thu âm switch không? Nếu không, cần duyệt pack CC0 cụ thể trước khi code.
- Có cần giữ tiếng Anh subtype không? v2 giữ nguyên, không mở rộng.
- Có cần đồng bộ cả `SurfaceMaterial` (Đặc/Kính/Trong) không? v2 chỉ đồng bộ **màu**; bàn phím IME không có backdrop thật để làm kính, nên dùng mặt đặc.

---

## 15. Phụ lục

### 15.1. Ví dụ JSON snapshot đầy đủ (rút gọn role)

```json
{
  "contract": 1,
  "revision": 7,
  "generatedAt": 1762646400000,
  "systemZeroVersion": "1.4.0",
  "mode": "WALLPAPER",
  "seed": -9671572,
  "style": "VIVID",
  "hasWallpaper": true,
  "wallpaperLight": false,
  "currentDark": true,
  "light": {
    "primary": -16777216,
    "onPrimary": -1,
    "primaryContainer": -1,
    "onPrimaryContainer": -16777216,
    "inversePrimary": -1,
    "secondary": -1,
    "onSecondary": -1,
    "secondaryContainer": -1,
    "onSecondaryContainer": -1,
    "tertiary": -1,
    "onTertiary": -1,
    "tertiaryContainer": -1,
    "onTertiaryContainer": -1,
    "background": -1,
    "onBackground": -1,
    "surface": -1,
    "onSurface": -1,
    "surfaceVariant": -1,
    "onSurfaceVariant": -1,
    "surfaceTint": -1,
    "inverseSurface": -1,
    "inverseOnSurface": -1,
    "error": -1,
    "onError": -1,
    "errorContainer": -1,
    "onErrorContainer": -1,
    "outline": -1,
    "outlineVariant": -1,
    "scrim": -16777216,
    "surfaceBright": -1,
    "surfaceDim": -1,
    "surfaceContainerLowest": -1,
    "surfaceContainerLow": -1,
    "surfaceContainer": -1,
    "surfaceContainerHigh": -1,
    "surfaceContainerHighest": -1
  },
  "dark": {
    "primary": -1,
    "onPrimary": -16777216,
    "primaryContainer": -1,
    "onPrimaryContainer": -1,
    "inversePrimary": -1,
    "secondary": -1,
    "onSecondary": -16777216,
    "secondaryContainer": -1,
    "onSecondaryContainer": -1,
    "tertiary": -1,
    "onTertiary": -16777216,
    "tertiaryContainer": -1,
    "onTertiaryContainer": -1,
    "background": -16777216,
    "onBackground": -1,
    "surface": -16777216,
    "onSurface": -1,
    "surfaceVariant": -1,
    "onSurfaceVariant": -1,
    "surfaceTint": -1,
    "inverseSurface": -1,
    "inverseOnSurface": -16777216,
    "error": -1,
    "onError": -16777216,
    "errorContainer": -1,
    "onErrorContainer": -1,
    "outline": -1,
    "outlineVariant": -1,
    "scrim": -16777216,
    "surfaceBright": -1,
    "surfaceDim": -1,
    "surfaceContainerLowest": -16777216,
    "surfaceContainerLow": -1,
    "surfaceContainer": -1,
    "surfaceContainerHigh": -1,
    "surfaceContainerHighest": -1
  },
  "extrasLight": {
    "success": -1,
    "onSuccess": -16777216,
    "successContainer": -1,
    "onSuccessContainer": -16777216
  },
  "extrasDark": {
    "success": -1,
    "onSuccess": -16777216,
    "successContainer": -1,
    "onSuccessContainer": -1
  }
}
```

> Các giá trị `-1`/`-16777216` trong ví dụ chỉ là placeholder; publisher phải ghi `Color.toArgb()` thật.

### 15.2. Ví dụ `pack.json`

```json
{
  "id": "cherry_mx_blue",
  "name": "Cherry MX Blue",
  "version": 1,
  "origin": "recorded",
  "license": "CC0-1.0",
  "source": "https://example.org/cherry-mx-blue",
  "author": "Example",
  "gain": 0.9,
  "press": ["press_01.ogg", "press_02.ogg", "press_03.ogg", "press_04.ogg"],
  "release": ["release_01.ogg", "release_02.ogg", "release_03.ogg"],
  "special": {
    "space": "space_01.ogg",
    "enter": "enter_01.ogg",
    "backspace": "backspace_01.ogg"
  }
}
```

### 15.3. Bản đồ file sau v2

```
Neo_keyboard/
├── docs/SPEC-v2.md
├── tools/
│   ├── sounds/
│   │   ├── import_pack.py
│   │   └── sources/                 # file gốc, không vào APK
│   └── lexicon/
│       ├── build_lexicon.py
│       └── vi_words.txt
└── android/app/src/main/
    ├── assets/
    │   ├── sounds/index.json
    │   ├── sounds/<pack>/...
    │   └── lexicon/vi_lexicon.bin
    └── java/app/neokeyboard/
        ├── clipboard/...
        ├── sound/KeySoundPlayer.kt
        ├── sound/SoundPackRepository.kt
        ├── suggest/LexiconSuggestionEngine.kt
        ├── suggest/BundledLexicon.kt
        ├── suggest/UserLexiconStore.kt
        ├── suggest/Suggestion.kt
        ├── theme/ThemeBridgeClient.kt
        ├── theme/ThemeSnapshot.kt
        ├── ime/TextEditor.kt
        ├── ime/BackspaceRepeater.kt
        └── ... (các file cũ)
```

### 15.4. Checklist test tay nhanh (in ra dán lên bàn)

```
[ ] Cài debug, chọn IME
[ ] Settings search: gõ "ngu" → chọn "người"
[ ] Gõ "tieesng" → "tiếng"
[ ] Giữ backspace 1s → xóa liên tục
[ ] Vuốt trái backspace → xóa từ
[ ] Nhấn a, vuốt ra ngoài → không có a
[ ] Ảnh phím nghỉ/nhấn → thấy thành bên
[ ] Gõ 20 phím → không giật
[ ] Đổi ảnh nền System Zero → màu bàn phím đổi
[ ] Copy text Chrome → panel → dán
[ ] Password: không gợi ý, không clipboard
[ ] Im lặng: không âm
[ ] Xoay ngang: không cắt
[ ] TalkBack: đọc phím
[ ] Không crash trong logcat
```

### 15.5. Prompt triển khai cho model

```
Đọc toàn bộ docs/SPEC-v2.md. Làm đúng thứ tự M1 → M5.
Không tự ý đổi scope, không thêm INTERNET, không tổng hợp âm,
không đặt dải gợi ý dưới phím, không ký release bằng debug key.
Mỗi mốc: sửa tối thiểu → build → unit test → lint → cài máy thật →
chụp ảnh → đối chiếu tiêu chí nghiệm thu của mốc đó.
Nếu thiếu dữ kiện (giấy phép âm, wordlist, quyền sửa System Zero),
dừng và hỏi một câu cụ thể; không bịa nguồn hoặc bịa kết quả test.
```

---

**Hết đặc tả v2.**
