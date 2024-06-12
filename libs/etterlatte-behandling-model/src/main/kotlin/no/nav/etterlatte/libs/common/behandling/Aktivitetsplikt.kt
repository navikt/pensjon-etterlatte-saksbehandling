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
    val jobbType: JobbType,
) {
    enum class JobbType(
        val beskrivelse: String,
    ) {
        OMS_DOED_6MND("Vurdering av aktivitetsplikt OMS etter 6 mnd"),
        OMS_DOED_12MND("Vurdering av aktivitetsplikt OMS etter 12 mnd"),
    }
}

data class OpprettRevurderingForAktivitetspliktResponse(
    val opprettetRevurdering: Boolean = false,
    val opprettetOppgave: Boolean = false,
    val nyBehandlingId: UUID? = null,
    val oppgaveId: UUID? = null,
    val forrigeBehandlingId: UUID,
)
