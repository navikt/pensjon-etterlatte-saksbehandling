package no.nav.etterlatte.config.modules

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials

internal class HttpClientFactory(
    private val config: Config,
) {
    private val azureAppClientId: String = config.getString("azure.app.client.id")
    private val azureAppJwk: String = config.getString("azure.app.jwk")
    private val azureAppWellKnownUrl: String = config.getString("azure.app.well.known.url")

    fun lagKlient(scope: String): HttpClient =
        httpClientClientCredentials(
            azureAppClientId = azureAppClientId,
            azureAppJwk = azureAppJwk,
            azureAppWellKnownUrl = azureAppWellKnownUrl,
            azureAppScope = scope,
        )

    fun pdlKlient(): HttpClient = lagKlient(config.getString("pdl.azure.scope"))

    fun skjermingKlient(): HttpClient = lagKlient(config.getString("skjerming.azure.scope"))

    fun navAnsattKlient(): HttpClient = lagKlient(config.getString("navansatt.azure.scope"))

    fun klageKlient(): HttpClient = lagKlient(config.getString("klage.azure.scope"))

    fun tilbakekrevingKlient(): HttpClient = lagKlient(config.getString("tilbakekreving.azure.scope"))

    fun krrKlient(): HttpClient = lagKlient(config.getString("krr.scope"))

    fun entraProxyKlient(): HttpClient = lagKlient(config.getString("entraProxy.scope"))

    fun sigrunKlient(): HttpClient = lagKlient(config.getString("sigrun.scope"))

    fun inntektskomponentKlient(): HttpClient = lagKlient(config.getString("inntektskomponenten.scope"))
}
