# WiFi Guardian (Android 10+)

Aplikasi Android Kotlin + Jetpack Compose untuk memantau koneksi Wi-Fi dan memberi peringatan saat koneksi terputus atau internet tidak tervalidasi.

## Batasan penting Android 10+
Aplikasi pihak ketiga dengan target Android 10+ tidak dapat mematikan/menyalakan Wi-Fi secara langsung melalui `WifiManager.setWifiEnabled()`. Karena itu, fitur Auto Disconnect pada project ini diwujudkan secara aman sebagai:
- mendeteksi gangguan koneksi,
- menampilkan notifikasi,
- menyediakan akses ke panel konektivitas Android agar pengguna dapat mematikan Wi-Fi.

Aplikasi tidak memaksa perpindahan jaringan dan tidak mengubah konfigurasi Wi-Fi perangkat.

## Fitur versi 1.0
- Dashboard dark premium.
- Status Wi-Fi dan validasi internet.
- Pemantauan real-time ketika aplikasi terbuka.
- Notifikasi saat jaringan hilang/tidak tervalidasi.
- Pemeriksaan latar belakang periodik melalui WorkManager (interval minimum Android sekitar 15 menit; bukan real-time).
- Tombol membuka panel konektivitas Android.

## Build APK hanya dengan HP + GitHub
1. Buat repository baru di GitHub, misalnya `wifi-guardian`.
2. Ekstrak ZIP project ini.
3. Upload seluruh isi folder project ke root repository (pastikan folder `.github/workflows` ikut terunggah).
4. Buka tab **Actions** pada repository.
5. Pilih workflow **Build WiFi Guardian APK** lalu tekan **Run workflow**. Bisa juga otomatis berjalan setelah push ke branch `main`.
6. Tunggu proses selesai dengan tanda hijau.
7. Buka run yang berhasil, bagian **Artifacts**, lalu unduh `WiFiGuardian-debug-apk`.
8. Ekstrak ZIP artifact dan instal `app-debug.apk` di HP. Izinkan instalasi dari sumber tersebut bila Android memintanya.

## Catatan teknis
- `minSdk = 29` (Android 10).
- `targetSdk = 35`.
- Build memakai JDK 17 dan Android Gradle Plugin 8.7.3.
- Untuk rilis publik, buat signing key dan konfigurasi signing release. APK debug cocok untuk uji coba pribadi.
- Notifikasi harus diizinkan pada Android 13+.
- Beberapa produsen membatasi proses latar belakang; WorkManager bukan pemantauan kontinu.

## Struktur
```text
WiFiGuardian/
├── .github/workflows/build-apk.yml
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/id/wifiguardian/app/
│           ├── MainActivity.kt
│           └── NetworkCheckWorker.kt
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```
