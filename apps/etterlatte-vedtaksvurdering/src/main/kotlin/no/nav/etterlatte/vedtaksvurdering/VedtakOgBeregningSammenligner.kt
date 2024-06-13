package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.vedtaksvurdering.BeregningOgAvkorting
import no.nav.etterlatte.vedtaksvurdering.Vedtak
import no.nav.etterlatte.vedtaksvurdering.VedtakInnhold
import org.slf4j.LoggerFactory
import java.math.BigDecimal

object VedtakOgBeregningSammenligner {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun sammenlign(
        beregning: BeregningOgAvkorting,
        vedtak: Vedtak,
    ) {
        if (vedtak.type in setOf(VedtakType.AVSLAG, VedtakType.AVVIST_KLAGE)) {
            return
        }
        when (vedtak.innhold) {
            is VedtakInnhold.Behandling -> sammenlignBehandling(beregning, vedtak)
            is VedtakInnhold.Klage -> sammenlignKlage(vedtak)
            is VedtakInnhold.Tilbakekreving -> sammenlignTilbakekreving(vedtak)
        }
    }

    private fun sammenlignBehandling(
        beregning: BeregningOgAvkorting,
        vedtak: Vedtak,
    ) {
        val innhold = vedtak.innhold as VedtakInnhold.Behandling
        val perioder = innhold.utbetalingsperioder
        val beregningsperioder = beregning.beregning.beregningsperioder
        val likeMangePerioder = perioder.size == beregningsperioder.size
        check(likeMangePerioder) {
            "Forventa like mange perioder i vedtak som i beregning for vedtak ${vedtak.id} i sak ${vedtak.sakId}. " +
                "Vedtak hadde ${perioder.size}, mens beregning hadde ${beregningsperioder.size}" +
                "Alle perioder fra vedtak: ${perioder.map { "${it.periode}: ${it.beloep}" }}. " +
                "Alle perioder fra beregning: ${beregningsperioder.map {
                    "${it.datoFOM}-${it.datoTOM} - ${it.utbetaltBeloep}"
                } }"
        }
        if (likeMangePerioder) {
            // Har alt sjekka det under, så hoppar over det no.
            return
        }
        for (i in perioder.indices) {
            val periode = perioder[i]
            val beregningsperiode = beregningsperioder[i]
            val avkorting =
                beregning.avkorting?.avkortetYtelse?.firstOrNull {
                    it.fom == beregningsperiode.datoFOM && it.tom == beregningsperiode.datoTOM
                }

            val sumFraBeregningOgEvAvkorting = BigDecimal(avkorting?.ytelseEtterAvkorting ?: beregningsperiode.utbetaltBeloep)
            check(sumFraBeregningOgEvAvkorting == periode.beloep) {
                "Beløp for periode ${periode.periode} i vedtak ${vedtak.id} og behandling ${vedtak.behandlingId} var ${periode.beloep} i vedtak, men $sumFraBeregningOgEvAvkorting fra beregning og eventuell avkorting"
            }
            check(Periode(beregningsperiode.datoFOM, beregningsperiode.datoTOM) == periode.periode) {
                "FOM og TOM for periode ${periode.periode} i vedtak ${vedtak.id} " +
                    "og behandling ${vedtak.behandlingId} i vedtak, men ${Periode(
                        beregningsperiode.datoFOM,
                        beregningsperiode.datoTOM,
                    )} fra beregning. Alle perioder fra vedtak:" +
                    "${perioder.map { it.periode }}. " +
                    "Alle perioder fra beregning: ${beregningsperioder.map { "${it.datoFOM}-${it.datoTOM}" } }"
            }
        }
    }

    private fun sammenlignKlage(vedtak: Vedtak) {
        val innhold = vedtak.innhold as VedtakInnhold.Klage
        logger.info("Sammenligning av innhold i klage er ikke implementert enno")
    }

    private fun sammenlignTilbakekreving(vedtak: Vedtak) {
        val innhold = vedtak.innhold as VedtakInnhold.Tilbakekreving
        logger.info("Sammenligning av innhold i tilbakekreving er ikke implementert enno")
    }
}
