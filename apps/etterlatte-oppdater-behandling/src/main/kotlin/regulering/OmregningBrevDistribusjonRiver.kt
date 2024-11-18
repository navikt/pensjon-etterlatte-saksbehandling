package no.nav.etterlatte.regulering

import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.brev.BrevHendelseType
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.inntektsjustering.AarligInntektsjusteringRequest
import no.nav.etterlatte.libs.common.sak.DisttribuertEllerIverksatt
import no.nav.etterlatte.rapidsandrivers.BREV_KODE
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.brevId
import no.nav.etterlatte.rapidsandrivers.brevKode
import no.nav.etterlatte.rapidsandrivers.sakId
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
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BREV_KODE) }
            validate { it.demandValue(BREV_KODE, Brevkoder.OMS_INNTEKTSJUSTERING_VEDTAK.name) } // TODO kan bli flere
        }
    }

    override fun kontekst() = Kontekst.OMREGNING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Setter status på brev til distribuert på omregning til sak=${packet.sakId}, brevid=${packet.brevId}")
        try {
            val brevkode = packet.brevKode
            when (brevkode) {
                Brevkoder.OMS_INNTEKTSJUSTERING_VEDTAK.name -> {
                    val kjoering = AarligInntektsjusteringRequest.utledKjoering()
                    behandlingService.lagreKjoeringBrevDistribuertEllerIverksatt(
                        packet.sakId,
                        kjoering,
                        DisttribuertEllerIverksatt.DISTRIBUERT,
                    )
                }

                else -> {}
            }
        } catch (e: Exception) {
            logger.error(
                "Kunne ikke oppdatere omregning sin status på brev distribusjon for sak ${packet.sakId} brevid: ${packet.brevId}",
                e,
            )
            throw InternfeilException(
                "Fikk ikke oppdatert kjøring med status på brev distribusjon for ${packet.sakId}",
                e,
            )
        }
    }
}
