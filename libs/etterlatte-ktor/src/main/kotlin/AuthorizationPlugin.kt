package no.nav.etterlatte.libs.ktor

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.log
import io.ktor.server.auth.AuthenticationChecked
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt

/**
 * Basically straight outta the ktor docs
 */
val AuthorizationPlugin =
    createRouteScopedPlugin(
        name = "AuthorizationPlugin",
        createConfiguration = ::PluginConfiguration,
    ) {
        val roles = pluginConfig.roles
        pluginConfig.apply {
            on(AuthenticationChecked) { call ->
                val userRoles =
                    call.firstValidTokenClaims()?.getAsList("roles") ?: emptyList()

                if (userRoles.intersect(roles).isEmpty()) {
                    application.log.info("Request avslått pga manglende rolle (gyldige: $roles)")
                    throw ForespoerselException(
                        status = HttpStatusCode.Unauthorized.value,
                        code = "GE-VALIDATE-ACCESS-ROLE",
                        detail = "Har ikke påkrevd rolle",
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

class PluginConfiguration {
    var roles: Set<String> = emptySet()
}
