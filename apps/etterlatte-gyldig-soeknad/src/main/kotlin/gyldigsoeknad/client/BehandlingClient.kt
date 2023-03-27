package no.nav.etterlatte.gyldigsoeknad.client

import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.FoedselsnummerDTO
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import java.time.LocalDateTime
import java.util.*

class BehandlingClient(
    private val sakOgBehandlingApp: HttpClient,
    private val url: String
) {
    fun initierBehandling(
        sak: Long,
        mottattDato: LocalDateTime,
        persongalleri: Persongalleri
    ): UUID {
        return runBlocking {
            sakOgBehandlingApp.post("$url/behandlinger/foerstegangsbehandling") {
                contentType(ContentType.Application.Json)
                setBody(NyBehandlingRequest(sak, persongalleri, mottattDato.toString()))
            }.body<String>()
        }.let {
            UUID.fromString(it)
        }
    }

    fun skaffSak(fnr: String, saktype: String): Long {
        return runBlocking {
            sakOgBehandlingApp.post("$url/personer/saker/$saktype") {
                contentType(ContentType.Application.Json)
                setBody(FoedselsnummerDTO(fnr))
            }.body<ObjectNode>()["id"].longValue()
        }
    }

    fun lagreGyldighetsVurdering(behandlingsId: UUID, gyldighet: GyldighetsResultat) {
        return runBlocking {
            sakOgBehandlingApp.post("$url/behandlinger/$behandlingsId/gyldigfremsatt") {
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