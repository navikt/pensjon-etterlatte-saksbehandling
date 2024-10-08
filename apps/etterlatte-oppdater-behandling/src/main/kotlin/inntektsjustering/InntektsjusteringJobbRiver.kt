package no.nav.etterlatte.inntektsjustering

import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
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
import no.nav.etterlatte.rapidsandrivers.saker
import no.nav.etterlatte.regulering.kjoerIBatch
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.YearMonth

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
            validate { it.requireKey(LOEPENDE_FOM) }
            validate { it.interestedIn(SPESIFIKKE_SAKER) }
            validate { it.interestedIn(EKSKLUDERTE_SAKER) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        if (!featureToggleService.isEnabled(InntektsjusterinFeatureToggle.START_INNTEKTSJUSTERING_JOBB, false)) {
            logger.info("Inntektsjustering jobb er deaktivert. Avbryter forespørsel.")
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
                sakerSomSkalInformeres.saker.forEach { sak ->

                    logger.info("$kjoering: Klar til å opprette, journalføre og distribuere varsel og vedtak for sakId ${sak.id}")
                    behandlingService.lagreKjoering(sak.id, KjoeringStatus.STARTA, kjoering)

                    val sakMedBehandlinger = behandlingService.hentBehandlingerForSak(FoedselsnummerDTO(sak.ident))
                    if (skalBehandlingOmregnes(sakMedBehandlinger.behandlinger, loependeFom)) {
                        // TODO: START OMREGNING
                    } else {
                        behandlingService.lagreKjoering(sak.id, KjoeringStatus.FERDIGSTILT, kjoering)
                    }
                }
            },
        )

        logger.info("$kjoering: Ferdig")
    }
}

fun skalBehandlingOmregnes(
    behandlinger: List<BehandlingSammendrag>,
    loependeFom: YearMonth,
): Boolean =
    behandlinger.any {
        it.status == BehandlingStatus.IVERKSATT &&
            it.aarsak == Revurderingaarsak.INNTEKTSENDRING.name &&
            !(
                it.virkningstidspunkt?.dato?.year == loependeFom.year &&
                    it.virkningstidspunkt?.dato?.monthValue == 1
            )
    }

enum class InntektsjusterinFeatureToggle(
    private val key: String,
) : FeatureToggle {
    START_INNTEKTSJUSTERING_JOBB("start-inntektsjustering-jobb"),
    ;

    override fun key() = key
}
