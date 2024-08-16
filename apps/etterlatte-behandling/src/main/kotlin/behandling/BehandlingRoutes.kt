package no.nav.etterlatte.behandling

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.behandling.domain.toBehandlingSammendrag
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingsBehov
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandetRequest
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.NyBehandlingRequest
import no.nav.etterlatte.libs.common.behandling.RedigertFamilieforhold
import no.nav.etterlatte.libs.common.behandling.SendBrev
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.norskKlokke
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.lagGrunnlagsopplysning
import no.nav.etterlatte.libs.ktor.route.routeLogger
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.sak.UtlandstilknytningRequest
import no.nav.etterlatte.tilgangsstyring.kunSaksbehandlerMedSkrivetilgang
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

enum class BehandlingFeatures(
    private val key: String,
) : FeatureToggle {
    OMGJOERE_AVSLAG("omgjoer-avslag"),
    ;

    override fun key(): String = key
}

data class OmgjoeringRequest(
    val skalKopiere: Boolean,
)

internal fun Route.behandlingRoutes(
    behandlingService: BehandlingService,
    gyldighetsproevingService: GyldighetsproevingService,
    kommerBarnetTilGodeService: KommerBarnetTilGodeService,
    behandlingFactory: BehandlingFactory,
    featureToggleService: FeatureToggleService,
) {
    val logger = routeLogger

    post("/api/behandling") {
        val request = call.receive<NyBehandlingRequest>()
        request.validerPersongalleri()

        val gjeldendeEnhet = inTransaction { behandlingFactory.finnGjeldendeEnhet(request.persongalleri, request.sakType) }

        kunSkrivetilgang(enhetNr = gjeldendeEnhet) {
            val behandling = behandlingFactory.opprettSakOgBehandlingForOppgave(request, brukerTokenInfo)
            call.respondText(behandling.id.toString())
        }
    }

    post("/api/behandling/omgjoer-avslag-avbrudd/{$SAKID_CALL_PARAMETER}") {
        if (!featureToggleService.isEnabled(BehandlingFeatures.OMGJOERE_AVSLAG, false)) {
            throw IkkeTillattException("NOT_SUPPORTED", "Omgjøring av førstegangsbehandling er ikke støttet")
        }
        kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
            val skalKopiereRequest = call.receive<OmgjoeringRequest>()
            val behandlingOgOppgave = behandlingFactory.opprettOmgjoeringAvslag(sakId, saksbehandler, skalKopiereRequest.skalKopiere)
            call.respond(behandlingOgOppgave.toBehandlingSammendrag())
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
                val body = call.receive<JaNeiMedBegrunnelse>()
                when (
                    val lagretGyldighetsResultat =
                        inTransaction {
                            gyldighetsproevingService.lagreGyldighetsproeving(
                                behandlingId,
                                body,
                                if (brukerTokenInfo is Saksbehandler) {
                                    Grunnlagsopplysning.Saksbehandler(
                                        brukerTokenInfo.ident(),
                                        Tidspunkt.from(norskKlokke()),
                                    )
                                } else {
                                    Grunnlagsopplysning.Gjenny.create(brukerTokenInfo.ident())
                                },
                            )
                        }
                ) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(HttpStatusCode.OK, lagretGyldighetsResultat)
                }
            }
        }

        post("/kommerbarnettilgode") {
            kunSkrivetilgang {
                val body = call.receive<JaNeiMedBegrunnelse>()
                val kommerBarnetTilgode =
                    KommerBarnetTilgode(
                        body.svar,
                        body.begrunnelse,
                        brukerTokenInfo.lagGrunnlagsopplysning(),
                        behandlingId,
                    )

                try {
                    inTransaction { kommerBarnetTilGodeService.lagreKommerBarnetTilgode(kommerBarnetTilgode) }
                    call.respond(HttpStatusCode.OK, kommerBarnetTilgode)
                } catch (e: TilstandException.UgyldigTilstand) {
                    logger.warn("Ugyldig tilstand for lagre kommer barnet til gode", e)
                    call.respond(HttpStatusCode.BadRequest, "Kunne ikke endre på feltet")
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
                logger.debug("Prøver å fastsette virkningstidspunkt")
                val body = call.receive<VirkningstidspunktRequest>()
                logger.debug("Tok imot virkningstidspunktrequest")
                logger.debug("Virkningstidspunktrequest hadde dato {}", body.dato)

                val erGyldigVirkningstidspunkt =
                    inTransaction {
                        runBlocking {
                            behandlingService.erGyldigVirkningstidspunkt(
                                behandlingId,
                                brukerTokenInfo,
                                body,
                            )
                        }
                    }

                if (!erGyldigVirkningstidspunkt) {
                    logger.warn("Ugyldig virkningstidspunkt")
                    return@post call.respond(HttpStatusCode.BadRequest, "Ugyldig virkningstidspunkt")
                }
                try {
                    logger.debug("Gyldig virkningstidspunkt")
                    val virkningstidspunkt =
                        inTransaction {
                            runBlocking {
                                try {
                                    behandlingService.oppdaterVirkningstidspunkt(
                                        behandlingId,
                                        body.dato,
                                        brukerTokenInfo,
                                        body.begrunnelse!!,
                                        kravdato = body.kravdato,
                                    )
                                } catch (e: Exception) {
                                    logger.warn("Kunne ikke oppdatere virkningstidspunktet", e)
                                    throw e
                                }
                            }
                        }

                    call.respondText(
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.OK,
                        text = FastsettVirkningstidspunktResponse.from(virkningstidspunkt).toJson(),
                    )
                } catch (e: TilstandException.UgyldigTilstand) {
                    logger.warn("Ugyldig tilstand for lagre virkningstidspunkt", e)
                    call.respond(HttpStatusCode.BadRequest, "Kan ikke endre feltet")
                }
            }
        }

        post("/utlandstilknytning") {
            kunSkrivetilgang {
                logger.debug("Prøver å fastsette utlandstilknytning")
                val body = call.receive<UtlandstilknytningRequest>()

                try {
                    val utlandstilknytning =
                        Utlandstilknytning(
                            type = body.utlandstilknytningType,
                            kilde = brukerTokenInfo.lagGrunnlagsopplysning(),
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
                    logger.warn("Ugyldig tilstand for lagre utlandstilknytning", e)
                    call.respond(HttpStatusCode.BadRequest, "Kan ikke endre feltet")
                }
            }
        }

        post("/viderefoert-opphoer") {
            kunSkrivetilgang {
                logger.debug("Prøver å fastsette videreført opphør")
                val body = call.receive<ViderefoertOpphoerRequest>()

                try {
                    val viderefoertOpphoer =
                        ViderefoertOpphoer(
                            skalViderefoere = body.skalViderefoere,
                            behandlingId = behandlingId,
                            dato = body.dato,
                            begrunnelse = body.begrunnelse,
                            vilkaar = body.vilkaar,
                            kilde = brukerTokenInfo.lagGrunnlagsopplysning(),
                            kravdato = body.kravdato,
                            aktiv = true,
                        )

                    inTransaction {
                        behandlingService.oppdaterViderefoertOpphoer(behandlingId, viderefoertOpphoer)
                    }

                    call.respondText(
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.OK,
                        text = viderefoertOpphoer.toJson(),
                    )
                } catch (e: TilstandException.UgyldigTilstand) {
                    logger.warn("Ugyldig tilstand for lagre videreført opphør", e)
                    call.respond(HttpStatusCode.BadRequest, "Kan ikke endre feltet")
                }
            }
        }

        delete("/viderefoert-opphoer") {
            kunSkrivetilgang {
                logger.debug("Prøver å fjerne videreført opphør")

                inTransaction {
                    behandlingService.fjernViderefoertOpphoer(
                        behandlingId,
                        brukerTokenInfo.lagGrunnlagsopplysning(),
                    )
                }
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/boddellerarbeidetutlandet") {
            kunSkrivetilgang {
                logger.debug("Prøver å fastsette boddEllerArbeidetUtlandet")
                val body = call.receive<BoddEllerArbeidetUtlandetRequest>()

                try {
                    val boddEllerArbeidetUtlandet =
                        BoddEllerArbeidetUtlandet(
                            boddEllerArbeidetUtlandet = body.boddEllerArbeidetUtlandet,
                            kilde = brukerTokenInfo.lagGrunnlagsopplysning(),
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
                    logger.warn("Ugyldig tilstand for lagre boddellerarbeidetutlandet", e)
                    call.respond(HttpStatusCode.BadRequest, "Kan ikke endre feltet")
                }
            }
        }

        post("/oppdater-grunnlag") {
            kunSkrivetilgang {
                inTransaction {
                    behandlingService.oppdaterGrunnlagOgStatus(behandlingId, brukerTokenInfo)
                }
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/rediger-familieforhold") {
            kunSkrivetilgang {
                val redigertFamilie = call.receive<RedigertFamilieforhold>()
                behandlingService.endrePersongalleri(behandlingId, brukerTokenInfo, redigertFamilie)
                call.respond(HttpStatusCode.OK)
            }
        }

        put("/skal-sende-brev") {
            kunSkrivetilgang {
                val request = call.receive<SendBrev>()
                behandlingService.endreSkalSendeBrev(behandlingId, request.sendBrev)
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
                val behandlingsBehov = call.receive<BehandlingsBehov>()

                kunSkrivetilgang(sakId = behandlingsBehov.sakId) {
                    val request =
                        inTransaction {
                            behandlingFactory.hentDataForOpprettBehandling(behandlingsBehov.sakId)
                        }
                    when (
                        val behandling =
                            inTransaction {
                                behandlingFactory.opprettBehandling(
                                    behandlingsBehov.sakId,
                                    behandlingsBehov.persongalleri,
                                    behandlingsBehov.mottattDato,
                                    Vedtaksloesning.GJENNY,
                                    request = request,
                                )
                            }?.also { it.sendMeldingForHendelse() }
                                ?.behandling
                    ) {
                        null -> call.respond(HttpStatusCode.NotFound)
                        else -> call.respondText(behandling.id.toString())
                    }
                }
            }
        }
    }
}

data class ViderefoertOpphoerRequest(
    @JsonProperty("dato") private val _dato: String?,
    val skalViderefoere: JaNei,
    val begrunnelse: String?,
    val kravdato: LocalDate? = null,
    val vilkaar: VilkaarType?,
) {
    val dato: YearMonth? = _dato?.tilYearMonth()
}

data class ViderefoertOpphoer(
    val skalViderefoere: JaNei,
    val behandlingId: UUID,
    val dato: YearMonth?,
    val vilkaar: VilkaarType?,
    val begrunnelse: String?,
    val kilde: Grunnlagsopplysning.Kilde,
    val kravdato: LocalDate?,
    val aktiv: Boolean = true,
)
