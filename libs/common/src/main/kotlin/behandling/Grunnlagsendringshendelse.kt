package no.nav.etterlatte.libs.common.behandling

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Utland
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Grunnlagsendringshendelse(
    val id: UUID,
    val sakId: Long,
    val type: GrunnlagsendringsType,
    val opprettet: LocalDateTime,
    val status: GrunnlagsendringStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
    val behandlingId: UUID? = null,
    val hendelseGjelderRolle: Saksrolle,
    val gjelderPerson: String,
    val samsvarMellomPdlOgGrunnlag: SamsvarMellomPdlOgGrunnlag? = null
)

enum class GrunnlagsendringsType {
    DOEDSFALL,
    UTFLYTTING,
    FORELDER_BARN_RELASJON
}

enum class GrunnlagsendringStatus {
    VENTER_PAA_JOBB, // naar hendelsen registreres // FØR: IKKE_VURDERT
    SJEKKET_AV_JOBB, // FØR: MED_I_BEHANDLING
    TATT_MED_I_BEHANDLING, // tatt med i behandling av saksbehandler
    FORKASTET,
    VURDERT_SOM_IKKE_RELEVANT;

    companion object {
        fun relevantForSaksbehandler() = listOf(SJEKKET_AV_JOBB, TATT_MED_I_BEHANDLING, VURDERT_SOM_IKKE_RELEVANT)
    }
}

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