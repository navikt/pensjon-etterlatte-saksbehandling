package no.nav.etterlatte.sak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.BehandlingRequestLogger
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringsListe
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.behandling.FoersteVirkDto
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.sak.HentSakerRequest
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.SakslisteDTO
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.kunSaksbehandler
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.medBody
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.tilgangsstyring.kunSaksbehandlerMedSkrivetilgang
import no.nav.etterlatte.tilgangsstyring.withFoedselsnummerInternal
import org.slf4j.LoggerFactory
import java.util.UUID

const val KJOERING = "kjoering"
const val ANTALL = "antall"

internal fun Route.sakSystemRoutes(
    tilgangService: TilgangService,
    sakService: SakService,
    behandlingService: BehandlingService,
    requestLogger: BehandlingRequestLogger,
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    route("/saker") {
        post("/{$KJOERING}/{$ANTALL}") {
            kunSystembruker {
                val kjoering = call.parameters[KJOERING]!!
                val antall = call.parameters[ANTALL]!!.toInt()
                val request = call.receive<HentSakerRequest>()
                val spesifikkeSaker = request.spesifikkeSaker
                val ekskluderteSaker = request.ekskluderteSaker
                val sakstype = request.sakType
                val loependeFom = request.loependeFom

                call.respond(
                    SakslisteDTO(
                        inTransaction {
                            sakService
                                .hentSakIdListeForKjoering(
                                    kjoering,
                                    antall,
                                    spesifikkeSaker,
                                    ekskluderteSaker,
                                    sakstype,
                                    loependeFom,
                                )
                        },
                    ),
                )
            }
        }

        post("hent") {
            kunSystembruker {
                medBody<HentSakerRequest> { dto ->
                    val saker = inTransaction { sakService.hentSakerMedIder(dto.spesifikkeSaker) }
                    call.respond(SakerDto(saker))
                }
            }
        }

        route("/{$SAKID_CALL_PARAMETER}") {
            get {
                val sak =
                    inTransaction {
                        sakService.finnSak(sakId)
                    } ?: throw GenerellIkkeFunnetException()
                call.respond(sak)
            }

            get("/behandlinger") {
                kunSystembruker {
                    val sakMedBehandlinger =
                        inTransaction {
                            val sak = sakService.finnSak(sakId) ?: throw SakIkkeFunnetException("Fant ikke sak=$sakId")

                            behandlingService.hentSakMedBehandlinger(listOf(sak))
                        }

                    call.respond(sakMedBehandlinger)
                }
            }

            get("/behandlinger/sisteIverksatte") {
                logger.info("Henter siste iverksatte behandling for $sakId")

                val sisteIverksatteBehandling =
                    inTransaction {
                        behandlingService
                            .hentSisteIverksatte(sakId)
                            ?.let { SisteIverksatteBehandling(it.id) }
                    } ?: throw GenerellIkkeFunnetException()

                call.respond(sisteIverksatteBehandling)
            }

            get("/gradering") {
                kunSystembruker { systemBruker ->
                    logger.info("Henter gradering i sak med id=$sakId")
                    val gradering =
                        inTransaction {
                            sakService.hentGraderingForSak(sakId, systemBruker)
                        }
                    call.respond(gradering)
                }
            }
        }
    }

    post("personer/saker/{type}") {
        withFoedselsnummerInternal(tilgangService) { fnr ->
            val type: SakType =
                enumValueOf(requireNotNull(call.parameters["type"]) { "Må ha en Saktype for å finne eller opprette sak" })
            val message = inTransaction { sakService.finnEllerOpprettSakMedGrunnlag(fnr = fnr.value, type) }
            requestLogger.loggRequest(brukerTokenInfo, fnr, "personer/saker")
            call.respond(message)
        }
    }

    post("personer/getsak/{type}") {
        withFoedselsnummerInternal(tilgangService) { fnr ->
            val type: SakType =
                enumValueOf(requireNotNull(call.parameters["type"]) { "Må ha en Saktype for å finne sak" })

            requestLogger.loggRequest(brukerTokenInfo, fnr, "personer/getsak/{type}")

            val sak =
                inTransaction { sakService.finnSak(fnr.value, type) } ?: throw GenerellIkkeFunnetException()

            call.respond(sak)
        }
    }
}

