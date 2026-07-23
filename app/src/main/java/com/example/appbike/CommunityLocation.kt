package com.example.appbike

import java.text.Normalizer
import java.util.Locale

/**
 * Local compatibility mapping for regions already deployed by the community backend.
 * The backend remains authoritative and should return region_code for every location.
 */
internal fun communityRegionCodeFor(countryCode: String, administrativeArea: String): String {
    if (!countryCode.equals("CL", ignoreCase = true)) return ""
    val normalized = Normalizer.normalize(administrativeArea, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
    return when {
        "los lagos" in normalized -> "LAS"
        else -> ""
    }
}
