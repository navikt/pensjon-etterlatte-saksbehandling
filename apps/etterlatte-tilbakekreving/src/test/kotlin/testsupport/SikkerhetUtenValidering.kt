package no.nav.etterlatte.testsupport

import io.ktor.http.*
import io.ktor.server.auth.*
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.util.*
import java.util.stream.Collectors


class TokenSupportAcceptAllProvider : AuthenticationProvider(ProviderConfiguration(null)) {

    class ProviderConfiguration internal constructor(name: String?) : AuthenticationProvider.Config(name)

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

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        context.principal(TokenValidationContextPrincipal(TokenValidationContext(getTokensFromHeader(context.call.request.headers).associateBy { it.issuer })))
    }
}

fun AuthenticationConfig.tokenTestSupportAcceptsAllTokens() = register(TokenSupportAcceptAllProvider())