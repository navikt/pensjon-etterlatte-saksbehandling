package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
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

sealed class Grunnlagsinformasjon {

    data class SoekerDoed(
        val hendelse: Doedshendelse
    ) : Grunnlagsinformasjon()
}

enum class GrunnlagsendringsType {
    SOEKER_DOED
}

enum class GrunnlagsendringStatus {
    IKKE_VURDERT,
    TATT_MED_I_BEHANDLING,
    GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING,
    FORKASTET
}