package no.nav.etterlatte.grunnlag.adresse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class PersondataAdresse(
    private val adresselinjer: List<String>,
    val type: String,
    private val land: String?,
    private val landkode: String?,
    private val navn: String?,
    private val postnr: String?,
    private val postnummer: String?,
    private val poststed: String?,
    private val vergePid: String?,
) {
    fun toBrevMottaker(): BrevMottaker {
        return BrevMottaker(
            navn = navn,
            foedselsnummer = vergePid?.let { Foedselsnummer(it) },
            adresse =
                Adresse(
                    adresseType =
                        when (toLandkode()) {
                            LANDKODE_NORGE -> "NORSKPOSTADRESSE"
                            else -> "UTENLANDSKPOSTADRESSE"
                        },
                    adresselinje1 = adresselinjer.getOrNull(0),
                    adresselinje2 = adresselinjer.getOrNull(1),
                    adresselinje3 = adresselinjer.getOrNull(2),
                    postnummer = listOfNotNull(postnummer, postnr).firstOrNull(),
                    poststed = poststed,
                    landkode = toLandkode(),
                    land =
                        when (toLandkode()) {
                            LANDKODE_NORGE -> "NORGE"
                            else -> (land ?: "UKJENT")
                        },
                ),
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
