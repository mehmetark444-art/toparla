# 0015 — Kullanıcı'nın gördüğü her tasarım `mobile-app-ui-design` yeteneğiyle yapılır

Tarih: 9 Ekim 2026  Durum: Kabul (Kullanıcı kararı)

**Bağlam:** Blueprint Bölüm C ayrıntılı bir tasarım sistemi tanımlıyor (renk jetonları, tipografi, bileşenler),
ama tasarımın **nasıl** yapılacağına ve kalitenin nasıl sınanacağına dair bir yöntem içermiyor. Kullanıcı:
"Benim göreceğim tüm UI/UX tasarımlar bu skill ile yapılacaktır… tüm tasarım çok iyi olmalıdır. Google Stitch
tasarım MCP kullanmak daha iyi sonuç verirse kombine halde de yapılabilir."

**Karar:**
1. `.claude/skills/mobile-app-ui-design/` projeye eklendi (kaynak ve inceleme notu: aynı klasörde `KAYNAK.md`).
   Kullanıcı'nın göreceği her yüzey bu yeteneğin 5 adımıyla tasarlanır.
2. **Önce göster, sonra kodla:** her ekran ya da ekran grubu önce görsel taslak olarak hazırlanır, Kullanıcı
   onaylar, sonra Compose ile yazılır, sonunda telefondaki görüntü taslakla karşılaştırılır. Onaysız ekran kodlanmaz.
3. Toparla'nın DEHB'ye özgü ve erişilebilirlik kuralları **sınırdır**, yetenek bunları gevşetemez: seri sayacı
   yok, kırmızı yalnız kriz/kritikte, geciken iş "Taşınan", sakin kutlama, ≥ 48 dp hedef, ≥ 4,5:1 kontrast,
   "Animasyonları azalt", utandırmayan dil, paylaşım ve bağımlılık artırıcı kalıp yok. Tam çizelge:
   `.claude/rules/tasarim.md`.
4. Görsel dilin ayrıntılarında (palet, yazı ölçeği, köşe, gölge) başlangıç blueprint Bölüm C'dir; yetenek daha
   iyi sonuç veriyorsa iki seçenek taslakta yan yana gösterilir ve Kullanıcı seçer. Blueprint'ten sapan seçim
   ayrı karar kaydına yazılır.
5. Ürün kodu Jetpack Compose + Material 3'tür; yetenekteki React / Tailwind notları yalnız taslak içindir.
6. **Google Stitch:** bu oturumdaki bağlayıcı dizininde Stitch yok (9 Ekim 2026'da arandı). İlk ekran grubu
   yalnız yetenekle taslaklanır; Kullanıcı sonucu yeterince iyi bulmazsa Stitch'in kurulumu ayrıca ele alınır.
   Kullanılırsa: Stitch'e yalnız uydurma örnek içerikli ekran tarifleri gider; kişisel veri gitmez.

**Alternatifler:** Yalnız blueprint Bölüm C ile doğrudan kodlamak: Kullanıcı ekranı ancak telefonda görür,
beğenmezse yeniden yazılır. Yeteneği sınırsız uygulamak: seri, kırmızı ve parlak kutlama gibi kalıplar
blueprint'in DEHB'ye dair temel kararlarını (K12, A5) bozar.

**Sonuçlar ve riskler:**
- Her ekran grubuna bir taslak ve onay adımı eklenir: F2-C'den itibaren ekran işleri biraz yavaşlar, ama
  Kullanıcı beğenmediği ekranı kodlanmadan görür.
- Blueprint'in görsel ayrıntıları (C2–C6) Kullanıcı seçimiyle değişebilir; otomatik kontrast testi (F2.20) hangi
  palet seçilirse seçilsin geçerlidir.
- Yetenek üçüncü taraf içeriktir; güncellenirse yeniden okunmadan alınmaz.

**İlgili blueprint bölümü:** C1–C9, D (ekranlar), K12, A5
