package no.nav.etterlatte.libs.common.person

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Norwegian national identity number
 *
 * The Norwegian national identity number is an 11-digit personal identifier.
 * Everyone on the Norwegian National Registry has a national identity number.
 *
 * @see <a href="https://www.skatteetaten.no/person/folkeregister/fodsel-og-navnevalg/barn-fodt-i-norge/fodselsnummer">Skatteetaten om fødselsnummer</a>
 */
class Folkeregisteridentifikator private constructor(
    @JsonValue val value: String,
) {
    companion object {
        @JvmStatic
        @JsonCreator
        fun of(fnr: String?): Folkeregisteridentifikator {
            if (fnr.isNullOrEmpty()) {
                throw InvalidFoedselsnummerException("Fødselsnummer er tomt")
            } else {
                return requireNotNull(ofNullable(fnr))
            }
        }

        fun ofNullable(fnr: String?): Folkeregisteridentifikator? {
            if (fnr.isNullOrEmpty()) {
                return null
            }
            val fnrMedGyldigeTall = fnr.replace(Regex("[^0-9]"), "")
            if (FolkeregisteridentifikatorValidator.isValid(fnrMedGyldigeTall)) {
                return Folkeregisteridentifikator(fnrMedGyldigeTall)
            } else {
                sikkerlogger().error("Ugyldig fødselsnummer: $fnr")
                throw InvalidFoedselsnummerException("Fødselsnummeret er ugyldig")
            }
        }

        fun isValid(fnr: String?): Boolean =
            fnr != null &&
                FolkeregisteridentifikatorValidator.isValid(fnr.replace(Regex("[^0-9]"), ""))
    }

    /**
     * @return birthdate as [LocalDate]
     */
    fun getBirthDate(): LocalDate {
        val fnrMonth = value.slice(2 until 4).toInt()

        val fnrDay = value.slice(0 until 2).toInt()
        val day = if (isDNumber()) fnrDay - 40 else fnrDay

        val month =
            if (isTestNorgeNumber()) {
                fnrMonth - 80
            } else if (isHNumber()) {
                fnrMonth - 40
            } else {
                fnrMonth
            }

        return LocalDate.of(getYearOfBirth(), month, day)
    }

    /**
     * @return the birthdate as a ISO 8601 [String]
     */
    fun getBirthDateAsIso() = getBirthDate().toString()

    /**
     * Checks if the identity number is of type D-number.
     *
     * A D-number consists of 11 digits, of which the first six digits show the date of birth,
     * but the first digit is increased by 4.
     */
    fun isDNumber(): Boolean = Character.getNumericValue(value[0]) in 4..7

    /**
     * Calculates year of birth using the individual number.
     *
     * @return 4 digit year of birth as [Int]
     */
    private fun getYearOfBirth(): Int {
        val year = value.slice(4 until 6).toInt()
        val individnummer = value.slice(6 until 9).toInt()

        return firesifretAarstallFraTosifret(year, individnummer)
    }

    fun getAge(): Int = ChronoUnit.YEARS.between(getBirthDate(), LocalDate.now()).toInt()

    /**
     * Sjekker om fødselsnummeret er av typen "Hjelpenummer".
     *
     * H-nummer er et hjelpenummer, en virksomhetsintern, unik identifikasjon av en person som
     * ikke har fødselsnummer eller D-nummer eller hvor dette er ukjent.
     *
     * Brukes også for identer i test som er opprettet som "NAV syntetisk" i Dolly
     */
    private fun isHNumber(): Boolean = Character.getNumericValue(value[2]) in 4..7

    /**
     * Sjekker om fødselsnummeret er av typen "Syntetisk bruker fra Skatteetaten".
     */
    private fun isTestNorgeNumber(): Boolean = Character.getNumericValue(value[2]) >= 8

    /**
     * Sjekker om fødselsnummeret er av typen "Felles Nasjonalt Hjelpenummer".
     *
     * Brukes av helsevesenet i tilfeller hvor de har behov for unikt å identifisere pasienter
     * som ikke har et kjent fødselsnummer eller D-nummer.
     */

    private fun isFhNumber(): Boolean = Character.getNumericValue(value[0]) in 8..9

    override fun equals(other: Any?): Boolean = this.value == (other as? Folkeregisteridentifikator)?.value

    override fun hashCode(): Int = this.value.hashCode()

    /**
     * Skal ALLTID returnere anonymisert fødselsnummer.
     *
     * Bruk [value] ved behov for komplett fødselsnummer.
     */
    override fun toString(): String = this.value.replaceRange(6 until 11, "*****")
}

internal fun firesifretAarstallFraTosifret(
    year: Int,
    individnummer: Int,
): Int =
    if (individnummer < 500) {
        (year + 1900)
    } else if ((individnummer < 750) && (54 < year)) {
        (year + 1800)
    } else if (year < 40) {
        year + 2000
    } else if (900 <= individnummer) {
        year + 1900
    } else {
        throw IllegalArgumentException("Ingen gyldig årstall funnet for individnummer $individnummer")
    }

class InvalidFoedselsnummerException(
    details: String,
) : UgyldigForespoerselException(
        code = "UGYLDIG_FNR",
        detail = details,
    )
