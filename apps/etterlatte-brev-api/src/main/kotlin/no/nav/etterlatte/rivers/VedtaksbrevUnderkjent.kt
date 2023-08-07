package no.nav.etterlatte.rivers

import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import rapidsandrivers.migrering.RiverMedLogging
import java.util.*

internal class VedtaksbrevUnderkjent(
    rapidsConnection: RapidsConnection,
    private val service: VedtaksbrevService
) : RiverMedLogging(rapidsConnection) {

    init {
        initialiser {
            eventName(KafkaHendelseType.UNDERKJENT.toString())
            validate { it.requireKey("vedtak") }
            validate { it.requireKey("vedtak.vedtakId") }
            validate { it.requireKey("vedtak.behandling.id") }
            validate {
                it.rejectValues("vedtak.behandling.type", listOf(BehandlingType.MANUELT_OPPHOER.name))
            }
        }
    }

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) {
        try {
            val vedtakId = packet["vedtak.vedtakId"].asLong()
            val behandlingId = UUID.fromString(packet["vedtak.behandling.id"].asText())

            logger.info("Vedtak (id=$vedtakId) er underkjent - starter sletting av vedtaksbrev")

            val vedtaksbrev = service.hentVedtaksbrev(behandlingId)
            if (vedtaksbrev == null) {
                logger.warn("Fant ingen vedtaksbrev for behandling (id=$behandlingId) - avbryter sletting")
                return
            }

            val slettetOK = service.slettVedtaksbrev(vedtaksbrev.id)

            if (slettetOK) {
                logger.info("Vedtaksbrev (id=${vedtaksbrev.id}) for vedtak (id=$vedtakId) er slettet")
            } else {
                throw Exception("Kunne ikke slette vedtaksbrev (id=${vedtaksbrev.id})")
            }
        } catch (e: Exception) {
            logger.error("Feil ved sletting av vedtaksbrev for underkjent vedtak: ", e)
            throw e
        }
    }
}