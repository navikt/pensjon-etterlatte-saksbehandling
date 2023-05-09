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
import no.nav.etterlatte.libs.common.grunnlag.PersongalleriRequest
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.ktor.bruker
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingMigreringRequest
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.vilkaarsvurdering.klienter.GrunnlagKlient
import java.util.*

fun Route.migrering(
    migreringService: MigreringService,
    vilkaarsvurderingService: VilkaarsvurderingService,
    grunnlagKlient: GrunnlagKlient
) {
    route("/api/vilkaarsvurdering/migrering") {
        val logger = application.log

        post("/") {
            val request = call.receive<VilkaarsvurderingMigreringRequest>()
            logger.info("Oppretter vilkårsvurdering for migrering for $request.behandlingId")
            grunnlagKlient.opprettPersongalleri(
                PersongalleriRequest(
                    request.sakId,
                    request.fnr,
                    request.persongalleri
                ),
                bruker
            )
            vilkaarsvurderingService.opprettVilkaarsvurdering(request.behandlingId, bruker)
            logger.info("Setter alle vilkår til ikke vurdert for behandling $request.behandlingId")
            migreringService.endreUtfallTilIkkeVurdertForAlleVilkaar(request.behandlingId)
            settVilkaarsvurderingaSomHelhetSomOppfylt(vilkaarsvurderingService, request.behandlingId)
            call.respond(HttpStatusCode.Accepted)
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.settVilkaarsvurderingaSomHelhetSomOppfylt(
    vilkaarsvurderingService: VilkaarsvurderingService,
    behandlingId: UUID
) = vilkaarsvurderingService.oppdaterTotalVurdering(
    behandlingId,
    bruker,
    VilkaarsvurderingResultat(
        utfall = VilkaarsvurderingUtfall.OPPFYLT,
        kommentar = "Automatisk overført fra Pesys. Enkeltvilkår ikke vurdert, totalvurdering satt til oppfylt.",
        tidspunkt = Tidspunkt.now().toLocalDatetimeUTC(),
        saksbehandler = bruker.ident()
    )
)