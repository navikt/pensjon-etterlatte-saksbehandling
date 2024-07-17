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
import no.nav.etterlatte.libs.common.isProd
import no.nav.etterlatte.libs.ktor.AuthorizationPlugin
import no.nav.etterlatte.libs.ktor.Issuer
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO

fun generateRoles(config: Config): Set<String> {
    val defaultRoles =
        setOf(
            config.getString("roller.pensjon-saksbehandler"),
            config.getString("roller.gjenny-saksbehandler"),
        )
    if (isProd()) {
        return defaultRoles
    } else {
        return defaultRoles + "les-oms-sak-for-person"
    }
}

fun Route.behandlingSakRoutes(
    behandlingService: BehandlingService,
    config: Config,
) {
    route("api/oms") {
        install(AuthorizationPlugin) {
            roles = generateRoles(config)
            issuers = setOf(Issuer.AZURE.issuerName)
        }
        post("/person/sak") {
            val fnrOgSaktype = call.receive<FoedselsnummerDTO>()
            call.respond(behandlingService.hentSakforPerson(fnrOgSaktype))
        }
    }
}
