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
        call.respond(inTransaction { service.hentBehandlinger().map { BehandlingSammendrag(it.id, it.sak, it.status) }.let { BehandlingSammendragListe(it) }})
    }

    post("/behandlinger") { //Søk
        val behandlingsBehov = call.receive<BehandlingsBehov>()
        inTransaction { service.startBehandling(behandlingsBehov.sak) }.also { call.respond(it.id) }
    }
    route("/behandlinger/{behandlingsid}") {
        get {
            call.respond(inTransaction { service.hentBehandling(behandlingsId)}?.let { DetaljertBehandling(it.id, it.sak, it.grunnlag, it.vilkårsprøving, it.beregning, it.fastsatt) }?: HttpStatusCode.NotFound)
        }
        post("vilkaarsproeving") {
            val body = call.receive<Vilkårsprøving>()
            inTransaction { service.vilkårsprøv(behandlingsId, body) }
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


data class BehandlingsBehov(val sak: String)

private fun <T> inTransaction(block:()->T): T = Kontekst.get().databasecontxt.inTransaction {
    block()
}

inline val PipelineContext<*, ApplicationCall>.behandlingsId get() = requireNotNull(call.parameters["behandlingsid"]).let { UUID.fromString(it) }

