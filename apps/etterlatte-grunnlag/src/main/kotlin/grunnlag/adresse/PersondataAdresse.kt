package no.nav.etterlatte.grunnlag.adresse

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.etterlatte.libs.common.person.BrevMottaker
import no.nav.etterlatte.libs.common.person.MottakerAdresse
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import org.slf4j.LoggerFactory

const val REGOPPSLAG_ADRESSE = "REGOPPSLAG_ADRESSE"

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = VergeSamhandlerFormat::class, name = "VERGE_SAMHANDLER_POSTADRESSE"),
    JsonSubTypes.Type(value = VergePersonFormat::class, name = "VERGE_PERSON_POSTADRESSE"),
    JsonSubTypes.Type(value = RegoppslagFormat::class, name = REGOPPSLAG_ADRESSE),
)
sealed class PersondataAdresse(
    val type: String,
    open val adresselinjer: List<String>,
    open val adresseString: String?,
) {
    /**
     * Med frittstående menes at adressen skal kunne brukes til å adressere bare denne personen, uten c/o. */
    abstract fun tilFrittstaendeBrevMottaker(foedselsnummer: String): BrevMottaker
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
    val landkode: String?,
    val land: String?,
) : PersondataAdresse("VERGE_SAMHANDLER_POSTADRESSE", adresselinjer, adresseString) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun tilFrittstaendeBrevMottaker(foedselsnummer: String): BrevMottaker {
        logger.debug("Tolker ${this::class.simpleName}, med landkode: ${landkode.quoted}, land: ${land.quoted}")
        return BrevMottaker(
            navn = navn ?: "Ukjent",
            foedselsnummer = null, // Brevmottaker skal hverken ha fnr eller orgnummer
            adresse =
                MottakerAdresse(
                    adresseType = adressetypeFromLand(landkode, land),
                    adresselinje1 = linje1,
                    adresselinje2 = linje2,
                    adresselinje3 = linje3,
                    postnummer = postnr,
                    poststed = poststed,
                    landkode = utledLandkode(landkode, land),
                    land = utledLand(landkode, land),
                ),
            adresseTypeIKilde = "VERGE_SAMHANDLER_POSTADRESSE",
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
    override fun tilFrittstaendeBrevMottaker(foedselsnummer: String): BrevMottaker =
        BrevMottaker(
            navn = navn ?: "Ukjent",
            foedselsnummer = MottakerFoedselsnummer(foedselsnummer),
            adresse =
                MottakerAdresse(
                    adresseType = adressetypeFromLand(adresse.landkode, adresse.land),
                    adresselinje1 = adresse.adresselinje1,
                    adresselinje2 = adresse.adresselinje2,
                    adresselinje3 = adresse.adresselinje3,
                    postnummer = adresse.postnummer,
                    poststed = adresse.poststed,
                    landkode = adresse.landkode,
                    land = adresse.land,
                ),
            adresseTypeIKilde = "VERGE_PERSON_POSTADRESSE",
        )
}

data class RegoppslagFormat(
    override val adresselinjer: List<String>,
    override val adresseString: String?,
    val adresse: RegoppslagAdresse,
    val navn: String?,
) : PersondataAdresse("REGOPPSLAG_ADRESSE", adresselinjer, adresseString) {
    override fun tilFrittstaendeBrevMottaker(foedselsnummer: String): BrevMottaker =
        BrevMottaker(
            navn = navn ?: "Ukjent",
            foedselsnummer = MottakerFoedselsnummer(foedselsnummer),
            adresse =
                MottakerAdresse(
                    adresseType = adressetypeFromLand(adresse.landkode, adresse.land),
                    adresselinje1 = adresse.adresselinje1,
                    adresselinje2 = adresse.adresselinje2,
                    adresselinje3 = adresse.adresselinje3,
                    postnummer = adresse.postnummer,
                    poststed = adresse.poststed,
                    landkode = adresse.landkode,
                    land = adresse.land,
                ),
            adresseTypeIKilde = "REGOPPSLAG_ADRESSE",
        )
}

private fun adressetypeFromLand(
    landkode: String?,
    land: String?,
): String =
    if (landkodeRegnesSomNorsk(landkode) && landRegnesSomNorsk(land)) {
        "NORSKPOSTADRESSE"
    } else {
        "UTENLANDSKPOSTADRESSE"
    }

private fun utledLand(
    landkode: String?,
    land: String?,
): String =
    if (landkodeRegnesSomNorsk(landkode) && landRegnesSomNorsk(land)) {
        "NORGE"
    } else {
        land ?: "UKJENT"
    }

private fun utledLandkode(
    landkode: String?,
    land: String?,
): String =
    if (landkodeRegnesSomNorsk(landkode) && landRegnesSomNorsk(land)) {
        "NO"
    } else {
        requireNotNull(landkode) { "Landkode kunne ikke settes " }
    }

private fun landkodeRegnesSomNorsk(landkode: String?) = landkode.isNullOrBlank() || listOf("no", "nor").contains(landkode.lowercase())

private fun landRegnesSomNorsk(land: String?) = land.isNullOrBlank() || listOf("norge", "norway").contains(land.lowercase())

private val String?.quoted: String?
    get() = this?.let { "\"$this\"" }
