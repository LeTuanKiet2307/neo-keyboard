# Neo Keyboard

Bàn phím Android gõ tiếng Việt (Telex), chạy offline hoàn toàn — app không xin quyền INTERNET,
mọi thứ bạn gõ ở lại trên máy.

## Tính năng

- **Telex** đặt dấu kiểu UniKey (`suarw` → sửa, `nguoiwf` → người, `tieesng` → tiếng), gõ thẳng vào ô
  nhập, **không gạch chân** từ đang gõ (bật lại được trong Nâng cao nếu app nào lỗi).
- **Gợi ý từ** từ danh sách ~3.400 từ đơn + học từ bạn gõ (chỉ lưu trên máy, không học trong ô mật khẩu).
- **Tự viết hoa đầu câu**, **hai lần dấu cách = dấu chấm**.
- **Phím ⚡ nhập nhanh**: lưu cụm từ hay gõ hoặc mật khẩu hay dùng (mã hóa AES-GCM bằng Android
  Keystore); chạm để nhập, giữ để chọn mục.
- **Phản hồi nhanh**: xóa không chờ app (KEYCODE_DEL một chiều), giữ xóa 300 ms rồi 50→25 ms; âm phím
  kiểu switch linear tự tổng hợp; rung có công tắc riêng (vẫn rung khi rung chạm của hệ thống tắt).
- **Keycap 3D**, khe phím hẹp nhưng vùng chạm phủ cả khe (chạm vào khe vẫn ra phím gần nhất).
- **Bộ nhớ tạm** có lịch sử (không lưu từ ô mật khẩu).
- **Màu**: theo [System Zero](#cầu-màu-system-zero), Material You của hệ thống, hoặc màu tự chọn.

## Cài đặt

Tải APK ở mục [Releases](../../releases), cài, rồi mở app **Neo Keyboard** → làm theo hướng dẫn bật
bàn phím trong Cài đặt nhập liệu và chọn Neo Keyboard. Cần Android 10 (API 29) trở lên.

## Build

Cần JDK 21 và Android SDK 35.

```bash
cd android
./gradlew :app:testDebugUnitTest :app:assembleRelease
```

APK ra ở `android/app/build/outputs/apk/release/`. Không có `android/keystore.properties` thì bản
release được ký bằng khóa debug của máy build (chỉ để dùng cá nhân). Muốn ký khóa riêng, tạo
`android/keystore.properties` với `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.

Âm phím được tổng hợp bằng toán học: `python3 tools/sounds/build_cream_sounds.py`.
Danh sách từ: `python3 tools/lexicon/build_lexicon.py`.

## Cầu màu System Zero

Nếu máy có launcher System Zero hỗ trợ cầu màu, bàn phím đọc bảng màu của nó qua content provider
`content://app.systemzero.theme/snapshot` và đổi màu ngay khi System Zero đổi. Không có System Zero thì
dùng màu hệ thống. Trạng thái và độ trễ cầu xem ở Cài đặt → Nâng cao.

## Cấu trúc

| Thư mục | Nội dung |
|---|---|
| `android/app/src/main/java/app/neokeyboard/telex` | Engine Telex thuần Kotlin |
| `…/ime` | Service IME, controller, gõ thẳng không composing |
| `…/ui` | Giao diện Compose: bàn phím, cài đặt, design token |
| `…/suggest` | Gợi ý từ + học từ |
| `…/trigger` | Phím ⚡ và lưu bí mật bằng Keystore |
| `…/theme` | Client cầu màu System Zero |
| `docs/SPEC-v2.md` | Đặc tả thiết kế |
| `tools/` | Script tổng hợp âm, dựng danh sách từ |

## Giấy phép

Mã nguồn: **GPL-2.0-or-later** ([LICENSE](LICENSE)).

Danh sách từ `android/app/src/main/assets/lexicon/vi_lexicon.bin` dựng từ
[duyet/vietnamese-wordlist](https://github.com/duyet/vietnamese-wordlist) (từ điển Hồ Ngọc Đức),
giấy phép GPL-2.0 — xem [LICENSE.md của nó](android/app/src/main/assets/lexicon/LICENSE.md). Vì vậy APK
phát hành (gồm cả danh sách từ) được phân phối theo GPL-2.0.

Font Nunito: SIL Open Font License 1.1 ([licenses/OFL-Nunito.txt](licenses/OFL-Nunito.txt)).
