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
import no.nav.etterlatte.rapidsandrivers.InntektsjusteringHendelseType
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.RapidEvents.ANTALL
import no.nav.etterlatte.rapidsandrivers.RapidEvents.EKSKLUDERTE_SAKER
import no.nav.etterlatte.rapidsandrivers.RapidEvents.KJOERING
import no.nav.etterlatte.rapidsandrivers.RapidEvents.LOEPENDE_FOM
import no.nav.etterlatte.rapidsandrivers.RapidEvents.SPESIFIKKE_SAKER
import no.nav.etterlatte.rapidsandrivers.antall
import no.nav.etterlatte.rapidsandrivers.ekskluderteSaker
import no.nav.etterlatte.rapidsandrivers.kjoering
import no.nav.etterlatte.rapidsandrivers.loependeFom
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.etterlatte.rapidsandrivers.saker
import no.nav.etterlatte.regulering.kjoerIBatch
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class InntektsjusteringVarselOmVedtakRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
    private val featureToggleService: FeatureToggleService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, InntektsjusteringHendelseType.SEND_INFOBREV) {
            validate { it.requireKey(KJOERING) }
            validate { it.requireKey(ANTALL) }
            validate { it.requireKey(LOEPENDE_FOM) }
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
        logger.info("$kjoering: Starter ")

        val antall = packet.antall
        val sakType = SakType.OMSTILLINGSSTOENAD
        val loependeFom = packet.loependeFom

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
                    loependeFom,
                )
            },
            haandterSaker = { sakerSomSkalInformeres ->
                sakerSomSkalInformeres.saker.forEach {
                    // TODO: sjekk på inntekt

                    opprettBrev(it, kjoering, packet, context)
                }
            },
        )

        logger.info("$kjoering: Ferdig")
    }

    private fun opprettBrev(
        sak: Sak,
        kjoering: String,
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("$kjoering: Klar til å opprette, journalføre og distribuere brev for sakId ${sak.id}")
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