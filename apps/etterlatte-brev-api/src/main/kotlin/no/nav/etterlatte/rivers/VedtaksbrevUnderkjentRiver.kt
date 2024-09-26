package no.nav.etterlatte.rivers

import no.nav.etterlatte.brev.vedtaksbrev.VedtaksbrevService
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.util.UUID

internal class VedtaksbrevUnderkjentRiver(
    rapidsConnection: RapidsConnection,
    private val service: VedtaksbrevService,
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

            val vedtaksbrev = service.hentVedtaksbrev(behandlingId)
            if (vedtaksbrev == null) {
                logger.warn("Fant ingen vedtaksbrev for behandling (id=$behandlingId) - avbryter ")
                return
            }

            val endretOK = service.fjernFerdigstiltStatusUnderkjentVedtak(vedtaksbrev.id, packet["vedtak"])

            if (endretOK) {
                logger.info("Vedtaksbrev (id=${vedtaksbrev.id}) for vedtak (id=$vedtakId) åpnet for endringer")
            } else {
                throw Exception("Kunne ikke åpne vedtaksbrev (id=${vedtaksbrev.id}) for endringer")
            }
        } catch (e: Exception) {
            logger.error("Feil ved gjenåpning av vedtaksbrev for underkjent vedtak (id=$vedtakId): ", e)
            throw e
        }
    }
}
