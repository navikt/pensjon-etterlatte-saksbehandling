package no.nav.etterlatte.samordning.sak

import com.typesafe.config.Config
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.ktor.AuthorizationPlugin
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO

fun Route.behandlingSakRoutes(
    behandlingService: BehandlingService,
    config: Config,
) {
    route("api/oms") {
        install(AuthorizationPlugin) {
            roles = setOf(config.getString("roller.pensjon-saksbehandler"), config.getString("roller.gjenny-saksbehandler"))
            issuers = setOf("azure")
        }
        get("/person/sak") {
            val fnrOgSaktype = call.receive<FoedselsnummerDTO>()
            behandlingService.hentSakforPerson(fnrOgSaktype)
        }
    }
}
