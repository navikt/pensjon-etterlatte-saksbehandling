package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import no.nav.etterlatte.Kontekst
import java.util.*

fun Route.behandlingRoutes(service: BehandlingService){
    get("/behandlinger") {
        call.respond(inTransaction { service.hentBehandlinger().map { BehandlingSammendrag(it.id, it.sakId, it.status) }.let { BehandlingSammendragListe(it) }})
    }
    get("/sak/{sakid}/behandlinger") {
        call.respond(inTransaction { service.hentBehandlingerISak(sakId).map { BehandlingSammendrag(it.id, it.sakId, it.status) }.let { BehandlingSammendragListe(it) }})
    }
    post("/behandlinger") { //Søk
        val behandlingsBehov = call.receive<BehandlingsBehov>()
        inTransaction { service.startBehandling(behandlingsBehov.sak, behandlingsBehov.opplysninger?: emptyList()) }.also { call.respondText(it.id.toString()) }
    }
    route("/behandlinger/{behandlingsid}") {
        get {
            call.respond(inTransaction { service.hentBehandling(behandlingsId)}?.let { DetaljertBehandling(it.id, it.sakId, it.grunnlag, it.vilkårsprøving, it.beregning, it.fastsatt) }?: HttpStatusCode.NotFound)
        }
        post("vilkaarsproeving") {
            inTransaction { service.vilkårsprøv(behandlingsId) }
            call.respond(HttpStatusCode.OK)

        }
        post("beregning") {
            val body = call.receive<Beregning>()
            inTransaction { service.beregn(behandlingsId, body) }
            call.respond(HttpStatusCode.OK)

        }
        post("grunnlag/{type}") {
            val body = call.receive<ObjectNode>()
            inTransaction { service.leggTilGrunnlag(behandlingsId, body ,call.parameters["type"]!!) }
            call.respond(HttpStatusCode.OK)
        }
    }
}


private fun <T> inTransaction(block:()->T): T = Kontekst.get().databasecontxt.inTransaction {
    block()
}

inline val PipelineContext<*, ApplicationCall>.behandlingsId get() = requireNotNull(call.parameters["behandlingsid"]).let { UUID.fromString(it) }
inline val PipelineContext<*, ApplicationCall>.sakId get() = requireNotNull(call.parameters["sakid"]).toLong()

