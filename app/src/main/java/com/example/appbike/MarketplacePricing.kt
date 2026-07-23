package com.example.appbike

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.math.BigDecimal
import java.text.Normalizer

internal data class MarketplaceCurrency(
    val code: String,
    val symbol: String,
    val name: String,
    val thousandsSeparator: Char
)

private val CLP = MarketplaceCurrency("CLP", "$", "peso chileno", '.')
private val ARS = MarketplaceCurrency("ARS", "$", "peso argentino", '.')

internal fun marketplaceCurrency(code: String): MarketplaceCurrency = when (code.uppercase()) {
    "ARS" -> ARS
    "BOB" -> MarketplaceCurrency("BOB", "Bs", "boliviano", '.')
    "BRL" -> MarketplaceCurrency("BRL", "R$", "real brasileño", '.')
    "COP" -> MarketplaceCurrency("COP", "$", "peso colombiano", '.')
    "PEN" -> MarketplaceCurrency("PEN", "S/", "sol peruano", '.')
    "PYG" -> MarketplaceCurrency("PYG", "₲", "guaraní", '.')
    "UYU" -> MarketplaceCurrency("UYU", "\$U", "peso uruguayo", '.')
    "VES" -> MarketplaceCurrency("VES", "Bs", "bolívar", '.')
    "USD" -> MarketplaceCurrency("USD", "$", "dólar estadounidense", ',')
    "MXN" -> MarketplaceCurrency("MXN", "$", "peso mexicano", ',')
    "CAD" -> MarketplaceCurrency("CAD", "C$", "dólar canadiense", ',')
    "EUR" -> MarketplaceCurrency("EUR", "€", "euro", '.')
    else -> CLP
}

/**
 * Currency is currently a presentation concern because the community backend
 * does not persist a currency field. A full geocoder label is preferred; the
 * coordinate fallback keeps existing Chile/Argentina locations useful.
 */
internal fun marketplaceCurrencyFor(point: GeoPoint?): MarketplaceCurrency {
    if (point == null) return CLP
    point.currencyCode.takeIf(String::isNotBlank)?.let { return marketplaceCurrency(it) }
    val countryCurrency = currencyCodeForCountry(point.countryCode)
    if (countryCurrency.isNotBlank()) return marketplaceCurrency(countryCurrency)
    val label = Normalizer.normalize(point.label, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()

    return when {
        label.containsAny("argentina", "argentine") -> ARS
        label.containsAny("chile") -> CLP
        label.containsAny("bolivia") -> marketplaceCurrency("BOB")
        label.containsAny("brasil", "brazil") -> marketplaceCurrency("BRL")
        label.containsAny("colombia") -> marketplaceCurrency("COP")
        label.containsAny("peru") -> marketplaceCurrency("PEN")
        label.containsAny("paraguay") -> marketplaceCurrency("PYG")
        label.containsAny("uruguay") -> marketplaceCurrency("UYU")
        label.containsAny("venezuela") -> marketplaceCurrency("VES")
        label.containsAny("ecuador", "estados unidos", "united states", "usa") ->
            marketplaceCurrency("USD")
        label.containsAny("mexico") -> marketplaceCurrency("MXN")
        label.containsAny("canada") -> marketplaceCurrency("CAD")
        label.containsAny(
            "espana", "spain", "francia", "france", "alemania", "germany",
            "italia", "italy", "portugal", "paises bajos", "netherlands"
        ) -> marketplaceCurrency("EUR")
        point.latitude in -55.2..-21.7 && point.longitude > -69.7 -> ARS
        else -> CLP
    }
}

internal fun currencyCodeForCountry(countryCode: String): String = when (countryCode.uppercase()) {
    "CL" -> "CLP"
    "AR" -> "ARS"
    "BO" -> "BOB"
    "BR" -> "BRL"
    "CO" -> "COP"
    "PE" -> "PEN"
    "PY" -> "PYG"
    "UY" -> "UYU"
    "VE" -> "VES"
    "EC", "US" -> "USD"
    "MX" -> "MXN"
    "CA" -> "CAD"
    "ES", "FR", "DE", "IT", "PT", "NL" -> "EUR"
    else -> ""
}

internal fun formatMarketplacePrice(
    rawPrice: String,
    currency: MarketplaceCurrency
): String {
    val wholeUnits = runCatching {
        BigDecimal(rawPrice.trim().replace(',', '.')).toBigInteger().toString()
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: rawPrice.filter(Char::isDigit)
    val amount = formatWholeUnits(wholeUnits.ifBlank { "0" }, currency.thousandsSeparator)
    return "${currency.symbol}$amount ${currency.code}"
}

internal fun normalizeWholeUnitInput(raw: String, maxDigits: Int = 15): String {
    val digits = raw.filter(Char::isDigit).take(maxDigits)
    if (digits.isEmpty()) return ""
    return digits.trimStart('0').ifBlank { "0" }
}

internal fun parseMarketplaceWholeUnitPrice(raw: String): Long? {
    val compact = raw.trim().replace(" ", "")
    val digits = when {
        compact.matches(Regex("""\d+""")) -> compact
        compact.matches(Regex("""\d{1,3}([.,]\d{3})+""")) ->
            compact.filter(Char::isDigit)
        else -> return null
    }
    return digits.toLongOrNull()?.takeIf { it > 0L }
}

internal class WholeUnitPriceVisualTransformation(
    private val separator: Char
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        val originalToTransformed = IntArray(original.length + 1)
        val formatted = buildString {
            originalToTransformed[0] = 0
            original.forEachIndexed { index, char ->
                if (index > 0 && (original.length - index) % 3 == 0) append(separator)
                append(char)
                originalToTransformed[index + 1] = length
            }
        }
        val transformedToOriginal = IntArray(formatted.length + 1)
        var originalOffset = 0
        formatted.forEachIndexed { index, char ->
            transformedToOriginal[index] = originalOffset
            if (char.isDigit()) originalOffset++
        }
        transformedToOriginal[formatted.length] = original.length

        return TransformedText(
            AnnotatedString(formatted),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int =
                    originalToTransformed[offset.coerceIn(0, original.length)]

                override fun transformedToOriginal(offset: Int): Int =
                    transformedToOriginal[offset.coerceIn(0, formatted.length)]
            }
        )
    }
}

private fun formatWholeUnits(digits: String, separator: Char): String = buildString {
    digits.forEachIndexed { index, char ->
        if (index > 0 && (digits.length - index) % 3 == 0) append(separator)
        append(char)
    }
}

private fun String.containsAny(vararg values: String): Boolean = values.any(::contains)
