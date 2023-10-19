package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BrevData
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
            erEtterbetaling: Boolean,
        ) = InnvilgetBrevDataEnkel(
            utbetalingsinfo = utbetalingsinfo,
            avdoed = generellBrevData.personerISak.avdoed,
            vedtaksdato =
                generellBrevData.forenkletVedtak.vedtaksdato?.let { YearMonth.from(it).atDay(1) }
                    ?: YearMonth.now().atDay(1),
            erEtterbetaling = erEtterbetaling,
        )
    }
}

data class InnvilgetHovedmalBrevData(
    val utbetalingsinfo: Utbetalingsinfo,
    val avkortingsinfo: Avkortingsinfo? = null,
    val beregningsinfo: BeregningsinfoBP,
    val etterbetalingDTO: EtterbetalingDTO? = null,
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
                utbetalingsinfo = utbetalingsinfo,
                avkortingsinfo = avkortingsinfo,
                beregningsinfo = BeregningsinfoBP.fra(utbetalingsinfo, trygdetid, grunnbeloep, innhold),
                etterbetalingDTO = etterbetalingDTO,
                innhold = innhold.innhold(),
            )
    }
}
