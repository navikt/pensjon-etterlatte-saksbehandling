package no.nav.etterlatte.vilkaarsvurdering

import io.ktor.http.HttpStatusCode
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
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.common.withParam
import no.nav.etterlatte.libs.ktor.accesstoken
import no.nav.etterlatte.libs.ktor.saksbehandler
import java.time.LocalDateTime
import java.util.*

fun Route.vilkaarsvurdering(vilkaarsvurderingService: VilkaarsvurderingService) {
    route("/api/vilkaarsvurdering") {
        val logger = application.log

        get("/{behandlingId}") {
            withBehandlingId { behandlingId ->
                try {
                    logger.info("Henter vilkårsvurdering for $behandlingId")
                    vilkaarsvurderingService.hentEllerOpprettVilkaarsvurdering(behandlingId, accesstoken)
                        .let { call.respond(it.toDto()) }
                } catch (e: VirkningstidspunktIkkeSattException) {
                    logger.info("Virkningstidspunkt er ikke satt for behandling $behandlingId")
                    call.respond(HttpStatusCode.PreconditionFailed)
                }
            }
        }

        post("/{behandlingId}") {
            withBehandlingId { behandlingId ->
                val vurdertVilkaarDto = call.receive<VurdertVilkaarDto>()
                val vurdertVilkaar = vurdertVilkaarDto.toVurdertVilkaar(saksbehandler)

                logger.info("Oppdaterer vilkårsvurdering for $behandlingId")
                vilkaarsvurderingService.oppdaterVurderingPaaVilkaar(behandlingId, vurdertVilkaar)
                    .let { call.respond(it.toDto()) }
            }
        }

        delete("/{behandlingId}/{vilkaarId}") {
            withParam("behandlingId", "vilkaarId") { behandlingId, vilkaarId ->
                logger.info("Sletter vurdering på vilkår $vilkaarId for $behandlingId")
                vilkaarsvurderingService.slettVurderingPaaVilkaar(behandlingId, vilkaarId)
                    .let { call.respond(it.toDto()) }
            }
        }

        route("/resultat") {
            post("/{behandlingId}") {
                withBehandlingId { behandlingId ->
                    val vurdertResultatDto = call.receive<VurdertVilkaarsvurderingResultatDto>()
                    val vurdertResultat = vurdertResultatDto.toVilkaarsvurderingResultat(saksbehandler)

                    logger.info("Oppdaterer vilkårsvurderingsresultat for $behandlingId")
                    vilkaarsvurderingService.oppdaterTotalVurdering(behandlingId, vurdertResultat)
                        .let { call.respond(it.toDto()) }
                }
            }

            delete("/{behandlingId}") {
                withBehandlingId { behandlingId ->
                    logger.info("Sletter vilkårsvurderingsresultat for $behandlingId")
                    vilkaarsvurderingService.slettTotalVurdering(behandlingId)
                        .let { call.respond(it.toDto()) }
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
        vurdering = VilkaarVurderingData(
            kommentar = kommentar,
            tidspunkt = LocalDateTime.now(),
            saksbehandler = saksbehandler
        )
    )

private fun VurdertVilkaarsvurderingResultatDto.toVilkaarsvurderingResultat(saksbehandler: String) =
    VilkaarsvurderingResultat(
        utfall = resultat,
        kommentar = kommentar,
        tidspunkt = LocalDateTime.now(),
        saksbehandler = saksbehandler
    )

data class VurdertVilkaarDto(
    val vilkaarId: UUID,
    val hovedvilkaar: VilkaarTypeOgUtfall,
    val unntaksvilkaar: VilkaarTypeOgUtfall? = null,
    val kommentar: String?
)

data class VurdertVilkaarsvurderingResultatDto(
    val resultat: VilkaarsvurderingUtfall,
    val kommentar: String?
)