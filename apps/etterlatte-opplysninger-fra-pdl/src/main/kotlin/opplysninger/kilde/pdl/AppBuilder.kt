package no.nav.etterlatte.opplysninger.kilde.pdl

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials

class AppBuilder(props: Miljoevariabler) {

    fun createPdlService(): PdlKlientInterface {
        return PdlKlient(pdlTjenester, "http://etterlatte-pdltjenester")
    }

    private val pdlTjenester: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = props.requireEnvValue("AZURE_APP_CLIENT_ID"),
            azureAppJwk = props.requireEnvValue("AZURE_APP_JWK"),
            azureAppWellKnownUrl = props.requireEnvValue("AZURE_APP_WELL_KNOWN_URL"),
            azureAppScope = props.requireEnvValue("PDLTJENESTER_AZURE_SCOPE"),
            ekstraJacksoninnstillinger = { it.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) }
        )
    }
}