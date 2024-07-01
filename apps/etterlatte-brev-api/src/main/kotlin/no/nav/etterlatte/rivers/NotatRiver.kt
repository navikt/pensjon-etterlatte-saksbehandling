package no.nav.etterlatte.rivers

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.NyNotatService
import no.nav.etterlatte.brev.notat.NotatMal
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class NotatRiver(
    rapidsConnection: RapidsConnection,
    private val notatService: NyNotatService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(JournalfoerVedtaksbrevRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, VedtakKafkaHendelseHendelseType.SAMORDNING_MANUELT_BEHANDLET) {
            validate { it.requireKey("sakId") }
            validate { it.requireKey("samordningsmeldingId") }
            validate { it.requireKey("kommentar") }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        try {
            val sakId = packet["sakId"].asLong()
            val samordningsmeldingId = packet["samordningsmeldingId"].asLong()
            val kommentar = packet["kommentar"].asText()

            runBlocking {
                val notat =
                    notatService.opprett(
                        sakId = sakId,
                        mal = NotatMal.TOM_MAL,
                        tittel = "Manuelt samordnet $samordningsmeldingId",
                        paragraf = kommentar,
                        bruker = Systembruker.brev,
                    )

                notatService.journalfoer(
                    id = notat.id,
                    bruker = Systembruker.brev,
                )
            }
        } catch (e: Exception) {
            logger.error("Feil ved opprettelse av notat", e)
            throw e
        }
    }
}
