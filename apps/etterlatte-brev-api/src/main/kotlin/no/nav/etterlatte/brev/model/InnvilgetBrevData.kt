package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Trygdetidsperiode
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.libs.common.behandling.erEtterbetaling
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class InnvilgetBrevData(
    val utbetalingsinfo: Utbetalingsinfo,
    val avkortingsinfo: Avkortingsinfo? = null,
    val avdoed: Avdoed,
) : BrevData() {
    companion object {
        fun fra(behandling: Behandling): InnvilgetBrevData =
            InnvilgetBrevData(
                utbetalingsinfo = behandling.utbetalingsinfo,
                avdoed = behandling.persongalleri.avdoed,
                avkortingsinfo = behandling.avkortingsinfo,
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
                avdoed = behandling.persongalleri.avdoed,
                utbetalingsbeloep = behandling.avkortingsinfo!!.beregningsperioder.first().utbetaltBeloep,
            )
    }
}

data class Beregningsinfo(
    val innhold: List<Slate.Element>,
    val grunnbeloep: Kroner,
    val beregningsperioder: List<NyBeregningsperiode>,
    val trygdetidsperioder: List<Trygdetidsperiode>,
)

data class NyBeregningsperiode(
    val inntekt: Kroner,
    val trygdetid: Int,
    val stoenadFoerReduksjon: Kroner,
    var utbetaltBeloep: Kroner,
)

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
            innhold: List<Slate.Element>,
            innholdVedlegg: List<BrevInnholdVedlegg>,
        ): InnvilgetBrevDataOMS =
            InnvilgetBrevDataOMS(
                utbetalingsinfo = behandling.utbetalingsinfo,
                avkortingsinfo = behandling.avkortingsinfo,
                avdoed = behandling.persongalleri.avdoed,
                etterbetalinginfo = null,
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

data class InnvilgetBrevDataEnkel(
    val utbetalingsinfo: Utbetalingsinfo,
    val avdoed: Avdoed,
    val erEtterbetalingMerEnnTreMaaneder: Boolean,
    val vedtaksdato: LocalDate,
    val erInstitusjonsopphold: Boolean,
) : BrevData() {
    companion object {
        fun fra(behandling: Behandling) =
            InnvilgetBrevDataEnkel(
                utbetalingsinfo = behandling.utbetalingsinfo,
                avdoed = behandling.persongalleri.avdoed,
                erEtterbetalingMerEnnTreMaaneder = erEtterbetaling(behandling.utbetalingsinfo.virkningsdato),
                vedtaksdato =
                    behandling.vedtak.vedtaksdato
                        ?: throw IllegalStateException("Trenger vedtaksdato, men var null"),
                erInstitusjonsopphold =
                    behandling.utbetalingsinfo.beregningsperioder
                        .filter { it.datoFOM.isBefore(LocalDate.now().plusDays(1)) }
                        .firstOrNull { it.datoTOM.erIkkeFoer(LocalDate.now()) }
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
            behandling: Behandling,
            innhold: List<Slate.Element>,
        ): InnvilgetHovedmalBrevData {
            val etterbetalingDTO =
                if (erEtterbetaling(behandling.utbetalingsinfo.virkningsdato)) {
                    val beregningsperioder =
                        behandling.utbetalingsinfo.beregningsperioder
                            .filter { it.datoFOM.isBefore(behandling.utbetalingsinfo.virkningsdato) }
                            .map {
                                Etterbetalingsperiode(
                                    datoFOM = it.datoFOM, // TODO: Desse datoane er litt shady
                                    datoTOM = it.datoTOM,
                                    grunnbeloep = it.grunnbeloep,
                                    stoenadFoerReduksjon = it.utbetaltBeloep,
                                    // TODO: Trur stoenadFoerReduksjon skal bort frå brevmalen, slett da også herifrå
                                    utbetaltBeloep = it.utbetaltBeloep,
                                )
                            }
                    EtterbetalingDTO(
                        fraDato = behandling.utbetalingsinfo.beregningsperioder.map { it.datoFOM }.minOf { it },
                        tilDato = LocalDate.now(),
                        beregningsperioder = beregningsperioder,
                    )
                } else {
                    null
                }
            return InnvilgetHovedmalBrevData(
                utbetalingsinfo = behandling.utbetalingsinfo,
                avkortingsinfo = behandling.avkortingsinfo,
                etterbetalingDTO = etterbetalingDTO,
                innhold = innhold,
            )
        }
    }
}

private fun LocalDate?.erIkkeFoer(dato: LocalDate): Boolean {
    if (this == null) {
        return true
    }
    return this.isAfter(dato.minusDays(1))
}
