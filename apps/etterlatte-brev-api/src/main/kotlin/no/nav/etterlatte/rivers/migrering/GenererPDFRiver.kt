package no.nav.etterlatte.rivers.migrering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.MigreringBrevRequest
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.migrering.KILDE_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.hendelseData
import no.nav.etterlatte.rivers.BrevEventTypes
import no.nav.etterlatte.token.Systembruker
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering

internal class GenererPDFRiver(
    rapidsConnection: RapidsConnection,
    private val service: VedtaksbrevService,
) : ListenerMedLoggingOgFeilhaandtering(BrevEventTypes.OPPRETTET.toString()) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, BrevEventTypes.OPPRETTET.toString()) {
            validate { it.requireKey("vedtak.sak.id") }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.requireValue(KILDE_KEY, Vedtaksloesning.PESYS.name) }
            validate { it.requireKey(VEDTAKSBREV) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val sakId = packet["vedtak.sak.id"].asLong()
        logger.info("Oppretter vedtaksbrev i sak $sakId")
        val brukerTokenInfo = Systembruker("migrering", "migrering")
        runBlocking {
            val hendelseData = packet.hendelseData
            val brevId = packet[VEDTAKSBREV].asLong()
            service.genererPdf(brevId, brukerTokenInfo, MigreringBrevRequest(hendelseData.beregning))
        }
        logger.info("Har oppretta vedtaksbrev i sak $sakId")
        packet.eventName = BrevEventTypes.PDF_GENERERT.toString()
        context.publish(packet.toJson())
    }
}
