package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.Slate
import java.time.LocalDate

data class InnvilgetBrevDataEnkel(
    val utbetalingsinfo: Utbetalingsinfo,
    val avdoed: Avdoed,
    val erEtterbetaling: Boolean,
    val vedtaksdato: LocalDate,
    val erInstitusjonsopphold: Boolean,
) : BrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            utbetalingsinfo: Utbetalingsinfo,
            etterbetalingDTO: EtterbetalingDTO?,
        ) = InnvilgetBrevDataEnkel(
            utbetalingsinfo = utbetalingsinfo,
            avdoed = generellBrevData.personerISak.avdoed,
            erEtterbetaling = etterbetalingDTO != null,
            vedtaksdato =
                generellBrevData.forenkletVedtak.vedtaksdato
                    ?: LocalDate.now(),
            erInstitusjonsopphold =
                utbetalingsinfo.beregningsperioder
                    .filter { it.datoFOM.isBefore(LocalDate.now().plusDays(1)) }
                    .firstOrNull { it.datoTOM.erSamtidigEllerEtter(LocalDate.now()) }
                    ?.institusjon ?: false,
        )
    }
}

data class InnvilgetHovedmalBrevData(
    val utbetalingsinfo: Utbetalingsinfo,
    val avkortingsinfo: Avkortingsinfo? = null,
    val etterbetalingDTO: EtterbetalingDTO? = null,
    val innhold: List<Slate.Element>,
) : BrevData() {
    companion object {
        fun fra(
            utbetalingsinfo: Utbetalingsinfo,
            avkortingsinfo: Avkortingsinfo,
            etterbetalingDTO: EtterbetalingDTO,
            innhold: List<Slate.Element>,
        ): InnvilgetHovedmalBrevData =
            InnvilgetHovedmalBrevData(
                utbetalingsinfo = utbetalingsinfo,
                avkortingsinfo = avkortingsinfo,
                etterbetalingDTO = etterbetalingDTO,
                innhold = innhold,
            )
    }
}

private fun LocalDate?.erSamtidigEllerEtter(dato: LocalDate) = this == null || this.isAfter(dato.minusDays(1))
