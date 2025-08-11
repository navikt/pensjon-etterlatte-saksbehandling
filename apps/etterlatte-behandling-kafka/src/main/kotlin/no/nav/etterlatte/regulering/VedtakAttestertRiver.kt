package no.nav.etterlatte.regulering

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.LagreKjoeringRequest
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.omregning.OmregningDataPacket
import no.nav.etterlatte.omregning.omregningData
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.AVKORTING_ETTER
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.AVKORTING_FOER
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.BEREGNING_BELOEP_ETTER
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.BEREGNING_BELOEP_FOER
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.BEREGNING_BRUKT_OMREGNINGSFAKTOR
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.BEREGNING_G_ETTER
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.BEREGNING_G_FOER
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.VEDTAK_BELOEP
import org.slf4j.LoggerFactory
import java.math.BigDecimal

internal class VedtakAttestertRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
    private val featureToggleService: FeatureToggleService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiverUtenEventName(rapidsConnection) {
            validate {
                it.demandAny(
                    EVENT_NAME_KEY,
                    listOf(
                        VedtakKafkaHendelseHendelseType.FATTET.lagEventnameForType(),
                        VedtakKafkaHendelseHendelseType.ATTESTERT.lagEventnameForType(),
                    ),
                )
            }
            validate { it.requireKey(OmregningDataPacket.KEY) }
            validate { it.requireKey(OmregningDataPacket.SAK_ID) }
            validate { it.requireKey(OmregningDataPacket.BEHANDLING_ID) }
            validate { it.requireKey(OmregningDataPacket.KJOERING) }
            validate { it.requireKey(OmregningDataPacket.REV_AARSAK) }
            validate { it.interestedIn(BEREGNING_BELOEP_FOER) }
            validate { it.interestedIn(BEREGNING_BELOEP_ETTER) }
            validate { it.interestedIn(BEREGNING_G_FOER) }
            validate { it.interestedIn(BEREGNING_G_ETTER) }
            validate { it.interestedIn(BEREGNING_BRUKT_OMREGNINGSFAKTOR) }
            validate { it.interestedIn(AVKORTING_FOER) }
            validate { it.interestedIn(AVKORTING_ETTER) }
            validate { it.requireKey(VEDTAK_BELOEP) }
        }
    }

    override fun kontekst() = Kontekst.OMREGNING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val erFattetVedtak = packet.eventName == VedtakKafkaHendelseHendelseType.FATTET.lagEventnameForType()
        if (erFattetVedtak && !skalStoppeEtterFattet(packet.omregningData.revurderingaarsak)) {
            return
        }

        val sakId = packet.omregningData.sakId
        val kjoering = packet.omregningData.kjoering
        logger.info("Sak $sakId er ferdig omregnet, oppdaterer status")
        val request =
            LagreKjoeringRequest(
                kjoering = kjoering,
                status =
                    when (erFattetVedtak) {
                        true -> KjoeringStatus.FERDIGSTILT_FATTET
                        false -> KjoeringStatus.FERDIGSTILT
                    },
                sakId = sakId,
                behandling = packet.omregningData.hentBehandlingId(),
                beregningBeloepFoer = bigDecimal(packet, BEREGNING_BELOEP_FOER),
                beregningBeloepEtter = bigDecimal(packet, BEREGNING_BELOEP_ETTER),
                beregningGFoer = bigDecimal(packet, BEREGNING_G_FOER),
                beregningGEtter = bigDecimal(packet, BEREGNING_G_ETTER),
                beregningBruktOmregningsfaktor = bigDecimal(packet, BEREGNING_BRUKT_OMREGNINGSFAKTOR),
                avkortingFoer = bigDecimal(packet, AVKORTING_FOER),
                avkortingEtter = bigDecimal(packet, AVKORTING_ETTER),
                vedtakBeloep = bigDecimal(packet, VEDTAK_BELOEP),
            )

        behandlingService.lagreFullfoertKjoering(request)
        logger.info("Sak $sakId er ferdig omregnet, status oppdatert til: ${request.status}")
    }

    private fun bigDecimal(
        packet: JsonMessage,
        noekkel: String,
    ): BigDecimal? = packet[noekkel].asText().takeIf { it.isNotEmpty() }?.let { BigDecimal(it) }

    private fun skalStoppeEtterFattet(revurderingaarsak: Revurderingaarsak): Boolean {
        if (featureToggleService.isEnabled(ReguleringFeatureToggle.SKAL_STOPPE_ETTER_FATTET_VEDTAK, false)) {
            return true
        }
        return when (revurderingaarsak) {
            Revurderingaarsak.INNTEKTSENDRING -> true
            else -> false
        }
    }
}
