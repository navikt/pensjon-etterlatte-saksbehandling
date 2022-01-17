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
import no.nav.etterlatte.libs.common.behandling.Opplysning
import no.nav.etterlatte.security.ktor.clientCredential
import java.time.Instant
import java.util.*

class AppBuilder(private val props: Map<String, String>) {

    private val behandling_app = behandlingHttpClient()


    fun createBehandlingService(): Behandling {
        return BehandlingsService(behandling_app, "http://etterlatte-behandling")
    }

    private fun behandlingHttpClient() = HttpClient(OkHttp) {
        install(JsonFeature) { serializer = JacksonSerializer() }
        install(Auth) {
            clientCredential {
                config = props.toMutableMap()
                    .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get("BEHANDLING_AZURE_SCOPE"))) }
            }
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }


}


class BehandlingsService(val behandling_app: HttpClient, val url: String) : Behandling {
    override fun initierBehandling(sak: Long, jsonNode: JsonNode, jsonNode1: Long): UUID {
        val opplysning = Opplysning(
            UUID.randomUUID(), Opplysning.Privatperson(
                "09018701453", Instant.parse("2022-01-03T13:44:25.88888840Z")
            ), "opplysningstype", jsonNode.deepCopy(), jsonNode.deepCopy(), null
        )

        return runBlocking {
            behandling_app.post<String>("$url/behandlinger") {
                contentType(ContentType.Application.Json)
                body = objectMapper.createObjectNode().put("sak", sak).also {
                    it.putArray("opplysninger").add(objectMapper.valueToTree(opplysning) as JsonNode)
                }

            }
        }.let {
            UUID.fromString(it)
        }
    }

    override fun skaffSak(person: String, saktype: String): Long {
        return runBlocking {
            behandling_app.get<ObjectNode>("$url/personer/$person/saker/$saktype")["id"].longValue()
        }
    }
}

