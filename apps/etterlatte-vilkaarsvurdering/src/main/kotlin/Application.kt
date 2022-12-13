package no.nav.etterlatte

import io.ktor.server.cio.CIO
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.embeddedServer
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.vilkaarsvurdering.config.ApplicationContext
import no.nav.etterlatte.vilkaarsvurdering.vilkaarsvurdering

fun main() {
    ApplicationContext().let { ctx ->
        ctx.dataSourceBuilder.migrate()

        embeddedServer(
            factory = CIO,
            environment = applicationEngineEnvironment {
                module { restModule { vilkaarsvurdering(ctx.vilkaarsvurderingService) } }
            }
        ).start()
    }
}