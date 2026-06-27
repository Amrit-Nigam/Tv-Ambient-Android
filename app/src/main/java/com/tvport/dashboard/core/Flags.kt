package com.tvport.dashboard.core

/**
 * Maps a country / national-team name to a crisp PNG flag from flagcdn.com (free, no key).
 * World Cup teams are countries, so the football-data team `name` ("England", "Panama") and the
 * F1 location `country` ("Austria") both resolve here. Unknown names return null so callers can
 * gracefully render no flag rather than a broken image.
 *
 * Codes are ISO 3166-1 alpha-2; the UK home nations use flagcdn's `gb-eng` / `gb-sct` / `gb-wls`
 * subdivision flags so England/Scotland/Wales show their own flag, not the Union Jack.
 */
object Flags {
    /** e.g. flagUrl("England") -> "https://flagcdn.com/w160/gb-eng.png". Null if unknown. */
    fun url(country: String?, width: Int = 160): String? {
        val code = codeFor(country) ?: return null
        return "https://flagcdn.com/w$width/$code.png"
    }

    private fun codeFor(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val k = raw.trim().lowercase()
            .removePrefix("ir ")        // "IR Iran"
            .replace("ã", "a").replace("é", "e").replace("ô", "o").replace("ç", "c")
            .replace("’", "'")
        return MAP[k]
    }

    private val MAP: Map<String, String> = mapOf(
        // UK home nations (subdivision flags)
        "england" to "gb-eng", "scotland" to "gb-sct", "wales" to "gb-wls",
        "northern ireland" to "gb-nir", "great britain" to "gb", "united kingdom" to "gb",
        // Americas
        "argentina" to "ar", "brazil" to "br", "uruguay" to "uy", "paraguay" to "py",
        "chile" to "cl", "colombia" to "co", "peru" to "pe", "ecuador" to "ec",
        "bolivia" to "bo", "venezuela" to "ve", "united states" to "us", "usa" to "us",
        "mexico" to "mx", "canada" to "ca", "costa rica" to "cr", "honduras" to "hn",
        "panama" to "pa", "jamaica" to "jm", "el salvador" to "sv", "haiti" to "ht",
        // Europe
        "france" to "fr", "spain" to "es", "germany" to "de", "portugal" to "pt",
        "netherlands" to "nl", "belgium" to "be", "italy" to "it", "croatia" to "hr",
        "switzerland" to "ch", "denmark" to "dk", "sweden" to "se", "norway" to "no",
        "poland" to "pl", "serbia" to "rs", "austria" to "at", "ukraine" to "ua",
        "turkey" to "tr", "turkiye" to "tr", "greece" to "gr", "czech republic" to "cz",
        "czechia" to "cz", "hungary" to "hu", "romania" to "ro", "russia" to "ru",
        "slovenia" to "si", "slovakia" to "sk", "ireland" to "ie", "republic of ireland" to "ie",
        "iceland" to "is", "finland" to "fi", "albania" to "al", "north macedonia" to "mk",
        "georgia" to "ge", "bulgaria" to "bg", "monaco" to "mc", "azerbaijan" to "az",
        // Africa
        "morocco" to "ma", "senegal" to "sn", "ghana" to "gh", "nigeria" to "ng",
        "cameroon" to "cm", "egypt" to "eg", "tunisia" to "tn", "algeria" to "dz",
        "ivory coast" to "ci", "cote d'ivoire" to "ci", "south africa" to "za",
        "mali" to "ml", "burkina faso" to "bf", "dr congo" to "cd", "angola" to "ao",
        // Asia / Oceania
        "japan" to "jp", "korea republic" to "kr", "south korea" to "kr", "australia" to "au",
        "saudi arabia" to "sa", "qatar" to "qa", "iran" to "ir", "iraq" to "iq",
        "united arab emirates" to "ae", "uae" to "ae", "china" to "cn", "china pr" to "cn",
        "new zealand" to "nz", "uzbekistan" to "uz", "bahrain" to "bh", "singapore" to "sg",
        "india" to "in", "indonesia" to "id", "thailand" to "th", "vietnam" to "vn",
    )
}
