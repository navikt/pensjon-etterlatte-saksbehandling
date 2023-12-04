package no.nav.etterlatte.behandling.klienter

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.retry
import java.util.UUID

class MigreringKlient(private val httpClient: HttpClient, private val apiUrl: String) {
    suspend fun opprettManuellMigrering(
        behandlingId: UUID,
        pesysId: Long,
    ) {
        retry {
            httpClient.post("$apiUrl/migrering/$behandlingId") {
                contentType(ContentType.Application.Json)
                setBody(pesysId)
            }
        }.let { result ->
            when (result) {
                is RetryResult.Success -> result.content
                is RetryResult.Failure -> {
                    throw MigreringKlientException(
                        "Legge til manuell migrering i migreringsapp for behandlind med behandlindId=$behandlingId feilet",
                        result.samlaExceptions(),
                    )
                }
            }
        }
    }
}

class MigreringKlientException(override val detail: String, override val cause: Throwable?) :
    InternfeilException(detail, cause)
