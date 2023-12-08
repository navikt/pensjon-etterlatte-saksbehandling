package no.nav.etterlatte.rivers.migrering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.MigreringBrevRequest
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseType
import no.nav.etterlatte.rapidsandrivers.OmregningEvents
import no.nav.etterlatte.rivers.BrevEventTypes
import no.nav.etterlatte.token.Systembruker
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering
import java.util.UUID

// TODO EY-3232 - Fjerne
internal class OpprettVedtaksbrevForOmregningNyRegelRiver(
    rapidsConnection: RapidsConnection,
    private val service: VedtaksbrevService,
) : ListenerMedLoggingOgFeilhaandtering(BrevEventTypes.OPPRETTET.toString()) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, VedtakKafkaHendelseType.FATTET.toString()) {
            validate { it.requireKey("vedtak.behandlingId") }
            validate { it.requireKey("vedtak.sak.id") }
            validate { it.requireKey(OmregningEvents.OMREGNING_NYE_REGLER) }
            validate { it.requireKey(OmregningEvents.OMREGNING_BRUTTO) }
            validate { it.rejectValue(PDF_GENERERT, true) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val sakId = packet["vedtak.sak.id"].asLong()
        logger.info("Oppretter vedtaksbrev i sak $sakId")
        val behandlingId = UUID.fromString(packet["vedtak.behandlingId"].asText())
        val brukerTokenInfo = Systembruker("migrering", "migrering") // TODO ?
        runBlocking {
            val migreringBrevRequest =
                MigreringBrevRequest( // TODO Må sjekke at det ikke er saker med yrkesskade
                    brutto = packet[OmregningEvents.OMREGNING_BRUTTO].asInt(),
                    utlandstilknytningType = UtlandstilknytningType.NASJONAL,
                    yrkesskade = false,
                    erOmregningGjenny = true,
                )
            val vedtaksbrev: Brev =
                service.opprettVedtaksbrev(
                    sakId,
                    behandlingId,
                    brukerTokenInfo,
                    migreringBrevRequest,
                )
            service.genererPdf(vedtaksbrev.id, brukerTokenInfo, migreringBrevRequest)
            service.ferdigstillVedtaksbrev(behandlingId, brukerTokenInfo, true)
        }
        logger.info("Har oppretta vedtaksbrev i sak $sakId")
        packet[PDF_GENERERT] = true
        context.publish(packet.toJson())
    }
}

private const val PDF_GENERERT = "PDF_GENERERT"
