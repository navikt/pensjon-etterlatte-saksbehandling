package no.nav.etterlatte.libs.ktor

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

/**
 * Basically straight outta the ktor docs
 */
val AuthorizationPlugin =
    createRouteScopedPlugin(
        name = "AuthorizationPlugin",
        createConfiguration = ::PluginConfiguration
    ) {
        val roles = pluginConfig.roles
        pluginConfig.apply {
            on(AuthenticationChecked) { call ->
                val userRoles =
                    call.principal<TokenValidationContextPrincipal>()
                        ?.context?.firstValidToken?.get()?.jwtTokenClaims?.getAsList("roles") ?: emptyList()

                if (userRoles.intersect(roles).isEmpty()) {
                    call.respond(HttpStatusCode.Unauthorized, "Har ikke påkrevd rolle ")
                }
            }
        }
    }

class PluginConfiguration {
    var roles: Set<String> = emptySet()
}