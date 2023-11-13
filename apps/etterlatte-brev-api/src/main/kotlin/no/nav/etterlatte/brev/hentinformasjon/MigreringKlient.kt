package no.nav.etterlatte.brev.hentinformasjon

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import java.util.UUID

class MigreringKlient(private val apiUrl: String, private val httpClient: HttpClient) {
    suspend fun hentMigreringRequest(behandlingId: UUID): MigreringRequest {
        return try {
            httpClient.get("$apiUrl/behandling/$behandlingId")
                .body()
        } catch (e: Exception) {
            throw MigreringKlientException(
                "Henting av migreringrequest for behandlind med behandlindId=$behandlingId feilet",
                e,
            )
        }
    }
}

class MigreringKlientException(override val detail: String, override val cause: Throwable?) :
    InternfeilException(detail, cause)
