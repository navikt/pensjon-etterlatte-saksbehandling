
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import java.util.*

class BehandlingsService(
    private val behandling_app: HttpClient,
    private val url: String,
) : Behandling {
    override fun initierBehandling(sak: Long, jsonNode: JsonNode, jsonNode1: Long, persongalleri: Persongalleri): UUID {
        return runBlocking {
            behandling_app.post<String>("$url/behandlinger") {
                contentType(ContentType.Application.Json)
                body = NyBehandlingRequest(sak, persongalleri)
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

    //TODO finn riktig url
    override fun lagreGyldighetsVurdering(behandlingsId: UUID, gyldighet: GyldighetsResultat) {
        return runBlocking {
            behandling_app.post<String>("$url/gyldighet") {
                contentType(ContentType.Application.Json)
                body = gyldighet
            }
        }
    }
}

data class NyBehandlingRequest(
    val sak: Long,
    val persongalleri: Persongalleri
)

