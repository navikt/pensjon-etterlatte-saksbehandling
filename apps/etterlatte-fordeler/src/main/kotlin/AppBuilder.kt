package no.nav.etterlatte.prosess

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.PersonService
import no.nav.etterlatte.security.ktor.clientCredential

class AppBuilder(private val props: Map<String, String>) {
    companion object {
        const val CONFIG_PDL_URL = "PDL_URL"
    }

    fun createPersonService(): PersonService {
        val pdlKlient = PdlKlient(pdlHttpClient(), props[CONFIG_PDL_URL]!!)

        return PersonService(pdlKlient)
    }

    private fun pdlHttpClient() = HttpClient(OkHttp) {
        install(JsonFeature) { serializer = JacksonSerializer() }
        install(Auth) {
            clientCredential {
                config = props.toMutableMap()
                    .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get("PDL_AZURE_SCOPE"))) }
            }
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }

/*

    fun fordel() = JournalfoeringService(opprettJournalfoeringKlient())

    private fun opprettJournalfoeringKlient() = HttpClient(OkHttp) {
        install(JsonFeature) { serializer = JacksonSerializer() }
        install(Auth) {
            clientCredential {
                config = System.getenv().toMutableMap().apply {
                    // put("AZURE_APP_OUTBOUND_SCOPE", "api://dev-fss.etterlatte.etterlatte-proxy/.default")
                    put("AZURE_APP_OUTBOUND_SCOPE", props[CONFIG_AZURE_DOKARKIV_SCOPE])
                }
            }
        }
    }.also {
        Runtime.getRuntime().addShutdownHook(Thread { it.close() })
    }.let {
        DokarkivKlient(it, props[CONFIG_DOKARKIV_URL]!!)
    }

    private fun pdfhttpclient() = HttpClient(OkHttp) {
        install(JsonFeature) { serializer = JacksonSerializer() }

    }.also {
        Runtime.getRuntime().addShutdownHook(Thread { it.close() })
    }

 */
}