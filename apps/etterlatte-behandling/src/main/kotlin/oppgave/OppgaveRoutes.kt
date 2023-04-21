package no.nav.etterlatte.oppgave

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Saksbehandler
import no.nav.etterlatte.behandling.OppgaveStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.oppgave.domain.Handling
import no.nav.etterlatte.oppgave.domain.Oppgave
import no.nav.etterlatte.oppgave.domain.OppgaveType
import java.util.*

internal fun Route.oppgaveRoutes(service: OppgaveService) {
    route("/api/oppgaver") {
        get {
            when (val bruker = Kontekst.get().AppUser) {
                is Saksbehandler -> call.respond(tilOppgaveListeDto(service.finnOppgaverForBruker(bruker)))
                else -> call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}

fun tilOppgaveListeDto(oppgaver: List<Oppgave>): OppgaveListeDto {
    return oppgaver.map {
        OppgaveDTO.fraOppgave(it)
    }.let { OppgaveListeDto(it) }
}

data class OppgaveListeDto(
    val oppgaver: List<OppgaveDTO>
)

data class OppgaveDTO(
    val behandlingId: UUID?,
    val sakId: Long,
    val status: BehandlingStatus?,
    val oppgaveStatus: OppgaveStatus?,
    val soeknadType: String,
    val oppgaveType: OppgaveType,
    val regdato: String,
    val fristdato: String,
    val fnr: String,
    val beskrivelse: String,
    val saksbehandler: String,
    val handling: Handling,
    val enhet: String? = null,
    val merknad: String? = null
) {
    companion object {
        fun fraOppgave(oppgave: Oppgave): OppgaveDTO {
            return when (oppgave) {
                is Oppgave.Grunnlagsendringsoppgave -> OppgaveDTO(
                    behandlingId = null,
                    sakId = oppgave.sak.id,
                    status = BehandlingStatus.OPPRETTET,
                    oppgaveStatus = oppgave.oppgaveStatus,
                    soeknadType = oppgave.sak.sakType.toString(),
                    oppgaveType = oppgave.oppgaveType,
                    regdato = oppgave.registrertDato.toLocalDatetimeUTC().toString(),
                    fristdato = oppgave.fristDato.atStartOfDay().toString(),
                    fnr = oppgave.sak.ident,
                    beskrivelse = oppgave.beskrivelse,
                    saksbehandler = "",
                    handling = oppgave.handling,
                    enhet = oppgave.sak.enhet
                )
                is Oppgave.BehandlingOppgave -> OppgaveDTO(
                    behandlingId = oppgave.behandlingId,
                    sakId = oppgave.sak.id,
                    status = oppgave.behandlingStatus,
                    oppgaveStatus = oppgave.oppgaveStatus,
                    soeknadType = oppgave.sak.sakType.toString(),
                    oppgaveType = oppgave.oppgaveType,
                    regdato = oppgave.registrertDato.toLocalDatetimeUTC().toString(),
                    fristdato = oppgave.fristDato.atStartOfDay().toString(),
                    fnr = oppgave.sak.ident,
                    beskrivelse = "",
                    saksbehandler = "",
                    handling = oppgave.handling,
                    merknad = oppgave.merknad,
                    enhet = oppgave.sak.enhet
                )
            }
        }
    }
}