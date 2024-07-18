package no.nav.etterlatte.libs.ktor

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.log
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.principal
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

/**
 * Basically straight outta the ktor docs
 */
val AuthorizationPlugin =
    createRouteScopedPlugin(
        name = "AuthorizationPlugin",
        createConfiguration = ::PluginConfiguration,
    ) {
        val rolesOrGroups = pluginConfig.accessPolicyRolesEllerAdGrupper
        pluginConfig.apply {
            on(AuthenticationChecked) { call ->
                // If no principal, probably not passed authentication (expired token etc)
                val principal = call.principal<TokenValidationContextPrincipal>() ?: return@on

                // If issuers are set and current authenticated user is not authenticated by one
                // of the issuers, then skip authorization - authorization should be handled elsewhere then.
                // If no issuers are set, then always perform authorization
                if (issuers.isEmpty() ||
                    principal.context.issuers
                        .intersect(issuers)
                        .isNotEmpty()
                ) {
                    val rollerEllerGrupper =
                        when (call.brukerTokenInfo) {
                            is Saksbehandler -> call.brukerTokenInfo.groups
                            is Systembruker -> (call.brukerTokenInfo as Systembruker).roller
                        }

                    if (rollerEllerGrupper.intersect(rolesOrGroups).isEmpty()) {
                        application.log.info(
                            "Request avslått pga manglende rolle (gyldige: $rolesOrGroups)." +
                                "Brukeren sendte med $rollerEllerGrupper",
                        )
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
    }

class PluginConfiguration {
    var accessPolicyRolesEllerAdGrupper: Set<String> = emptySet()
    var issuers: Set<String> = emptySet()
}
