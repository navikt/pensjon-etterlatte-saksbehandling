package no.nav.etterlatte.libs.common.behandling

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

sealed class Grunnlagsinformasjon {
    abstract val type: String

    data class Utflytting(
        val hendelse: UtflyttingsHendelse
    ) : Grunnlagsinformasjon() {
        override val type: String = "UTFLYTTING"
    }

    data class SoekerDoed(
        val hendelse: Doedshendelse
    ) : Grunnlagsinformasjon() {
        override val type: String = "SOEKER_DOED"
    }

    data class ForelderBarnRelasjon(
        val hendelse: ForelderBarnRelasjonHendelse
    ) : Grunnlagsinformasjon() {
        override val type: String = "FORELDER_BARN_RELASJON"
    }
}

enum class GrunnlagsendringsType {
    SOEKER_DOED, UTFLYTTING, FORELDER_BARN_RELASJON
}

enum class GrunnlagsendringStatus {
    IKKE_VURDERT,
    TATT_MED_I_BEHANDLING,
    GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING,
    FORKASTET
}