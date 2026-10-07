# 0005 — Netleştirme turu 2: diğer kararlar ve belge boşlukları

Tarih: 7 Ekim 2026  Durum: Kabul

Kullanıcı'nın 7 Ekim 2026 yanıtları ve kabul ettiği öneriler.

| # | Konu | Karar |
|---|---|---|
| 1 | Depo | Bu klasör; uzak adres `https://github.com/mehmetark444-art/toparla.git`. Şimdilik yalnız yerel commit; push Kullanıcı isteyince. |
| 2 | CI | Şimdilik yerel (`./gradlew check`). Depo GitHub'a taşınınca Actions kurulur. |
| 3 | Dil | Belgeler, commit mesajları, kod yorumları, kullanıcı metinleri Türkçe. Kod tanımlayıcıları blueprint'teki gibi İngilizce. |
| 4 | Dilim bekleme süreleri | Gerçek kullanım günü isteyen ölçütler beklenirken sonraki dilim feature flag arkasında yazılır. |
| 5 | Yedek şifreleme | Anahtar Kullanıcı'nın belirlediği paroladan türetilir (cihaza bağlı değil; sıfır telefona geri yükleme mümkün). Parola unutulursa yedek açılamaz: kurulum ekranı bunu açıkça söyler. |
| 6 | Cihaz içi model dosyası | Kullanıcı elle indirir; adımlar sıfır bilgi varsayımıyla anlatılır. Uygulama içi indirme yolu S0'da kaynağın erişim koşullarına göre kararlaştırılır `[DOĞRULA]`. |
| 7 | Kriz sözlüğü ve destek hatları | Sözlüğü ve varsayılan hat listesini ben hazırlarım, Kullanıcı inceler; onaysız sürüme girmez. |
| 8 | v3'te eksik şemalar | v3 belgesi §11.5'te bitiyor. Günün Mimarı, Haftalık Ayna, Mesaj Yazarı, Karar Daraltıcı çıktı şemalarını ilgili dilimde ben tasarlarım. |
| 9 | Hava durumu | D16'daki "hava açık" ifadesi kullanılmaz (T5: hava verisi yok). |
| 10 | Onboarding şablonları | 7. şablon "Serbest alışkanlık"tır. |
| 11 | Arama algılama | Müdahale ve odak için arama durumu izin gerektirmeyen ses modu kontrolüyle (`AudioManager.getMode`) okunur; `READ_PHONE_STATE` eklenmez. `[Spike]` |
| 12 | Eksik izinler | Ev Wi-Fi adı için gereken izinler ve ilaç ekranı için `USE_BIOMETRIC`, ilgili özellik yazılırken ayrı karar kaydıyla eklenir. |
| 13 | Eksik tablolar | Yanıt bekleyenler, Karar Kapısı, güvenilir kişi, destek hatları ilgili dilimde H bölümü kurallarıyla eklenir. |
| 14 | Tasarımı yazılmamış ekranlar | Check-in, Çıkış Kontrolü, İlaç, Bak ve Yardım Et, Karar Daraltıcı, Yanıt bekleyenler, Dopamin menüsü düzenleme: Bölüm C/D kalıplarıyla tasarlanır. |
| 15 | JDK | Gradle, Android Studio'nun JBR 21'i ile koşar; Kotlin/Java hedefi 17 (blueprint B1). Ayrı JDK kurulmaz. |

**İlgili blueprint bölümü:** A0, A7, B1, B5, B7, D1, D16, F4.6, F10, H
