package no.nav.etterlatte.regulering

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.client.HttpClient
import no.nav.etterlatte.VedtakServiceImpl
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.requireEnvValue
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials

class AppBuilder(props: Miljoevariabler) {
    private val vedtakUrl = requireNotNull(props["ETTERLATTE_VEDTAK_URL"])
    private val env = System.getenv()

    fun lagVedtakKlient(): VedtakServiceImpl {
        return VedtakServiceImpl(vedtakHttpKlient, vedtakUrl)
    }

    private val vedtakHttpKlient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = props.requireEnvValue("AZURE_APP_CLIENT_ID"),
            azureAppJwk = env.requireEnvValue("AZURE_APP_JWK"),
            azureAppWellKnownUrl = env.requireEnvValue("AZURE_APP_WELL_KNOWN_URL"),
            azureAppScope = env.requireEnvValue("VEDTAK_AZURE_SCOPE"),
            ekstraJacksoninnstillinger = { it.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) }
        )
    }
}