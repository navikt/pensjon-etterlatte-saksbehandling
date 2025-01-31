package no.nav.etterlatte.vilkaarsvurdering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vilkaarsvurdering.StatusOppdatertDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarTypeOgUtfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VurdertVilkaar
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.withUuidParam
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.libs.vilkaarsvurdering.VurdertVilkaarsvurderingResultatDto
import no.nav.etterlatte.vilkaarsvurdering.service.BehandlingstilstandException
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService
import no.nav.etterlatte.vilkaarsvurdering.service.VirkningstidspunktIkkeSattException
import org.slf4j.LoggerFactory
import java.util.UUID

private val logger = LoggerFactory.getLogger("VilkaarsvurderingRoute")

fun Route.vilkaarsvurdering(vilkaarsvurderingService: VilkaarsvurderingService) {
    route("/api/vilkaarsvurdering") {
        get("/{$BEHANDLINGID_CALL_PARAMETER}") {
            logger.info("Henter vilkårsvurdering for $behandlingId")
            val vilkaarsvurdering = inTransaction { vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId) }

            if (vilkaarsvurdering != null) {
                call.respond(
                    toDto(
                        vilkaarsvurdering,
                        inTransaction { behandlingGrunnlagVersjon(vilkaarsvurderingService, behandlingId) },
                    ),
                )
            } else {
                logger.info("Fant ingen vilkårsvurdering for behandling ($behandlingId)")
                call.respond(HttpStatusCode.NoContent)
            }
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}/migrert-yrkesskadefordel") {
            logger.info("Henter vilkårsvurdering for $behandlingId")
            val result = inTransaction { vilkaarsvurderingService.erMigrertYrkesskadefordel(behandlingId) }
            call.respond(MigrertYrkesskadefordel(result))
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}/rett-uten-tidsbegrensning") {
            logger.info("Henter vilkårsvurdering for $behandlingId")
            val result = inTransaction { vilkaarsvurderingService.harRettUtenTidsbegrensning(behandlingId) }
            call.respond(mapOf("rettUtenTidsbegrensning" to result))
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}/typer") {
            logger.info("Henter vilkårtyper for $behandlingId")
            val result = inTransaction { vilkaarsvurderingService.hentVilkaartyper(behandlingId) }
            call.respond(VilkaartypeDTO(result))
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/opprett") {
            try {
                val kopierVedRevurdering =
                    call.request.queryParameters["kopierVedRevurdering"]?.toBoolean()
                        ?: true

                logger.info("Oppretter vilkårsvurdering for $behandlingId")
                val (vilkaarsvurdering, behandlingGrunnlagsversjon) =
                    inTransaction {
                        vilkaarsvurderingService.opprettVilkaarsvurdering(
                            behandlingId,
                            brukerTokenInfo,
                            kopierVedRevurdering,
                        )
                    }

                call.respond(
                    toDto(
                        vilkaarsvurdering,
                        behandlingGrunnlagsversjon,
                    ),
                )
            } catch (_: VirkningstidspunktIkkeSattException) {
                logger.info("Virkningstidspunkt er ikke satt for behandling $behandlingId")
                call.respond(HttpStatusCode.PreconditionFailed)
            } catch (e: BehandlingstilstandException) {
                logger.info(
                    "Kunne ikke opprette vilkaarsvurdering for behandling $behandlingId. " +
                        "Statussjekk for behandling feilet",
                    e,
                )
                call.respond(HttpStatusCode.PreconditionFailed, "Statussjekk for behandling feilet")
            }
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}/behandling-med-vilkaarsvurdering-for-avdoede") {
            logger.info("Sjekker om det finnes andre behandlinger for samme avdøde som har vilkårsvurdering")
            val kandidatBehandlingId =
                inTransaction {
                    vilkaarsvurderingService.finnBehandlingMedVilkaarsvurderingForSammeAvdoede(
                        behandlingId,
                    )
                }

            when (kandidatBehandlingId) {
                null -> call.respond(HttpStatusCode.NoContent)
                else -> call.respondText(kandidatBehandlingId.toString())
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/kopier-vilkaar/{kildeBehandlingId}") {
            withUuidParam("kildeBehandlingId") { kildeBehandlingId ->
                logger.info("Kopierer vilkår fra behandling $kildeBehandlingId til behandling $behandlingId")
                call.respond(
                    inTransaction {
                        vilkaarsvurderingService.kopierVilkaarForAvdoede(
                            behandlingId = behandlingId,
                            kildeBehandlingId = kildeBehandlingId,
                            brukerTokenInfo = brukerTokenInfo,
                        )
                    },
                )
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/kopier") {
            // TODO: OpprettVilkaarsvurderingFraBehandling skal ikke brukes her men mot vv
            val forrigeBehandling = call.receive<OpprettVilkaarsvurderingMotBehandling>().forrigeBehandling

            try {
                logger.info("Kopierer vilkårsvurdering for $behandlingId fra $forrigeBehandling")
                val (vilkaarsvurdering, behandlingGrunnversjon) =
                    inTransaction {
                        vilkaarsvurderingService.kopierVilkaarsvurdering(
                            behandlingId = behandlingId,
                            kopierFraBehandling = forrigeBehandling,
                            brukerTokenInfo = brukerTokenInfo,
                        )
                    }

                call.respond(
                    toDto(
                        vilkaarsvurdering,
                        behandlingGrunnversjon,
                    ),
                )
            } catch (_: VirkningstidspunktIkkeSattException) {
                logger.info("Virkningstidspunkt er ikke satt for behandling $behandlingId")
                call.respond(HttpStatusCode.PreconditionFailed)
            } catch (e: BehandlingstilstandException) {
                logger.error(
                    "Kunne ikke opprette vilkaarsvurdering for behandling $behandlingId. " +
                        "Statussjekk for behandling feilet",
                    e,
                )
                call.respond(HttpStatusCode.PreconditionFailed, "Statussjekk for behandling feilet")
            } catch (e: NullPointerException) {
                logger.error(
                    "Kunne ikke kopiere vilkårsvurdering fra $forrigeBehandling. Fant ikke vilkårsvurdering",
                    e,
                )
                throw GenerellIkkeFunnetException()
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}") {
            val vurdertVilkaarDto = call.receive<VurdertVilkaarDto>()
            val vurdertVilkaar = vurdertVilkaarDto.toVurdertVilkaar(brukerTokenInfo.ident())

            logger.info("Oppdaterer vilkårsvurdering for $behandlingId")
            try {
                val vilkaarsvurdering =
                    inTransaction {
                        vilkaarsvurderingService.oppdaterVurderingPaaVilkaar(
                            behandlingId,
                            brukerTokenInfo,
                            vurdertVilkaar,
                        )
                    }
                call.respond(
                    toDto(
                        vilkaarsvurdering,
                        inTransaction { behandlingGrunnlagVersjon(vilkaarsvurderingService, behandlingId) },
                    ),
                )
            } catch (e: BehandlingstilstandException) {
                logger.error(
                    "Kunne ikke oppdatere vilkaarsvurdering for behandling $behandlingId. " +
                        "Statussjekk for behandling feilet",
                    e,
                )
                call.respond(HttpStatusCode.PreconditionFailed, "Statussjekk for behandling feilet")
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/oppdater-status") {
            val statusOppdatert =
                inTransaction { vilkaarsvurderingService.sjekkGyldighetOgOppdaterBehandlingStatus(behandlingId, brukerTokenInfo) }
            call.respond(HttpStatusCode.OK, StatusOppdatertDto(statusOppdatert))
        }

        delete("/{$BEHANDLINGID_CALL_PARAMETER}/{vilkaarId}") {
            withUuidParam("vilkaarId") { vilkaarId ->
                logger.info("Sletter vurdering på vilkår $vilkaarId for $behandlingId")
                try {
                    val vilkaarsvurdering =
                        inTransaction { vilkaarsvurderingService.slettVurderingPaaVilkaar(behandlingId, brukerTokenInfo, vilkaarId) }
                    call.respond(
                        toDto(
                            vilkaarsvurdering,
                            inTransaction { behandlingGrunnlagVersjon(vilkaarsvurderingService, behandlingId) },
                        ),
                    )
                } catch (e: BehandlingstilstandException) {
                    logger.error(
                        "Kunne ikke slette vilkaarsvurdering for behandling $behandlingId. " +
                            "Statussjekk for behandling feilet",
                        e,
                    )
                    call.respond(HttpStatusCode.PreconditionFailed, "Statussjekk for behandling feilet")
                }
            }
        }

        delete("/{$BEHANDLINGID_CALL_PARAMETER}") {
            logger.info("Sletter vilkårsvurdering for $behandlingId")

            try {
                inTransaction { vilkaarsvurderingService.slettVilkaarsvurdering(behandlingId, brukerTokenInfo) }
                call.respond(HttpStatusCode.OK)
            } catch (e: BehandlingstilstandException) {
                logger.error(
                    "Kunne ikke slette vilkårsvurdering for behandling $behandlingId. " +
                        "Statussjekk feilet for behandling feilet",
                    e,
                )
                call.respond(HttpStatusCode.PreconditionFailed, "Statussjekk for behandling feilet")
            }
        }

        route("/resultat") {
            post("/{$BEHANDLINGID_CALL_PARAMETER}") {
                val vurdertResultatDto = call.receive<VurdertVilkaarsvurderingResultatDto>()
                val vurdertResultat =
                    vurdertResultatDto.toVilkaarsvurderingResultat(
                        brukerTokenInfo.ident(),
                    )

                logger.info("Oppdaterer vilkårsvurderingsresultat for $behandlingId")
                try {
                    val (vilkaarsvurdering, behandlingGrunnlagversjon) =
                        inTransaction {
                            vilkaarsvurderingService.oppdaterTotalVurdering(
                                behandlingId,
                                brukerTokenInfo,
                                vurdertResultat,
                            )
                        }
                    call.respond(
                        toDto(
                            vilkaarsvurdering,
                            behandlingGrunnlagversjon,
                        ),
                    )
                } catch (e: BehandlingstilstandException) {
                    logger.error(
                        "Kunne ikke oppdatere total-vurdering for behandling $behandlingId. " +
                            "Statussjekk for behandling feilet",
                        e,
                    )
                    call.respond(HttpStatusCode.PreconditionFailed, "Statussjekk for behandling feilet")
                }
            }

            delete("/{$BEHANDLINGID_CALL_PARAMETER}") {
                logger.info("Sletter vilkårsvurderingsresultat for $behandlingId")
                try {
                    val vilkaarsvurdering =
                        inTransaction {
                            vilkaarsvurderingService.slettTotalVurdering(
                                behandlingId,
                                brukerTokenInfo,
                            )
                        }
                    call.respond(
                        toDto(
                            vilkaarsvurdering,
                            inTransaction { behandlingGrunnlagVersjon(vilkaarsvurderingService, behandlingId) },
                        ),
                    )
                } catch (e: BehandlingstilstandException) {
                    logger.error(
                        "Kunne ikke slette vilkårsvurderingsresultat for behandling $behandlingId. " +
                            "Statussjekk feilet for behandling feilet",
                        e,
                    )
                    call.respond(HttpStatusCode.PreconditionFailed, "Statussjekk for behandling feilet")
                }
            }
        }
    }
}

