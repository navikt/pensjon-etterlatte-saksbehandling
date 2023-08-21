package migrering

import kotlinx.coroutines.runBlocking
import migrering.pen.PenKlient
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.migrering.PesysRepository
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.MIGRER_SPESIFIKK_SAK
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering
import rapidsandrivers.sakId

internal class MigrerSpesifikkSak(
    rapidsConnection: RapidsConnection,
    private val penKlient: PenKlient,
    private val pesysRepository: PesysRepository,
    private val sakmigrerer: Sakmigrerer
) : ListenerMedLoggingOgFeilhaandtering(MIGRER_SPESIFIKK_SAK) {
    init {
        River(rapidsConnection).apply {
            eventName(hendelsestype)
            validate { it.requireKey(SAK_ID_KEY) }
            correlationId()
        }.register(this)
    }

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) {
        val sakFraPEN = runBlocking { penKlient.hentSak(packet.sakId) }
        pesysRepository.lagrePesyssak(pesyssak = sakFraPEN)
        sakmigrerer.migrerSak(packet, sakFraPEN, context)
    }
}