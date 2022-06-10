package no.nav.etterlatte.oppgave

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Saksbehandler

data class OppgaveListeDto (
    val oppgaver: List<Oppgave>
)

fun Route.oppgaveRoutes(repo: OppgaveDao) {
    route("/oppgaver"){
        get{
            val bruker = Kontekst.get().AppUser

            if( bruker is Saksbehandler){
                repo.finnOppgaverForRoller(listOfNotNull(
                    Rolle.SAKSBEHANDLER.takeIf { bruker.harRolleSaksbehandler() },
                    Rolle.ATTESTANT.takeIf { bruker.harRolleAttestant() }
                )).let { OppgaveListeDto(it) }
                    .also { call.respond(it) }
            }else{
                call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}

