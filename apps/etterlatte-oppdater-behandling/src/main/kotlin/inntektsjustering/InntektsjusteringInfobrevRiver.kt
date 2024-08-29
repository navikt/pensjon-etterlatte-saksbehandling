package no.nav.etterlatte.inntektsjustering

import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.brev.BREVMAL_RIVER_KEY
import no.nav.etterlatte.brev.BrevRequestHendelseType
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.etterlatte.regulering.kjoerIBatch
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.InntektsjusteringHendelseType
import rapidsandrivers.RapidEvents.ANTALL
import rapidsandrivers.RapidEvents.EKSKLUDERTE_SAKER
import rapidsandrivers.RapidEvents.KJOERING
import rapidsandrivers.RapidEvents.SPESIFIKKE_SAKER
import rapidsandrivers.antall
import rapidsandrivers.ekskluderteSaker
import rapidsandrivers.kjoering
import rapidsandrivers.saker

internal class InntektsjusteringInfobrevRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
    private val featureToggleService: FeatureToggleService,
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
        if (!featureToggleService.isEnabled(InntektsjusterinFeatureToggle.START_INFOBREV_INNTEKTSJUSTERING, false)) {
            logger.info("Utsending av informasjonsbrev er deaktivert ved funksjonsbryter. Avbryter forespørsel.")
            return
        }

        val kjoering = packet.kjoering
        val antall = packet.antall

        val sakType = SakType.OMSTILLINGSSTOENAD
        // TODO: må vi filtrere på mer? ta bort saker som opphøres innværende år?

        logger.info("$kjoering: Starter ")
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

        logger.info("$kjoering: Ferdig")
    }

    private fun journalfoerOgDistribuer(
        sak: Sak,
        kjoering: String,
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("$kjoering: Journalfører og distribuerer infobrev for sak ${sak.id}")
        behandlingService.lagreKjoering(sak.id, KjoeringStatus.STARTA, kjoering)

        packet.setEventNameForHendelseType(BrevRequestHendelseType.OPPRETT_JOURNALFOER_OG_DISTRIBUER)
        packet.sakId = sak.id
        packet[BREVMAL_RIVER_KEY] = Brevkoder.OMS_INNTEKTSJUSTERING
        context.publish(packet.toJson())
    }
}

enum class InntektsjusterinFeatureToggle(
    private val key: String,
) : FeatureToggle {
    START_INFOBREV_INNTEKTSJUSTERING("start-infobrev-inntektsjustering"),
    ;

    override fun key() = key
}
