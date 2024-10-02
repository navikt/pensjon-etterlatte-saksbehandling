package no.nav.etterlatte.rivers

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.BrevapiKlient
import no.nav.etterlatte.brev.model.BrevOgVedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.util.UUID

internal class VedtaksbrevUnderkjentRiver(
    rapidsConnection: RapidsConnection,
    private val brevapiKlient: BrevapiKlient,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(VedtaksbrevUnderkjentRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, VedtakKafkaHendelseHendelseType.UNDERKJENT) {
            validate { it.requireKey("vedtak") }
            validate { it.requireKey("vedtak.id") }
            validate { it.requireKey("vedtak.behandlingId") }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val vedtakId = packet["vedtak.id"].asLong()

        try {
            val behandlingId = UUID.fromString(packet["vedtak.behandlingId"].asText())

            logger.info("Vedtak (id=$vedtakId) er underkjent - åpner vedtaksbrev for nye endringer")

            val vedtaksbrev = runBlocking { brevapiKlient.hentVedtaksbrev(behandlingId) }
            if (vedtaksbrev == null) {
                logger.warn("Fant ingen vedtaksbrev for behandling (id=$behandlingId) - avbryter ")
                return
            }

            runBlocking {
                brevapiKlient.fjernFerdigstiltStatusUnderkjentVedtak(
                    BrevOgVedtakDto(vedtaksbrev, packet["vedtak"]),
                    behandlingId,
                )
            }
        } catch (e: Exception) {
            logger.error("Feil ved gjenåpning av vedtaksbrev for underkjent vedtak (id=$vedtakId): ", e)
            throw e
        }
    }
}
