package no.nav.etterlatte.trygdetid

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.inTransactionIfNeeded
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.MigreringOverstyringDto
import no.nav.etterlatte.libs.common.trygdetid.StatusOppdatertDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidBegrunnelseDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidOverstyringDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidYrkesskadeDto
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.uuid
import no.nav.etterlatte.libs.ktor.route.withUuidParam
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

private const val TRYGDETIDID_CALL_PARAMETER = "trygdetidId"

private inline val RoutingContext.trygdetidId: UUID
    get() =
        try {
            this.call.uuid(TRYGDETIDID_CALL_PARAMETER)
        } catch (_: Exception) {
            throw UgyldigForespoerselException(
                "MANGLER_TRYGDETID_ID",
                "Kunne ikke lese ut parameteret trygdetidId",
            )
        }

private val logger: Logger = LoggerFactory.getLogger("TrygdetidRoutes")

fun Route.trygdetid(
    trygdetidService: TrygdetidService,
    behandlingsStatusService: BehandlingStatusService,
) {
    route("/api/trygdetid_v2") {
        route("/{$BEHANDLINGID_CALL_PARAMETER}") {
            get {
                logger.info("Henter trygdetid for behandling $behandlingId")
                val trygdetider = trygdetidService.hentTrygdetiderIBehandling(behandlingId, brukerTokenInfo)
                if (trygdetider.isNotEmpty()) {
                    call.respond(trygdetider.map { it.toDto() })
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }

            post {
                kunSkrivetilgang {
                    logger.info("Oppretter trygdetid(er) for behandling $behandlingId")
                    val overskriv = call.request.queryParameters["overskriv"]?.toBoolean() ?: false

                    trygdetidService.opprettTrygdetiderForBehandling(behandlingId, brukerTokenInfo, overskriv)
                    call.respond(
                        trygdetidService
                            .hentTrygdetiderIBehandling(behandlingId, brukerTokenInfo)
                            .map { it.toDto() },
                    )
                }
            }

            route("pesys") {
                post {
                    kunSkrivetilgang {
                        logger.info("Oppretter trygdetid(er) fra pesys for behandling $behandlingId")

                        trygdetidService.leggInnTrygdetidsgrunnlagFraPesys(behandlingId, brukerTokenInfo)
                        call.respond(
                            trygdetidService
                                .hentTrygdetiderIBehandling(behandlingId, brukerTokenInfo)
                                .map { it.toDto() },
                        )
                    }
                }
                get("/sjekk-pesys-trygdetidsgrunnlag") {
                    kunSkrivetilgang {
                        logger.info("Sjekker om avdød for behandling $behandlingId har trygdetidsgrunnlag i Pesys for AP og Uføre")
                        val harTrygdetidsgrunnlagIPesys =
                            trygdetidService.harTrygdetidsgrunnlagIPesysForApOgUfoere(
                                behandlingId,
                                brukerTokenInfo,
                            )
                        call.respond(harTrygdetidsgrunnlagIPesys)
                    }
                }
            }

            post("oppdater-opplysningsgrunnlag") {
                kunSkrivetilgang {
                    logger.info("Oppdaterer opplysningsgrunnlag på trygdetider for behandling $behandlingId")

                    trygdetidService.oppdaterOpplysningsgrunnlagForTrygdetider(behandlingId, brukerTokenInfo)
                    call.respond(
                        trygdetidService
                            .hentTrygdetiderIBehandling(behandlingId, brukerTokenInfo)
                            .minBy { it.ident }
                            .toDto(),
                    )
                }
            }

            post("overstyr") {
                kunSkrivetilgang {
                    logger.info("Oppdater trygdetid (overstyring) for behandling $behandlingId")
                    val trygdetidOverstyringDto = call.receive<TrygdetidOverstyringDto>()

                    trygdetidService.overstyrNorskPoengaaarForTrygdetid(
                        trygdetidOverstyringDto.id,
                        behandlingId,
                        trygdetidOverstyringDto.overstyrtNorskPoengaar,
                        brukerTokenInfo,
                    )
                    call.respond(
                        trygdetidService
                            .hentTrygdetidIBehandlingMedId(
                                behandlingId,
                                trygdetidOverstyringDto.id,
                                brukerTokenInfo,
                            )!!
                            .toDto(),
                    )
                }
            }

            post("yrkesskade") {
                kunSkrivetilgang {
                    logger.info("Oppdater trygdetid (yrkesskade) for behandling $behandlingId")
                    val trygdetidYrkesskadeDto = call.receive<TrygdetidYrkesskadeDto>()

                    trygdetidService.setYrkesskade(
                        trygdetidYrkesskadeDto.id,
                        behandlingId,
                        trygdetidYrkesskadeDto.yrkesskade,
                        brukerTokenInfo,
                    )

                    call.respond(
                        trygdetidService
                            .hentTrygdetidIBehandlingMedId(
                                behandlingId,
                                trygdetidYrkesskadeDto.id,
                                brukerTokenInfo,
                            )!!
                            .toDto(),
                    )
                }
            }

            post("begrunnelse") {
                kunSkrivetilgang {
                    logger.info("Oppdater begrunnelse for behandling $behandlingId")
                    val trygdetidBegrunnelse = call.receive<TrygdetidBegrunnelseDto>()

                    val trygdetid =
                        trygdetidService.oppdaterTrygdetidMedBegrunnelse(
                            trygdetidBegrunnelse.id,
                            behandlingId,
                            trygdetidBegrunnelse.begrunnelse,
                            brukerTokenInfo,
                        )

                    call.respond(trygdetid.toDto())
                }
            }

            get("/behandling-med-trygdetid-for-avdoede") {
                val kandidatBehandlingId =
                    trygdetidService.finnBehandlingMedTrygdetidForSammeAvdoede(
                        behandlingId,
                        brukerTokenInfo,
                    )
                when (kandidatBehandlingId) {
                    null -> call.respond(HttpStatusCode.NoContent)
                    else -> call.respondText(kandidatBehandlingId.toString())
                }
            }

            route("{$TRYGDETIDID_CALL_PARAMETER}") {
                post("grunnlag") {
                    kunSkrivetilgang {
                        logger.info("Legger til trygdetidsgrunnlag for behandling $behandlingId")
                        val trygdetidgrunnlagDto = call.receive<TrygdetidGrunnlagDto>()

                        trygdetidService.lagreTrygdetidGrunnlagForTrygdetidMedIdIBehandlingMedSjekk(
                            behandlingId,
                            trygdetidId,
                            trygdetidgrunnlagDto.toTrygdetidGrunnlag(brukerTokenInfo),
                            brukerTokenInfo,
                        )
                        call.respond(
                            trygdetidService
                                .hentTrygdetidIBehandlingMedId(
                                    behandlingId,
                                    trygdetidId,
                                    brukerTokenInfo,
                                )!!
                                .toDto(),
                        )
                    }
                }

                delete("/grunnlag/{trygdetidGrunnlagId}") {
                    kunSkrivetilgang {
                        withUuidParam("trygdetidGrunnlagId") { trygdetidGrunnlagId ->
                            logger.info("Sletter trygdetidsgrunnlag for behandling $behandlingId")
                            trygdetidService.slettTrygdetidGrunnlagForTrygdetid(
                                behandlingId,
                                trygdetidId,
                                trygdetidGrunnlagId,
                                brukerTokenInfo,
                            )
                            call.respond(
                                trygdetidService
                                    .hentTrygdetidIBehandlingMedId(
                                        behandlingId,
                                        trygdetidId,
                                        brukerTokenInfo,
                                    )!!
                                    .toDto(),
                            )
                        }
                    }
                }

                delete("/grunnlag/slett-pesys") {
                    kunSkrivetilgang {
                        logger.info("Sletter trygdetidsgrunnlag for behandling $behandlingId")
                        trygdetidService.slettPesysTrygdetidGrunnlagForTrygdetid(
                            behandlingId,
                            trygdetidId,
                            brukerTokenInfo,
                        )
                        call.respond(
                            trygdetidService
                                .hentTrygdetidIBehandlingMedId(
                                    behandlingId,
                                    trygdetidId,
                                    brukerTokenInfo,
                                )!!
                                .toDto(),
                        )
                    }
                }
            }

            post("/oppdater-status") {
                kunSkrivetilgang {
                    val statusOppdatert =
                        trygdetidService.sjekkGyldighetOgOppdaterBehandlingStatus(behandlingId, brukerTokenInfo)
                    call.respond(HttpStatusCode.OK, StatusOppdatertDto(statusOppdatert))
                }
            }

            post("/kopier/{forrigeBehandlingId}") {
                kunSkrivetilgang {
                    withUuidParam("forrigeBehandlingId") { forrigeBehandlingId ->
                        logger.info("Oppretter kopi av forrige trygdetider for behandling $behandlingId")
                        trygdetidService.kopierSisteTrygdetidberegninger(
                            behandlingId,
                            forrigeBehandlingId,
                            brukerTokenInfo,
                        )
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            post("/kopier-grunnlag/{kildeBehandlingId}") {
                // Alias for kopier-og-overskriv (temp)
                kunSkrivetilgang {
                    withUuidParam("kildeBehandlingId") { kildeBehandlingId ->
                        call.respond(
                            trygdetidService.kopierOgOverskrivTrygdetid(
                                behandlingId = behandlingId,
                                kildeBehandlingId = kildeBehandlingId,
                                brukerTokenInfo = brukerTokenInfo,
                            ),
                        )
                    }
                }
            }

            post("/kopier-og-overskriv/{kildeBehandlingId}") {
                kunSkrivetilgang {
                    withUuidParam("kildeBehandlingId") { kildeBehandlingId ->
                        call.respond(
                            trygdetidService.kopierOgOverskrivTrygdetid(
                                behandlingId = behandlingId,
                                kildeBehandlingId = kildeBehandlingId,
                                brukerTokenInfo = brukerTokenInfo,
                            ),
                        )
                    }
                }
            }

            route("/migrering") {
                post {
                    kunSkrivetilgang {
                        logger.info("Migrering overstyrer trygdetid for behandling $behandlingId")
                        val overstyringDto = call.receive<MigreringOverstyringDto>()
                        trygdetidService.overstyrBeregnetTrygdetidForAvdoed(
                            behandlingId,
                            overstyringDto.ident,
                            overstyringDto.detaljertBeregnetTrygdetidResultat,
                        )
                        call.respond(
                            trygdetidService
                                .hentTrygdetiderIBehandling(behandlingId, brukerTokenInfo)
                                .first { trygdetid -> trygdetid.ident == overstyringDto.ident }
                                .toDto(),
                        )
                    }
                }

                post("/manuell/opprett") {
                    kunSkrivetilgang {
                        val overskriv = call.request.queryParameters["overskriv"]?.toBoolean() ?: false

                        logger.info(
                            "Oppretter trygdetid med overstyrt for behandling $behandlingId (overskriv: $overskriv)",
                        )
                        trygdetidService.opprettOverstyrtBeregnetTrygdetid(behandlingId, overskriv, brukerTokenInfo)
                        call.respond(HttpStatusCode.OK)
                    }
                }

                route("/{$TRYGDETIDID_CALL_PARAMETER}") {
                    post("/manuell/lagre") {
                        kunSkrivetilgang {
                            logger.info("Oppdaterer trygdetid med overstyrt for behandling $behandlingId")
                            val beregnetTrygdetid = call.receive<DetaljertBeregnetTrygdetidResultat>()

                            val eksisterendeTrygdetid =
                                trygdetidService.hentTrygdetidIBehandlingMedId(
                                    behandlingId,
                                    trygdetidId,
                                    brukerTokenInfo,
                                ) ?: throw GenerellIkkeFunnetException()

                            val trygdetid =
                                trygdetidService.overstyrBeregnetTrygdetidForAvdoed(
                                    behandlingId,
                                    eksisterendeTrygdetid.ident,
                                    beregnetTrygdetid,
                                )

                            inTransactionIfNeeded {
                                behandlingsStatusService.settTrygdetidOppdatert(
                                    behandlingId = trygdetid.behandlingId,
                                    brukerTokenInfo = brukerTokenInfo,
                                    dryRun = false,
                                )
                            }

                            call.respond(
                                trygdetidService
                                    .hentTrygdetidIBehandlingMedId(
                                        behandlingId,
                                        trygdetidId,
                                        brukerTokenInfo,
                                    )!!
                                    .toDto(),
                            )
                        }
                    }
                }
            }
        }
    }
}

fun TrygdetidGrunnlagDto.toTrygdetidGrunnlag(brukerTokenInfo: BrukerTokenInfo): TrygdetidGrunnlag =
    TrygdetidGrunnlag(
        id = id ?: UUID.randomUUID(),
        type = TrygdetidType.valueOf(type),
        bosted = bosted,
        periode = TrygdetidPeriode(periodeFra, periodeTil),
        kilde =
            if (brukerTokenInfo is Systembruker) {
                Grunnlagsopplysning.Pesys(Tidspunkt.now())
            } else {
                Grunnlagsopplysning.Saksbehandler(brukerTokenInfo.ident(), Tidspunkt.now())
            },
        begrunnelse = begrunnelse,
        poengInnAar = poengInnAar,
        poengUtAar = poengUtAar,
        prorata = prorata,
    )
