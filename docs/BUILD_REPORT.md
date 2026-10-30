# OxygenForge Music — Derleme ve test raporu

Bu rapor yalnızca gerçekten çalıştırılan komutların çıktısına dayanır. Üretilmeyen
veya doğrulanmayan hiçbir çıktı "tamamlandı" olarak işaretlenmemiştir.

## Araç zinciri ve ortam

| Bileşen | Değer |
| --- | --- |
| Gradle Wrapper | 8.11.1 |
| Android Gradle Plugin | 8.10.1 |
| Kotlin | 2.3.20 |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 |
| JDK | 17 (Temurin 17.0.20.1+1, proje dışı geçici dizinden kullanıldı) |
| Android SDK | `E:\gercek_android_sdk` (build-tools 35.0.0 ve 36.0.0, platform android-37) |
| Uygulama kimliği | `com.oxygenforge.music` (dev: `com.oxygenforge.music.dev`) |
| Sürüm | versionCode 1, versionName 1.0.0 |
| Ana makine | Windows, JDK 17 + Android SDK yol ayarı `local.properties` (Git dışı) |

`E:\Android sdk` yolu bu makinede Android Studio kurulum dizinidir (içinde `jbr`
vardır) ve gerçek SDK platformlarını içermez. Derlemeler bu nedenle
`E:\gercek_android_sdk` ile yapıldı.

## Derleme sonuçları

| Komut | Sonuç |
| --- | --- |
| `:app:assembleProdDebug` | **BAŞARILI** (son çalışma 2 dk 6 sn; metin düzeltmelerinden sonraki yeniden derleme 6 dk 55 sn) |
| `:app:bundleProdRelease` (imzasız) | **BAŞARILI** (24 dk 52 sn, 321 görev) |
| `:app:assembleProdRelease :app:bundleProdRelease` (sağlanan keystore ile imzalı) | **BAŞARILI** (6 dk 21 sn, 199 görev) |
| `:app:testProdDebugUnitTest` | Tam koşu: **1102 test, 17 başarısız, 1 atlandı** (bkz. aşağıdaki ayrıntı) |
| `:app:testProdDebugUnitTest --tests LocaleStringsTest` | **BAŞARILI** (16 test, 0 başarısız) |
| `:app:lintProdDebug` | **BAŞARISIZ** — 220 hata, 616 uyarı, 5 öneri (bkz. Lint bölümü) |

## Üretilen dosyalar

### Debug APK (yalnızca test / yan yükleme)

Kökteki `app/build/outputs/apk/prod/debug/` dizini:

| Dosya | Boyut (bayt) | SHA-256 |
| --- | --- | --- |
| `app-prod-universal-debug.apk` | 205 482 895 | `ae107219d1eec606456f831f6a0a9e98308e0ab75a4beca8f76b7ba23b97a363` |
| `app-prod-arm64-v8a-debug.apk` | 101 550 249 | `d4920be37c832d1f47bdf6cfa86fa5e4c6d592d05b26d2c78177bcfe31780ea5` |
| `app-prod-armeabi-v7a-debug.apk` | 91 704 181 | `6e335471906bfc29c6a78085847629777f85ac1786aa6f133f69a4ebf10bb3fd` |
| `app-prod-x86_64-debug.apk` | 108 143 523 | `445c07321a7fe3d369c9b47218032a15497252259d49284b20e2cc63a1e3ec78` |

`aapt dump badging` doğrulaması: `package: name='com.oxygenforge.music'
versionCode='1' versionName='1.0.0'`, `sdkVersion:'26'`, `targetSdkVersion:'36'`,
`application-label:'OxygenForge Music'`, `native-code: 'arm64-v8a' 'armeabi-v7a'
'x86' 'x86_64'`.

`apksigner verify --print-certs` doğrulaması: imza **geçerli**, imzalayan
`CN=Android Debug, O=Android, C=US` (sertifika SHA-256
`b35534162ce42ff9e1b37471675822d569d7a325fbf144549ac7bf996ab92242`), APK Signature
Scheme v2. **Bu Android'in otomatik debug anahtarıdır; release veya Google Play
imzası değildir.**

### İmzalı release APK

`app/build/outputs/apk/prod/release/` dizini:

