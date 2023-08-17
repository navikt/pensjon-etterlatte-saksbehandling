package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Trygdetidsperiode
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class InnvilgetBrevData(
    val utbetalingsinfo: Utbetalingsinfo,
    val avkortingsinfo: Avkortingsinfo? = null,
    val avdoed: Avdoed
) : BrevData() {

    companion object {
        fun fra(behandling: Behandling): InnvilgetBrevData =
            InnvilgetBrevData(
                utbetalingsinfo = behandling.utbetalingsinfo,
                avdoed = behandling.persongalleri.avdoed,
                avkortingsinfo = behandling.avkortingsinfo
            )
    }
}

data class Beregningsinfo(
    val grunnbeloep: Kroner,
    val beregningsperioder: List<NyBeregningsperiode>,
    val trygdetidsperioder: List<Trygdetidsperiode>
)

data class NyBeregningsperiode(
    val inntekt: Kroner,
    val trygdetid: Int,
    val stoenadFoerReduksjon: Kroner,
    var utbetaltBeloep: Kroner
)

data class InnvilgetBrevDataOMS(
    val utbetalingsinfo: Utbetalingsinfo,
    val avkortingsinfo: Avkortingsinfo? = null,
    val avdoed: Avdoed,
    val etterbetalinginfo: EtterbetalingDTO? = null,
    val beregningsinfo: Beregningsinfo? = null,
    val innhold: List<Slate.Element>
) : BrevData() {

    companion object {
        fun fra(behandling: Behandling, innhold: List<Slate.Element>): InnvilgetBrevDataOMS =
            InnvilgetBrevDataOMS(
                utbetalingsinfo = behandling.utbetalingsinfo!!,
                avkortingsinfo = behandling.avkortingsinfo,
                avdoed = behandling.persongalleri.avdoed,
                etterbetalinginfo = null,
                beregningsinfo = Beregningsinfo(
                    grunnbeloep = behandling.avkortingsinfo!!.grunnbeloep,
                    beregningsperioder = behandling.avkortingsinfo.beregningsperioder.map {
                        NyBeregningsperiode(
                            inntekt = it.inntekt,
                            trygdetid = it.trygdetid,
                            stoenadFoerReduksjon = it.ytelseFoerAvkorting,
                            utbetaltBeloep = it.utbetaltBeloep
                        )
                    },
                    trygdetidsperioder = behandling.trygdetid!!
                ),
                innhold = innhold
            )
    }
}

data class FoerstegangsvedtakUtfallDTO(
    val virkningsdato: LocalDate,
    val avdoed: Avdoed,
    val utbetalingsbeloep: Kroner
) : BrevData() {
    companion object {
        fun fra(behandling: Behandling): FoerstegangsvedtakUtfallDTO =
            FoerstegangsvedtakUtfallDTO(
                virkningsdato = behandling.utbetalingsinfo!!.virkningsdato,
                avdoed = behandling.persongalleri.avdoed,
                utbetalingsbeloep = behandling.avkortingsinfo!!.beregningsperioder.first().utbetaltBeloep
            )
    }
}

data class InnvilgetBrevDataNy(
    val utbetalingsinfo: Utbetalingsinfo,
    val avkortingsinfo: Avkortingsinfo? = null,
    val avdoed: Avdoed,
    val etterbetalingDTO: EtterbetalingDTO? = null,
    val innhold: List<Slate.Element>
) : BrevData() {

    companion object {
        fun fra(behandling: Behandling): InnvilgetBrevDataNy =
            InnvilgetBrevDataNy(
                utbetalingsinfo = behandling.utbetalingsinfo,
                avdoed = behandling.persongalleri.avdoed,
                avkortingsinfo = behandling.avkortingsinfo,
                etterbetalingDTO = null,
                innhold = listOf()
            )
    }
}