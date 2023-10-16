package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.model.Beregningsinfo
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.NyBeregningsperiode
import no.nav.etterlatte.brev.model.Slate

data class InntektsendringRevurderingOMS(
    val erEndret: Boolean,
    val avkortingsinfo: Avkortingsinfo? = null,
    val etterbetalinginfo: EtterbetalingDTO? = null,
    val beregningsinfo: Beregningsinfo? = null,
    val innhold: List<Slate.Element>,
) : BrevData() {
    companion object {
        fun fra(
            avkortingsinfo: Avkortingsinfo,
            etterbetalingDTO: EtterbetalingDTO,
            trygdetid: Trygdetid,
            innholdMedVedlegg: InnholdMedVedlegg,
        ): InntektsendringRevurderingOMS =
            InntektsendringRevurderingOMS(
                erEndret = true,
                avkortingsinfo = avkortingsinfo,
                etterbetalinginfo = etterbetalingDTO,
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
