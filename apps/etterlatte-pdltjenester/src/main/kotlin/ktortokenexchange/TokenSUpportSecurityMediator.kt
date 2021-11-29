package no.nav.etterlatte.ktortokenexchange

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.principal
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.config.ApplicationConfig
import io.ktor.routing.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import no.nav.etterlatte.oauth.ClientConfig
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.ktor.TokenValidationContextPrincipal
import no.nav.security.token.support.ktor.tokenValidationSupport

class TokenSecurityContext(private val tokens: TokenValidationContext): SecurityContext {
    fun tokenIssuedBy(issuer: String): JwtToken? {
        return tokens.getJwtToken(issuer)
    }

    override fun user() = tokens.firstValidToken.get().jwtTokenClaims?.get("pid")?.toString()
}

class TokenSupportSecurityContextMediator(private val configuration: ApplicationConfig): SecurityContextMediator {

    private val defaultHttpClient = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                setSerializationInclusion(JsonInclude.Include.NON_NULL)
            }
        }
    }

    val tokenexchangeIssuer = "tokenx"
    val tokenxKlient = runBlocking {
        configuration.propertyOrNull("no.nav.etterlatte.app.ventmedutgaaendekall")?.getString()?.toLong()?.also {
            println("Venter ${it} sekunder før kall til token-issuers")
            delay(it * 1000)
        }
        checkNotNull(ClientConfig(configuration, defaultHttpClient).clients[tokenexchangeIssuer])
    }


    private fun attachToRoute(route: Route){
        route.intercept(ApplicationCallPipeline.Call) {
            withContext(
                Dispatchers.Default + ThreadBoundSecCtx.asContextElement(
                    value = TokenSecurityContext(
                        call.principal<TokenValidationContextPrincipal>()?.context!!
                    )
                )
            ) {
                proceed()
            }
        }
    }

    override fun outgoingToken(
        audience: String
    ) = suspend {
        (ThreadBoundSecCtx.get() as TokenSecurityContext).tokenIssuedBy(tokenexchangeIssuer)?.let {
            tokenxKlient.tokenExchange(
                it.tokenAsString,
                audience
            ).accessToken
        }!!
    }
    override fun secureRoute(ctx: Route, block: Route.()->Unit){
        ctx.authenticate {
            attachToRoute(this)
            block()
        }
    }
    override fun installSecurity(ktor: Application){
        ktor.install(Authentication) {
            tokenValidationSupport(config = configuration)
        }
    }
}