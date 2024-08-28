package no.nav.etterlatte

import no.nav.etterlatte.brev.BREVMAL_RIVER_KEY
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.ANTALL
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.EKSKLUDERTE_SAKER
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.KJOERING
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.SPESIFIKKE_SAKER
import no.nav.etterlatte.rapidsandrivers.ekskluderteSaker
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.etterlatte.rapidsandrivers.saker
import no.nav.etterlatte.regulering.kjoerIBatch
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.InntektsjusteringHendelseType

internal class InntektsjusteringInfobrevRiver(
    rapidsConnection: RapidsConnection,
    val behandlingService: BehandlingService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, InntektsjusteringHendelseType.SEND_INFOBREV) {
            validate { it.requireKey(KJOERING) }
            validate { it.requireKey(ANTALL) }
            validate { it.interestedIn(SPESIFIKKE_SAKER) }
            validate { it.interestedIn(EKSKLUDERTE_SAKER) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val kjoering = packet[KJOERING].asText()
        val antall = packet[ANTALL].asInt()
        val sakType = SakType.OMSTILLINGSSTOENAD

        logger.info("Starter å sende infobrev for inntektsjustering: $kjoering")
        kjoerIBatch(
            logger = logger,
            antall = antall,
            finnSaker = { antallIDenneRunden ->
                behandlingService.hentAlleSaker(
                    kjoering,
                    antallIDenneRunden,
                    packet.saker,
                    packet.ekskluderteSaker,
                    sakType,
                )
            },
            haandterSaker = { sakerSomSkalInformeres ->
                sakerSomSkalInformeres.saker.forEach {
                    journalfoerOgDistribuer(it, kjoering, packet, context)
                }
            },
        )

        logger.info("Avslutter: $kjoering")
    }

    private fun journalfoerOgDistribuer(
        sak: Sak,
        kjoering: String,
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("$kjoering: Journaliserer og sender infobrev for sak ${sak.id}")
//        behandlingService.lagreKjoering(sak.id, KjoeringStatus.JOURNALFORT, kjoering)
//        logger.debug("Lagrer kjøring starta for sak ${sak.id}")
//        behandlingService.lagreKjoering(sak.id, KjoeringStatus.STARTA, kjoering)
//        logger.debug("Ferdig lagra kjøring starta for sak ${sak.id}")

        packet.setEventNameForHendelseType(InntektsjusteringHendelseType.SEND_INFOBREV)
        packet.sakId = sak.id
        packet[BREVMAL_RIVER_KEY] = Brevkoder.OMS_INNTEKTSJUSTERING
        context.publish(packet.toJson())
    }
}
