package no.nav.etterlatte

import no.nav.etterlatte.libs.common.FoedselsNummerMedGraderingDTO
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.BrevEventKeys
import no.nav.etterlatte.libs.common.rapidsandrivers.SAK_TYPE_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.migrering.FNR_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering
import rapidsandrivers.sakId

internal class OpprettBrevRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
) : ListenerMedLoggingOgFeilhaandtering(BrevEventKeys.OPPRETT_BREV) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, BrevEventKeys.OPPRETT_BREV) {
            validate { it.requireKey(FNR_KEY) }
            validate { it.requireKey(SAK_TYPE_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val sakType = enumValueOf<SakType>(packet[SAK_TYPE_KEY].textValue())
        val fnr = packet[rapidsandrivers.FNR_KEY].textValue()
        logger.info("Finner eller oppretter sak av type $sakType")
        val sak = behandlingService.finnEllerOpprettSak(sakType, FoedselsNummerMedGraderingDTO(foedselsnummer = fnr))
        packet.sakId = sak.id
        packet.eventName = BrevEventKeys.OPPRETT_JOURNALFOER_OG_DISTRIBUER
        context.publish(packet.toJson())
    }
}
