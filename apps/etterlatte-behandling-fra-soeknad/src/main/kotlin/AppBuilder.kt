package no.nav.etterlatte

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.security.ktor.clientCredential
import java.util.*

class AppBuilder(private val props: Map<String, String>) {

    private val behandling_app = behadnlingHttpClient()


    fun createSakService(): Sak {
        return object :Sak {
            override fun skaffSak(person: String, saktype: String): Long {
                return runBlocking {
                    behandling_app.get<ObjectNode>("http://etterlatte-behandling/personer/$person/saker/$saktype")["id"].longValue()
                }
            }

        }
    }

    fun createBehandlingService(): Behandling {
        return object :Behandling {
            override fun initierBehandling(sak: Long, jsonNode: JsonNode, jsonNode1: Long): UUID {
                return runBlocking {
                    behandling_app.post<String>("http://etterlatte-behandling/behandlinger"){
                        contentType(ContentType.Application.Json)
                        body = objectMapper.createObjectNode().put("sak", sak).also { it.putArray("opplysninger") }
                    }
                }.let { UUID.fromString(it) }
            }

        }
    }

    private fun behadnlingHttpClient() = HttpClient(OkHttp) {
        install(JsonFeature) { serializer = JacksonSerializer() }
        install(Auth) {
            clientCredential {
                config = props.toMutableMap()
                    .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get("BEHANDLING_AZURE_SCOPE"))) }
            }
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }


}