package no.nav.etterlatte.behandling

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendragListe
import no.nav.etterlatte.libs.common.behandling.Beregning
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.SoeknadMottattDato
import java.time.LocalDateTime
import no.nav.etterlatte.libs.common.objectMapper
import java.util.*

fun Route.behandlingRoutes(service: BehandlingService) {
    get("/behandlinger") {
        call.respond(inTransaction {
            service.hentBehandlinger().map { BehandlingSammendrag(it.id, it.sak, it.status, mapDate(it)) }
                .let { BehandlingSammendragListe(it) }
        })
    }
    get("/sak/{sakid}/behandlinger") {
        call.respond(inTransaction {
            service.hentBehandlingerISak(sakId)
                .map {
                    println(it.grunnlag)
                    BehandlingSammendrag(it.id, it.sak, it.status, mapDate(it))
                }
                .let { BehandlingSammendragListe(it) }
        })
    }

    delete ("/sak/{sakid}/behandlinger") {
        inTransaction { service.slettBehandlingerISak(sakId) }
        call.respond(HttpStatusCode.OK)
    }

    post("/behandlinger") { //Søk
        val behandlingsBehov = call.receive<BehandlingsBehov>()
        println(behandlingsBehov.sak)
        println(behandlingsBehov.opplysninger)
        println(behandlingsBehov)
        inTransaction {
            service.startBehandling(
                behandlingsBehov.sak,
                behandlingsBehov.opplysninger ?: emptyList()
            )
        }.also { call.respondText(it.id.toString()) }
    }
    route("/behandlinger/{behandlingsid}") {
        get {
            call.respond(inTransaction { service.hentBehandling(behandlingsId) }?.let {
                DetaljertBehandling(
                    it.id,
                    it.sak,
                    it.grunnlag,
                    it.vilkårsprøving,
                    it.beregning,
                    it.fastsatt
                )
            } ?: HttpStatusCode.NotFound)
        }

        post("grunnlag") {
            val body = call.receive<LeggTilOpplysningerRequest>()
            inTransaction { service.leggTilGrunnlagFraRegister(behandlingsId, body.opplysninger) }
            call.respond(HttpStatusCode.OK)
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
    }
}

private fun mapDate(behandling: Behandling): LocalDateTime? {
    if (behandling.grunnlag.isEmpty()) {
        return null
    }
    val dato = behandling.grunnlag.find { it.opplysningType == Opplysningstyper.SOEKNAD_MOTTATT_DATO }?.opplysning?.let {
        objectMapper.readValue(it.toString(), SoeknadMottattDato::class.java)
    }
    return dato?.mottattDato
}

private fun <T> inTransaction(block: () -> T): T = Kontekst.get().databasecontxt.inTransaction {
    block()
}

inline val PipelineContext<*, ApplicationCall>.behandlingsId
    get() = requireNotNull(call.parameters["behandlingsid"]).let {
        UUID.fromString(
            it
        )
    }
inline val PipelineContext<*, ApplicationCall>.sakId get() = requireNotNull(call.parameters["sakid"]).toLong()

