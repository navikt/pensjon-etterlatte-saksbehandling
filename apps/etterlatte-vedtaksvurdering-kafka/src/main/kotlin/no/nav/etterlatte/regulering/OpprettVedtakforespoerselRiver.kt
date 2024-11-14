package no.nav.etterlatte.regulering

import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.GenererOgFerdigstillVedtaksbrev
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.no.nav.etterlatte.klienter.BrevKlient
import no.nav.etterlatte.no.nav.etterlatte.klienter.UtbetalingKlient
import no.nav.etterlatte.no.nav.etterlatte.regulering.ReguleringFeatureToggle
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.OmregningDataPacket
import no.nav.etterlatte.rapidsandrivers.OmregningHendelseType
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents
import no.nav.etterlatte.rapidsandrivers.UtbetalingVerifikasjon
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
    private val brevKlient: BrevKlient,
    private val featureToggleService: FeatureToggleService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, OmregningHendelseType.BEREGNA) {
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.requireKey(OmregningDataPacket.SAK_ID) }
            validate { it.requireKey(OmregningDataPacket.BEHANDLING_ID) }
            validate { it.requireKey(OmregningDataPacket.FRA_DATO) }
            validate { it.requireKey(OmregningDataPacket.REV_AARSAK) }
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
        val revurderingaarsak = omregningData.revurderingaarsak

        val skalSendeBrev =
            when (omregningData.revurderingaarsak) {
                Revurderingaarsak.AARLIG_INNTEKTSJUSTERING -> true
                else -> false
            }

        val respons =
            if (featureToggleService.isEnabled(ReguleringFeatureToggle.SkalStoppeEtterFattetVedtak, false)) {
                opprettBrev(behandlingId, sakId, revurderingaarsak)
                vedtak.opprettVedtakOgFatt(sakId, behandlingId)
            } else {
                when (omregningData.utbetalingVerifikasjon) {
                    UtbetalingVerifikasjon.INGEN -> {
                        if (skalSendeBrev) {
                            vedtakOgBrev(sakId, behandlingId, revurderingaarsak)
                        } else {
                            vedtak.opprettVedtakFattOgAttester(sakId, behandlingId)
                        }
                    }

                    UtbetalingVerifikasjon.SIMULERING -> vedtakOgBrev(sakId, behandlingId, revurderingaarsak, false)

                    UtbetalingVerifikasjon.SIMULERING_AVBRYT_ETTERBETALING_ELLER_TILBAKEKREVING ->
                        vedtakOgBrev(
                            sakId,
                            behandlingId,
                            revurderingaarsak,
                            true,
                        )
                }
            }

        hentBeloep(respons, dato)?.let { packet[ReguleringEvents.VEDTAK_BELOEP] = it }
        logger.info("Opprettet vedtak ${respons.vedtak.id} for sak: $sakId og behandling: $behandlingId")
        RapidUtsender.sendUt(respons, packet, context)
    }

    private fun verifiserUendretUtbetaling(
        behandlingId: UUID,
        skalAvbryte: Boolean,
    ) {
        val simulertBeregning = utbetalingKlient.simuler(behandlingId)

        val etterbetalingSum = simulertBeregning.etterbetaling.sumOf { it.beloep }
        val etterbetaling = etterbetalingSum.compareTo(BigDecimal.ZERO) != 0

        val tilbakekrevingSum = simulertBeregning.tilbakekreving.sumOf { it.beloep }
        val tilbakekreving = tilbakekrevingSum.compareTo(BigDecimal.ZERO) != 0

        if (etterbetaling) {
            val msg = "Omregningen fører til etterbetaling på $etterbetalingSum kr"
            if (skalAvbryte) {
                throw Exception("$msg, avbryter behandlingen")
            } else {
                logger.info(msg)
            }
        }

        if (tilbakekreving) {
            val msg = "Omregningen fører til tilbakekreving på $tilbakekrevingSum kr"
            if (skalAvbryte) {
                throw Exception("$msg, avbryter behandlingen")
            } else {
                logger.info(msg)
            }
        }

        if (!etterbetaling && !tilbakekreving) {
            logger.info("Omregningen førte ikke til tilbakekreving eller etterbetaling")
        }
    }

    private fun vedtakOgBrev(
        sakId: SakId,
        behandlingId: UUID,
        revurderingaarsak: Revurderingaarsak,
        skalAvbryteUtbetaling: Boolean? = null,
    ): VedtakOgRapid {
        vedtak.opprettVedtakOgFatt(sakId, behandlingId)
        val brev = opprettBrev(behandlingId, sakId, revurderingaarsak)

        if (skalAvbryteUtbetaling != null) {
            verifiserUendretUtbetaling(behandlingId, skalAvbryte = skalAvbryteUtbetaling)
        }

        if (brev != null) {
            ferdigstillBrev(behandlingId, brev.id, revurderingaarsak)
        }

        return vedtak.attesterVedtak(sakId, behandlingId)
    }

    private fun opprettBrev(
        behandlingId: UUID,
        sakId: SakId,
        revurderingaarsak: Revurderingaarsak,
    ): Brev? =
        when (revurderingaarsak) {
            Revurderingaarsak.AARLIG_INNTEKTSJUSTERING -> brevKlient.opprettBrev(behandlingId, sakId)
            else -> null
        }

    private fun ferdigstillBrev(
        behandlingId: UUID,
        brevId: BrevID,
        revurderingaarsak: Revurderingaarsak,
    ) {
        when (revurderingaarsak) {
            Revurderingaarsak.AARLIG_INNTEKTSJUSTERING ->
                brevKlient.genererPdfOgFerdigstillVedtaksbrev(
                    behandlingId,
                    GenererOgFerdigstillVedtaksbrev(behandlingId, brevId),
                )

            else -> throw InternfeilException("Støtter ikke brev under automatisk omregning for $revurderingaarsak")
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
