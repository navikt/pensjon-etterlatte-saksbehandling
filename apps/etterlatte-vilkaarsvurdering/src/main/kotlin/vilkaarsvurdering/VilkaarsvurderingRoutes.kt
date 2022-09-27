package no.nav.etterlatte.vilkaarsvurdering

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.time.LocalDateTime

fun Route.vilkaarsvurdering(vilkaarsvurderingService: VilkaarsvurderingService) {
    route("vilkaarsvurdering") {
        val logger = application.log

        get("/{behandlingId}") {
            val behandlingId = requireNotNull(call.parameters["behandlingId"])
            val vilkaarsvurdering = vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId)
            call.respond(vilkaarsvurdering)
        }

        post("/{behandlingId}") {
            val behandlingId = requireNotNull(call.parameters["behandlingId"])
            val saksbehandler = requireNotNull(call.navIdent)
            val vurdertResultatDto = call.receive<VurdertResultatDto>()
            val oppdatertVilkaarsvurdering =
                vilkaarsvurderingService.oppdaterVilkaarsvurdering(
                    behandlingId,
                    toVilkaar(vurdertResultatDto, saksbehandler)
                )
            call.respond(oppdatertVilkaarsvurdering)
        }
    }
}

val ApplicationCall.navIdent: String? get() = principal<TokenValidationContextPrincipal>()
    ?.context?.getJwtToken("azure")
    ?.jwtTokenClaims?.getStringClaim("NAVident")

private fun toVilkaar(vurdertResultatDto: VurdertResultatDto, saksbehandler: String) =
    Vilkaar(
        type = vurdertResultatDto.type,
        vurdering = VurdertResultat(
            resultat = vurdertResultatDto.resultat,
            kommentar = vurdertResultatDto.kommentar,
            tidspunkt = LocalDateTime.now(),
            saksbehandler = saksbehandler
        )
    )

data class VurdertResultatDto(
    val type: VilkaarType,
    val resultat: Utfall,
    val kommentar: String?
)

// get vilkaarsvurdering

/*fun vilkaarsvurdering(behandlingId: String) = listOf<Vilkaar>()
fun oppdaterResultat(behandlingId: String, type: Vilkaar, resultat: VurderingsResultatV2, kommentar: String) {}

fun vilkaarsResultat(behandlingsId: String) {}
*/
// database
// id - behandlingId - melding - vilkaarsvurdering