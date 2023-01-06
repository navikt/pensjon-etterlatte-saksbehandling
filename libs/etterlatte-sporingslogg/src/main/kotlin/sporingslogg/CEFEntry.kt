package sporingslogg

import java.time.Instant

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
    Delete;

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
    val extension: Extension?
) {
    fun format(): String {
        val formatertExtension = if (extension != null) "|${extension.format()}" else ""
        return "$format:$versjon|$deviceVendor|etterlatte-$deviceProduct|$deviceVersion|" +
            "${deviceEventClassId.format()}|${name.tekst}|$severity$formatertExtension"
    }
}

data class Extension(
    val endTime: Instant = Instant.now(),
    val sourceUserId: String, // merk: Vi støtter pr i dag ikke Azure AD ID’er, og vil ha behov for vanlig NAV ID.
    val destinationUserId: String,
    val sourceProcessName: String,
    val request: String? = null,
    val flexString1: Decision? = null,
    val flexString2: Pair<String, String>? = null,
    val message: String
) {
    fun format(): String {
        val flexString1Formatert =
            if (flexString1 != null) " flexString1Label=Decision flexString1=$flexString1" else ""
        val flexString2Formatert =
            if (flexString2 != null) "flexString2Label=${flexString2.first} flexString2=${flexString2.second}" else ""
        val requestFormatert = if (request != null) " request=$request" else ""
        return "end=${endTime.toEpochMilli()} suid=$sourceUserId duid=$destinationUserId sproc=$sourceProcessName" +
            "$requestFormatert$flexString1Formatert$flexString2Formatert msg=$message"
    }
}