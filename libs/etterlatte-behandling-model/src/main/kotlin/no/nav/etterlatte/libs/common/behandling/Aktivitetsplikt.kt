package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.tidshendelser.JobbType
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
    val sakId: SakId,
    val frist: Tidspunkt,
    val behandlingsmaaned: YearMonth,
    val jobbType: JobbType,
)

data class OpprettOppgaveForAktivitetspliktVarigUnntakDto(
    val sakId: SakId,
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
