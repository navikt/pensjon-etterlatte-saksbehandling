package no.nav.etterlatte.libs.common.beregning

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.SakId
import java.time.YearMonth
import java.util.UUID

data class SanksjonertYtelse(
    val sanksjonId: UUID,
    val sanksjonType: SanksjonType,
)

data class Sanksjon(
    val id: UUID?,
    val behandlingId: UUID,
    val sakId: SakId,
    val type: SanksjonType,
    val fom: YearMonth,
    val tom: YearMonth?,
    val opprettet: Grunnlagsopplysning.Saksbehandler,
    val endret: Grunnlagsopplysning.Saksbehandler?,
    val beskrivelse: String,
)

enum class SanksjonType {
    BORTFALL,
    OPPHOER,
    STANS,
    UTESTENGING,
    IKKE_INNVILGET_PERIODE,
}
