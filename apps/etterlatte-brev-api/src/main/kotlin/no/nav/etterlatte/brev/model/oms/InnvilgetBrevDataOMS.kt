package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.Beregningsinfo
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.BrevVedleggKey
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
    val etterbetalinginfo: EtterbetalingDTO? = null,
    val beregningsinfo: Beregningsinfo? = null,
    val innhold: List<Slate.Element>,
) : BrevData() {
    companion object {
        fun fra(
            behandling: Behandling,
            innholdMedVedlegg: InnholdMedVedlegg,
        ): InnvilgetBrevDataOMS =
            InnvilgetBrevDataOMS(
                utbetalingsinfo = behandling.utbetalingsinfo!!,
                avkortingsinfo = behandling.avkortingsinfo,
                avdoed = behandling.personerISak.avdoed,
                etterbetalinginfo = behandling.etterbetalingDTO,
                beregningsinfo =
                    Beregningsinfo(
                        innhold = innholdMedVedlegg.finnVedlegg(BrevVedleggKey.BEREGNING_INNHOLD),
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
        fun fra(behandling: Behandling): FoerstegangsvedtakUtfallDTO =
            FoerstegangsvedtakUtfallDTO(
                virkningsdato = behandling.utbetalingsinfo!!.virkningsdato,
                avdoed = behandling.personerISak.avdoed,
                utbetalingsbeloep = behandling.avkortingsinfo!!.beregningsperioder.first().utbetaltBeloep,
            )
    }
}
