import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import java.util.*

class BehandlingsService(
    private val behandling_app: HttpClient,
    private val url: String
) : Behandling {
    override fun initierBehandling(
        sak: Long,
        mottattDato: String,
        persongalleri: Persongalleri
    ): UUID {
        return runBlocking {
            behandling_app.post("$url/behandlinger") {
                contentType(ContentType.Application.Json)
                setBody(NyBehandlingRequest(sak, persongalleri, mottattDato))
            }.body<String>()
        }.let {
            UUID.fromString(it)
        }
    }

    override fun skaffSak(person: String, saktype: String): Long {
        return runBlocking {
            behandling_app.get("$url/personer/$person/saker/$saktype").body<ObjectNode>()["id"].longValue()
        }
    }

    override fun lagreGyldighetsVurdering(behandlingsId: UUID, gyldighet: GyldighetsResultat) {
        return runBlocking {
            behandling_app.post("$url/behandlinger/$behandlingsId/gyldigfremsatt") {
                contentType(ContentType.Application.Json)
                setBody(gyldighet)
            }.body<String>()
        }
    }
}

data class NyBehandlingRequest(
    val sak: Long,
    val persongalleri: Persongalleri,
    val mottattDato: String
)