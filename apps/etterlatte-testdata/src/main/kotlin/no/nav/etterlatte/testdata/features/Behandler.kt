package no.nav.etterlatte.no.nav.etterlatte.testdata.features

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.no.nav.etterlatte.testdata.automatisk.VilkaarsvurderingService
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
import org.slf4j.LoggerFactory

class Behandler(
    rapidsConnection: RapidsConnection,
    private val vilkaarsvurderingService: VilkaarsvurderingService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

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
        val sak = packet.sakId
        val behandling = packet.behandlingId

        runBlocking {
            vilkaarsvurderingService.vilkaarsvurder(behandling)

            // vilkårsvurder
            // trygdetid
            // beregn
            // avkorta
            // vedtaksbrev oppretta
            // vedtak fatta
            // vedtak attester
            // vedtaksbrev distribuert
            // sendt til oppdrag
            // iverksatt
        }
    }

    override fun kontekst() = Kontekst.TEST
}
