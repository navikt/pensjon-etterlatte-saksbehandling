package no.nav.etterlatte.vilkaarsvurdering

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
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.routeLogger
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.route.withParam
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.libs.vilkaarsvurdering.VurdertVilkaarsvurderingResultatDto
import vilkaarsvurdering.VilkaarTypeOgUtfall
import vilkaarsvurdering.Vilkaarsvurdering
import vilkaarsvurdering.VurdertVilkaar
import java.util.UUID

fun Route.vilkaarsvurdering(vilkaarsvurderingService: VilkaarsvurderingService) {
    route("/api/vilkaarsvurdering") {
        val logger = routeLogger

        get("/{$BEHANDLINGID_CALL_PARAMETER}") {
            logger.info("Henter vilkårsvurdering for $behandlingId")
            val vilkaarsvurdering = vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId)

            if (vilkaarsvurdering != null) {
                call.respond(
                    toDto(
                        vilkaarsvurdering,
                        behandlingGrunnlagVersjon(vilkaarsvurderingService, behandlingId),
                    ),
                )
            } else {
                logger.info("Fant ingen vilkårsvurdering for behandling ($behandlingId)")
                call.respond(HttpStatusCode.NoContent)
            }
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}/migrert-yrkesskadefordel/${SAKID_CALL_PARAMETER}") {
            logger.info("Henter vilkårsvurdering for $behandlingId")
            val result = vilkaarsvurderingService.erMigrertYrkesskadefordel(sakId)
            call.respond(MigrertYrkesskadefordel(result))
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}/rett-uten-tidsbegrensning") {
            logger.info("Henter vilkårsvurdering for $behandlingId")
            val result = vilkaarsvurderingService.harRettUtenTidsbegrensning(behandlingId)
            call.respond(mapOf("rettUtenTidsbegrensning" to result))
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}/typer") {
            logger.info("Henter vilkårtyper for $behandlingId")
            val result = vilkaarsvurderingService.hentVilkaartyper(behandlingId, brukerTokenInfo)
            call.respond(VilkaartypeDTO(result))
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/opprett") {
            val vilkaarsvurdering = call.receive<Vilkaarsvurdering>()
            call.respond(vilkaarsvurderingService.opprettVilkaarsvurdering(vilkaarsvurdering))
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/kopier") {
            val kopierbart = call.receive<OpprettVilkaarsvurderingFraBehandling>()

            logger.info("Kopierer vilkårsvurdering for $behandlingId fra ${kopierbart.forrigeBehandling}")
            val vilkaarsvurdering =
                vilkaarsvurderingService.kopierVilkaarsvurdering(
                    nyVilkaarsvurdering = kopierbart.vilkaarsvurdering,
                    tidligereVilkaarsvurderingId = kopierbart.forrigeBehandling,
                )

            call.respond(vilkaarsvurdering)
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}") {
            val vurdertVilkaarDto = call.receive<VurdertVilkaarDto>()
            val vurdertVilkaar = vurdertVilkaarDto.toVurdertVilkaar(brukerTokenInfo.ident())

            logger.info("Oppdaterer vilkårsvurdering for $behandlingId")
            try {
                val vilkaarsvurdering =
                    vilkaarsvurderingService.oppdaterVurderingPaaVilkaar(
                        behandlingId,
                        brukerTokenInfo,
                        vurdertVilkaar,
                    )
                call.respond(
                    toDto(
                        vilkaarsvurdering,
                        behandlingGrunnlagVersjon(vilkaarsvurderingService, behandlingId),
                    ),
                )
            } catch (e: BehandlingstilstandException) {
                logger.error(
                    "Kunne ikke oppdatere vilkaarsvurdering for behandling $behandlingId. " +
                        "Statussjekk for behandling feilet",
                )
                call.respond(HttpStatusCode.PreconditionFailed, "Statussjekk for behandling feilet")
            } catch (e: VilkaarsvurderingTilstandException) {
                logger.error(e.message)
                call.respond(
                    HttpStatusCode.PreconditionFailed,
                    "Kan ikke endre vurdering av vilkår på en vilkårsvurdering som har et resultat.",
                )
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/oppdater-status") {
            val statusOppdatert =
                vilkaarsvurderingService.sjekkGyldighetOgOppdaterBehandlingStatus(behandlingId, brukerTokenInfo)
            call.respond(HttpStatusCode.OK, StatusOppdatertDto(statusOppdatert))
        }

        delete("/{$BEHANDLINGID_CALL_PARAMETER}/{vilkaarId}") {
            withParam("vilkaarId") { vilkaarId ->
                logger.info("Sletter vurdering på vilkår $vilkaarId for $behandlingId")
                try {
                    val vilkaarsvurdering =
                        vilkaarsvurderingService.slettVurderingPaaVilkaar(behandlingId, brukerTokenInfo, vilkaarId)
                    call.respond(
                        toDto(
                            vilkaarsvurdering,
                            behandlingGrunnlagVersjon(vilkaarsvurderingService, behandlingId),
                        ),
                    )
                } catch (e: BehandlingstilstandException) {
                    logger.error(
                        "Kunne ikke slette vilkaarsvurdering for behandling $behandlingId. " +
                            "Statussjekk for behandling feilet",
                    )
                    call.respond(HttpStatusCode.PreconditionFailed, "Statussjekk for behandling feilet")
                } catch (e: VilkaarsvurderingTilstandException) {
                    logger.error(e.message)
                    call.respond(
                        HttpStatusCode.PreconditionFailed,
                        "Kan ikke slette vurdering av vilkår på en vilkårsvurdering som har et resultat.",
                    )
                }
            }
        }

        delete("/{$BEHANDLINGID_CALL_PARAMETER}") {
            logger.info("Sletter vilkårsvurdering for $behandlingId")

            try {
                vilkaarsvurderingService.slettVilkaarsvurdering(behandlingId, brukerTokenInfo)
                call.respond(HttpStatusCode.OK)
            } catch (e: BehandlingstilstandException) {
                logger.error(
                    "Kunne ikke slette vilkårsvurdering for behandling $behandlingId. " +
                        "Statussjekk feilet for behandling feilet",
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
                        vilkaarsvurderingService.oppdaterTotalVurdering(
                            behandlingId,
                            brukerTokenInfo,
                            vurdertResultat,
                        )
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
                    )
                    call.respond(HttpStatusCode.PreconditionFailed, "Statussjekk for behandling feilet")
                }
            }

            delete("/{$BEHANDLINGID_CALL_PARAMETER}") {
                val vilkaarsvurdering = vilkaarsvurderingService.slettVilkaarsvurderingResultat(behandlingId)
                call.respond(vilkaarsvurdering)
            }
        }
    }
}

data class StatusOppdatertDto(
    val statusOppdatert: Boolean,
)

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

private suspend fun PipelineContext<Unit, ApplicationCall>.behandlingGrunnlagVersjon(
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
