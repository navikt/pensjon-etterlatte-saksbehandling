package no.nav.etterlatte.regulering

import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.brev.BrevRequestHendelseType
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.libs.common.inntektsjustering.AarligInntektsjusteringKjoering
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.LagreKjoeringRequest
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.OmregningDataPacket
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.AVKORTING_ETTER
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.AVKORTING_FOER
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.BEREGNING_BELOEP_ETTER
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.BEREGNING_BELOEP_FOER
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.BEREGNING_BRUKT_OMREGNINGSFAKTOR
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.BEREGNING_G_ETTER
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.BEREGNING_G_FOER
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.VEDTAK_BELOEP
import no.nav.etterlatte.rapidsandrivers.brevKode
import no.nav.etterlatte.rapidsandrivers.omregningData
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.math.BigDecimal

internal class VedtakAttestertRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, VedtakKafkaHendelseHendelseType.ATTESTERT) {
            validate { it.requireKey(OmregningDataPacket.KEY) }
            validate { it.requireKey(OmregningDataPacket.SAK_ID) }
            validate { it.requireKey(OmregningDataPacket.KJOERING) }
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
        val sakId = packet.omregningData.sakId
        val kjoering = packet.omregningData.kjoering
        logger.info("Sak $sakId er ferdig omregnet, oppdaterer status")
        val request =
            LagreKjoeringRequest(
                kjoering = kjoering,
                status = KjoeringStatus.FERDIGSTILT,
                sakId = sakId,
                beregningBeloepFoer = bigDecimal(packet, BEREGNING_BELOEP_FOER),
                beregningBeloepEtter = bigDecimal(packet, BEREGNING_BELOEP_ETTER),
                beregningGFoer = bigDecimal(packet, BEREGNING_G_FOER),
                beregningGEtter = bigDecimal(packet, BEREGNING_G_ETTER),
                beregningBruktOmregningsfaktor = bigDecimal(packet, BEREGNING_BRUKT_OMREGNINGSFAKTOR),
                avkortingFoer = bigDecimal(packet, AVKORTING_FOER),
                avkortingEtter = bigDecimal(packet, AVKORTING_ETTER),
                vedtakBeloep = bigDecimal(packet, VEDTAK_BELOEP),
            )

        // inntektsjustering, send vedtak og varselbrev
        if (kjoering === AarligInntektsjusteringKjoering.getKjoering()) {
            packet.setEventNameForHendelseType(BrevRequestHendelseType.OPPRETT_JOURNALFOER_OG_DISTRIBUER)
            packet.brevKode = Brevkoder.OMS_INNTEKTSJUSTERING_VARSEL.toString()
            packet.sakId = packet.omregningData.sakId
            context.publish(packet.toJson())
        } else {
            behandlingService.lagreFullfoertKjoering(request)
            logger.info("Sak $sakId er ferdig omregnet, status er oppdatert")
        }
    }

    private fun bigDecimal(
        packet: JsonMessage,
        noekkel: String,
    ): BigDecimal? = packet[noekkel].asText().takeIf { it.isNotEmpty() }?.let { BigDecimal(it) }
}
