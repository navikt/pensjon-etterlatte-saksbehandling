package no.nav.etterlatte.regulering

import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.no.nav.etterlatte.klienter.UtbetalingKlient
import no.nav.etterlatte.no.nav.etterlatte.regulering.ReguleringFeatureToggle
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.OmregningDataPacket
import no.nav.etterlatte.rapidsandrivers.OmregningHendelseType
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents
import no.nav.etterlatte.rapidsandrivers.omregningData
import no.nav.etterlatte.vedtaksvurdering.RapidUtsender
import no.nav.etterlatte.vedtaksvurdering.VedtakOgRapid
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import tidspunkt.erEtter
import tidspunkt.erFoerEllerPaa
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

internal class OpprettVedtakforespoerselRiver(
    rapidsConnection: RapidsConnection,
    private val vedtak: VedtakService,
    private val utbetalingKlient: UtbetalingKlient,
    private val featureToggleService: FeatureToggleService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, OmregningHendelseType.BEREGNA) {
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.requireKey(OmregningDataPacket.SAK_ID) }
            validate { it.requireKey(OmregningDataPacket.BEHANDLING_ID) }
            validate { it.requireKey(OmregningDataPacket.FRA_DATO) }
        }
    }

    override fun kontekst() = Kontekst.OMREGNING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val omregningData = packet.omregningData
        val sakId = omregningData.sakId
        logger.info("Leser opprett-vedtak forespoersel for sak $sakId")
        val behandlingId = omregningData.hentBehandlingId()
        val dato = omregningData.hentFraDato()

        val respons =
            if (featureToggleService.isEnabled(ReguleringFeatureToggle.SkalStoppeEtterFattetVedtak, false)) {
                vedtak.opprettVedtakOgFatt(sakId, behandlingId)
            } else {
                // TODO bør se på flyten her også
                if (omregningData.revurderingaarsak == Revurderingaarsak.REGULERING) {
                    vedtak.opprettVedtakFattOgAttester(sakId, behandlingId)
                } else {
                    vedtak.opprettVedtakOgFatt(sakId, behandlingId)
                    verifiserUendretUtbetaling(behandlingId)
                    vedtak.attesterVedtak(sakId, behandlingId)
                }
            }
        hentBeloep(respons, dato)?.let { packet[ReguleringEvents.VEDTAK_BELOEP] = it }
        logger.info("Opprettet vedtak ${respons.vedtak.id} for sak: $sakId og behandling: $behandlingId")
        RapidUtsender.sendUt(respons, packet, context)
    }

    private fun verifiserUendretUtbetaling(behandlingId: UUID) {
        val simulertBeregning = utbetalingKlient.simuler(behandlingId)
        if (simulertBeregning.etterbetaling.isNotEmpty() || simulertBeregning.tilbakekreving.isNotEmpty()) {
            throw Exception("Omregningen hadde etterbetaling eller tilbakekreving, avbryter..")
        }
    }

    private fun hentBeloep(
        respons: VedtakOgRapid,
        dato: LocalDate,
    ): BigDecimal? =
        if (respons.vedtak.innhold is VedtakInnholdDto.VedtakBehandlingDto) {
            (respons.vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto)
                .utbetalingsperioder
                .filter { it.periode.fom.erFoerEllerPaa(dato) }
                .first { it.periode.tom.erEtter(dato) }
                .beloep
        } else {
            null
        }
}
