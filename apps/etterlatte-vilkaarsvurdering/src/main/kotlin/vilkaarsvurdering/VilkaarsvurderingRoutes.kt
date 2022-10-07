package no.nav.etterlatte.vilkaarsvurdering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.time.LocalDateTime
import java.util.*

fun Route.vilkaarsvurdering(vilkaarsvurderingService: VilkaarsvurderingService) {
    route("/api/vilkaarsvurdering") {
        val logger = application.log

        get("/{behandlingId}") {
            val behandlingId = requireNotNull(call.parameters["behandlingId"]).toUUID()

            logger.info("Henter vilkårsvurdering for $behandlingId")
            val vilkaarsvurdering = vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId)
            vilkaarsvurdering?.let { call.respond(vilkaarsvurdering) } ?: call.respond(HttpStatusCode.NotFound)
        }

        post("/{behandlingId}") {
            val behandlingId = requireNotNull(call.parameters["behandlingId"]).toUUID()
            val saksbehandler = requireNotNull(call.navIdent)
            val vurdertResultatDto = call.receive<VurdertResultatDto>()

            logger.info("Oppdaterer vilkårsvurdering for $behandlingId")
            val oppdatertVilkaarsvurdering =
                vilkaarsvurderingService.oppdaterVurderingPaaVilkaar(
                    behandlingId,
                    toVurdertVilkaar(vurdertResultatDto, saksbehandler)
                )
            call.respond(oppdatertVilkaarsvurdering)
        }

        delete("/{behandlingId}/{vilkaarType}") {
            val behandlingId = requireNotNull(call.parameters["behandlingId"]).toUUID()
            val vilkaarType = VilkaarType.valueOf(requireNotNull(call.parameters["vilkaarType"]))

            logger.info("Sletter vurdering på vilkår $vilkaarType for $behandlingId")
            vilkaarsvurderingService.slettVurderingPaaVilkaar(behandlingId, vilkaarType)
            call.respond(HttpStatusCode.OK)
        }

        route("/resultat") {
            post("/{behandlingId}") {
                val behandlingId = requireNotNull(call.parameters["behandlingId"]).toUUID()
                val saksbehandler = requireNotNull(call.navIdent)
                val vurdertResultatDto = call.receive<VurdertVilkaarsvurderingResultatDto>()

                logger.info("Oppdaterer vilkårsvurderingsresultat for $behandlingId")
                val oppdatertVilkaarsvurdering =
                    vilkaarsvurderingService.oppdaterTotalVurdering(
                        behandlingId,
                        VilkaarsvurderingResultat(
                            vurdertResultatDto.resultat,
                            vurdertResultatDto.kommentar,
                            LocalDateTime.now(),
                            saksbehandler
                        )
                    )
                call.respond(oppdatertVilkaarsvurdering)
            }

            delete("/{behandlingId}") {
                val behandlingId = requireNotNull(call.parameters["behandlingId"]).toUUID()

                logger.info("Sletter vilkårsvurderingsresultat for $behandlingId")
                val oppdatertVilkaarsvurdering = vilkaarsvurderingService.slettTotalVurdering(behandlingId)
                call.respond(oppdatertVilkaarsvurdering)
            }
        }
    }
}

private fun String.toUUID() = UUID.fromString(this)

private val ApplicationCall.navIdent: String?
    get() = principal<TokenValidationContextPrincipal>()
        ?.context?.getJwtToken("azure")
        ?.jwtTokenClaims?.getStringClaim("NAVident")

private fun toVurdertVilkaar(vurdertResultatDto: VurdertResultatDto, saksbehandler: String) =
    VurdertVilkaar(
        vilkaarType = vurdertResultatDto.type,
        vurdertResultat = VurdertResultat(
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

data class VurdertVilkaarsvurderingResultatDto(
    val resultat: VilkaarsvurderingUtfall,
    val kommentar: String?
)