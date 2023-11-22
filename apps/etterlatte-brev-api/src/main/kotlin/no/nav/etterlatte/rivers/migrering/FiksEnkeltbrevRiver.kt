package no.nav.etterlatte.rivers.migrering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.MigreringBrevRequest
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.hentinformasjon.VedtaksvurderingService
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseType
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.hendelseData
import no.nav.etterlatte.rivers.BrevEventTypes
import no.nav.etterlatte.token.Systembruker
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering
import rapidsandrivers.sakId

internal class FiksEnkeltbrevRiver(
    rapidsConnection: RapidsConnection,
    private val service: VedtaksbrevService,
    private val vedtaksvurderingService: VedtaksvurderingService,
) : ListenerMedLoggingOgFeilhaandtering(BrevEventTypes.FIKS_ENKELTBREV.toString()) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Migreringshendelser.FIKS_ENKELTBREV) {
            validate { it.requireKey(SAK_ID_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val sakId = packet.sakId
        val brev = service.hentBrev(id = sakId)

        logger.info("Fikser vedtaksbrev i sak $sakId")
        val behandlingId = brev.behandlingId!!
        val brukerTokenInfo = Systembruker("migrering", "migrering")
        runBlocking {
            val hendelseData = packet.hendelseData
            service.genererPdf(brev.id, brukerTokenInfo, MigreringBrevRequest(hendelseData.beregning))
            service.ferdigstillVedtaksbrev(behandlingId, brukerTokenInfo, true)
            logger.info("Har oppretta vedtaksbrev i sak $sakId")

            packet.eventName = VedtakKafkaHendelseType.ATTESTERT.toString()
            val vedtak = vedtaksvurderingService.hentVedtak(behandlingId, brukerTokenInfo)
            packet["vedtak"] = vedtak
        }
        context.publish(packet.toJson())
    }
}
