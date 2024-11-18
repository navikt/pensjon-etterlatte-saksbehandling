package no.nav.etterlatte.regulering

import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.brev.BrevHendelseType
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.sak.DisttribuertEllerIverksatt
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.OmregningDataPacket
import no.nav.etterlatte.rapidsandrivers.omregningData
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class OmregningBrevDistribusjonRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, BrevHendelseType.DISTRIBUERT) {
            validate { it.requireKey(OmregningDataPacket.KEY) }
            validate { it.requireKey(OmregningDataPacket.SAK_ID) }
            validate { it.requireKey(OmregningDataPacket.KJOERING) }
            validate { it.requireKey(OmregningDataPacket.REV_AARSAK) }
        }
    }

    override fun kontekst() = Kontekst.OMREGNING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val sakId = packet.omregningData.sakId
        logger.info("Setter status på brev til distribuert på omregning til sak=$sakId")
        try {
            val revurderingsaarsak = packet.omregningData.revurderingaarsak
            when (revurderingsaarsak) {
                Revurderingaarsak.AARLIG_INNTEKTSJUSTERING -> {
                    behandlingService.lagreKjoeringBrevDistribuertEllerIverksatt(
                        sakId,
                        packet.omregningData.kjoering,
                        DisttribuertEllerIverksatt.DISTRIBUERT,
                    )
                }

                else -> {}
            }
        } catch (e: Exception) {
            logger.error(
                "Kunne ikke oppdatere omregning sin status på brev distribusjon for sak $sakId",
                e,
            )
            throw InternfeilException(
                "Fikk ikke oppdatert kjøring med status på brev distribusjon for $sakId",
                e,
            )
        }
    }
}
