package no.nav.etterlatte.rivers

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.SamordningManueltBehandletRequest
import no.nav.etterlatte.klienter.BrevapiKlient
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class SamordningsnotatRiver(
    rapidsConnection: RapidsConnection,
    private val brevapiKlient: BrevapiKlient,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(SamordningsnotatRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, VedtakKafkaHendelseHendelseType.SAMORDNING_MANUELT_BEHANDLET) {
            validate { it.requireKey("sakId") }
            validate { it.requireKey("vedtakId") }
            validate { it.requireKey("samordningsmeldingId") }
            validate { it.requireKey("saksbehandlerId") }
            validate { it.requireKey("kommentar") }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        try {
            val sakId = SakId(packet["sakId"].asLong())
            val vedtakId = packet["vedtakId"].asLong()
            val samordningsmeldingId = packet["samordningsmeldingId"].asLong()
            val kommentar = packet["kommentar"].asText()
            val saksbehandlerId = packet["saksbehandlerId"].asText()

            logger.info("Oppretter notat for sak $sakId, samID $samordningsmeldingId")

            runBlocking {
                brevapiKlient.opprettOgJournalfoerNotat(
                    sakId = sakId,
                    samordningManueltBehandletRequest =
                        SamordningManueltBehandletRequest(
                            tittel = "Manuell samordning - vedtak $vedtakId",
                            vedtakId = vedtakId,
                            samordningsmeldingId = samordningsmeldingId,
                            kommentar = kommentar,
                            saksbehandlerId = saksbehandlerId,
                        ),
                )
            }
        } catch (e: Exception) {
            logger.error("Feil ved opprettelse av notat", e)
            throw e
        }
    }
}
