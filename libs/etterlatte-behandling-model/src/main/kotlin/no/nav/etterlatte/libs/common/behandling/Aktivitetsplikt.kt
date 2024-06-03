package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.YearMonth
import java.util.UUID

data class AktivitetspliktOppfolging(
    val behandlingId: UUID,
    val aktivitet: String,
    val opprettet: Tidspunkt,
    val opprettetAv: String,
)

data class OpprettAktivitetspliktOppfolging(
    val aktivitet: String,
)

data class OpprettRevurderingForAktivitetspliktDto(
    val sakId: Long,
    val frist: Tidspunkt,
    val behandlingsmaaned: YearMonth,
    val vurderingVedMaaned: VurderingVedMaaned,
) {
    enum class VurderingVedMaaned {
        SEKS_MND,
        TOLV_MND,
    }
}

data class OpprettRevurderingForAktivitetspliktResponse(
    val opprettetRevurdering: Boolean,
    val nyBehandlingId: UUID?,
    val forrigeBehandlingId: UUID,
)
