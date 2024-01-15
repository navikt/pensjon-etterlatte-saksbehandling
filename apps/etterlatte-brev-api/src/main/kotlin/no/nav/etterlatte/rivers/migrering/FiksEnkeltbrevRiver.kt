package no.nav.etterlatte.rivers.migrering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.MigreringBrevRequest
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.hentinformasjon.VedtaksvurderingService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseType
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rivers.BrevEventTypes
import no.nav.etterlatte.token.Systembruker
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering

internal class FiksEnkeltbrevRiver(
    rapidsConnection: RapidsConnection,
    private val service: VedtaksbrevService,
    private val vedtaksvurderingService: VedtaksvurderingService,
) : ListenerMedLoggingOgFeilhaandtering(BrevEventTypes.FIKS_ENKELTBREV.toString()) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Migreringshendelser.FIKS_ENKELTBREV) {
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(SUM) }
            validate { it.requireKey(UTLANDSTILKNYTNINGTYPE) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet.behandlingId
        logger.info("Fikser vedtaksbrev for behandling $behandlingId")

        val brukerTokenInfo = Systembruker("migrering", "migrering")
        val sum = packet[SUM].asInt()
        val utlandstilknytningType = packet[UTLANDSTILKNYTNINGTYPE].asText().let { UtlandstilknytningType.valueOf(it) }
        val migreringBrevRequest =
            MigreringBrevRequest(brutto = sum, yrkesskade = false, utlandstilknytningType = utlandstilknytningType)
        runBlocking {
            val sakId = vedtaksvurderingService.hentVedtak(behandlingId, brukerTokenInfo).sak.id
            val vedtaksbrev: Brev =
                service.opprettVedtaksbrev(
                    sakId,
                    behandlingId,
                    brukerTokenInfo,
                    migreringBrevRequest,
                )
            service.genererPdf(vedtaksbrev.id, brukerTokenInfo, migreringBrevRequest)
            service.ferdigstillVedtaksbrev(behandlingId, brukerTokenInfo, true)
            logger.info("Har oppretta vedtaksbrev for behandling $behandlingId")

            packet.eventName = VedtakKafkaHendelseType.ATTESTERT.toString()
            val vedtak = vedtaksvurderingService.hentVedtak(behandlingId, brukerTokenInfo)
            packet["vedtak"] = vedtak
        }
        context.publish(packet.toJson())
    }
}

const val SUM = "sum"
const val UTLANDSTILKNYTNINGTYPE = "UTLANDSTILKNYTNINGTYPE"

val behandlingerAaJournalfoereBrevFor = listOf<Triple<String, Int, UtlandstilknytningType>>()
