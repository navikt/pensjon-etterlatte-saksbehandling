package no.nav.etterlatte

import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
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
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering
import rapidsandrivers.sakId
import java.util.UUID

internal class OpprettBrevRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
    private val featureToggleService: FeatureToggleService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, BrevEventKeys.OPPRETT_BREV) {
            validate { it.interestedIn(FNR_KEY) }
            validate { it.interestedIn(BEHANDLING_ID_KEY) }
            validate { it.requireKey(SAK_TYPE_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val sakType = enumValueOf<SakType>(packet[SAK_TYPE_KEY].textValue())
        val fnr = packet[rapidsandrivers.FNR_KEY].textValue()
        val behandlingId = packet[BEHANDLING_ID_KEY].textValue()
        if (!featureToggleService.isEnabled(InformasjonsbrevFeatureToggle.SendInformasjonsbrev, false)) {
            logger.info("Utsending av informasjonsbrev er skrudd av. Avbryter.")
            return
        }
        val sak =
            if (!fnr.isNullOrEmpty()) {
                logger.info("Finner eller oppretter sak av type $sakType")
                behandlingService.finnEllerOpprettSak(sakType, FoedselsNummerMedGraderingDTO(foedselsnummer = fnr)).id
            } else if (!behandlingId.isNullOrEmpty()) {
                behandlingService.hentBehandling(UUID.fromString(behandlingId)).sak
            } else {
                throw IllegalArgumentException("Verken fnr eller behandlingId er definert")
            }
        packet.sakId = sak
        packet.eventName = BrevEventKeys.OPPRETT_JOURNALFOER_OG_DISTRIBUER
        context.publish(packet.toJson())
    }
}

enum class InformasjonsbrevFeatureToggle(private val key: String) : FeatureToggle {
    SendInformasjonsbrev("send-informasjonsbrev"),
    ;

    override fun key() = key
}
