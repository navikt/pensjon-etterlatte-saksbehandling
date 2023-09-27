package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.AvslagBrevData.valider
import no.nav.etterlatte.libs.common.behandling.BarnepensjonSoeskenjusteringGrunn
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

abstract class EndringBrevData : BrevData()

data class EtterbetalingDTO(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val beregningsperioder: List<Etterbetalingsperiode>,
)

data class Etterbetalingsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val grunnbeloep: Kroner,
    val stoenadFoerReduksjon: Kroner,
    var utbetaltBeloep: Kroner,
)

data class EndringHovedmalBrevData(
    val erEndret: Boolean,
    val etterbetaling: EtterbetalingDTO,
    val utbetalingsinfo: Utbetalingsinfo,
    val innhold: List<Slate.Element>,
) : EndringBrevData() {
    companion object {
        fun fra(
            behandling: Behandling,
            innhold: List<Slate.Element>,
        ): BrevData =
            EndringHovedmalBrevData(
                erEndret = true, // TODO når resten av fengselsopphold implementerast
                etterbetaling =
                    EtterbetalingDTO(
                        fraDato = LocalDate.now(), // TODO når resten av fengselsopphold implementerast
                        tilDato = LocalDate.now(), // TODO når resten av fengselsopphold implementerast
                        beregningsperioder =
                            behandling.utbetalingsinfo.beregningsperioder.map {
                                Etterbetalingsperiode(
                                    datoFOM = it.datoFOM,
                                    datoTOM = it.datoTOM,
                                    grunnbeloep = it.grunnbeloep,
                                    stoenadFoerReduksjon = it.utbetaltBeloep, // TODO når resten av fengselsopphold implementerast
                                    utbetaltBeloep = it.utbetaltBeloep,
                                )
                            },
                    ),
                utbetalingsinfo = behandling.utbetalingsinfo,
                innhold = innhold,
            )
    }
}

data class SoeskenjusteringRevurderingBrevdata(
    val utbetalingsinfo: Utbetalingsinfo,
    val grunnForJustering: BarnepensjonSoeskenjusteringGrunn,
) : EndringBrevData() {
    companion object {
        fun fra(behandling: Behandling): SoeskenjusteringRevurderingBrevdata {
            val revurderingsinfo =
                valider<RevurderingInfo.Soeskenjustering>(
                    behandling,
                    RevurderingAarsak.SOESKENJUSTERING,
                )

            return SoeskenjusteringRevurderingBrevdata(
                utbetalingsinfo = behandling.utbetalingsinfo,
                grunnForJustering = revurderingsinfo.grunnForSoeskenjustering,
            )
        }
    }
}

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
