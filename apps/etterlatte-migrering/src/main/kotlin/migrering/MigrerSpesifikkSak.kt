package no.nav.etterlatte.migrering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.migrering.pen.PenKlient
import no.nav.etterlatte.migrering.pen.tilVaarModell
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.MIGRER_SPESIFIKK_SAK
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering
import rapidsandrivers.sakId

internal class MigrerSpesifikkSak(
    rapidsConnection: RapidsConnection,
    private val penKlient: PenKlient,
    private val pesysRepository: PesysRepository,
    private val sakmigrerer: Sakmigrerer
) : ListenerMedLoggingOgFeilhaandtering(MIGRER_SPESIFIKK_SAK) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(hendelsestype)
            validate { it.requireKey(SAK_ID_KEY) }
            correlationId()
        }.register(this)
    }

    // testendring for å trigge bygg

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) {
        val sakId = packet.sakId
        if (pesysRepository.hentStatus(sakId) != null) {
            logger.info("Har allerede migrert sak $sakId. Avbryter.")
            return
        }

        logger.info("Prøver å hente sak $sakId fra PEN")
        val sakFraPEN = runBlocking { penKlient.hentSak(sakId) }
        logger.info("Henta sak $sakId fra PEN")

        val pesyssak = sakFraPEN.tilVaarModell()
        pesysRepository.lagrePesyssak(pesyssak = pesyssak)
        sakmigrerer.migrerSak(packet, pesyssak, context)
    }
}