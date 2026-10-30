# OxygenForge Music — NOTICE

## Köken ve değişiklikler

OxygenForge Music, Kushagra Singh'in [BitChord](https://github.com/kushagrasinghx/BitChord) açık kaynak projesinden türetilmiştir. Bu yeniden markalama, orijinal proje ve katkıcılarının telif/atıf bildirimlerini kaldırmaz veya devralınan kodu yeniden lisanslamaz. `LICENSE` dosyasındaki GNU GPL v3 metni aynen korunmuştur.

Bu çalışma kapsamında uygulamanın Android üretim kimliği `com.oxygenforge.music` olarak ayrılmış, görünür ürün adı OxygenForge Music yapılmış, özgün OxygenForge müzik amblemi Android kaynaklarına eklenmiş ve Liquid Glass tercihi ilk kullanımda açık olacak şekilde ayarlanmıştır. Liquid Glass seçimi uygulama ayarlarında saklanır; bu görünümün API 31 öncesi cihazlarda GPU bulanıklığı/refraction uygulanmadığına dair kaynak içi geri dönüş korunur. Logo ve ürün adı değişiklikleri BitChord'un telif/lisans geçmişini değiştirmez.

## Değişiklik günlüğü (OxygenForge Music 1.0.0)

- Ürün kimliği `com.oxygenforge.music` olarak ayrıldı; uygulama adı, launcher etiketi, bildirim ve splash kaynakları OxygenForge Music'e taşındı. BitChord'a ait telif başlıkları, `LICENSE` ve üçüncü taraf lisans metinleri yerinde bırakıldı.
- Özgün OxygenForge amblemi üretildi ve launcher, adaptive, monochrome, önizleme ve uygulama içi kaynaklara uygulandı (`tools/generate-icons.py`). Başka bir markanın görseli kullanılmadı.
- Liquid Glass ilk kurulumda açık; tercih ayarlarda kalıcı olarak saklanıyor. API 31 öncesi cihazlarda GPU bulanıklığı/refraction uygulanmaz, arayüz donanımsız geri dönüş görünümüyle çalışır.
- Launcher simgesi değiştirme (`AppIcon.apply`) artık istisna fırlatmıyor: alias varlığı ve sonuç doğrulanıyor, değişiklik yarım kalırsa önceki simge geri yükleniyor ve sonuç uygulama genelinde çizilen gerçek Liquid Glass bilgi kartıyla (`GlassNotice.kt`) bildiriliyor. Kart, sayfanın backdrop katmanının dışında durduğu için bulanıklık/mercek efektini arkadaki ekrandan örnekleyebiliyor; hata durumunda kart kapanmıyor ve “tekrar dene” eylemi sunuyor.
- Ayarların sonundaki kaynak/atıf satırı korundu; `settings_source_code` ve `settings_derived_notice` anahtarları tüm dil dosyalarına eklendi.
- Hiçbir lisans metni, telif bildirimi veya üçüncü taraf atfı kaldırılmadı veya gizlenmedi.

## Depoda tespit edilen lisanslar

- **GNU General Public License v3.0 (GPL-3.0):** ana BitChord/OxygenForge kaynak ağacı; tam metin `LICENSE` dosyasındadır. Dağıtılan türevler GPLv3 koşullarına ve Corresponding Source yükümlülüğüne tabidir.
- **GNU Lesser General Public License v2.1-or-later (LGPL-2.1-or-later):** DST decoder; lisans metni `app/src/main/cpp/dsd/dst/COPYING.LGPL-2.1`, köken/değişiklik açıklaması `app/src/main/cpp/dsd/dst/NOTICE.md` içindedir. Decoder ayrı bir yerel paylaşımlı kütüphane olarak derlenir.
- **GNU Affero General Public License v3.0 (AGPL-3.0):** web AutoMix kodunun ilgili kısmı; metin ve kod kapsamı `web/app/src/playback/automix/LICENSE` ve `web/app/src/playback/automix/NOTICE.md` dosyalarındadır. Web üzerinden kullanımda AGPL'nin ağ üzerinden kaynak sunma koşulları ayrıca gözetilmelidir.
- **Apache-2.0, MIT, ISC, BSD-3-Clause, MPL-2.0 ve diğer üçüncü taraf lisansları:** kaynak içinde lisans başlığı bulunan uyarlanmış dosyalar ve Gradle/Maven bağımlılıkları bu lisansları içerir. Örnekler: AndroidX/Jetpack ve Media3 (Apache-2.0), Haze/backdrop kaynakları (Apache-2.0), AutoMix'te belirtilen idb (ISC), `GlassSelectionPill.kt` (MIT), Rhino (MPL-2.0), Protobuf (BSD-3-Clause). Bazı bileşenler çoklu lisanslıdır; örneğin `libadb-android` GPL-3.0/Apache-2.0 olarak Gradle yapılandırmasında belirtilmiştir.

- **MIT (bundle edilmiş sinir ağı ağırlıkları):** `app/src/main/assets/beat_this_int8.onnx` (CPJKU *Beat This!*; kod ve yayımlanan model ağırlıkları MIT) ve `app/src/main/assets/vocals_umxhq_int8.onnx` (sigsep *Open-Unmix* UMX-HQ; MIT). Bu iki dosya çıkarım (inference) için gömülüdür, eğitim verisi içermez; kaynakları ilgili projelerin depolarındadır ve değiştirilmemiştir (int8'e dönüştürülmüş sürümleri kullanılır).

Bu özet kaynakta açıkça tanımlanmış lisansları ve doğrudan bilinen bağımlılıkları kapsar; bütün transitif Gradle/Maven/Web bağımlılıkları için makine tarafından doğrulanmış, sürüm bazlı bir SBOM/lisans raporu değildir. Yayın öncesi release runtime bağımlılıklarının tam lisans dökümü, model/weights lisansları ve APK içindeki lisans bildirimleri ayrıca denetlenmelidir. Yeni paket eklenmedi ve üçüncü taraf lisans metinleri kaldırılmadı.

## GPLv3 dağıtım notu

Kaynak dağıtımında `LICENSE`, ilgili kaynak dosyalarındaki mevcut telif/atıf başlıkları, üçüncü taraf alt dizin lisansları ve bu NOTICE birlikte sunulmalıdır. İkili dağıtım yapan yayıncı, dağıtılan tam kaynak kodunu aynı sürüm için erişilebilir tutmalı ve GPLv3'ün ilgili kaynak, lisans bildirimi ve kurulum bilgisi şartlarını dağıtım biçimine göre karşılamalıdır. Google Play'e yükleme GPL uyumluluğunu kendiliğinden sağlamaz; uygulama/mağaza hesabı sahibi yükümlülüklerden sorumludur.

OxygenForge Music adı, logosu ve özgün simge tasarımı bu türevin markasıdır; orijinal yazarın onayı veya ortaklık anlamına gelmez. BitChord adı yalnızca köken, atıf, paket içi eski uyumluluk adları ve tarihsel lisans bildirimleri için korunur.
