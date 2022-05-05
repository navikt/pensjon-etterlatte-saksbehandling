package no.nav.etterlatte.behandling

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendragListe
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import java.util.*

fun Route.behandlingRoutes(service: BehandlingService) {
    get("/behandlinger") {
        call.respond(
            service.hentBehandlinger().map { BehandlingSammendrag(it.id, it.sak, it.status, it.soeknadMottattDato) }
                .let { BehandlingSammendragListe(it) }
        )
    }
    get("/sak/{sakid}/behandlinger") {
        call.respond(
            service.hentBehandlingerISak(sakId)
                .map {
                    BehandlingSammendrag(it.id, it.sak, it.status, it.soeknadMottattDato)
                }
                .let { BehandlingSammendragListe(it) }
        )
    }

    delete("/sak/{sakid}/behandlinger") {
        service.slettBehandlingerISak(sakId)
        call.respond(HttpStatusCode.OK)
    }

    post("/behandlinger") { //Søk
        val behandlingsBehov = call.receive<BehandlingsBehov>()

            service.startBehandling(
                behandlingsBehov.sak,
            )
        .also { call.respondText(it.id.toString()) }
    }
    route("/behandlinger/{behandlingsid}") {
        get {
            call.respond(service.hentBehandling(behandlingsId)?.let {
                DetaljertBehandling(
                    it.id,
                    it.sak,
                )
            } ?: HttpStatusCode.NotFound)
        }


        post("lagregyldighetsproeving") {
            val body = call.receive<GyldighetsResultat>()
            service.lagreGyldighetsprøving(behandlingsId, body)
            call.respond(HttpStatusCode.OK)
        }


        post("avbrytBehandling/{behandlingsid}") {
             service.avbrytBehandling(behandlingsId)
            call.respond(HttpStatusCode.OK)
        }
    }
}

inline val PipelineContext<*, ApplicationCall>.behandlingsId
    get() = requireNotNull(call.parameters["behandlingsid"]).let {
        UUID.fromString(
            it
        )
    }
inline val PipelineContext<*, ApplicationCall>.sakId get() = requireNotNull(call.parameters["sakid"]).toLong()

