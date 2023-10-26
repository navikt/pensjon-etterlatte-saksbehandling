package no.nav.etterlatte.vilkaarsvurdering.migrering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
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
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Oppretter vilkårsvurdering for migrering for $behandlingId")
                vilkaarsvurderingService.opprettVilkaarsvurdering(behandlingId, brukerTokenInfo)
                logger.info("Setter alle vilkår til ikke vurdert for behandling $behandlingId")
                migreringService.endreUtfallTilIkkeVurdertForAlleVilkaar(behandlingId)
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
        kommentar = "Automatisk overført fra Pesys. Enkeltvilkår ikke vurdert, totalvurdering satt til oppfylt.",
        tidspunkt = Tidspunkt.now().toLocalDatetimeUTC(),
        saksbehandler = brukerTokenInfo.ident(),
    ),
)
