package no.nav.etterlatte.regulering

import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.BrevutfallOgEtterbetalingDto
import no.nav.etterlatte.libs.common.behandling.Feilutbetaling
import no.nav.etterlatte.libs.common.behandling.FeilutbetalingValg
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.libs.common.revurdering.AutomatiskRevurderingRequest
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.omregning.OmregningData
import no.nav.etterlatte.omregning.OmregningDataPacket
import no.nav.etterlatte.omregning.OmregningHendelseType
import no.nav.etterlatte.omregning.omregningData
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class OmregningsHendelserBehandlingRiver(
    rapidsConnection: RapidsConnection,
    private val behandlinger: BehandlingService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, OmregningHendelseType.KLAR_FOR_OMREGNING) {
            validate { it.rejectKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(OmregningDataPacket.KEY) }
            validate { it.requireKey(OmregningDataPacket.FRA_DATO) }
        }
    }

    override fun kontekst() = Kontekst.OMREGNING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Mottatt omregningshendelse")
        val omregningData: OmregningData = packet.omregningData

        if (omregningData.revurderingaarsak != Revurderingaarsak.REGULERING) {
            behandlinger.lagreKjoering(omregningData.sakId, KjoeringStatus.STARTA, omregningData.kjoering)
        }

        val (behandlingId, forrigeBehandlingId, sakType) =
            behandlinger.opprettAutomatiskRevurdering(
                AutomatiskRevurderingRequest(
                    sakId = omregningData.sakId,
                    fraDato = omregningData.hentFraDato(),
                    revurderingAarsak = omregningData.revurderingaarsak,
                    mottattDato = omregningData.hentInntektsjustering().mottatt,
                ),
            )

        val revurderingaarsak = omregningData.revurderingaarsak
        if (revurderingaarsak == Revurderingaarsak.AARLIG_INNTEKTSJUSTERING || revurderingaarsak == Revurderingaarsak.INNTEKTSENDRING) {
            behandlinger.leggInnBrevutfall(
                BrevutfallOgEtterbetalingDto(
                    behandlingId = behandlingId,
                    opphoer = false, // TODO opphørfom?a
                    etterbetaling = null,
                    brevutfall =
                        BrevutfallDto(
                            behandlingId = behandlingId,
                            aldersgruppe = Aldersgruppe.OVER_18, // eller null siden oms?
                            feilutbetaling =
                                Feilutbetaling(
                                    valg = FeilutbetalingValg.NEI,
                                    kommentar = null,
                                ),
                            kilde = Grunnlagsopplysning.automatiskSaksbehandler,
                            frivilligSkattetrekk = false,
                        ),
                ),
            )
        }

        omregningData.endreSakType(sakType)
        omregningData.endreBehandlingId(behandlingId)
        omregningData.endreForrigeBehandlingid(forrigeBehandlingId)
        packet.omregningData = omregningData

        packet.setEventNameForHendelseType(OmregningHendelseType.BEHANDLING_OPPRETTA)
        context.publish(packet.toJson())
        logger.info("Publiserte oppdatert omregningshendelse")
    }
}
