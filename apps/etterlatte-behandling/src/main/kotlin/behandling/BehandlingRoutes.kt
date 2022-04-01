package no.nav.etterlatte.behandling

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendragListe
import no.nav.etterlatte.libs.common.behandling.Beregning
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.SoeknadMottattDato
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import java.time.LocalDateTime
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import java.util.*

fun Route.behandlingRoutes(service: BehandlingService) {
    get("/behandlinger") {
        call.respond(
            service.hentBehandlinger().map { BehandlingSammendrag(it.id, it.sak, it.status, mapDate(it)) }
                .let { BehandlingSammendragListe(it) }
        )
    }
    get("/sak/{sakid}/behandlinger") {
        call.respond(
            service.hentBehandlingerISak(sakId)
                .map {
                    println(it.grunnlag)
                    BehandlingSammendrag(it.id, it.sak, it.status, mapDate(it))
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
                behandlingsBehov.opplysninger ?: emptyList()
            )
        .also { call.respondText(it.id.toString()) }
    }
    route("/behandlinger/{behandlingsid}") {
        get {
            call.respond(service.hentBehandling(behandlingsId)?.let {
                DetaljertBehandling(
                    it.id,
                    it.sak,
                    it.grunnlag,
                    it.gyldighetsprøving,
                    it.vilkårsprøving,
                    it.beregning,
                    it.fastsatt
                )
            } ?: HttpStatusCode.NotFound)
        }

        post("grunnlag") {
            val body = call.receive<LeggTilOpplysningerRequest>()
            service.leggTilGrunnlagFraRegister(behandlingsId, body.opplysninger)
            call.respond(HttpStatusCode.OK)
        }

        post("lagregyldighetsproeving") {
            val body = call.receive<GyldighetsResultat>()
            service.lagreGyldighetsprøving(behandlingsId, body)
            call.respond(HttpStatusCode.OK)
        }

        post("lagrevilkaarsproeving") {
            val body = call.receive<VilkaarResultat>()
            service.lagreVilkårsprøving(behandlingsId, body)
            call.respond(HttpStatusCode.OK)
        }

        post("beregning") {
            val body = call.receive<Beregning>()
             service.beregn(behandlingsId, body)
            call.respond(HttpStatusCode.OK)
        }

        post("avbrytBehandling/{behandlingsid}") {
             service.avbrytBehandling(behandlingsId)
            call.respond(HttpStatusCode.OK)
        }
    }
}

private fun mapDate(behandling: Behandling): LocalDateTime? {
    if (behandling.grunnlag.isEmpty()) {
        return null
    }
    val dato =
        behandling.grunnlag.find { it.opplysningType == Opplysningstyper.SOEKNAD_MOTTATT_DATO }?.opplysning?.let {
            objectMapper.readValue(it.toString(), SoeknadMottattDato::class.java)
        }
    return dato?.mottattDato
}

inline val PipelineContext<*, ApplicationCall>.behandlingsId
    get() = requireNotNull(call.parameters["behandlingsid"]).let {
        UUID.fromString(
            it
        )
    }
inline val PipelineContext<*, ApplicationCall>.sakId get() = requireNotNull(call.parameters["sakid"]).toLong()

