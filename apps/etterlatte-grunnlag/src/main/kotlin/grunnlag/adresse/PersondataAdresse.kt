package no.nav.etterlatte.grunnlag.adresse

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = VergeSamhandlerFormat::class, name = "VERGE_SAMHANDLER_POSTADRESSE"),
    JsonSubTypes.Type(value = VergePersonFormat::class, name = "VERGE_PERSON_POSTADRESSE"),
    JsonSubTypes.Type(value = RegoppslagFormat::class, name = "REGOPPSLAG_ADRESSE"),
)
sealed class PersondataAdresse(
    val type: String,
    open val adresselinjer: List<String>,
    open val adresseString: String?,
) {
    /**
     * Med frittstående menes at adressen skal kunne brukes til å adressere bare denne personen, uten c/o. */
    abstract fun tilFrittstaendeBrevMottaker(): BrevMottaker
}

data class VergeSamhandlerFormat(
    override val adresselinjer: List<String>,
    override val adresseString: String?,
    val linje1: String?,
    val linje2: String?,
    val linje3: String?,
    val postnr: String?,
    val poststed: String?,
    val navn: String?,
    val landkode: String,
    val land: String,
) : PersondataAdresse("VERGE_SAMHANDLER_POSTADRESSE", adresselinjer, adresseString) {
    override fun tilFrittstaendeBrevMottaker(): BrevMottaker {
        return BrevMottaker(
            navn = navn ?: "Ukjent",
            foedselsnummer = null,
            adresse =
                Adresse(
                    adresseType = adressetypeFromLand(landkode, land),
                    adresselinje1 = linje1,
                    adresselinje2 = linje2,
                    adresselinje3 = linje3,
                    postnummer = postnr,
                    poststed = poststed,
                    landkode = landkode,
                    land = land,
                ),
        )
    }
}

data class VergePersonFormat(
    override val adresselinjer: List<String>,
    override val adresseString: String?,
    val adresse: RegoppslagAdresse,
    val vergePid: String,
    val navn: String?,
) : PersondataAdresse("VERGE_PERSON_POSTADRESSE", adresselinjer, adresseString) {
    override fun tilFrittstaendeBrevMottaker(): BrevMottaker {
        return BrevMottaker(
            navn = navn ?: "Ukjent",
            foedselsnummer = Foedselsnummer(vergePid),
            adresse =
                Adresse(
                    adresseType = adressetypeFromLand(adresse.landkode, adresse.land),
                    adresselinje1 = adresse.adresselinje1,
                    adresselinje2 = adresse.adresselinje2,
                    adresselinje3 = adresse.adresselinje3,
                    postnummer = adresse.postnummer,
                    poststed = adresse.poststed,
                    landkode = adresse.landkode,
                    land = adresse.land,
                ),
        )
    }
}

data class RegoppslagFormat(
    override val adresselinjer: List<String>,
    override val adresseString: String?,
    val adresse: RegoppslagAdresse,
    val navn: String?,
) :
    PersondataAdresse("REGOPPSLAG_ADRESSE", adresselinjer, adresseString) {
    override fun tilFrittstaendeBrevMottaker(): BrevMottaker {
        return BrevMottaker(
            navn = navn ?: "Ukjent",
            foedselsnummer = null,
            adresse =
                Adresse(
                    adresseType = adressetypeFromLand(adresse.landkode, adresse.land),
                    adresselinje1 = adresse.adresselinje1,
                    adresselinje2 = adresse.adresselinje2,
                    adresselinje3 = adresse.adresselinje3,
                    postnummer = adresse.postnummer,
                    poststed = adresse.poststed,
                    landkode = adresse.landkode,
                    land = adresse.land,
                ),
        )
    }
}

private fun adressetypeFromLand(
    landkode: String?,
    land: String?,
): String {
    if (listOf("no", "nor").contains(landkode?.lowercase())) {
        return "NORSKPOSTADRESSE"
    }
    if (listOf("norge", "norway").contains(land?.lowercase())) {
        return "NORSKPOSTADRESSE"
    }
    return "UTENLANDSKPOSTADRESSE"
}
