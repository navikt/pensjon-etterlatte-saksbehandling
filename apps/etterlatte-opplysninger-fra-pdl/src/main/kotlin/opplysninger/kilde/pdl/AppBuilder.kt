package no.nav.etterlatte.opplysninger.kilde.pdl

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import rapidsandrivers.RapidsAndRiversAppBuilder

class AppBuilder(props: Miljoevariabler) : RapidsAndRiversAppBuilder(props) {

    fun createPdlService(): Pdl {
        return PdlService(pdlTjenester, "http://etterlatte-pdltjenester")
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