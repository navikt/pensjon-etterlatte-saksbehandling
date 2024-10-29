package no.nav.etterlatte.rivers

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.BREVMAL_RIVER_KEY
import no.nav.etterlatte.brev.BrevRequestHendelseType
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.klienter.BrevapiKlient
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class OpprettDistribuerVarselOgVedtakRiver(
    private val brevapiKlient: BrevapiKlient,
    rapidsConnection: RapidsConnection,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, BrevRequestHendelseType.OPPRETT_DISTRIBUER_VARSEL_OG_VEDTAK) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BREVMAL_RIVER_KEY, Brevkoder.OMS_INNTEKTSJUSTERING_VARSEL.toString()) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        runBlocking {
            // TODO: opprett og distribuer brev
        }
    }

    override fun kontekst() = Kontekst.BREV
}
