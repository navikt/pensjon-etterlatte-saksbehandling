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

enum class JobbType(
    val beskrivelse: String,
) {
    OMS_DOED_6MND("Vurdering av aktivitetsplikt OMS etter 6 mnd"),
    OMS_DOED_12MND("Vurdering av aktivitetsplikt OMS etter 12 mnd"),
    OMS_DOED_6MND_INFORMASJON_VARIG_UNNTAK("Infobrev om OMS etter 6 mnd - varig unntak"),
}

data class OpprettRevurderingForAktivitetspliktDto(
    val sakId: no.nav.etterlatte.libs.common.sak.SakId,
    val frist: Tidspunkt,
    val behandlingsmaaned: YearMonth,
    val jobbType: JobbType,
)

data class OpprettOppgaveForAktivitetspliktVarigUnntakDto(
    val sakId: no.nav.etterlatte.libs.common.sak.SakId,
    val referanse: String? = null,
    val frist: Tidspunkt,
    val jobbType: JobbType,
)

data class OpprettRevurderingForAktivitetspliktResponse(
    val opprettetRevurdering: Boolean = false,
    val opprettetOppgave: Boolean = false,
    val nyBehandlingId: UUID? = null,
    val oppgaveId: UUID? = null,
    val forrigeBehandlingId: UUID,
)

data class OpprettOppgaveForAktivitetspliktVarigUnntakResponse(
    val opprettetOppgave: Boolean = false,
    val oppgaveId: UUID? = null,
)
