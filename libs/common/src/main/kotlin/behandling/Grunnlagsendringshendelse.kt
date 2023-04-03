package no.nav.etterlatte.libs.common.behandling

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Utland
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

data class PersonMedSakerOgRoller(
    val fnr: String,
    val sakerOgRoller: List<SakOgRolle>
)

data class SakOgRolle(
    val sakId: Long,
    val rolle: Saksrolle
)

enum class Saksrolle {
    SOEKER,
    SOESKEN,
    AVDOED,
    GJENLEVENDE,
    UKJENT;

    fun toPersonrolle(): PersonRolle =
        when (this) {
            SOEKER -> PersonRolle.BARN
            SOESKEN -> PersonRolle.BARN
            AVDOED -> PersonRolle.AVDOED
            GJENLEVENDE -> PersonRolle.AVDOED
            UKJENT -> throw Exception("Ukjent Saksrolle kan ikke castes til PersonRolle")
        }

    companion object {
        val log: Logger = LoggerFactory.getLogger(Saksrolle::class.java)
        fun enumVedNavnEllerUkjent(rolle: String) =
            try {
                Saksrolle.valueOf(rolle.uppercase())
            } catch (e: Exception) {
                log.error(
                    "Kunne ikke bestemme rolle fra kolonnen $rolle i databasen. Dette betyr at kolonne-navnet i " +
                        "databasen verdien er hentet fra ikke samsvarer med noen av enum-verdiene til Saksrolle. " +
                        "Setter rolle som ukjent"
                )
                UKJENT
            }
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class SamsvarMellomPdlOgGrunnlag {
    abstract val samsvar: Boolean

    @JsonTypeName("DOEDSDATO")
    data class Doedsdatoforhold(
        val fraGrunnlag: LocalDate?,
        val fraPdl: LocalDate?,
        override val samsvar: Boolean
    ) : SamsvarMellomPdlOgGrunnlag()

    @JsonTypeName("UTLAND")
    data class Utlandsforhold(
        val fraPdl: Utland?,
        val fraGrunnlag: Utland?,
        override val samsvar: Boolean
    ) : SamsvarMellomPdlOgGrunnlag()

    @JsonTypeName("ANSVARLIGE_FORELDRE")
    data class AnsvarligeForeldre(
        val fraPdl: List<Folkeregisteridentifikator>?,
        val fraGrunnlag: List<Folkeregisteridentifikator>?,
        override val samsvar: Boolean
    ) : SamsvarMellomPdlOgGrunnlag()

    @JsonTypeName("BARN")
    data class Barn(
        val fraPdl: List<Folkeregisteridentifikator>?,
        val fraGrunnlag: List<Folkeregisteridentifikator>?,
        override val samsvar: Boolean
    ) : SamsvarMellomPdlOgGrunnlag()

    // Verge

    // institusjonsopphold?
}