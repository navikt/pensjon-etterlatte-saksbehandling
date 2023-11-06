package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.EtterbetalingBrev
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import java.time.LocalDate
import java.time.YearMonth

data class InnvilgetBrevDataEnkel(
    val utbetalingsinfo: Utbetalingsinfo,
    val avdoed: Avdoed,
    val vedtaksdato: LocalDate,
    val erEtterbetaling: Boolean,
) : BrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            utbetalingsinfo: Utbetalingsinfo,
            etterbetaling: EtterbetalingDTO?,
        ) = InnvilgetBrevDataEnkel(
            utbetalingsinfo = utbetalingsinfo,
            avdoed = generellBrevData.personerISak.avdoed,
            vedtaksdato = finnVedtaksmaaned(generellBrevData, etterbetaling).atDay(1),
            erEtterbetaling = etterbetaling != null,
        )

        private fun finnVedtaksmaaned(
            generellBrevData: GenerellBrevData,
            etterbetaling: EtterbetalingDTO?,
        ): YearMonth {
            if (etterbetaling != null) {
                return YearMonth.from(etterbetaling.tilDato).plusMonths(1)
            }
            return generellBrevData.forenkletVedtak.vedtaksdato?.let { YearMonth.from(it) }
                ?: YearMonth.now()
        }
    }
}

data class InnvilgetHovedmalBrevData(
    val utbetalingsinfo: Utbetalingsinfo,
    val avkortingsinfo: Avkortingsinfo? = null,
    val beregningsinfo: BeregningsinfoBP,
    val etterbetaling: EtterbetalingBrev? = null,
    val innhold: List<Slate.Element>,
) : BrevData() {
    companion object {
        fun fra(
            utbetalingsinfo: Utbetalingsinfo,
            avkortingsinfo: Avkortingsinfo?,
            etterbetalingDTO: EtterbetalingDTO?,
            trygdetid: Trygdetid,
            grunnbeloep: Grunnbeloep,
            innhold: InnholdMedVedlegg,
        ): InnvilgetHovedmalBrevData =
            InnvilgetHovedmalBrevData(
                utbetalingsinfo = Utbetalingsinfo.kopier(utbetalingsinfo, etterbetalingDTO),
                avkortingsinfo = avkortingsinfo,
                beregningsinfo = BeregningsinfoBP.fra(utbetalingsinfo, trygdetid, grunnbeloep, innhold),
                etterbetaling = EtterbetalingBrev.fra(etterbetalingDTO, utbetalingsinfo.beregningsperioder),
                innhold = innhold.innhold(),
            )
    }
}
