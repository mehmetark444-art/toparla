# HyperOS ayar derin bağlantıları

Cihazda denenip **çalıştığı görülen** bağlantılar buraya yazılır. Her bağlantı kodda
`try/catch` ile denenir; açılmazsa uygulama bilgi sayfasına düşülür.

Ölçüm: 8 Ekim 2026, HyperOS `OS3.0.310.0.WPSMIXM`, uygulama içinden `startActivity`, kilit açık,
bağlantı başına 1–2 deneme (`:spike` `AGroup.openLink`). Ham kayıt: `platform-bulgulari.md` § Spike 4.
"Açıldı" = sistem hedefi çözdü ve hata vermedi; sayfanın doğru uygulamayı gösterdiği Kullanıcı
gözüyle ayrıca doğrulanmadı.

**10 Ekim 2026 (asıl uygulama, debug paketi):** Hatırlatma Sağlığı ve kurulum sihirbazının kullandığı yedi bağlantı
(uygulama bildirim ayarları, bildirim kanalı, tam vaktinde alarm, otomatik başlatma, pil, tam ekran bildirim, Rahatsız
Etme erişimi) adb ile aynı niyetlerle açıldı ve **ekran görüntüsüyle** doğru sayfayı, doğru uygulamayı gösterdikleri
görüldü (`platform-bulgulari.md` § F2-E kalanları). Kullanıcı'nın kendi gözüyle doğrulaması yol haritası F2.45'te.

| Ayar | Intent (paket / sınıf / eylem) | Sonuç |
|---|---|---|
| Otomatik başlatma | bileşen `com.miui.securitycenter` / `com.miui.permcenter.autostart.AutoStartManagementActivity` | Açıldı (liste sayfası; uygulamayı Kullanıcı listeden bulur) |
| Pil: Kısıtlama yok | `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` + `package:` → `com.miui.powercenter.legacypowerrank.PowerDetailActivity` | Açıldı (HyperOS'in uygulamaya özel pil sayfası) |
| Pil kısıtı (eski yol) | bileşen `com.miui.powerkeeper` / `…ui.HiddenAppsConfigActivity` | **Yok** (`ActivityNotFoundException`); kullanılmaz |
| Kilit ekranında göster / Arka planda açılır pencere | eylem `miui.intent.action.APP_PERM_EDITOR`, sınıf `com.miui.permcenter.permissions.PermissionsEditorActivity`, ek `extra_pkgname` | Açıldı ("Diğer izinler" sayfası) |
| Uygulama bilgisi (yedek hedef) | `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` + `package:` | Açıldı |
| Tam ekran bildirim özel erişimi | `Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` + `package:` | Açıldı |
| Bildirim erişimi | `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` | Açıldı |
| Erişilebilirlik | `Settings.ACTION_ACCESSIBILITY_SETTINGS` → `MiuiAccessibilitySettingsActivity` | Açıldı |
| Rahatsız Etme erişimi | `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` | Açıldı |
| Kullanım erişimi | `Settings.ACTION_USAGE_ACCESS_SETTINGS` | Açıldı |
| Uygulama bildirim ayarları | `Settings.ACTION_APP_NOTIFICATION_SETTINGS` + `EXTRA_APP_PACKAGE` | Açıldı (1 deneme; ikinci koşuda kayıt düşmedi, yeniden denenecek) |
| Bildirim kanalı (tek kanalın sayfası) | `Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS` + `EXTRA_APP_PACKAGE` + `EXTRA_CHANNEL_ID` → `SubSettings` | Açıldı (10 Ekim 2026, 1 deneme; "Önemli": Bildirimleri göster, Kayan bildirimler, Ses, Titreşim) |
| Tam vaktinde alarm | `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` + `package:` → `Settings$AlarmsAndRemindersAppActivity` | Açıldı (10 Ekim 2026; anahtar soluk: izin kurulumda verili) |
| Kısıtlı ayarlara izin ver | — | Doğrudan bağlantısı yok; yol: Uygulama bilgisi → ⋮. Bu telefonda gerekip gerekmediği F1.8'de ölçülecek |
| Gri tonlama | eylem `com.android.settings.ACCESSIBILITY_COLOR_SPACE_SETTINGS` → `Settings$AccessibilityDaltonizerSettingsActivity` | Hedef var (yalnız `resolve-activity`); açılış denenmedi |
