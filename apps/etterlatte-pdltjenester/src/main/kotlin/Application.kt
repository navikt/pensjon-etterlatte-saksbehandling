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
import no.nav.etterlatte.kodeverk.KodeverkKlient
import no.nav.etterlatte.kodeverk.KodeverkService
import no.nav.etterlatte.ktortokenexchange.SecurityContextMediatorFactory
import no.nav.etterlatte.ktortokenexchange.bearerToken
import no.nav.etterlatte.person.PersonKlient
import no.nav.etterlatte.person.PersonService
import no.nav.etterlatte.security.ktor.clientCredential

class ApplicationContext(configLocation: String? = null) {
    private val closables = mutableListOf<() -> Unit>()

    private val config: Config = configLocation?.let { ConfigFactory.load(it) } ?: ConfigFactory.load()

    fun close() {
        closables.forEach { it() }
    }

    //val personService: PersonService
    val securityMediator = SecurityContextMediatorFactory.from(config)
    val personServiceAad: PersonService
    val kodeverkService: KodeverkService


    //TODO fikse noe ift AAD støtte her
    init {
       /* personService = tokenSecuredEndpoint(config.getConfig("no.nav.etterlatte.tjenester.pdl"))
            .also { closables.add(it::close) }
            .let { PersonService(PersonKlient(it)) }

        */
        kodeverkService = tokenSecuredEndpoint(config.getConfig("no.nav.etterlatte.tjenester.kodeverk"))
            .also { closables.add(it::close) }
            .let { KodeverkService(KodeverkKlient(it)) }
        personServiceAad = pdlhttpclient(config.getConfig("no.nav.etterlatte.tjenester.pdl.aad"))
            .also { closables.add(it::close) }
            .let { PersonService(PersonKlient(it), kodeverkService) }
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
        install(JsonFeature) { serializer = JacksonSerializer() }
        install(Auth) {
            clientCredential {
                config = env
            }
        }
        defaultRequest {
            url.takeFrom(aad.getString("url") + url.encodedPath)
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }
}

fun main() {
    ApplicationContext()
        .also { Server(it).run() }
        .close()
}
