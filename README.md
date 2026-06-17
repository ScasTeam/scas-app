# SCAS App (Aplikasi Android Native)

Repositori ini berisi aplikasi klien seluler (Android) untuk Smart Campus Attendance System (SCAS). Aplikasi ini difokuskan bagi mahasiswa untuk melakukan presensi dengan menghasilkan QR Code batch secara dinamis serta verifikasi perangkat dengan OTP.

## Tech Stack
- **Platform:** Android Native
- **Bahasa:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Arsitektur:** MVVM (Model-View-ViewModel) dengan Dependency Injection
- **Minimum SDK:** 24 (Android 7.0)
- **Target SDK:** 36

## Daftar Library / Dependencies
- **Dependency Injection:** Dagger Hilt
- **Networking:** Retrofit2 & OkHttp (dengan Logging Interceptor)
- **State/Cache Persistence:** Jetpack DataStore Preferences
- **Google Sign-In:** Androidx Credentials API & Play Services Auth
- **Navigasi UI:** Jetpack Navigation Compose (`androidx.navigation.compose`, `hilt-navigation-compose`)
- **QR Code Generator:** ZXing Core
- **Keamanan:** Hardened window (menggunakan `FLAG_SECURE` untuk memblokir tangkapan layar)

## Cara Menjalankan Secara Lokal

> **Catatan Penting:** Agar aplikasi dapat berfungsi dengan baik, pastikan backend **scas-api** sudah berjalan di latar belakang.

1. Pastikan Anda telah menginstal **Android Studio** versi terbaru.
2. Buka Android Studio, lalu pilih **Open** dan arahkan ke direktori `scas-app`.
3. Tunggu hingga proses *Gradle Sync* selesai.
4. Buat file `.env` di root direktori `scas-app` (sejajar dengan `build.gradle.kts` tingkat project) untuk environment variables lokal:
   ```env
   WEB_CLIENT_ID=your_google_web_client_id
   API_BASE_URL=http://your_local_ip:8000/api/
   ```
   *(Catatan: Anda dapat menggunakan IP lokal jaringan Anda, misal `192.168.x.x`. Jika Anda mengatur URL ke `localhost` atau `127.0.0.1` dan menjalankannya di Android Emulator, pastikan Anda menjalankan perintah `adb reverse tcp:8000 tcp:8000` di terminal Anda agar emulator dapat mengakses server lokal).*
5. Pilih target perangkat keras (Emulator atau Device Fisik yang terhubung via USB/Wireless Debugging).
6. Klik tombol **Run 'app'** (ikon panah hijau) atau gunakan kombinasi `Shift + F10`.
