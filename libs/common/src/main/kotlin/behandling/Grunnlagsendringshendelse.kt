package no.nav.etterlatte.libs.common.behandling

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

data class Grunnlagsendringshendelse(
    val id: UUID,
    val sakId: Long,
    val type: GrunnlagsendringsType,
    val opprettet: LocalDateTime,
    val data: Grunnlagsinformasjon?,
    val status: GrunnlagsendringStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
    val behandlingId: UUID? = null,
    val hendelseGjelderRolle: Saksrolle,
    val korrektIPDL: KorrektIPDL = KorrektIPDL.IKKE_SJEKKET
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class Grunnlagsinformasjon {

    @JsonTypeName("UTFLYTTING")
    data class Utflytting(
        val hendelse: UtflyttingsHendelse
    ) : Grunnlagsinformasjon()

    @JsonTypeName("DOEDSFALL") // FØR: "SOEKER_DOED"
    data class Doedsfall(
        val hendelse: Doedshendelse
    ) : Grunnlagsinformasjon()

    @JsonTypeName("FORELDER_BARN_RELASJON")
    data class ForelderBarnRelasjon(
        val hendelse: ForelderBarnRelasjonHendelse
    ) : Grunnlagsinformasjon()
}

enum class GrunnlagsendringsType {
    DOEDSFALL, UTFLYTTING, FORELDER_BARN_RELASJON
}

enum class GrunnlagsendringStatus {
    VENTER_PAA_JOBB, // naar hendelsen registreres // FØR: IKKE_VURDERT
    SJEKKET_AV_JOBB, // skifte til noe som indikerer at venteperiode er utført? FØR: ED_I_BEHANDLING
    TATT_MED_I_BEHANDLING, // tatt med i behandling av saksbehandler
    FORKASTET,
    VURDERT_SOM_IKKE_RELEVANT
}

enum class Saksrolle {
    SOEKER,
    INNSENDER,
    SOESKEN,
    AVDOED,
    GJENLEVENDE,
    UKJENT;

    companion object {
        val log = LoggerFactory.getLogger(Saksrolle::class.java)
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

enum class KorrektIPDL { JA, NEI, IKKE_SJEKKET }