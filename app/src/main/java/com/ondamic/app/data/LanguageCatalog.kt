package com.ondamic.app.data

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

data class SupportedLanguage(
    val bcp47Tag: String,
    val nativeName: String,
    val regionGroup: String,
    val flagEmoji: String
)

object LanguageCatalog {
    val languages: List<SupportedLanguage> = listOf(
        // System Default
        SupportedLanguage("", "Auto (Idioma do Dispositivo / System)", "Global", "🌐"),

        // Americas & Caribbean
        SupportedLanguage("pt-BR", "Português (Brasil)", "Américas & Caribe", "🇧🇷"),
        SupportedLanguage("en", "English (Global)", "Americas & Global", "🇺🇸"),
        SupportedLanguage("es", "Español (Latinoamérica / España)", "Américas & Europa", "🇪🇸"),
        SupportedLanguage("fr", "Français (Monde / Afrique)", "Amériques, Europe & Afrique", "🇫🇷"),
        SupportedLanguage("ht", "Kreyòl Ayisyen (Crioulo Haitiano)", "Caraïbes / Ayiti", "🇭🇹"),
        SupportedLanguage("gn", "Avañe'ẽ (Guarani)", "América do Sul", "🇵🇾"),
        SupportedLanguage("qu", "Runasimi (Quechua)", "Andes / América do Sul", "🇵🇪"),

        // Africa (17 Languages)
        SupportedLanguage("sw", "Kiswahili (Suaíli)", "Afrika Mashariki", "🇰🇪"),
        SupportedLanguage("ha", "Hausa (Hauçá)", "Afirka ta Yamma", "🇳🇬"),
        SupportedLanguage("yo", "Èdè Yorùbá (Iorubá)", "Ìwọ̀-oòrùn Áfíríkà", "🇳🇬"),
        SupportedLanguage("ig", "Ásụ̀sụ́ Ìgbò (Igbo)", "West Africa", "🇳🇬"),
        SupportedLanguage("am", "አማርኛ (Amárico / Amharic)", "ኢትዮጵያ / East Africa", "🇪🇹"),
        SupportedLanguage("om", "Afaan Oromoo (Oromo)", "Oromiyaa / East Africa", "🇪🇹"),
        SupportedLanguage("so", "Soomaali (Somali)", "Geeska Afrika", "🇸🇴"),
        SupportedLanguage("zu", "isiZulu (Zulu)", "iNingizimu Afrika", "🇿🇦"),
        SupportedLanguage("xh", "isiXhosa (Xhosa)", "uMzantsi Afrika", "🇿🇦"),
        SupportedLanguage("af", "Afrikaans", "Suid-Afrika / Namibië", "🇿🇦"),
        SupportedLanguage("ln", "Lingála (Lingala)", "Kongó / Afrika ya Ntei", "🇨🇩"),
        SupportedLanguage("wo", "Wolof (Uolofe)", "Senegaal / Gambi", "🇸🇳"),
        SupportedLanguage("rw", "Ikinyarwanda (Quiniaruanda)", "Rwanda", "🇷🇼"),
        SupportedLanguage("mg", "Malagasy (Malgaxe)", "Madagasikara", "🇲🇬"),
        SupportedLanguage("sn", "chiShona (Chona)", "Zimbabwe", "🇿🇼"),
        SupportedLanguage("ny", "Chichewa / Chinyanja", "Malawi / Zambia / Moçambique", "🇲🇼"),
        SupportedLanguage("ak", "Akan / Twi", "Ghana", "🇬🇭"),

        // Europe, Middle East & Asia
        SupportedLanguage("de", "Deutsch", "Europa", "🇩🇪"),
        SupportedLanguage("it", "Italiano", "Europa", "🇮🇹"),
        SupportedLanguage("nl", "Nederlands", "Europa", "🇳🇱"),
        SupportedLanguage("pl", "Polski", "Europa", "🇵🇱"),
        SupportedLanguage("ru", "Русский", "Европа / Азия", "🌍"),
        SupportedLanguage("uk", "Українська", "Європа", "🇺🇦"),
        SupportedLanguage("tr", "Türkçe", "Avrupa / Asya", "🇹🇷"),
        SupportedLanguage("ar", "العربية (Arabic)", "الشرق الأوسط وشمال أفريقيا", "🇸🇦"),
        SupportedLanguage("iw", "עברית (Hebrew)", "המזרח התיכון", "🇮🇱"),
        SupportedLanguage("ur", "اردو (Urdu)", "جنوبی ایشیا", "🇵🇰"),
        SupportedLanguage("hi", "हिन्दी (Hindi)", "दक्षिण एशिया", "🇮🇳"),
        SupportedLanguage("bn", "বাংলা (Bengali)", "দক্ষিণ এশিয়া", "🇧🇩"),
        SupportedLanguage("in", "Bahasa Indonesia", "Asia Tenggara", "🇮🇩"),
        SupportedLanguage("tl", "Wikang Tagalog / Filipino", "Timog-Silangang Asya", "🇵🇭"),
        SupportedLanguage("vi", "Tiếng Việt", "Đông Nam Á", "🇻🇳"),
        SupportedLanguage("zh-CN", "简体中文 (Chinese Simplified)", "东亚", "🇨🇳"),
        SupportedLanguage("ja", "日本語 (Japanese)", "東アジア", "🇯🇵"),
        SupportedLanguage("ko", "한국어 (Korean)", "동아시아", "🇰🇷")
    )

    fun applyLanguage(bcp47Tag: String) {
        if (bcp47Tag.isBlank()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(bcp47Tag))
        }
    }

    fun getCurrentLanguageTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) "" else (locales[0]?.toLanguageTag() ?: "")
    }
}
