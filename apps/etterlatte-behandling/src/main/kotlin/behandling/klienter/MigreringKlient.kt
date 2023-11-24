package no.nav.etterlatte.behandling.klienter

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import java.util.UUID

class MigreringKlient(private val httpClient: HttpClient, private val apiUrl: String) {
    suspend fun opprettManuellMigrering(
        behandlingId: UUID,
        pesysId: Long,
    ) {
        try {
            httpClient.post("$apiUrl/migrering/$behandlingId") {
                contentType(ContentType.Application.Json)
                setBody(pesysId)
            }
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
