package no.nav.etterlatte.rivers.migrering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.MigreringBrevRequest
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.hentinformasjon.VedtaksvurderingService
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseType
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rivers.BrevEventTypes
import no.nav.etterlatte.token.Systembruker
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering

internal class FiksEnkeltbrevRiver(
    rapidsConnection: RapidsConnection,
    private val service: VedtaksbrevService,
    private val vedtaksvurderingService: VedtaksvurderingService,
) : ListenerMedLoggingOgFeilhaandtering(BrevEventTypes.FIKS_ENKELTBREV.toString()) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Migreringshendelser.FIKS_ENKELTBREV) {
            validate { it.requireKey(BREV_ID_KEY) }
            validate { it.requireKey(SUM) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val brevId = packet[BREV_ID_KEY].asLong()
        val brev = service.hentBrev(id = brevId)

        logger.info("Fikser vedtaksbrev for brev $brevId")
        val behandlingId = brev.behandlingId!!
        val brukerTokenInfo = Systembruker("migrering", "migrering")
        val sum = packet[SUM].asInt()
        val migreringBrevRequest = MigreringBrevRequest(brutto = sum, utenlandstilknytningType = null)
        runBlocking {
            service.genererPdf(brevId, brukerTokenInfo, migreringBrevRequest)
            service.ferdigstillVedtaksbrev(behandlingId, brukerTokenInfo, true)
            logger.info("Har oppretta vedtaksbrev for brev $brevId")

            packet.eventName = VedtakKafkaHendelseType.ATTESTERT.toString()
            val vedtak = vedtaksvurderingService.hentVedtak(behandlingId, brukerTokenInfo)
            packet["vedtak"] = vedtak
        }
        context.publish(packet.toJson())
    }
}

const val BREV_ID_KEY = "brev_id"
const val SUM = "sum"
