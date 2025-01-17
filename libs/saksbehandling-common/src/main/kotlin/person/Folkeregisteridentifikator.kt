package no.nav.etterlatte.libs.common.person

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.logging.sikkerlogger

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
                return krevIkkeNull(ofNullable(fnr)) {
                    "Fødselsnummer mangler"
                }
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
     * Checks if the identity number is of type D-number.
     *
     * A D-number consists of 11 digits, of which the first six digits show the date of birth,
     * but the first digit is increased by 4.
     */
    fun isDNumber(): Boolean = Character.getNumericValue(value[0]) in 4..7

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
