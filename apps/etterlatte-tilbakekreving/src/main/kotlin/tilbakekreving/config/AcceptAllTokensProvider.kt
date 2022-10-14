package no.nav.etterlatte.tilbakekreving.config

import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationProvider
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

internal class AcceptAllTokensProvider : AuthenticationProvider(ProviderConfiguration(null)) {

    class ProviderConfiguration internal constructor(name: String?) : Config(name)

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val authorizationHeader = context.call.request.headers["Authorization"]
        if (authorizationHeader != null) {
            val (_, token) = authorizationHeader.split(" ")
            val jwtToken = JwtToken(token)

            context.principal(
                TokenValidationContextPrincipal(
                    TokenValidationContext(mapOf(jwtToken.issuer to jwtToken))
                )
            )
        }
    }
}

fun AuthenticationConfig.tokenAcceptAllTokensSupport() = register(AcceptAllTokensProvider())