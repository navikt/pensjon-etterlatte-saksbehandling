package no.nav.etterlatte.rivers.migrering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.MigreringBrevRequest
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.migrering.KILDE_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.hendelseData
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.util.UUID

internal class OpprettVedtaksbrevForMigreringRiver(
    rapidsConnection: RapidsConnection,
    private val service: VedtaksbrevService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, VedtakKafkaHendelseHendelseType.FATTET) {
            validate { it.requireKey("vedtak.behandlingId") }
            validate { it.requireKey("vedtak.sak.id") }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.requireValue(KILDE_KEY, Vedtaksloesning.PESYS.name) }
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
        val brukerTokenInfo = Systembruker.migrering
        runBlocking {
            val hendelseData = packet.hendelseData
            val migreringBrevRequest =
                with(hendelseData) {
                    MigreringBrevRequest(
                        brutto = beregning.brutto,
                        utlandstilknytningType = utlandstilknytningType,
                        yrkesskade = dodAvYrkesskade,
                    )
                }
            if (migreringBrevRequest.utlandstilknytningType == null) {
                logger.warn(
                    "Fikk null i utenlandstilknytningstype for migreringrequest med " +
                        "pesysId=${hendelseData.pesysId}.",
                )
            } else {
                logger.info(
                    "Fikk utlandstilknytningstype=${migreringBrevRequest.utlandstilknytningType} for " +
                        "migreringrequesten med pesysId=${hendelseData.pesysId}",
                )
            }
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
