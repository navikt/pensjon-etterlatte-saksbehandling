package no.nav.etterlatte.oppgave

import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import java.util.UUID

class OppgaveAlleredeTildeltException(oppgaveId: UUID) : ForespoerselException(
    status = HttpStatusCode.Conflict.value,
    code = "OPPGAVEN_HAR_ALLEREDE_SAKSBEHANDLER",
    detail = "Oppgaven er allerede tildelt en saksbehandler, oppgave-ID: $oppgaveId",
)
