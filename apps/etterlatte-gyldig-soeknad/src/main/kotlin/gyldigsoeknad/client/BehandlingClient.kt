package no.nav.etterlatte.gyldigsoeknad.client

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
import no.nav.etterlatte.libs.common.sak.Sak
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

    fun hentSak(fnr: String, saktype: String): Sak {
        return runBlocking {
            sakOgBehandlingApp.post("$url/personer/getsak/$saktype") {
                contentType(ContentType.Application.Json)
                setBody(FoedselsnummerDTO(fnr))
            }.body()
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