# F-Droid başvurusu

Bu klasör, OxygenForge Music'in F-Droid'e kabulü için gereken dosyayı içerir.
F-Droid uygulamayı **kendi derleme sunucusunda, kaynaktan** üretir ve **kendi
anahtarıyla** imzalar; GitHub'a yüklediğimiz APK'lar bu sürece girmez.

## Dosyalar

| Dosya | Ne işe yarar |
|---|---|
| `metadata/com.oxygenforge.music.yml` | F-Droid'in istediği tarif (repo, sürüm, derleme komutu, NDK, anti-feature'lar, mağaza metni) |

## Adım adım

1. [GitLab](https://gitlab.com) hesabıyla `https://gitlab.com/fdroid/fdroiddata`
   deposunu çatallayın.
2. Çatalınızı klonlayın ve bu dosyayı tam yola kopyalayın:

   ```bash
   git clone https://gitlab.com/<kullanıcı>/fdroiddata.git
   cd fdroiddata
   cp /yol/oxygenforge-music/fdroid/metadata/com.oxygenforge.music.yml \
      metadata/com.oxygenforge.music.yml
   git checkout -b oxygenforge-music
   git add metadata/com.oxygenforge.music.yml
   git commit -m "New app: OxygenForge Music"
   git push origin oxygenforge-music
   ```

3. GitLab'da merge request açın. Açıklamaya şunları yazın:
   - Uygulama `com.oxygenforge.music`, GPL-3.0, kaynak GitHub'da herkese açık.
   - BitChord'un türevi olduğu ve değişikliklerin `NOTICE.md`'de listelendiği.
   - `NonFreeNet`: akış YouTube'un ücretsiz olmayan servisine bağlı.
   - `NonFreeDep`: Cast ve Wear OS entegrasyonu Google Play services kullanıyor.
     Bu iki kütüphane olmadan da uygulama açılır; yerel oynatma çalışır.
4. F-Droid gönüllüleri `fdroid lint` ve bir deneme derlemesi çalıştırır. Derleme
   bizim makinemizde doğrulandı: Gradle 8.11.1, AGP 8.10.1, JDK 17+, SDK 37,
   NDK 27.0.12077973, CMake 3.22.1.
5. Kabul edildikten sonra sürümler `UpdateCheckMode: Tags` ile otomatik bulunur:
   her yeni sürümde `versionCode` artırıp `vX.Y.Z` etiketi push etmek yeterlidir.

## Gözden geçirenin soracağı üç şey

1. **Depodaki iki `.onnx` dosyası ne?** `app/src/main/assets/beat_this_int8.onnx`
   (CPJKU *Beat This!*, ağırlıklar MIT) ve `vocals_umxhq_int8.onnx` (sigsep
   *Open-Unmix* UMX-HQ, MIT). İkisi de yalnızca çıkarım içindir, eğitim verisi
   içermez ve kaynakları kendi depolarındadır; ayrıntı `NOTICE.md`'de.
2. **Neden `NonFreeDep`?** Cast (`play-services-cast-framework`) ve Wear OS
   (`play-services-wearable`) entegrasyonu Google Play services kullanıyor.
   Bu iki kütüphane olmadan uygulama açılır, yerel oynatma çalışır.
3. **Neden `NonFreeNet`?** Akış kaynağı YouTube'un ücretsiz olmayan servisi.

## Bilinmesi gerekenler

- F-Droid imzası ile bu depodaki APK imzası farklıdır; kullanıcı birinden
  diğerine geçerken uygulamayı kaldırıp yeniden kurmak zorunda kalır.
- Depo `v1.0.0` etiketini taşıyor; tarif o etikete sabitlendi.
- `keystore.properties`, `local.properties` ve `*.jks` Git'te yok, dolayısıyla
  F-Droid'in derleme makinesinde imzalama devre dışı kalır ve imzasız APK üretilir
  (`-unsigned` ekli dosya adı). F-Droid bu dosyayı kendi imzasıyla imzalar.
