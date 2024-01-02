package no.nav.etterlatte.behandling

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktService
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingsBehov
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.NyBehandlingRequest
import no.nav.etterlatte.libs.common.behandling.OpprettAktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.hentNavidentFraToken
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.sak.UtlandstilknytningRequest
import no.nav.etterlatte.tilgangsstyring.kunSaksbehandlerMedSkrivetilgang
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang

internal fun Route.behandlingRoutes(
    behandlingService: BehandlingService,
    gyldighetsproevingService: GyldighetsproevingService,
    kommerBarnetTilGodeService: KommerBarnetTilGodeService,
    aktivitetspliktService: AktivitetspliktService,
    behandlingFactory: BehandlingFactory,
) {
    val logger = application.log

    post("/api/behandling") {
        kunSaksbehandlerMedSkrivetilgang {
            val request = call.receive<NyBehandlingRequest>()
            val behandling = behandlingFactory.opprettSakOgBehandlingForOppgave(request)
            call.respondText(behandling.id.toString())
        }
    }

    route("/api/behandling/{$BEHANDLINGID_CALL_PARAMETER}/") {
        get {
            val detaljertBehandlingDTO =
                behandlingService.hentDetaljertBehandlingMedTilbehoer(behandlingId, brukerTokenInfo)
            call.respond(detaljertBehandlingDTO)
        }

        post("/gyldigfremsatt") {
            kunSkrivetilgang {
                hentNavidentFraToken { navIdent ->
                    val body = call.receive<JaNeiMedBegrunnelse>()
                    when (
                        val lagretGyldighetsResultat =
                            inTransaction {
                                gyldighetsproevingService.lagreGyldighetsproeving(
                                    behandlingId,
                                    navIdent,
                                    body,
                                )
                            }
                    ) {
                        null -> call.respond(HttpStatusCode.NotFound)
                        else -> call.respond(HttpStatusCode.OK, lagretGyldighetsResultat)
                    }
                }
            }
        }

        post("/kommerbarnettilgode") {
            kunSkrivetilgang {
                hentNavidentFraToken { navIdent ->
                    val body = call.receive<JaNeiMedBegrunnelse>()
                    val kommerBarnetTilgode =
                        KommerBarnetTilgode(
                            body.svar,
                            body.begrunnelse,
                            Grunnlagsopplysning.Saksbehandler.create(navIdent),
                            behandlingId,
                        )

                    try {
                        inTransaction { kommerBarnetTilGodeService.lagreKommerBarnetTilgode(kommerBarnetTilgode) }
                        call.respond(HttpStatusCode.OK, kommerBarnetTilgode)
                    } catch (e: TilstandException.UgyldigTilstand) {
                        call.respond(HttpStatusCode.BadRequest, "Kunne ikke endre på feltet")
                    }
                }
            }
        }

        post("/avbryt") {
            kunSkrivetilgang {
                inTransaction { behandlingService.avbrytBehandling(behandlingId, brukerTokenInfo) }
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/virkningstidspunkt") {
            kunSkrivetilgang {
                hentNavidentFraToken { navIdent ->
                    logger.debug("Prøver å fastsette virkningstidspunkt")
                    val body = call.receive<VirkningstidspunktRequest>()

                    val erGyldigVirkningstidspunkt =
                        behandlingService.erGyldigVirkningstidspunkt(
                            behandlingId,
                            brukerTokenInfo,
                            body,
                        )
                    if (!erGyldigVirkningstidspunkt) {
                        return@post call.respond(HttpStatusCode.BadRequest, "Ugyldig virkningstidspunkt")
                    }

                    try {
                        val virkningstidspunkt =
                            inTransaction {
                                behandlingService.oppdaterVirkningstidspunkt(
                                    behandlingId,
                                    body.dato,
                                    navIdent,
                                    body.begrunnelse!!,
                                    kravdato = body.kravdato,
                                )
                            }

                        call.respondText(
                            contentType = ContentType.Application.Json,
                            status = HttpStatusCode.OK,
                            text = FastsettVirkningstidspunktResponse.from(virkningstidspunkt).toJson(),
                        )
                    } catch (e: TilstandException.UgyldigTilstand) {
                        call.respond(HttpStatusCode.BadRequest, "Kan ikke endre feltet")
                    }
                }
            }
        }

        post("/utlandstilknytning") {
            kunSkrivetilgang {
                hentNavidentFraToken { navIdent ->
                    logger.debug("Prøver å fastsette utlandstilknytning")
                    val body = call.receive<UtlandstilknytningRequest>()

                    try {
                        val utlandstilknytning =
                            Utlandstilknytning(
                                type = body.utlandstilknytningType,
                                kilde = Grunnlagsopplysning.Saksbehandler.create(navIdent),
                                begrunnelse = body.begrunnelse,
                            )

                        inTransaction {
                            behandlingService.oppdaterUtlandstilknytning(behandlingId, utlandstilknytning)
                        }

                        call.respondText(
                            contentType = ContentType.Application.Json,
                            status = HttpStatusCode.OK,
                            text = utlandstilknytning.toJson(),
                        )
                    } catch (e: TilstandException.UgyldigTilstand) {
                        call.respond(HttpStatusCode.BadRequest, "Kan ikke endre feltet")
                    }
                }
            }
        }

        post("/boddellerarbeidetutlandet") {
            kunSkrivetilgang {
                hentNavidentFraToken { navIdent ->
                    logger.debug("Prøver å fastsette boddEllerArbeidetUtlandet")
                    val body = call.receive<BoddEllerArbeidetUtlandetRequest>()

                    try {
                        val boddEllerArbeidetUtlandet =
                            BoddEllerArbeidetUtlandet(
                                boddEllerArbeidetUtlandet = body.boddEllerArbeidetUtlandet,
                                kilde = Grunnlagsopplysning.Saksbehandler.create(navIdent),
                                begrunnelse = body.begrunnelse,
                                boddArbeidetIkkeEosEllerAvtaleland = body.boddArbeidetIkkeEosEllerAvtaleland,
                                boddArbeidetEosNordiskKonvensjon = body.boddArbeidetEosNordiskKonvensjon,
                                boddArbeidetAvtaleland = body.boddArbeidetAvtaleland,
                                vurdereAvoededsTrygdeavtale = body.vurdereAvoededsTrygdeavtale,
                                skalSendeKravpakke = body.skalSendeKravpakke,
                            )

                        inTransaction {
                            behandlingService.oppdaterBoddEllerArbeidetUtlandet(
                                behandlingId,
                                boddEllerArbeidetUtlandet,
                            )
                        }

                        call.respondText(
                            contentType = ContentType.Application.Json,
                            status = HttpStatusCode.OK,
                            text = boddEllerArbeidetUtlandet.toJson(),
                        )
                    } catch (e: TilstandException.UgyldigTilstand) {
                        call.respond(HttpStatusCode.BadRequest, "Kan ikke endre feltet")
                    }
                }
            }
        }

        route("/aktivitetsplikt") {
            get {
                val result = aktivitetspliktService.hentAktivitetspliktOppfolging(behandlingId)
                call.respond(result ?: HttpStatusCode.NoContent)
            }

            post {
                kunSkrivetilgang {
                    hentNavidentFraToken { navIdent ->
                        val oppfolging = call.receive<OpprettAktivitetspliktOppfolging>()

                        try {
                            val result =
                                aktivitetspliktService.lagreAktivitetspliktOppfolging(
                                    behandlingId,
                                    oppfolging,
                                    navIdent,
                                )
                            call.respond(result)
                        } catch (e: TilstandException.UgyldigTilstand) {
                            call.respond(HttpStatusCode.BadRequest, "Kunne ikke endre på feltet")
                        }
                    }
                }
            }
        }

        post("/oppdater-grunnlag") {
            kunSkrivetilgang {
                inTransaction {
                    behandlingService.oppdaterGrunnlagOgStatus(behandlingId)
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    route("/behandlinger") {
        route("/{$BEHANDLINGID_CALL_PARAMETER}") {
            get {
                logger.info("Henter detaljert behandling for behandling med id=$behandlingId")
                when (val behandling = behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo)) {
                    is DetaljertBehandling -> call.respond(behandling)
                    else -> call.respond(HttpStatusCode.NotFound, "Fant ikke behandling med id=$behandlingId")
                }
            }

            post("/gyldigfremsatt") {
                kunSkrivetilgang {
                    val body = call.receive<GyldighetsResultat>()
                    gyldighetsproevingService.lagreGyldighetsproeving(behandlingId, body)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        route("/opprettbehandling") {
            post {
                kunSkrivetilgang {
                    val behandlingsBehov = call.receive<BehandlingsBehov>()

                    when (
                        val behandling =
                            inTransaction {
                                behandlingFactory.opprettBehandling(
                                    behandlingsBehov.sakId,
                                    behandlingsBehov.persongalleri,
                                    behandlingsBehov.mottattDato,
                                    Vedtaksloesning.GJENNY,
                                )
                            }?.behandling
                    ) {
                        null -> call.respond(HttpStatusCode.NotFound)
                        else -> call.respondText(behandling.id.toString())
                    }
                }
            }
        }
    }
}
