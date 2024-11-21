package no.nav.etterlatte.inntektsjustering

import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.inntektsjustering.AarligInntektsjusteringRequest
import no.nav.etterlatte.rapidsandrivers.InntektsjusteringHendelseType
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.RapidEvents.ANTALL
import no.nav.etterlatte.rapidsandrivers.RapidEvents.EKSKLUDERTE_SAKER
import no.nav.etterlatte.rapidsandrivers.RapidEvents.KJOERING
import no.nav.etterlatte.rapidsandrivers.RapidEvents.SPESIFIKKE_SAKER
import no.nav.etterlatte.rapidsandrivers.antall
import no.nav.etterlatte.rapidsandrivers.ekskluderteSaker
import no.nav.etterlatte.rapidsandrivers.kjoering
import no.nav.etterlatte.rapidsandrivers.saker
import no.nav.etterlatte.regulering.kjoerIBatch
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class InntektsjusteringJobbRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
    private val featureToggleService: FeatureToggleService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, InntektsjusteringHendelseType.START_INNTEKTSJUSTERING_JOBB) {
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
        val kjoering = packet.kjoering
        logger.info("$kjoering: Starter ")

        if (!featureToggleService.isEnabled(InntektsjusterinFeatureToggle.START_INNTEKTSJUSTERING_JOBB, false)) {
            logger.info("Inntektsjustering jobb er deaktivert. Avbryter forespørsel.")
            return
        }

        val antall = packet.antall
        val sakType = SakType.OMSTILLINGSSTOENAD
        val loependeFom = AarligInntektsjusteringRequest.utledLoependeFom()

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
                if (sakerSomSkalInformeres.sakIdListe.isEmpty()) {
                    logger.warn("Ingen saker funnet til årlig inntektsjustering $kjoering")
                } else {
                    logger.info("Starter årlig inntektsjustering $kjoering")
                    val request =
                        AarligInntektsjusteringRequest(
                            kjoering = kjoering,
                            saker = sakerSomSkalInformeres.sakIdListe,
                        )
                    behandlingService.startAarligInntektsjustering(request)
                }
            },
        )
    }
}

enum class InntektsjusterinFeatureToggle(
    private val key: String,
) : FeatureToggle {
    START_INNTEKTSJUSTERING_JOBB("start-inntektsjustering-jobb"),
    ;

    override fun key() = key
}
