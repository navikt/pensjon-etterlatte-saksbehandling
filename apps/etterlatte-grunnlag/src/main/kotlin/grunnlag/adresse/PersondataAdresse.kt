package no.nav.etterlatte.grunnlag.adresse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class PersondataAdresse(
    val adresselinjer: List<String>,
    val type: String,
    val land: String?,
    val landkode: String?,
    val navn: String?,
    val postnr: String?,
    val poststed: String?,
    val vergePid: String?,
) {
    fun toVergeAdresse(): VergeAdresse {
        return VergeAdresse(
            navn = navn,
            adresseType =
                when (toLandkode()) {
                    LANDKODE_NORGE -> "NORSKPOSTADRESSE"
                    else -> "UTENLANDSKPOSTADRESSE"
                },
            adresselinje1 = adresselinjer.getOrNull(0),
            adresselinje2 = adresselinjer.getOrNull(1),
            adresselinje3 = adresselinjer.getOrNull(2),
            postnummer = postnr,
            poststed = poststed,
            landkode = toLandkode(),
            land =
                when (toLandkode()) {
                    LANDKODE_NORGE -> "NORGE"
                    else -> (land ?: "UKJENT")
                },
        )
    }

    private fun toLandkode(): String {
        return if (landkode == null || norskeLandkoder.contains(this.landkode)) {
            LANDKODE_NORGE
        } else {
            landkode
        }
    }

    companion object {
        private const val LANDKODE_NORGE = "NO"
        private val norskeLandkoder = listOf(LANDKODE_NORGE, "NOR")
    }
}
