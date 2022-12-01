package no.nav.etterlatte.vilkaarsvurdering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarTypeOgUtfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VurdertVilkaar
import no.nav.etterlatte.libs.ktor.accesstoken
import no.nav.etterlatte.libs.ktor.saksbehandler
import java.time.LocalDateTime
import java.util.*

fun Route.vilkaarsvurdering(
    vilkaarsvurderingService: VilkaarsvurderingService
) {
    route("/api/vilkaarsvurdering") {
        val logger = application.log

        get("/{behandlingId}") {
            withBehandlingId { behandlingId ->
                logger.info("Henter vilkårsvurdering for $behandlingId")

                try {
                    val vilkaarsvurdering = vilkaarsvurderingService.hentEllerOpprettVilkaarsvurdering(
                        behandlingId = behandlingId,
                        accessToken = accesstoken
                    )
                    call.respond(vilkaarsvurdering.toDto())
                } catch (e: VirkningstidspunktIkkeSattException) {
                    logger.info("Virkningstidspunkt ikke satt for behandling $behandlingId")
                    call.respond(HttpStatusCode.PreconditionFailed)
                }
            }
        }

        post("/{behandlingId}") {
            withBehandlingId { behandlingId ->
                val vurdertVilkaarDto = call.receive<VurdertVilkaarDto>()

                logger.info("Oppdaterer vilkårsvurdering for $behandlingId")
                val oppdatertVilkaarsvurdering =
                    vilkaarsvurderingService.oppdaterVurderingPaaVilkaar(
                        behandlingId = behandlingId,
                        vurdertVilkaar = toVurdertVilkaar(vurdertVilkaarDto, saksbehandler)
                    )

                call.respond(oppdatertVilkaarsvurdering)
            }
        }

        delete("/{behandlingId}/{vilkaarType}") {
            withBehandlingId { behandlingId ->
                val vilkaarType = VilkaarType.valueOf(requireNotNull(call.parameters["vilkaarType"]))

                logger.info("Sletter vurdering på vilkår $vilkaarType for $behandlingId")
                val oppdatertVilkaarsvurdering =
                    vilkaarsvurderingService.slettVurderingPaaVilkaar(
                        behandlingId = behandlingId,
                        hovedVilkaarType = vilkaarType
                    )

                call.respond(oppdatertVilkaarsvurdering)
            }
        }

        route("/resultat") {
            post("/{behandlingId}") {
                withBehandlingId { behandlingId ->
                    val vurdertResultatDto = call.receive<VurdertVilkaarsvurderingResultatDto>()

                    logger.info("Oppdaterer vilkårsvurderingsresultat for $behandlingId")
                    val oppdatertVilkaarsvurdering = vilkaarsvurderingService.oppdaterTotalVurdering(
                        behandlingId = behandlingId,
                        resultat = toVilkaarsvurderingResultat(vurdertResultatDto, saksbehandler),
                        accessToken = accesstoken
                    )

                    call.respond(oppdatertVilkaarsvurdering)
                }
            }

            delete("/{behandlingId}") {
                withBehandlingId { behandlingId ->
                    logger.info("Sletter vilkårsvurderingsresultat for $behandlingId")
                    val oppdatertVilkaarsvurdering = vilkaarsvurderingService.slettTotalVurdering(behandlingId)

                    call.respond(oppdatertVilkaarsvurdering)
                }
            }
        }
    }
}

private suspend inline fun PipelineContext<*, ApplicationCall>.withBehandlingId(onSuccess: (id: UUID) -> Unit) {
    val id = call.parameters["behandlingId"]
    if (id == null) {
        call.respond(HttpStatusCode.BadRequest, "Fant ikke behandlingId")
    }

    try {
        onSuccess(UUID.fromString(id))
    } catch (e: IllegalArgumentException) {
        call.respond(HttpStatusCode.BadRequest, "behandlingId må være en UUID")
    }
}

private fun toVurdertVilkaar(vurdertVilkaarDto: VurdertVilkaarDto, saksbehandler: String) =
    VurdertVilkaar(
        hovedvilkaar = vurdertVilkaarDto.hovedvilkaar,
        unntaksvilkaar = vurdertVilkaarDto.unntaksvilkaar,
        vurdering = VilkaarVurderingData(
            kommentar = vurdertVilkaarDto.kommentar,
            tidspunkt = LocalDateTime.now(),
            saksbehandler = saksbehandler
        )
    )

private fun toVilkaarsvurderingResultat(
    vurdertResultatDto: VurdertVilkaarsvurderingResultatDto,
    saksbehandler: String
) = VilkaarsvurderingResultat(
    vurdertResultatDto.resultat,
    vurdertResultatDto.kommentar,
    LocalDateTime.now(),
    saksbehandler
)

data class VurdertVilkaarDto(
    val hovedvilkaar: VilkaarTypeOgUtfall,
    val unntaksvilkaar: VilkaarTypeOgUtfall? = null,
    val kommentar: String?
)

data class VurdertVilkaarsvurderingResultatDto(
    val resultat: VilkaarsvurderingUtfall,
    val kommentar: String?
)