package no.nav.etterlatte.samordning.vedtak

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.log
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.principal
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.Issuer
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

/**
 * Sjekk av at bruker kun spør etter egne data
 */
val SelvbetjeningAuthorizationPlugin =
    createRouteScopedPlugin(
        name = "SelvbetjeningAuthorizationPlugin",
        createConfiguration = ::PluginConfiguration,
    ) {
        pluginConfig.apply {
            on(AuthenticationChecked) { call ->
                // If no principal, probably not passed authentication (expired token etc)
                val principal = call.principal<TokenValidationContextPrincipal>() ?: return@on

                if (principal.context.issuers.contains(issuer)) {
                    val subject = principal.context.getClaims(pluginConfig.issuer).subject

                    if (!validator.invoke(call, Folkeregisteridentifikator.of(subject))) {
                        application.log.info("Request avslått pga mismatch mellom subject og etterspurt fnr")
                        throw IkkeTillattException(
                            code = "GE-VALIDATE-ACCESS-FNR",
                            detail = "Kan kun etterspørre egne data",
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
    var issuer: String = Issuer.TOKENX.issuerName
    var validator: (ApplicationCall, Folkeregisteridentifikator) -> Boolean = { _, _ -> false }
}
