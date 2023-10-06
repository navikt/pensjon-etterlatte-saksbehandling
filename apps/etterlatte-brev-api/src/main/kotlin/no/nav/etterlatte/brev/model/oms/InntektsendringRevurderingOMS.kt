package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.model.Beregningsinfo
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.EtterbetalingDTO
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
            behandling: Behandling,
            innhold: List<Slate.Element>,
            innholdVedlegg: List<BrevInnholdVedlegg>,
        ): InntektsendringRevurderingOMS =
            InntektsendringRevurderingOMS(
                erEndret = true,
                avkortingsinfo = behandling.avkortingsinfo,
                etterbetalinginfo = behandling.etterbetalingDTO,
                beregningsinfo =
                    Beregningsinfo(
                        innhold =
                            innholdVedlegg.find {
                                    vedlegg ->
                                vedlegg.key == BrevVedleggKey.BEREGNING_INNHOLD
                            }?.payload!!.elements,
                        grunnbeloep = behandling.avkortingsinfo!!.grunnbeloep,
                        beregningsperioder =
                            behandling.avkortingsinfo.beregningsperioder.map {
                                NyBeregningsperiode(
                                    inntekt = it.inntekt,
                                    trygdetid = it.trygdetid,
                                    stoenadFoerReduksjon = it.ytelseFoerAvkorting,
                                    utbetaltBeloep = it.utbetaltBeloep,
                                )
                            },
                        trygdetidsperioder = behandling.trygdetid!!,
                    ),
                innhold = innhold,
            )
    }
}
