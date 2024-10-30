package no.nav.etterlatte.no.nav.etterlatte.klienter

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.utbetaling.common.SimulertBeregning
import org.slf4j.LoggerFactory
import java.util.UUID

interface UtbetalingKlient {
    fun simuler(behandlingId: UUID): SimulertBeregning
}

class UtbetalingKlientImpl(
    private val httpClient: HttpClient,
    private val url: String,
) : UtbetalingKlient {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun simuler(behandlingId: UUID): SimulertBeregning {
        logger.info("Simulerer utbetaling for behandlingId=$behandlingId")
        return runBlocking {
            httpClient.post("$url/api/utbetaling/behandling/$behandlingId/simulering").body<SimulertBeregning>()
        }
    }
}
