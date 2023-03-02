package no.nav.etterlatte.libs.sporingslogg

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt

enum class Format {
    CEF
}

enum class Severity {
    INFO, WARN
}

enum class Decision {
    Permit,
    Deny
}

enum class DeviceEventClassId {
    Access,
    Create,
    Update,
    Delete,
    Meta;

    fun format() = "audit:${name.lowercase()}"
}

enum class Name(val tekst: String) {
    OnBehalfOfAccess("On-behalf-of access")
}

// CEF-formatet er definert på https://community.microfocus.com/cfs-file/__key/communityserver-wikis-components-files/00-00-00-00-23/3731.CommonEventFormatV25.pdf
data class CEFEntry(
    val format: Format = Format.CEF,
    val versjon: Int = 0,
    val deviceVendor: String = "pensjon",
    val deviceProduct: String,
    val deviceVersion: String = "1.0",
    val deviceEventClassId: DeviceEventClassId,
    val name: Name,
    val severity: Severity,
    val extension: Extension
) {
    fun format(): String = "$format:$versjon|$deviceVendor|etterlatte-$deviceProduct|$deviceVersion|" +
        "${deviceEventClassId.format()}|${name.tekst}|$severity|${extension.format()}"
}

data class Extension(
    val endTime: Tidspunkt = Tidspunkt.now(),
    val sourceUserId: String, // merk: Vi støtter pr i dag ikke Azure AD ID’er, og vil ha behov for vanlig NAV ID.
    val destinationUserId: String,
    val request: String? = null,
    val flexString1: Decision? = null,
    val message: String
) {
    fun format(): String {
        val flexString1Formatert =
            if (flexString1 != null) " flexString1Label=Decision flexString1=$flexString1" else ""
        val requestFormatert = if (request != null) " request=$request" else ""
        return "end=${endTime.toEpochMilli()} suid=$sourceUserId duid=$destinationUserId" +
            "$requestFormatert$flexString1Formatert msg=$message"
    }
}