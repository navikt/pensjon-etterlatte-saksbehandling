package no.nav.etterlatte.oppgaveny

import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.*

data class OppgaveNy(
    val id: UUID,
    val status: Status,
    val enhet: String,
    val sakId: Long,
    val saksbehandler: String?,
    val referanse: String?,
    val merknad: String?,
    val opprettet: Tidspunkt
)

enum class Status {
    NY,
    UNDER_BEHANDLING,
    FERDIGSTILT
}

fun opprettNyOppgaveMedReferanseOgSak(referanse: String, sak: Sak): OppgaveNy {
    return OppgaveNy(
        id = UUID.randomUUID(),
        status = Status.NY,
        enhet = sak.enhet,
        sakId = sak.id,
        saksbehandler = null,
        referanse = referanse,
        merknad = null,
        opprettet = Tidspunkt.now()
    )
}