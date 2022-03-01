package no.nav.etterlatte

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.takeFrom
import no.nav.etterlatte.ktortokenexchange.SecurityContextMediatorFactory
import no.nav.etterlatte.ktortokenexchange.bearerToken
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.person.PersonService
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.security.ktor.clientCredential


class ApplicationContext(configLocation: String? = null) {
    private val closables = mutableListOf<() -> Unit>()

    private val config: Config = configLocation?.let { ConfigFactory.load(it) } ?: ConfigFactory.load()

    fun close() {
        closables.forEach { it() }
    }

    val securityMediator = SecurityContextMediatorFactory.from(config)
    val personService: PersonService


    //TODO fikse noe ift AAD støtte her
    init {

        val ppsUrl = config.getString("no.nav.etterlatte.tjenester.pps.url")
        val pdlHttpClient = pdlhttpclient(config.getConfig("no.nav.etterlatte.tjenester.pdl.aad"))
        val ppsHttpClient = ppsHttpClient()

        closables.add(pdlHttpClient::close)
        closables.add(ppsHttpClient::close)

        personService = PersonService(PdlKlient(pdlHttpClient), ParallelleSannheterKlient(ppsHttpClient(), ppsUrl))
    }

    private fun tokenSecuredEndpoint(endpointConfig:Config) = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                setSerializationInclusion(JsonInclude.Include.NON_NULL)
                registerModule(JavaTimeModule())
            }
        }

        install(Auth) {
            bearerToken {
                tokenprovider = securityMediator.outgoingToken(endpointConfig.getString("audience"))
            }
        }

        defaultRequest {
            url.takeFrom(endpointConfig.getString("url") + url.encodedPath)
        }
    }
    private fun pdlhttpclient(aad: Config) = HttpClient(OkHttp) {
        val env = mutableMapOf(
            "AZURE_APP_CLIENT_ID" to aad.getString("client_id"),
            "AZURE_APP_WELL_KNOWN_URL" to aad.getString("well_known_url"),
            "AZURE_APP_OUTBOUND_SCOPE" to aad.getString("outbound"),
            "AZURE_APP_JWK" to aad.getString("client_jwk")
        )
        install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
        install(Auth) {
            clientCredential {
                config = env
            }
        }
        defaultRequest {
            url.takeFrom(aad.getString("url") + url.encodedPath)
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }

    private fun ppsHttpClient() = HttpClient(OkHttp) {
        install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }
}



fun main() {
    ApplicationContext()
        .also { Server(it).run() }
        .close()
}