private fun VurdertVilkaarDto.toVurdertVilkaar(saksbehandler: String) =
    VurdertVilkaar(
        vilkaarId = vilkaarId,
        hovedvilkaar = hovedvilkaar,
        unntaksvilkaar = unntaksvilkaar,
        vurdering =
            VilkaarVurderingData(
                kommentar = kommentar,
                tidspunkt = Tidspunkt.now().toLocalDatetimeUTC(),
                saksbehandler = saksbehandler,
            ),
    )

private fun VurdertVilkaarsvurderingResultatDto.toVilkaarsvurderingResultat(saksbehandler: String) =
    VilkaarsvurderingResultat(
        utfall = resultat,
        kommentar = kommentar,
        tidspunkt = Tidspunkt.now().toLocalDatetimeUTC(),
        saksbehandler = saksbehandler,
    )

data class VurdertVilkaarDto(
    val vilkaarId: UUID,
    val hovedvilkaar: VilkaarTypeOgUtfall,
    val unntaksvilkaar: VilkaarTypeOgUtfall? = null,
    val kommentar: String?,
)

fun toDto(
    vilkaarsvurdering: Vilkaarsvurdering,
    behandlingGrunnlagVersjon: Long?,
) = VilkaarsvurderingDto(
    behandlingId = vilkaarsvurdering.behandlingId,
    virkningstidspunkt = vilkaarsvurdering.virkningstidspunkt,
    vilkaar = vilkaarsvurdering.vilkaar,
    resultat = vilkaarsvurdering.resultat,
    grunnlagVersjon = vilkaarsvurdering.grunnlagVersjon,
    behandlingGrunnlagVersjon = behandlingGrunnlagVersjon,
)

private fun PipelineContext<Unit, ApplicationCall>.behandlingGrunnlagVersjon(
    vilkaarsvurderingService: VilkaarsvurderingService,
    behandlingId: UUID,
): Long =
    vilkaarsvurderingService
        .hentBehandlingensGrunnlag(behandlingId, brukerTokenInfo)
        .metadata
        .versjon

data class VilkaartypeDTO(
    val typer: List<VilkaartypePair>,
)

data class VilkaartypePair(
    val name: String,
    val tittel: String,
)
