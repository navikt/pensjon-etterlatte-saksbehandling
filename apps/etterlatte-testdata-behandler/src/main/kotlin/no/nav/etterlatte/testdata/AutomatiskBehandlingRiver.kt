package no.nav.etterlatte.testdata

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.Behandlingssteg
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.behandlingId
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection

class AutomatiskBehandlingRiver(
    rapidsConnection: RapidsConnection,
    private val behandler: Behandler,
) : ListenerMedLoggingOgFeilhaandtering() {
    init {
        initialiserRiver(rapidsConnection, EventNames.NY_OPPLYSNING) {
            validate { it.requireKey(Behandlingssteg.KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BEHANDLING_ID_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingssteg = Behandlingssteg.valueOf(packet[Behandlingssteg.KEY].asText())
        runBlocking {
            behandler.behandle(
                packet.sakId,
                packet.behandlingId,
                behandlingssteg,
                packet,
                context,
                Systembruker.testdata,
            )
        }
    }

    override fun kontekst() = Kontekst.TEST
}
