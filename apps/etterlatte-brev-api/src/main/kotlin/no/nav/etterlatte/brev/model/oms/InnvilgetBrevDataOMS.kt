package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.Beregningsinfo
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.EtterbetalingBrev
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.NyBeregningsperiode
import no.nav.etterlatte.brev.model.Slate
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class InnvilgetBrevDataOMS(
    val utbetalingsinfo: Utbetalingsinfo,
    val avkortingsinfo: Avkortingsinfo? = null,
    val avdoed: Avdoed,
    val etterbetalinginfo: EtterbetalingBrev? = null,
    val beregningsinfo: Beregningsinfo? = null,
    val innhold: List<Slate.Element>,
) : BrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            utbetalingsinfo: Utbetalingsinfo,
            avkortingsinfo: Avkortingsinfo? = null,
            etterbetalinginfo: EtterbetalingDTO? = null,
            trygdetid: Trygdetid,
            innholdMedVedlegg: InnholdMedVedlegg,
        ): InnvilgetBrevDataOMS =
            InnvilgetBrevDataOMS(
                utbetalingsinfo = Utbetalingsinfo.kopier(utbetalingsinfo, etterbetalinginfo),
                avkortingsinfo = avkortingsinfo,
                avdoed = generellBrevData.personerISak.avdoed,
                etterbetalinginfo = EtterbetalingBrev.fra(etterbetalinginfo, utbetalingsinfo.beregningsperioder),
                beregningsinfo =
                    Beregningsinfo(
                        innhold = innholdMedVedlegg.finnVedlegg(BrevVedleggKey.BEREGNING_INNHOLD),
                        grunnbeloep = avkortingsinfo!!.grunnbeloep,
                        beregningsperioder =
                            avkortingsinfo.beregningsperioder.map {
                                NyBeregningsperiode(
                                    inntekt = it.inntekt,
                                    trygdetid = it.trygdetid,
                                    stoenadFoerReduksjon = it.ytelseFoerAvkorting,
                                    utbetaltBeloep = it.utbetaltBeloep,
                                )
                            },
                        trygdetidsperioder = trygdetid.perioder,
                    ),
                innhold = innholdMedVedlegg.innhold(),
            )
    }
}

data class FoerstegangsvedtakUtfallDTO(
    val virkningsdato: LocalDate,
    val avdoed: Avdoed,
    val utbetalingsbeloep: Kroner,
) : BrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            utbetalingsinfo: Utbetalingsinfo,
            avkortingsinfo: Avkortingsinfo,
        ): FoerstegangsvedtakUtfallDTO =
            FoerstegangsvedtakUtfallDTO(
                virkningsdato = utbetalingsinfo.virkningsdato,
                avdoed = generellBrevData.personerISak.avdoed,
                utbetalingsbeloep = avkortingsinfo.beregningsperioder.first().utbetaltBeloep,
            )
    }
}