| Dosya | Boyut (bayt) | SHA-256 |
| --- | --- | --- |
| `app-prod-universal-release.apk` | 171 315 642 | `b8c2e3710ae96d37372b49e01f3bf3b25128e253fcc0723fe7673819f566a3bb` |
| `app-prod-arm64-v8a-release.apk` | 67 546 836 | `173aee4da124b767a331ac223af4af00e42c202d00787231b0ff700758cf70b6` |
| `app-prod-armeabi-v7a-release.apk` | 57 733 536 | `dbfc013b5f220fd52875babe9e4d6d0812b12f2bcce74367005bd32d4a7c21f6` |
| `app-prod-x86_64-release.apk` | 74 140 110 | `abaf49837c84253865ed5a756f47681c06c69e8be9e880a7c01db0fa79ba3197` |

`apksigner verify --print-certs` → imza **geçerli**, APK Signature Scheme v2,
imzalayan: `C=Tr, ST=Ankara, L=Ankara, O=MericDeveloper, CN=Meric Efe
Şeribaşı`, sertifika SHA-256
`06b72c64178143801c29b6579f55520a90fea8501a94c90334dca2c3c3e67e9c`, RSA 2048.
Bu sertifika `keytool -list` çıktısıyla birebir aynıdır.

### Google Play AAB (imzalı)

| Dosya | Boyut (bayt) | SHA-256 |
| --- | --- | --- |
| `app/build/outputs/bundle/prodRelease/app-prod-release.aab` | 121 288 117 | `7fd1b6313f450fde100f6364789452e925c873b82d3272c73e0fa81a62dabfc6` |

Doğrulamalar:

- `jarsigner -verify -certs` → **exit 0**, imzalı; imzalayan aynı sertifika
  (`C=Tr, ..., CN=Meric Efe Şeribaşı`), `SHA256withRSA` 2048-bit. `jarsigner`
  imzalı bir JAR için "jar verified" satırı basmadı; doğrulama girişleri ve imza
  sahibi bilgisi çıktıda göründü ve çıkış kodu 0'dı. Sertifika kendinden imzalı
  olduğu için "Invalid certificate chain: PKIX path building failed" uyarısı
  normaldir ve imza geçerliliğini etkilemez.
- AAB içindeki `base/manifest/AndroidManifest.xml` içinde `com.oxygenforge.music`,
  `com.oxygenforge.music.fileprovider`, `com.oxygenforge.music.androidx` ve
  `1.0.0` dizeleri bulundu; paket adı ve sürüm adı doğrulandı.
- AAB 1628 giriş, `base/dex/` altında 3 dex, `BUNDLE-METADATA/` içeriyor.
- Birleştirilmiş manifest (`app/build/intermediates/merged_manifest/prodRelease/
  processProdReleaseMainManifest/AndroidManifest.xml`): `package="com.oxygenforge.music"`,
  `android:versionCode="1"`, `android:versionName="1.0.0"`.

**AAB, sağlanan keystore ile imzalanmıştır.** Play Console'a yüklenebilir; yükleme
sırasında Play, App Signing açıksa dağıtım imzasını kendi anahtarıyla atar.

> Aynı dizinde daha önce üretilen **imzasız** AAB (121 190 561 bayt,
> `a1dd7db172af803612384166912a68082c1da38790e22e44dbb4631b422c505d`) imzalı
> derleme tarafından geçersiz kılınmıştır; güncel dosya yukarıdaki imzalı dosyadır.

## Test ayrıntıları

Tam birim test koşusu (`:app:testProdDebugUnitTest`):

- 1102 test çalıştı, 17 başarısız, 1 atlandı.
- 16 başarısızlık `LocaleStringsTest.strings_containsAllBaseKeys[...]` idi ve tek
  nedeni yeni eklenen `liquid_glass_fallback_subtitle` anahtarının 18 yerelleştirme
  dosyasında bulunmamasıydı. Anahtar tüm dillere eklendi.
