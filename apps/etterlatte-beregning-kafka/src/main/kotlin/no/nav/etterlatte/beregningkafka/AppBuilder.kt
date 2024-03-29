package no.nav.etterlatte.beregningkafka

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials

class AppBuilder(props: Miljoevariabler) {
    private val beregningapp: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = props.requireEnvValue("AZURE_APP_CLIENT_ID"),
            azureAppJwk = props.requireEnvValue("AZURE_APP_JWK"),
            azureAppWellKnownUrl = props.requireEnvValue("AZURE_APP_WELL_KNOWN_URL"),
            azureAppScope = props.requireEnvValue("BEREGNING_AZURE_SCOPE"),
            ekstraJacksoninnstillinger = { it.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) },
        )
    }

    private val trygdetidapp: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = props.requireEnvValue("AZURE_APP_CLIENT_ID"),
            azureAppJwk = props.requireEnvValue("AZURE_APP_JWK"),
            azureAppWellKnownUrl = props.requireEnvValue("AZURE_APP_WELL_KNOWN_URL"),
            azureAppScope = props.requireEnvValue("TRYGDETID_AZURE_SCOPE"),
            ekstraJacksoninnstillinger = { it.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) },
        )
    }

    fun createBeregningService(): BeregningService {
        return BeregningService(beregningapp, "http://etterlatte-beregning")
    }

    fun createTrygdetidService(): TrygdetidService {
        return TrygdetidService(trygdetidapp, "http://etterlatte-trygdetid")
    }
}
