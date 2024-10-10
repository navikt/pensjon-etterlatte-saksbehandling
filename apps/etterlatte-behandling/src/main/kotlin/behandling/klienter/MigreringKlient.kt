package no.nav.etterlatte.behandling.klienter

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.Pingable
import no.nav.etterlatte.libs.ktor.ping
import org.slf4j.LoggerFactory
import java.util.UUID

class MigreringKlient(
    private val client: HttpClient,
    private val url: String,
) : Pingable {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun opprettManuellMigrering(
        behandlingId: UUID,
        pesysId: Long,
        sakId: SakId,
    ) {
        retry {
            client.post("$url/migrering/${sakId.sakId}/$behandlingId") {
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

    override val serviceName: String
        get() = this.javaClass.simpleName
    override val beskrivelse: String
        get() = "Oppretter manuell migrering for GJENNY"
    override val endpoint: String
        get() = this.url

    override suspend fun ping(konsument: String?): PingResult =
        client.ping(
            pingUrl = url.plus("/health/isready"),
            logger = logger,
            serviceName = serviceName,
            beskrivelse = beskrivelse,
        )
}

class MigreringKlientException(
    override val detail: String,
    override val cause: Throwable?,
) : InternfeilException(detail, cause)
