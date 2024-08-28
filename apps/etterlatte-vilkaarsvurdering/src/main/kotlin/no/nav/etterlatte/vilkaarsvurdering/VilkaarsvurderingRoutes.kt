package no.nav.etterlatte.vilkaarsvurdering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.vilkaarsvurdering.OppdaterVurdertVilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.routeLogger
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.route.withUuidParam
import no.nav.etterlatte.libs.vilkaarsvurdering.VurdertVilkaarsvurderingDto

fun Route.vilkaarsvurdering(vilkaarsvurderingService: VilkaarsvurderingService) {
    route("/api/vilkaarsvurdering") {
        val logger = routeLogger

        get("/{$BEHANDLINGID_CALL_PARAMETER}") {
            logger.info("Henter vilkårsvurdering for $behandlingId")
            val vilkaarsvurdering = vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId)

            if (vilkaarsvurdering != null) {
                call.respond(vilkaarsvurdering)
            } else {
                logger.info("Fant ingen vilkårsvurdering for behandling ($behandlingId)")
                call.respond(HttpStatusCode.NoContent)
            }
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}/migrert-yrkesskadefordel/{$SAKID_CALL_PARAMETER}") {
            logger.info("Henter vilkårsvurdering for $behandlingId")
            val result = vilkaarsvurderingService.erMigrertYrkesskadefordel(sakId)
            call.respond(MigrertYrkesskadefordel(result))
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}/rett-uten-tidsbegrensning") {
            logger.info("Henter vilkårsvurdering for $behandlingId")
            val result = vilkaarsvurderingService.harRettUtenTidsbegrensning(behandlingId)
            call.respond(mapOf("rettUtenTidsbegrensning" to result))
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
            val vurdertVilkaarDto = call.receive<OppdaterVurdertVilkaar>()

            logger.info("Oppdaterer vilkårsvurdering for $behandlingId")
            try {
                val vilkaarsvurdering =
                    vilkaarsvurderingService.oppdaterVurderingPaaVilkaar(vurdertVilkaarDto)
                call.respond(vilkaarsvurdering)
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

        post("/{$BEHANDLINGID_CALL_PARAMETER}/oppdater-status/{grunnlagsVersjon}") {
            val grunnlagsVersjon = call.parameters["grunnlagsVersjon"]!!.toLong()
            vilkaarsvurderingService.oppdaterGrunnlagsversjon(behandlingId, grunnlagsVersjon)
            call.respond(HttpStatusCode.OK, StatusOppdatertDto(true))
        }

        delete("/{$BEHANDLINGID_CALL_PARAMETER}/{vilkaarId}") {
            withUuidParam("vilkaarId") { vilkaarId ->
                logger.info("Sletter vurdering på vilkår $vilkaarId for $behandlingId")
                val vilkaarsvurdering =
                    vilkaarsvurderingService.slettVurderingPaaVilkaar(behandlingId, vilkaarId)
                call.respond(vilkaarsvurdering)
            }
        }

        delete("/{$BEHANDLINGID_CALL_PARAMETER}/{vilkaarsvurderingId}") {
            withUuidParam("vilkaarsvurderingId") { vilkaarsvurderingId ->
                logger.info("Sletter vilkårsvurdering for $behandlingId $vilkaarsvurderingId")
                vilkaarsvurderingService.slettVilkaarsvurdering(vilkaarsvurderingId)
                call.respond(HttpStatusCode.OK)
            }
        }

        route("/resultat") {
            post("/{$BEHANDLINGID_CALL_PARAMETER}") {
                val vurdertResultatDto = call.receive<VurdertVilkaarsvurderingDto>()

                logger.info("Oppdaterer vilkårsvurderingsresultat for $behandlingId")
                val vilkaarsvurdering =
                    vilkaarsvurderingService.oppdaterTotalVurdering(vurdertResultatDto)
                call.respond(vilkaarsvurdering)
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
