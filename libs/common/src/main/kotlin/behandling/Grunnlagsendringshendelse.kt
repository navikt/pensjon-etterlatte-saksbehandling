package no.nav.etterlatte.libs.common.behandling

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import java.time.LocalDateTime
import java.util.*

data class Grunnlagsendringshendelse(
    val id: UUID,
    val sakId: Long,
    val type: GrunnlagsendringsType,
    val opprettet: LocalDateTime,
    val data: Grunnlagsinformasjon?,
    val status: GrunnlagsendringStatus = GrunnlagsendringStatus.IKKE_VURDERT,
    val behandlingId: UUID? = null
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
    IKKE_VURDERT,
    TATT_MED_I_BEHANDLING,
    GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING,
    FORKASTET,
    VURDERT_SOM_IKKE_RELEVANT
}