- Düzeltmeden sonra yalnızca bu test sınıfı yeniden koşuldu: **16 test, 0 başarısız**.
- Kalan 1 başarısızlık
  `QueueShuffleTierTest > benchmark toggle shuffle and restore performance on 1000
  items`: `AssertionError: Restore should complete in < 50ms`. Bu bir duvar saati
  performans eşiğidir ve yüklü makinede (paralel Gradle daemon'ları çalışırken)
  aşılmıştır. Eşik gevşetilmedi; testin kendisi değiştirilmedi. Tam paket, düzeltme
  sonrası yeniden koşulmadığı için bu testin düzeltme sonrası durumu doğrulanamadı.

Doğrulanamayanlar:

- Emülatör/gerçek cihaz testi yapılmadı (bu makinede çalışan bir emülatör/cihaz
  hazırlanmadı). Açılış, oynatma/durdurma, listeleme, ayarlar, arka planda oynatma
  ve API 31 öncesi Liquid Glass geri dönüşü **çalışır durumda gözlemlenmedi**.
- Performans ve pil tüketimi ölçülmedi.
- GPL karşılık gelen kaynak (Corresponding Source) yayını yapılmadı.

## Lint

`:app:lintProdDebug` 9 dk 23 sn sürdü ve **başarısız** oldu: 220 hata, 616 uyarı,
5 öneri. Hataların dağılımı:

| Kural | Hata sayısı |
| --- | --- |
| `UnsafeOptInUsageError` | 112 |
| `LocalContextGetResourceValueCall` | 66 |
| `MissingQuantity` | 28 |
| `StateFlowValueCalledInComposition` | 10 |
| `NewApi` | 1 |
| `PropertyEscape` | 1 |
| `FlowOperatorInvokedInComposition` | 1 |
| `StringFormatInvalid` | 1 |

Hatalar ağırlıklı olarak bu değişiklikte dokunulmayan dosyalarda toplanıyor
(`MainActivity.kt`, `SmbDataSource.kt`, `values-he/strings.xml`, `DspChain.kt`,
`FloatAudioProcessor.kt`, `Downloads.kt`, `TransitionFilterProcessor.kt` vb.) ve
`RuntimeShaderCache.kt:24` gibi kaynakta koşullu API kullanımına ilişkin mevcut
kural ihlallerinden geliyor. Liquid Glass varsayılanı, ayar anahtarı, yeni
çeviri metni veya yeniden markalama kaynaklı bir lint hatası **bulunmadı**
(`liquid`/çeviri ile eşleşen hata yok).

Bu 220 hata giderilmedi: bu, istenen yeniden markalama işinin kapsamı dışında ve
ayrı bir temizlik çalışması gerektiriyor. Lint bu nedenle **geçmedi** olarak
raporlanır; baseline eklenmedi ve kurallar gevşetilmedi.

## İmzalama durumu

- İmzalama, kullanıcının `C:\Users\oxyge\OneDrive\Desktop\önemli\imzalama\
geniskapibrowser` PKCS#12 keystore'u ile yapıldı (alias `key0`, RSA 2048).
  Keystore proje dışında kaldı; kopyalanmadı.
- Parola, kullanıcının `...\imzalama\parola.txt` dosyasından okunup Git tarafından
  yok sayılan `keystore.properties` içine yazıldı. Parola değeri hiçbir komut
  çıktısında, logda veya rapor metninde gösterilmedi.
- `keystore.properties` `.gitignore:34` ile yok sayılıyor; `git status` listesinde
  görünmüyor. Keystore ve parola dosyası depo dışında.
- `keystore.properties.example` yalnızca şablondur, değerleri boştur.
- **Not:** `parola.txt` kullanımdan sonra silinmeli veya güvenli bir kasaya
  taşınmalıdır; düz metin parola OneDrive klasöründe duruyor.
- İmzalı APK/AAB **upload key** ile imzalandı. Google Play App Signing açıksa
  Play, kullanıcılara dağıtılan APK'ları kendi app signing anahtarıyla yeniden
  imzalar; bu durumda yüklenen AAB'nin upload key ile imzalanmış olması doğrudur.
  App Signing kapalıysa bu anahtar doğrudan uygulama imzalama anahtarıdır ve
  kaybı uygulamanın güncellenemez hâle gelmesi demektir.

## Bilinen engeller

1. İmzalı APK ve AAB üretildi. Ancak Play Console'a yükleme ve App Signing
   tercihi (bu anahtar upload key mi, app signing key mi) kullanıcı tarafından
   yapılmalı; ayrıca gizlilik politikası URL'si ve GPLv3 karşılık gelen kaynak
   kodu URL'si sağlanmalı.
2. Keystore + parola yedeği alınmalı; bu anahtar kaybolursa mevcut kurulumlar
   güncellenemez.
3. `QueueShuffleTierTest` performans eşiği bu makinede aşıldı; sonuç ortam yüküne
   bağlı olarak yorumlanmalı.
4. TV modülü (`tv/`) bu yeniden markalamada güncellenmedi: kimliği hâlâ
   `com.music.bitchord.tv` ve adı "BitChord TV". Telefon uygulaması ve AAB bu
   modülden etkilenmez, ancak TV APK'sı ayrı olarak yeniden markalanmalıdır.
