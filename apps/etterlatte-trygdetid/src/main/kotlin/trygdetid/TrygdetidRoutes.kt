package no.nav.etterlatte.trygdetid

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.BeregnetTrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.MigreringOverstyringDto
import no.nav.etterlatte.libs.common.trygdetid.StatusOppdatertDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagKildeDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidOverstyringDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidYrkesskadeDto
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.uuid
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.libs.ktor.route.withParam
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

private const val TRYGDETIDID_CALL_PARAMETER = "trygdetidId"

fun PipelineContext<*, ApplicationCall>.trygdetidId(): UUID =
    try {
        this.call.parameters[TRYGDETIDID_CALL_PARAMETER]?.let { UUID.fromString(it) }!!
    } catch (e: Exception) {
        throw UgyldigForespoerselException(
            "MANGLER_TRYGDETID_ID",
            "Kunne ikke lese ut parameteret trygdetidId",
        )
    }

private val logger: Logger = LoggerFactory.getLogger("TrygdetidRoutes")

fun Route.trygdetid(
    trygdetidService: TrygdetidService,
    behandlingKlient: BehandlingKlient,
) {
    route("/api/trygdetid_v2/{$BEHANDLINGID_CALL_PARAMETER}") {
        get {
            withBehandlingId(behandlingKlient) {
                logger.info("Henter trygdetid for behandling $behandlingId")
                val trygdetider = trygdetidService.hentTrygdetiderIBehandling(behandlingId, brukerTokenInfo)
                if (trygdetider.isNotEmpty()) {
                    call.respond(trygdetider.map { it.toDto() })
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }

        post {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
                logger.info("Oppretter trygdetid(er) for behandling $behandlingId")
                trygdetidService.opprettTrygdetiderForBehandling(behandlingId, brukerTokenInfo)
                call.respond(
                    trygdetidService
                        .hentTrygdetiderIBehandling(behandlingId, brukerTokenInfo)
                        .map { it.toDto() },
                )
            }
        }

        post("oppdater-opplysningsgrunnlag") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
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
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
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
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
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

        route("{$TRYGDETIDID_CALL_PARAMETER}") {
            post("grunnlag") {
                withBehandlingId(behandlingKlient, skrivetilgang = true) {
                    logger.info("Legger til trygdetidsgrunnlag for behandling $behandlingId")
                    val trygdetidgrunnlagDto = call.receive<TrygdetidGrunnlagDto>()

                    trygdetidService.lagreTrygdetidGrunnlagForTrygdetidMedIdIBehandlingMedSjekk(
                        behandlingId,
                        trygdetidId(),
                        trygdetidgrunnlagDto.toTrygdetidGrunnlag(brukerTokenInfo),
                        brukerTokenInfo,
                    )
                    call.respond(
                        trygdetidService
                            .hentTrygdetidIBehandlingMedId(
                                behandlingId,
                                trygdetidId(),
                                brukerTokenInfo,
                            )!!
                            .toDto(),
                    )
                }
            }

            delete("/grunnlag/{trygdetidGrunnlagId}") {
                withBehandlingId(behandlingKlient, skrivetilgang = true) {
                    withParam("trygdetidGrunnlagId") { trygdetidGrunnlagId ->
                        logger.info("Sletter trygdetidsgrunnlag for behandling $behandlingId")
                        trygdetidService.slettTrygdetidGrunnlagForTrygdetid(
                            behandlingId,
                            trygdetidId(),
                            trygdetidGrunnlagId,
                            brukerTokenInfo,
                        )
                        call.respond(
                            trygdetidService
                                .hentTrygdetidIBehandlingMedId(
                                    behandlingId,
                                    trygdetidId(),
                                    brukerTokenInfo,
                                )!!
                                .toDto(),
                        )
                    }
                }
            }
        }

        post("/oppdater-status") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                val statusOppdatert =
                    trygdetidService.sjekkGyldighetOgOppdaterBehandlingStatus(behandlingId, brukerTokenInfo)
                call.respond(HttpStatusCode.OK, StatusOppdatertDto(statusOppdatert))
            }
        }

        post("/kopier/{forrigeBehandlingId}") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
                logger.info("Oppretter kopi av forrige trygdetider for behandling $behandlingId")
                val forrigeBehandlingId = call.uuid("forrigeBehandlingId")
                trygdetidService.kopierSisteTrygdetidberegninger(behandlingId, forrigeBehandlingId, brukerTokenInfo)
                call.respond(HttpStatusCode.OK)
            }
        }

        route("/migrering") {
            post {
                withBehandlingId(behandlingKlient, skrivetilgang = true) {
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
                withBehandlingId(behandlingKlient, skrivetilgang = true) {
                    val overskriv = call.request.queryParameters["overskriv"]?.toBoolean() ?: false

                    logger.info("Oppretter trygdetid med overstyrt for behandling $behandlingId (overskriv: $overskriv)")
                    trygdetidService.opprettOverstyrtBeregnetTrygdetid(behandlingId, overskriv, brukerTokenInfo)
                    call.respond(HttpStatusCode.OK)
                }
            }

            route("/{$TRYGDETIDID_CALL_PARAMETER}") {
                post("/manuell/lagre") {
                    withBehandlingId(behandlingKlient, skrivetilgang = true) {
                        logger.info("Oppdaterer trygdetid med overstyrt for behandling $behandlingId")
                        val beregnetTrygdetid = call.receive<DetaljertBeregnetTrygdetidResultat>()

                        val eksisterendeTygdetid =
                            trygdetidService.hentTrygdetidIBehandlingMedId(
                                behandlingId,
                                trygdetidId(),
                                brukerTokenInfo,
                            ) ?: throw GenerellIkkeFunnetException()

                        val trygdetid =
                            trygdetidService.overstyrBeregnetTrygdetidForAvdoed(
                                behandlingId,
                                eksisterendeTygdetid.ident,
                                beregnetTrygdetid,
                            )

                        behandlingKlient.settBehandlingStatusTrygdetidOppdatert(trygdetid.behandlingId, brukerTokenInfo)

                        call.respond(
                            trygdetidService
                                .hentTrygdetidIBehandlingMedId(
                                    behandlingId,
                                    trygdetidId(),
                                    brukerTokenInfo,
                                )!!
                                .toDto(),
                        )
                    }
                }

                post("/uten_fremtidig") {
                    withBehandlingId(behandlingKlient, skrivetilgang = true) {
                        logger.info("Beregn trygdetid uten fremtidig trygdetid for behandling $behandlingId")

                        val trygdetid =
                            trygdetidService.hentTrygdetidIBehandlingMedId(behandlingId, trygdetidId(), brukerTokenInfo)

                        if (trygdetid != null) {
                            trygdetidService.reberegnUtenFremtidigTrygdetid(
                                behandlingId,
                                trygdetid.id,
                                brukerTokenInfo,
                            )
                            call.respond(
                                trygdetidService
                                    .hentTrygdetidIBehandlingMedId(
                                        behandlingId,
                                        trygdetidId(),
                                        brukerTokenInfo,
                                    )!!
                                    .toDto(),
                            )
                        } else {
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                }
            }
        }
    }
}

fun Trygdetid.toDto(): TrygdetidDto =
    TrygdetidDto(
        id = id,
        behandlingId = behandlingId,
        beregnetTrygdetid = beregnetTrygdetid?.toDto(),
        trygdetidGrunnlag = trygdetidGrunnlag.map { it.toDto() },
        opplysninger = this.opplysninger.toDto(),
        overstyrtNorskPoengaar = this.overstyrtNorskPoengaar,
        ident = this.ident,
        opplysningerDifferanse = requireNotNull(opplysningerDifferanse),
    )

private fun DetaljertBeregnetTrygdetid.toDto(): DetaljertBeregnetTrygdetidDto =
    DetaljertBeregnetTrygdetidDto(
        resultat = resultat,
        tidspunkt = tidspunkt,
    )

fun TrygdetidGrunnlagDto.toTrygdetidGrunnlag(brukerTokenInfo: BrukerTokenInfo): TrygdetidGrunnlag =
    TrygdetidGrunnlag(
        id = id ?: UUID.randomUUID(),
        type = TrygdetidType.valueOf(type),
        bosted = bosted,
        periode = TrygdetidPeriode(periodeFra, periodeTil),
        kilde =
            if (brukerTokenInfo is Systembruker) {
                // p.t. er det kun ved migrering at vi legger til perioder med systembruker
                Grunnlagsopplysning.Pesys(Tidspunkt.now())
            } else {
                Grunnlagsopplysning.Saksbehandler(brukerTokenInfo.ident(), Tidspunkt.now())
            },
        begrunnelse = begrunnelse,
        poengInnAar = poengInnAar,
        poengUtAar = poengUtAar,
        prorata = prorata,
    )

private fun TrygdetidGrunnlag.toDto(): TrygdetidGrunnlagDto =
    TrygdetidGrunnlagDto(
        id = id,
        type = type.name,
        bosted = bosted,
        periodeFra = periode.fra,
        periodeTil = periode.til,
        beregnet =
            beregnetTrygdetid?.let {
                BeregnetTrygdetidGrunnlagDto(it.verdi.days, it.verdi.months, it.verdi.years)
            },
        kilde =
            when (kilde) {
                is Grunnlagsopplysning.Saksbehandler ->
                    TrygdetidGrunnlagKildeDto(
                        tidspunkt = kilde.tidspunkt.toString(),
                        ident = kilde.ident,
                    )

                is Grunnlagsopplysning.Pesys ->
                    TrygdetidGrunnlagKildeDto(
                        tidspunkt = kilde.tidspunkt.toString(),
                        ident = kilde.type,
                    )

                else -> throw UnsupportedOperationException("Kilde for trygdetid maa vaere saksbehandler eller pesys")
            },
        begrunnelse = begrunnelse,
        poengInnAar = poengInnAar,
        poengUtAar = poengUtAar,
        prorata = prorata,
    )
