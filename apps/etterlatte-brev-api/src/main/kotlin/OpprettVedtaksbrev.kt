import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.BrevService
import no.nav.etterlatte.domene.vedtak.Vedtak
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class OpprettVedtaksbrev(rapidsConnection: RapidsConnection, private val brevService: BrevService) :
    River.PacketListener {
    private val logger = LoggerFactory.getLogger(OpprettVedtaksbrev::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event", "vedtak_fattet") }
            validate { it.requireKey("@vedtak") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext {
            val vedtak: Vedtak = objectMapper.readValue(packet["@vedtak"].asText())
            logger.info("Nytt vedtak med id ${vedtak.vedtakId} er fattet. Oppretter vedtaksbrev.")

            val brev = runBlocking {
                brevService.opprettFraVedtak(vedtak)
            }

            logger.info("Brev for vedtak med id ${vedtak.vedtakId} er opprettet med brevId ${brev.id}")
        }
    }
}
