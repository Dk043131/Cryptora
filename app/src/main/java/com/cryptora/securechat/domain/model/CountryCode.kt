package com.cryptora.securechat.domain.model

data class CountryCode(
    val name: String,
    val dialCode: String,
    val flagEmoji: String,
    val isoCode: String
)

object SupportedCountryCodes {
    val default = CountryCode("India", "+91", "🇮🇳", "IN")

    val list = listOf(
        CountryCode("India", "+91", "🇮🇳", "IN"),
        CountryCode("United States", "+1", "🇺🇸", "US"),
        CountryCode("United Kingdom", "+44", "🇬🇧", "GB"),
        CountryCode("Canada", "+1", "🇨🇦", "CA"),
        CountryCode("Australia", "+61", "🇦🇺", "AU"),
        CountryCode("Germany", "+49", "🇩🇪", "DE"),
        CountryCode("France", "+33", "🇫🇷", "FR"),
        CountryCode("United Arab Emirates", "+971", "🇦🇪", "AE"),
        CountryCode("Singapore", "+65", "🇸🇬", "SG"),
        CountryCode("Japan", "+81", "🇯🇵", "JP")
    )
}
