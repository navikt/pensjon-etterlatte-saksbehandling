package no.nav.etterlatte.regulering

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.omregning.omregningData
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.RapidEvents.KJOERING
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.etterlatte.rapidsandrivers.kjoering
import org.slf4j.LoggerFactory

internal class YtelseIkkeLoependeRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, ReguleringHendelseType.YTELSE_IKKE_LOEPENDE) {
            validate { it.requireKey(KJOERING) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
        }
    }

    override fun kontekst() = Kontekst.REGULERING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val kjoering = packet.kjoering
        val hendelsesdata = packet.omregningData
        val sakId = hendelsesdata.sakId
        logger.info("Sak $sakId har ikke løpende ytelse, regulerer derfor ikke")
        behandlingService.lagreKjoering(sakId, KjoeringStatus.IKKE_LOEPENDE, kjoering)
        logger.info("Lagra kjøringsstatus som ikke løpende for sak $sakId")
    }
}
