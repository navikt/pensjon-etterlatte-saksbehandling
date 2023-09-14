package no.nav.etterlatte

import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.samordning.ApplicationContext
import no.nav.etterlatte.samordning.vedtak.samordningVedtakRoute
import no.nav.security.token.support.core.context.TokenValidationContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

fun main() {
    Server(ApplicationContext(Miljoevariabler(System.getenv()))).run()
}

class Server(applicationContext: ApplicationContext) {
    init {
        sikkerLogg.info("SikkerLogg: etterlatte-samordning-vedtak oppstart")
    }

    private val engine =
        embeddedServer(
            CIO,
            environment =
                applicationEngineEnvironment {
                    config = HoconApplicationConfig(applicationContext.config)

                    module {
                        restModule(
                            sikkerLogg,
                            withMetrics = true,
                            additionalValidation = validateMaskinportenScope()
                        ) {
                            samordningVedtakRoute(samordningVedtakService = applicationContext.samordningVedtakService)
                        }
                    }
                    connector { port = applicationContext.httpPort }
                }
        )

    fun run() = setReady().also { engine.start(true) }
}

fun validateMaskinportenScope(): (TokenValidationContext) -> Boolean {
    val scopeValidation: (TokenValidationContext) -> Boolean = { ctx ->
        val scopes =
            ctx.getClaims("maskinporten")
                ?.getStringClaim("scope")
                ?.split(" ")
                ?: emptyList()

        val allowedScopes = setOf("nav:etterlatteytelser:vedtaksinformasjon.read")
        scopes.any(allowedScopes::contains)
    }
    return scopeValidation
}