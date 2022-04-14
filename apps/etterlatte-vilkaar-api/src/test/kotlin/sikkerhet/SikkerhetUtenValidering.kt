package no.nav.etterlatte.sikkerhet

import io.ktor.application.call
import io.ktor.auth.Authentication
import io.ktor.auth.AuthenticationPipeline
import io.ktor.auth.AuthenticationProvider
import io.ktor.http.Headers
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.ktor.TokenValidationContextPrincipal
import java.util.*
import java.util.stream.Collectors

class TokenSupportAcceptAllProvider : AuthenticationProvider(ProviderConfiguration()) {
    class ProviderConfiguration : Configuration(null)

    init {
        pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
            context.principal(TokenValidationContextPrincipal(TokenValidationContext(getTokensFromHeader(call.request.headers).associateBy { it.issuer })))
        }
    }

    private fun getTokensFromHeader(request: Headers): List<JwtToken> {
        try {
            val authorization = request["Authorization"]
            if (authorization != null) {
                val headerValues = authorization.split(",".toRegex()).toTypedArray()
                return extractBearerTokens(*headerValues)
                    .stream()
                    .map { encodedToken: String? ->
                        JwtToken(
                            encodedToken
                        )
                    }
                    .collect(Collectors.toList())
            }
        } catch (e: java.lang.Exception) {
            //Ingen sikkerhet
        }
        return emptyList()
    }


    private fun extractBearerTokens(vararg headerValues: String): List<String> {
        return Arrays.stream(headerValues)
            .map { s: String ->
                s.split(
                    " ".toRegex()
                ).toTypedArray()
            }
            .filter { pair: Array<String> -> pair.size == 2 }
            .filter { pair: Array<String> ->
                pair[0].trim { it <= ' ' }
                    .equals("Bearer", ignoreCase = true)
            }
            .map { pair: Array<String> ->
                pair[1].trim { it <= ' ' }
            }
            .collect(Collectors.toList())
    }
}

fun Authentication.Configuration.tokenTestSupportAcceptsAllTokens() = register(TokenSupportAcceptAllProvider())