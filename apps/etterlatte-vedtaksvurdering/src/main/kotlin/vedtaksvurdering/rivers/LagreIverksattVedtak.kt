package no.nav.etterlatte.vedtaksvurdering.rivers

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingResponseDto
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingStatusDto
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.*

internal class LagreIverksattVedtak(
    rapidsConnection: RapidsConnection,
    private val vedtaksvurderingService: VedtaksvurderingService,
    private val behandlingHttpClient: HttpClient
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            eventName("UTBETALING:OPPDATERT")
            validate { it.requireKey("utbetaling_response") }
            correlationId()
        }.register(this)
    }

    fun postTilBehandling(behandlingId: UUID, vedtakId: Long) = runBlocking {
        behandlingHttpClient.post(
            "http://etterlatte-behandling/behandlinger/$behandlingId/iverksett"
        ) {
            contentType(ContentType.Application.Json)
            setBody(
                VedtakHendelse(
                    vedtakId = vedtakId,
                    inntruffet = Tidspunkt.now()
                )
            )
        }
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            val respons = objectMapper.readValue<UtbetalingResponseDto>(packet["utbetaling_response"].toString())

            try {
                when (respons.status) {
                    UtbetalingStatusDto.GODKJENT, UtbetalingStatusDto.GODKJENT_MED_FEIL -> {
                        respons.behandlingId?.also { behandlingId ->
                            vedtaksvurderingService.iverksattVedtak(behandlingId)
                            requireNotNull(vedtaksvurderingService.hentVedtak(behandlingId)).also { vedtak ->
                                postTilBehandling(behandlingId, vedtak.id)
                            }
                        }
                            ?: logger.error(
                                "Utbetaling mangler behandlingId. " +
                                    "Kan derfor ikke lagre at vedtaket er iverksatt. Utbetaling: $respons"
                            )
                    }
                    // Her kan vi haandtere utbetalingsproblemer om vi oensker
                    else -> {}
                }
            } catch (e: Exception) {
                logger.error("Kunne ikke lagre iverksatt vedtak: $respons", e)
            }
        }
    }
}