package no.nav.etterlatte.vilkaarsvurdering.migrering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingMigreringRequest
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.vilkaarsvurdering.klienter.BehandlingKlient
import java.util.UUID

fun Route.migrering(
    migreringService: MigreringService,
    behandlingKlient: BehandlingKlient,
    vilkaarsvurderingService: VilkaarsvurderingService,
) {
    route("/api/vilkaarsvurdering/migrering") {
        val logger = application.log

        post("/{$BEHANDLINGID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                val request = call.receive<VilkaarsvurderingMigreringRequest>()
                logger.info("Oppretter vilkårsvurdering for gjenoppretting for $behandlingId")
                val vilkaarsvurdering = vilkaarsvurderingService.opprettVilkaarsvurdering(behandlingId, brukerTokenInfo)

                logger.info("Oppdaterer vilkårene med korrekt utfall for gjenoppretting $behandlingId")
                migreringService.settUtfallForAlleVilkaar(vilkaarsvurdering, request.yrkesskadeFordel)

                settVilkaarsvurderingaSomHelhetSomOppfylt(vilkaarsvurderingService, behandlingId)
                call.respond(HttpStatusCode.Accepted)
            }
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.settVilkaarsvurderingaSomHelhetSomOppfylt(
    vilkaarsvurderingService: VilkaarsvurderingService,
    behandlingId: UUID,
) = vilkaarsvurderingService.oppdaterTotalVurdering(
    behandlingId,
    brukerTokenInfo,
    VilkaarsvurderingResultat(
        utfall = VilkaarsvurderingUtfall.OPPFYLT,
        kommentar = "Automatisk gjenopprettet basert på opphørt sak fra Pesys. Enkeltvilkår ikke vurdert, totalvurdering satt til oppfylt.",
        tidspunkt = Tidspunkt.now().toLocalDatetimeUTC(),
        saksbehandler = brukerTokenInfo.ident(),
    ),
)
