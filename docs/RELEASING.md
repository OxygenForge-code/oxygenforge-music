# OxygenForge Music — Derleme ve yayınlama

## Araç zinciri ve paket bilgileri

- Gradle Wrapper: 8.11.1
- Android Gradle Plugin: 8.10.1
- Kotlin: 2.3.20
- Uygulama `compileSdk`: 37, `targetSdk`: 36, `minSdk`: 26
- Telefon uygulama kimliği: `com.oxygenforge.music`; geliştirme kimliği: `com.oxygenforge.music.dev`
- İlk OxygenForge sürümü: version code `1`, version name `1.0.0`
- JDK: 17; Android SDK 37/build-tools/CMake/NDK proje gereksinimlerine uygun kurulmalıdır.

SDK yolunu proje kökündeki `local.properties` içinde yerel olarak ayarlayın. Bu dosya Git tarafından izlenmez. Windows örneği: `sdk.dir=E:/Android/sdk`.

## Debug derlemesi

```powershell
.\gradlew.bat :app:assembleProdDebug
```

Debug APK Android'in otomatik debug anahtarıyla imzalanır; yalnızca test/yan yükleme içindir, Google Play release imzası değildir. Çıktı normalde `app/build/outputs/apk/prod/debug/` altındadır. Gradle'ın gerçek çıktısını kontrol edin; yolun oluştuğunu varsaymayın.

## Keystore olmadan release doğrulama

```powershell
.\gradlew.bat :app:assembleProdRelease :app:bundleProdRelease
```

`keystore.properties` ve belirtilen gerçek keystore yoksa proje yapılandırması release APK'yı unsigned üretmek üzere düzenlenmiştir. Böyle bir APK Google Play'e yüklenemez ve imzalanmış release olarak sunulmamalıdır. `bundleProdRelease` çıktısının unsigned/Play kabul durumu Gradle/Play doğrulamasıyla ayrıca kontrol edilmelidir; imzasız AAB'yi yayınlanabilir kabul etmeyin.

## Gerçek yerel upload/release anahtarıyla imzalama

1. Play Console uygulaması için önce Google Play App Signing'e kaydolun. Yerel anahtar upload key olabilir; Google Play, kullanıcılara teslim edilen APK'ları Play App Signing anahtarıyla yeniden imzalar. Upload key, uygulama imzalama anahtarıyla aynı şey olmak zorunda değildir.
2. Gerçek anahtar sahibi tarafından `keystore.properties.example` kopyalanıp `keystore.properties` olarak doldurulur; storeFile, keyAlias, storePassword, keyPassword ve keystore dosyası asla sohbete, kaynak koda, Git'e veya loglara eklenmez. Keystore yolu proje köküne göre olmalıdır.
3. Özel Windows makinesinde `keytool -list -v -keystore <yerel-keystore-yolu>` ile alias/parmak izini doğrulayın; parolayı komut argümanı veya log olarak açığa çıkarmayın.
4. Sonra `:app:bundleProdRelease` ve istenirse `:app:assembleProdRelease` çalıştırın. AAB, Google Play'e gönderilecek esas artifact'tır. AAB doğrudan cihaza kurulmaz; Play tarafından APK setlerine dönüştürülür.
5. İlk üretimden sonra upload key ve Play App Signing yapılandırmasını güvenli çevrimdışı yedekleyin. Anahtar kaybı/rotasyonu için Google Play Console prosedürleri izlenir.

Bu depoda keystore veya parola saklanmaz. İmzalı derleme yapılan makinede
`keystore.properties` yerelde oluşturulmuştur (Git tarafından yok sayılır) ve
keystore dosyası proje dışında tutulmuştur. İmzalı APK/AAB çıktıları ve imza
parmak izi `docs/BUILD_REPORT.md` içinde listelenmiştir.

## GitHub'a yayınlama (F-Droid kaynağı)

F-Droid, APK'yı kendi derleme sunucusunda kaynak koddan üretir ve kendi
anahtarıyla imzalar. Bu yüzden yayına giren şey imzalı APK değil, **kaynak
kodu ve sürüm etiketi**dir.

1. Depo herkese açık olmalıdır; `.gitignore` keystore, `keystore.properties`,
   `local.properties`, APK/AAB ve log dosyalarını zaten dışarıda bırakır.
   Yüklemeden önce `git status --ignored` ile bunları doğrulayın.
2. Sürümü etiketleyin; F-Droid `v1.0.0` biçimindeki etiketleri `versionName`
   ile eşleştirir:

   ```bash
   git tag -a v1.0.0 -m "OxygenForge Music 1.0.0"
   git push origin main --follow-tags
   ```

3. F-Droid'e ekleme, [fdroiddata](https://gitlab.com/fdroid/fdroiddata) deposuna
   bir `metadata/com.oxygenforge.music.yml` merge request'i ile yapılır. Gerekli
   alanlar: `Repo`, `License: GPL-3.0-only`, `SourceCode`, en az bir `Builds`
   girdisi (`versionName`/`versionCode` ve `gradle: [assembleProdRelease]`).
   Sürüm kodları APK'nın kendi manifestinden okunur; her yeni sürümde
   `versionCode` artırılmalıdır.
4. F-Droid anti-feature bildirimleri elle işaretlenir. Bu uygulamada hesap
   girişi ve çevrimiçi akış isteğe bağlıdır; ağ izni olmadan yerel oynatma
   çalışır. Ücretli/özel bir bileşen eklenirse `NonFreeDep`/`NonFreeNet`
   işaretlenmelidir.
5. NDK/CMake ile derlenen yerel kütüphaneler (DST decoder, LGPL-2.1-or-later)
   F-Droid derleyicisinde de derlenir; önceden derlenmiş `.so` dosyası
   yüklenmez. `app/src/main/cpp/CMakeLists.txt` dışa bağımlılık içermediği
   sürece ek yapılandırma gerekmez.

## Kontroller

```powershell
.\gradlew.bat :app:testProdDebugUnitTest :app:lintProdDebug
.\gradlew.bat :app:bundleProdRelease
```

`lintProdDebug` şu anda bu kod tabanında başarısız oluyor: 220 hata (ağırlıklı
olarak `UnsafeOptInUsageError`, `LocalContextGetResourceValueCall`,
`MissingQuantity`). Bu hatalar yeniden markalama değişikliklerinden değil, mevcut
koddan gelir; baseline eklenmedi ve kurallar gevşetilmedi. Ayrıntı:
`docs/BUILD_REPORT.md`.

Gerçek artefaktlar için Windows'ta `Get-FileHash <dosya> -Algorithm SHA256`, Android SDK `aapt dump badging <apk>` ve `<sdk>/build-tools/<sürüm>/apksigner verify --print-certs <apk>` kullanın. APK üzerinde package/version ve imza bilgilerini doğrulayın; AAB için `jarsigner -verify -verbose -certs <aab>` ve `bundletool dump manifest --bundle <aab> --module base` kullanılabilir. Emülatör/device playback, arka plan servisi ve izin akışları ayrıca gerçek cihaz/emülatörde doğrulanmalıdır.

Bu repository çıktısında yalnızca doğrulanan dosyalar raporlanır. Derleme zaman aşımına uğrarsa veya hata verirse hiçbir APK/AAB'nin üretildiği ileri sürülmez. Yapılandırılmış target API, o andaki Play Console şartlarının tek başına garantisi değildir; Play Console güncel target API politikasını yayın tarihinde kontrol edin.
