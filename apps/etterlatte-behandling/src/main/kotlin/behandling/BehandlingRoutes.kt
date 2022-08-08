package no.nav.etterlatte.behandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingService
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.libs.common.behandling.BehandlingListe
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.*

fun Route.behandlingRoutes(
    generellBehandlingService: GenerellBehandlingService,
    foerstegangsbehandlingService: FoerstegangsbehandlingService,
    revurderingService: RevurderingService

) {
    val logger = application.log


    route("/behandlinger") {

        get {
            call.respond(
                generellBehandlingService.hentBehandlinger()
                    .map {
                        BehandlingSammendrag(
                            it.id,
                            it.sak,
                            it.status,
                            if (it is Foerstegangsbehandling) it.soeknadMottattDato else it.behandlingOpprettet,
                            it.behandlingOpprettet
                        )
                    }
                    .let { BehandlingListe(it) }
            )
        }

        route("/{behandlingsid}") {
            get {
                generellBehandlingService.hentBehandlingstype(behandlingsId)?.let { type ->
                    when (type) {
                        BehandlingType.FØRSTEGANGSBEHANDLING -> {
                            with(foerstegangsbehandlingService.hentBehandling(behandlingsId)!!) {
                                DetaljertBehandling(
                                    id = id,
                                    sak = sak,
                                    behandlingOpprettet = behandlingOpprettet,
                                    sistEndret = sistEndret,
                                    soeknadMottattDato = soeknadMottattDato,
                                    innsender = persongalleri.innsender,
                                    soeker = persongalleri.soeker,
                                    gjenlevende = persongalleri.gjenlevende,
                                    avdoed = persongalleri.avdoed,
                                    soesken = persongalleri.soesken,
                                    gyldighetsproeving = gyldighetsproeving,
                                    status = status,
                                )
                            }
                        }
                        BehandlingType.REVURDERING -> {
                            with(revurderingService.hentRevurdering(behandlingsId)!!) {
                                DetaljertBehandling(
                                    id = id,
                                    sak = sak,
                                    behandlingOpprettet = behandlingOpprettet,
                                    sistEndret = sistEndret,
                                    soeknadMottattDato = behandlingOpprettet,
                                    innsender = persongalleri.innsender,
                                    soeker = persongalleri.soeker,
                                    gjenlevende = persongalleri.gjenlevende,
                                    avdoed = persongalleri.avdoed,
                                    soesken = persongalleri.soesken,
                                    gyldighetsproeving = null,
                                    status = status,
                                )
                            }
                        }
                    }
                }?.let { it1 ->
                    call.respond(it1)
                    logger.info("Henter detaljert for behandling: $behandlingsId: $it1")
                } ?: HttpStatusCode.NotFound
            }

            route("/hendelser") {

                route("/vedtak") {

                    get {
                        call.respond(
                            generellBehandlingService.hentHendelserIBehandling(behandlingsId)
                                .let { LagretHendelser(it) }
                        )
                    }

                    post("/{hendelse}") {
                        val body = call.receive<VedtakHendelse>()
                        generellBehandlingService.registrerVedtakHendelse(
                            behandlingsId,
                            body.vedtakId,
                            requireNotNull(call.parameters["hendelse"]),
                            body.inntruffet,
                            body.saksbehandler,
                            body.kommentar,
                            body.valgtBegrunnelse
                        )
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            post("/avbrytbehandling") {
                generellBehandlingService.avbrytBehandling(behandlingsId)
                call.respond(HttpStatusCode.OK)
            }

            post("/gyldigfremsatt") {
                val body = call.receive<GyldighetsResultat>()
                foerstegangsbehandlingService.lagreGyldighetsprøving(behandlingsId, body)
                call.respond(HttpStatusCode.OK)
            }

        }

        route("/sak") {

            get("/{sakid}") {
                call.respond(
                    generellBehandlingService.hentBehandlingerISak(sakId)
                        .map {
                            BehandlingSammendrag(
                                it.id,
                                it.sak,
                                it.status,
                                if (it is Foerstegangsbehandling) it.soeknadMottattDato else it.behandlingOpprettet,
                                it.behandlingOpprettet
                            )
                        }
                        .let { BehandlingListe(it) }
                )
            }

            delete("/{sakid}") {
                generellBehandlingService.slettBehandlingerISak(sakId)
                call.respond(HttpStatusCode.OK)
            }
        }

        // TODO: fases ut -> nytt endepunkt: /behandlinger/foerstegangsbehandling
        post {
            val behandlingsBehov = call.receive<BehandlingsBehov>()

            foerstegangsbehandlingService.startFoerstegangsbehandling(
                behandlingsBehov.sak,
                behandlingsBehov.persongalleri,
                behandlingsBehov.mottattDato,
            )
                .also { call.respondText(it.id.toString()) }
        }

        route("/foerstegangsbehandling") {

            get {
                call.respond(foerstegangsbehandlingService.hentBehandling(behandlingsId)?.let {
                    DetaljertBehandling(
                        it.id,
                        it.sak,
                        it.behandlingOpprettet,
                        it.sistEndret,
                        it.soeknadMottattDato,
                        it.persongalleri.innsender,
                        it.persongalleri.soeker,
                        it.persongalleri.gjenlevende,
                        it.persongalleri.avdoed,
                        it.persongalleri.soesken,
                        it.gyldighetsproeving,
                        it.status,
                    )
                } ?: HttpStatusCode.NotFound)
            }

            post {
                val behandlingsBehov = call.receive<BehandlingsBehov>()

                foerstegangsbehandlingService.startFoerstegangsbehandling(
                    behandlingsBehov.sak,
                    behandlingsBehov.persongalleri,
                    behandlingsBehov.mottattDato,
                )
                    .also { call.respondText(it.id.toString()) }
            }
        }


        route("/revurdering") {
            get {
                call.respond(revurderingService.hentRevurdering(behandlingsId)?.let {
                    DetaljertBehandling(
                        it.id,
                        it.sak,
                        it.behandlingOpprettet,
                        it.sistEndret,
                        it.behandlingOpprettet,
                        it.persongalleri.innsender,
                        it.persongalleri.soeker,
                        it.persongalleri.gjenlevende,
                        it.persongalleri.avdoed,
                        it.persongalleri.soesken,
                        null,
                        it.status,
                    )
                } ?: HttpStatusCode.NotFound)
            }


            // TODO: SLETT! DENNE!! DETTE ER KUN TIL TESTING! IKKE KJØR I PROD MED MINDRE DU VIL HA SPARKEN
            route("/{sakid}/") {
                delete {
                    revurderingService.slettRevurderingISak(sakId)
                }
            }

            route("/pdlhendelse") {

                // TODO: finne ut av hva som skal gjøres dersom det allerede finnes en revurdering som er under behandling når det kommer inn en hendelse
                route("/doedshendelse") {
                    post {
                        val doedshendelse = call.receive<Doedshendelse>()

                        call.respond(
                            generellBehandlingService.alleBehandlingerForSoekerMedFnr(doedshendelse.avdoedFnr)
                                .sortedByDescending { it.behandlingOpprettet }
                                .takeUnless { it.any { it.type == BehandlingType.REVURDERING && (it as Revurdering).revurderingsaarsak == RevurderingAarsak.SOEKER_DOD } }// TODO: fjern denne naar problemet med flere dødshendelser postet er løst
                                ?.takeIf {
                                    it.isNotEmpty()
                                }?.first {
                                    it.status in listOf(
                                        BehandlingStatus.OPPRETTET,
                                        BehandlingStatus.GYLDIG_SOEKNAD,
                                        BehandlingStatus.UNDER_BEHANDLING,
                                        BehandlingStatus.FATTET_VEDTAK,
                                        BehandlingStatus.ATTESTERT,
                                        BehandlingStatus.RETURNERT,
                                        BehandlingStatus.IVERKSATT,
                                    )
                                }?.let {
                                    revurderingService.startRevurdering(it, doedshendelse, RevurderingAarsak.SOEKER_DOD)
                                }
                                ?: HttpStatusCode.OK)
                    }
                }

            }
        }
    }

//TODO: fases ut -> nytt endepunkt: /behandlinger/foerstegangsbehandling
    post {
        val behandlingsBehov = call.receive<BehandlingsBehov>()

        foerstegangsbehandlingService.startFoerstegangsbehandling(
            behandlingsBehov.sak,
            behandlingsBehov.persongalleri,
            behandlingsBehov.mottattDato,
        )
            .also { call.respondText(it.id.toString()) }
    }


// TODO: fases ut -> nytt endepunkt: /behandlinger/sak/{sakid}
    route("/sak") {

        get("/{sakid}/behandlinger") {
            call.respond(
                generellBehandlingService.hentBehandlingerISak(sakId)
                    .map {
                        BehandlingSammendrag(
                            it.id,
                            it.sak,
                            it.status,
                            if (it is Foerstegangsbehandling) it.soeknadMottattDato else it.behandlingOpprettet,
                            it.behandlingOpprettet
                        )
                    }
                    .let { BehandlingListe(it) }
            )
        }

        delete("/{sakid}/behandlinger") {
            generellBehandlingService.slettBehandlingerISak(sakId)
            call.respond(HttpStatusCode.OK)
        }
    }


    post("/saker/{sakid}/hendelse/grunnlagendret") { //Søk
        generellBehandlingService.grunnlagISakEndret(sakId)
        call.respond(HttpStatusCode.OK)
    }


// TODO: fases ut -> nytt endepunkt: /foerstegangsbehandling/gyldigfremsatt
    post("gyldigfremsatt") {
        val body = call.receive<GyldighetsResultat>()
        foerstegangsbehandlingService.lagreGyldighetsprøving(behandlingsId, body)
        call.respond(HttpStatusCode.OK)
    }

// TODO: fases ut -> nytt endepunkt: /behandlinger/{behandlingsid}/avbrytbehandling/
    post("avbrytBehandling/{behandlingsid}") {
        generellBehandlingService.avbrytBehandling(behandlingsId)
        call.respond(HttpStatusCode.OK)
    }

}

inline val PipelineContext<*, ApplicationCall>.behandlingsId
    get() = requireNotNull(call.parameters["behandlingsid"]).let {
        UUID.fromString(
            it
        )
    }
inline val PipelineContext<*, ApplicationCall>.sakId get() = requireNotNull(call.parameters["sakid"]).toLong()

data class VedtakHendelse(
    val vedtakId: Long,
    val saksbehandler: String?,
    val inntruffet: Tidspunkt,
    val kommentar: String?,
    val valgtBegrunnelse: String?,
)

data class LagretHendelser(
    val hendelser: List<LagretHendelse>,
)