class PersonManglerSak :
    IkkeFunnetException(
        code = "PERSON_MANGLER_SAK",
        detail = "Personen har ingen saker i Gjenny",
    )

class SakIkkeFunnetException(
    message: String,
) : UgyldigForespoerselException(
        code = "FANT_INGEN_SAK",
        detail = message,
    )

internal fun Route.sakWebRoutes(
    tilgangService: TilgangService,
    sakService: SakService,
    behandlingService: BehandlingService,
    grunnlagsendringshendelseService: GrunnlagsendringshendelseService,
    oppgaveService: OppgaveService,
    requestLogger: BehandlingRequestLogger,
    hendelseDao: HendelseDao,
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    route("/api") {
        route("/sak/{$SAKID_CALL_PARAMETER}") {
            get {
                val sak =
                    inTransaction {
                        sakService.finnSak(sakId)
                    } ?: throw GenerellIkkeFunnetException()
                call.respond(sak)
            }

            get("/grunnlagsendringshendelser") {
                call.respond(
                    inTransaction {
                        GrunnlagsendringsListe(
                            grunnlagsendringshendelseService.hentAlleHendelserForSak(
                                sakId,
                            ),
                        )
                    },
                )
            }

            post("/oppdater_ident") {
                kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                    val hendelseId =
                        call.request.queryParameters["hendelseId"]?.let(UUID::fromString)
                            ?: throw UgyldigForespoerselException("HENDELSE_ID_MANGLER", "HendelseID mangler")

                    val oppdatertSak =
                        inTransaction {
                            val sak =
                                sakService.finnSak(sakId)
                                    ?: throw SakIkkeFunnetException("Fant ikke sak med id=$sakId")

                            logger.info("Oppdaterer sak ${sak.id} og tilhørende oppgaver med nyeste ident fra PDL")

                            val oppdatertSak = sakService.oppdaterIdentForSak(sak)
                            oppgaveService.oppdaterIdentForOppgaver(oppdatertSak)

                            behandlingService.hentAapneBehandlingerForSak(sakId).forEach {
                                behandlingService.avbrytBehandling(
                                    behandlingId = it.behandlingId,
                                    saksbehandler = brukerTokenInfo,
                                    aarsak = AarsakTilAvbrytelse.ANNET,
                                    kommentar =
                                        "Avbrytes pga. overføring av sak fra fnr. ${sak.ident} " +
                                            "til ny ident ${oppdatertSak.ident}",
                                )
                            }

                            grunnlagsendringshendelseService.arkiverHendelseMedKommentar(
                                hendelseId = hendelseId,
                                kommentar = "Sak er oppdatert med ny ident på bruker (fra=${sak.ident}, til=${oppdatertSak.ident})",
                                saksbehandler = saksbehandler,
                            )

                            oppdatertSak
                        }

                    call.respond(oppdatertSak)
                }
            }

            post("/endre_enhet") {
                kunSaksbehandlerMedSkrivetilgang { navIdent ->
                    val enhetrequest = call.receive<EnhetRequest>()
                    try {
                        if (enhetrequest.enhet !in Enheter.entries.map { it.enhetNr }) {
                            throw UgyldigForespoerselException(
                                code = "ENHET IKKE GYLDIG",
                                detail = "enhet ${enhetrequest.enhet} er ikke i listen over gyldige enheter",
                            )
                        }

                        inTransaction { sakService.finnSak(sakId) }
                            ?: throw SakIkkeFunnetException("Fant ingen sak å endre enhet på sakid: $sakId")

                        val sakMedEnhet =
                            SakMedEnhet(
                                enhet = enhetrequest.enhet,
                                id = sakId,
                            )

                        inTransaction {
                            sakService.oppdaterEnhetForSaker(listOf(sakMedEnhet))
                            oppgaveService.oppdaterEnhetForRelaterteOppgaver(listOf(sakMedEnhet))
                        }

                        logger.info(
                            "Saksbehandler ${navIdent.ident} endret enhet på sak: $sakId og " +
                                "tilhørende oppgaver til enhet: ${sakMedEnhet.enhet}",
                        )
                        call.respond(HttpStatusCode.OK)
                    } catch (e: TilstandException.UgyldigTilstand) {
                        call.respond(HttpStatusCode.BadRequest, "Kan ikke endre enhet på sak og oppgaver")
                    }
                }
            }

            get("flyktning") {
                val flyktning = inTransaction { sakService.finnFlyktningForSak(sakId) }
                call.respond(flyktning ?: HttpStatusCode.NoContent)
            }

            get("/behandlinger/foerstevirk") {
                logger.info("Henter første virkningstidspunkt på en iverksatt behandling i sak med id $sakId")
                val foersteVirk =
                    inTransaction { behandlingService.hentFoerstegangsbehandling(sakId).virkningstidspunkt?.dato }
                        ?: throw GenerellIkkeFunnetException()
                call.respond(FoersteVirkDto(foersteVirk.atDay(1), sakId))
            }

            get("/hendelser") {
                logger.info("Henter behandlingshendelser i sak med sakId=$sakId")
                val hendelser = inTransaction { hendelseDao.hentHendelserISak(sakId) }
                call.respond(hendelser)
            }
        }

        route("/personer/") {
            post("/navkontor") {
                withFoedselsnummerInternal(tilgangService) { fnr ->
                    val navkontor = sakService.finnNavkontorForPerson(fnr.value)
                    call.respond(navkontor)
                }
            }

            post("/behandlingerforsak") {
                withFoedselsnummerInternal(tilgangService) { fnr ->
                    requestLogger.loggRequest(brukerTokenInfo, fnr, "behandlinger")

                    val sakMedBehandlinger =
                        inTransaction {
                            val saker = sakService.finnSaker(fnr.value)

                            behandlingService.hentSakMedBehandlinger(saker)
                        }

                    call.respond(sakMedBehandlinger)
                }
            }

            post("sak/{type}") {
                withFoedselsnummerInternal(tilgangService) { fnr ->
                    val opprettHvisIkkeFinnes = call.request.queryParameters["opprettHvisIkkeFinnes"].toBoolean()

                    val type: SakType =
                        requireNotNull(call.parameters["type"]) {
                            "Mangler påkrevd parameter {type} for å hente sak på bruker"
                        }.let { enumValueOf(it) }

                    requestLogger.loggRequest(brukerTokenInfo, fnr, "personer/sak/type")

                    val sak =
                        inTransaction {
                            if (opprettHvisIkkeFinnes) {
                                sakService.finnEllerOpprettSakMedGrunnlag(fnr.value, type)
                            } else {
                                sakService.finnSak(fnr.value, type)
                            }
                        }

                    call.respond(sak ?: HttpStatusCode.NoContent)
                }
            }

            post("/getsak/oms") {
                withFoedselsnummerInternal(tilgangService) { fnr ->
                    requestLogger.loggRequest(brukerTokenInfo, fnr, "api/personer/getsak/oms")

                    val saker = inTransaction { sakService.finnSakerOmsOgHvisAvdoed(fnr.value) }

                    call.respond(saker)
                }
            }

            post("arkivergrunnlagsendringshendelse") {
                kunSaksbehandler { saksbehandler ->
                    val arkivertHendelse = call.receive<Grunnlagsendringshendelse>()

                    inTransaction {
                        grunnlagsendringshendelseService.arkiverHendelseMedKommentar(
                            hendelseId = arkivertHendelse.id,
                            kommentar = arkivertHendelse.kommentar,
                            saksbehandler = saksbehandler,
                        )
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

data class EnhetRequest(
    val enhet: Enhetsnummer,
)

data class SakerDto(
    val saker: Map<SakId, Sak>,
)
