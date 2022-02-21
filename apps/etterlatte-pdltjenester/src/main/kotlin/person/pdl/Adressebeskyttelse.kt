package no.nav.etterlatte.libs.common.pdl

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Klasser for adressebeskyttelse i PDL.
 *
 * @see <a href="https://navikt.github.io/pdl/#_adressebeskyttelse">PDL 4.2 Adressebeskyttelse</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AdressebeskyttelseResponse(
    val data: HentAdressebeskyttelse? = null,
    val errors: List<ResponseError>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HentAdressebeskyttelse(
    val hentPersonBolk: List<AdressebeskyttelseBolkPerson>? = null,
    val hentPerson: AdressebeskyttelsePerson? = null
)

/**
 * Wrapper fra PDL når man etterspør flere elementer (bolk).
 *
 * @param person: Person fra PDL. Vil være null dersom personen ikke ble funnet.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AdressebeskyttelseBolkPerson(
    val person: AdressebeskyttelsePerson? = null
)

/**
 * Wrapper for liste over graderinger/adressebeskyttelser på en person.
 *
 * @param adressebeskyttelse: Liste over adressebeskyttelser registrert på personen.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AdressebeskyttelsePerson(
    val adressebeskyttelse: List<Adressebeskyttelse>
)

data class Adressebeskyttelse(
    val gradering: Gradering?
)

/**
 * Enum for å håndtere verdier fra PDL. Alle med unntak av [Gradering.UGRADERT] er offisielle verdier.
 * [Gradering.UGRADERT] er hva PDL regner en person som når de ikke har gradering, men et ugradert resultat fra PDL
 * vil bestå av en tom liste med [AdressebeskyttelsePerson.adressebeskyttelse]
 *
 * @throws IllegalArgumentException: Kaster feil dersom den mottar ukjent [Gradering].
 */
enum class Gradering {
    STRENGT_FORTROLIG_UTLAND,
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT;

    companion object {
        @JsonCreator
        fun fra(verdi: String?): Gradering =
            values().firstOrNull { it.name == verdi }
                ?: throw IllegalArgumentException("Ugyldig verdi for gradering: $verdi")
    }
}
