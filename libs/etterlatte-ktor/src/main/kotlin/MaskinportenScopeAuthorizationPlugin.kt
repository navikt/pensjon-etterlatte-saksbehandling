package no.nav.etterlatte.libs.ktor

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.log
import io.ktor.server.auth.AuthenticationChecked
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.firstValidTokenClaims

val MaskinportenScopeAuthorizationPlugin =
    createRouteScopedPlugin(
        name = "MaskinportenScopeAuthorizationPlugin",
        createConfiguration = ::MaskinportenScopePluginConfiguration,
    ) {
        val scopes = pluginConfig.scopes
        pluginConfig.apply {
            on(AuthenticationChecked) { call ->
                call.firstValidTokenClaims()?.let { token ->
                    val userScopes =
                        token
                            .getStringClaim("scope")
                            ?.split(" ")
                            ?: emptyList()

                    if (userScopes.intersect(scopes).isEmpty()) {
                        application.log.info("Request avslått pga manglende scope (gyldige: $scopes)")
                        throw ForespoerselException(
                            status = HttpStatusCode.Unauthorized.value,
                            code = "GE-MASKINPORTEN-SCOPE",
                            detail = "Har ikke påkrevd scope",
                            meta =
                                mapOf(
                                    "correlation-id" to getCorrelationId(),
                                    "tidspunkt" to Tidspunkt.now(),
                                ),
                        )
                    }
                }
            }
        }
    }

class MaskinportenScopePluginConfiguration {
    var scopes: Set<String> = emptySet()
}
