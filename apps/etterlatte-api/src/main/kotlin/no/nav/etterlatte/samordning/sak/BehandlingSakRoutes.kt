package no.nav.etterlatte.samordning.sak

import com.typesafe.config.Config
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.AuthorizationPlugin
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.Issuer

private fun generateRoles(config: Config): Set<String> {
    val defaultRoles =
        setOf(
            config.getString("roller.pensjon-saksbehandler"),
            config.getString("roller.gjenny-saksbehandler"),
        )
    // TODO: les-oms-sak-for-person kan fjernes siden de kaller oss med OBO......
    return defaultRoles + "les-oms-sak-for-person"
}

fun Route.behandlingSakRoutes(
    behandlingService: BehandlingService,
    config: Config,
) {
    route("api/oms") {
        install(AuthorizationPlugin) {
            accessPolicyRolesEllerAdGrupper = generateRoles(config)
            issuers = setOf(Issuer.AZURE.issuerName)
        }

        post("/person/sak") {
            val foedselsnummer = call.receive<FoedselsnummerDTO>()
            val saker = behandlingService.hentSakforPerson(foedselsnummer)
            call.respond(saker)
        }
    }

    route("api/sak") {
        route("oms/har_sak") {
            install(AuthorizationPlugin) {
                accessPolicyRolesEllerAdGrupper = setOf("les-oms-sak")
            }
            post {
                val foedselsnummer = call.receive<FoedselsnummerDTO>()
                val saker = behandlingService.hentSakforPerson(foedselsnummer)

                call.respond(HarOMSSakIGjenny(saker.isNotEmpty()))
            }
        }

        route("/{$SAKID_CALL_PARAMETER}") {
            install(AuthorizationPlugin) {
                accessPolicyRolesEllerAdGrupper = setOf("les-bp-sak", "les-oms-sak")
            }
            get {
                val sak =
                    behandlingService.hentSak(sakId)
                        ?: throw IkkeFunnetException(
                            code = "SAK_IKKE_FUNNET",
                            detail = "Sak med id=$sakId finnes ikke",
                        )

                call.respond(sak)
            }
        }
    }
}

data class HarOMSSakIGjenny(
    val harOMSSak: Boolean,
)
