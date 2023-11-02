package no.nav.etterlatte.libs.ktor

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.log
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.response.respond

val MaskinportenScopeAuthorizationPlugin =
    createRouteScopedPlugin(
        name = "MaskinportenScopeAuthorizationPlugin",
        createConfiguration = ::MaskinportenScopePluginConfiguration,
    ) {
        val scopes = pluginConfig.scopes
        pluginConfig.apply {
            on(AuthenticationChecked) { call ->
                val userScopes =
                    call.firstValidTokenClaims()?.getStringClaim("scope")
                        ?.split(" ")
                        ?: emptyList()

                if (userScopes.intersect(scopes).isEmpty()) {
                    application.log.info("Request avslått pga manglende scope (gyldige: $scopes)")
                    call.respond(HttpStatusCode.Unauthorized, "Har ikke påkrevd scope")
                }
            }
        }
    }

class MaskinportenScopePluginConfiguration {
    var scopes: Set<String> = emptySet()
}
