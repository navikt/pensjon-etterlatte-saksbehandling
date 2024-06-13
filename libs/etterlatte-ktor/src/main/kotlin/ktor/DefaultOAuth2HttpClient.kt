package no.nav.etterlatte.libs.ktor.ktor

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.ktor.ktor.client.ClientCallLogging
import no.nav.security.token.support.client.core.http.OAuth2HttpClient
import no.nav.security.token.support.client.core.http.OAuth2HttpRequest
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse

class DefaultOAuth2HttpClient : OAuth2HttpClient {
    private val defaultHttpClient =
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                expectSuccess = true
                jackson {
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    setSerializationInclusion(JsonInclude.Include.NON_NULL)
                }
            }
            install(ClientCallLogging)
        }

    // Override default POST with other form parameters specified for Idp request
    override fun post(req: OAuth2HttpRequest): OAuth2AccessTokenResponse =
        runBlocking {
            defaultHttpClient
                .submitForm(
                    url = req.tokenEndpointUrl.toString(),
                    formParameters =
                        Parameters.build {
                            req.formParameters.forEach {
                                append(it.key, it.value)
                            }
                        },
                ).body()
        }
}
