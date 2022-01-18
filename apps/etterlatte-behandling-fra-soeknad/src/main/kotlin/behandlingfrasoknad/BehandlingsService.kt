package no.nav.etterlatte.behandlingfrasoknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.Opplysning
import java.util.*

class BehandlingsService(private val behandling_app: HttpClient, private val url: String, private val opplysningsuthenter: Opplysningsuthenter = Opplysningsuthenter()) : Behandling {
    override fun initierBehandling(sak: Long, jsonNode: JsonNode, jsonNode1: Long): UUID {
        return runBlocking {
            behandling_app.post<String>("$url/behandlinger") {
                contentType(ContentType.Application.Json)
                body = NyBehandlingRequest(sak, opplysningsuthenter.lagOpplysningsListe(jsonNode))
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

data class NyBehandlingRequest(
    val sak: Long,
    val opplysninger: List<Opplysning>
)

