package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BarnepensjonBeregning
import no.nav.etterlatte.brev.model.BarnepensjonBeregningsperiode
import no.nav.etterlatte.brev.model.BarnepensjonEtterbetaling
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.BrevDataValidator.valider
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.EndringBrevData
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.TrygdetidMedBeregningsmetode
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.behandling.BarnepensjonSoeskenjusteringGrunn
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.YearMonth

data class BarnepensjonRevurderingDTO(
    val innhold: List<Slate.Element>,
    val erEndret: Boolean,
    val beregning: BarnepensjonBeregning,
    val etterbetaling: BarnepensjonEtterbetaling?,
    val brukerUnder18Aar: Boolean,
    val bosattUtland: Boolean,
    val kunNyttRegelverk: Boolean,
    val harFlereUtbetalingsperioder: Boolean,
) : EndringBrevData() {
    companion object {
        fun fra(
            innhold: InnholdMedVedlegg,
            utbetalingsinfo: Utbetalingsinfo,
            forrigeUtbetalingsinfo: Utbetalingsinfo?,
            etterbetaling: EtterbetalingDTO?,
            trygdetid: Trygdetid,
            grunnbeloep: Grunnbeloep,
            utlandstilknytning: UtlandstilknytningType?,
            brukerUnder18Aar: Boolean,
        ): BrevData {
            val beregningsperioder =
                utbetalingsinfo.beregningsperioder.map {
                    BarnepensjonBeregningsperiode(
                        datoFOM = it.datoFOM,
                        datoTOM = it.datoTOM,
                        grunnbeloep = it.grunnbeloep,
                        utbetaltBeloep = it.utbetaltBeloep,
                        antallBarn = it.antallBarn,
                    )
                }

            return BarnepensjonRevurderingDTO(
                innhold = innhold.innhold(),
                erEndret = forrigeUtbetalingsinfo == null || forrigeUtbetalingsinfo.beloep != utbetalingsinfo.beloep,
                beregning =
                    BarnepensjonBeregning(
                        innhold = innhold.finnVedlegg(BrevVedleggKey.BEREGNING_INNHOLD),
                        antallBarn = utbetalingsinfo.antallBarn,
                        virkningsdato = utbetalingsinfo.virkningsdato,
                        grunnbeloep = Kroner(grunnbeloep.grunnbeloep),
                        beregningsperioder = beregningsperioder,
                        sisteBeregningsperiode = beregningsperioder.maxByOrNull { it.datoFOM }!!,
                        trygdetid =
                            TrygdetidMedBeregningsmetode(
                                trygdetidsperioder = trygdetid.perioder,
                                beregnetTrygdetidAar = trygdetid.aarTrygdetid,
                                beregnetTrygdetidMaaneder = trygdetid.maanederTrygdetid,
                                prorataBroek = trygdetid.prorataBroek,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = trygdetid.mindreEnnFireFemtedelerAvOpptjeningstiden,
                                beregningsMetodeFraGrunnlag = utbetalingsinfo.beregningsperioder.first().beregningsMetodeFraGrunnlag,
                                beregningsMetodeAnvendt = utbetalingsinfo.beregningsperioder.first().beregningsMetodeAnvendt,
                            ),
                    ),
                etterbetaling =
                    etterbetaling?.let { dto ->
                        BarnepensjonEtterbetaling(
                            fraDato = dto.datoFom,
                            tilDato = dto.datoTom,
                            etterbetalingsperioder =
                                beregningsperioder
                                    .filter { YearMonth.from(it.datoFOM) <= YearMonth.from(dto.datoTom) },
                        )
                    },
                brukerUnder18Aar = brukerUnder18Aar,
                bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
                harFlereUtbetalingsperioder = utbetalingsinfo.beregningsperioder.size > 1,
                kunNyttRegelverk =
                    utbetalingsinfo.beregningsperioder.all {
                        it.datoFOM.isAfter(BarnepensjonInnvilgelseDTO.tidspunktNyttRegelverk) ||
                            it.datoFOM.isEqual(
                                BarnepensjonInnvilgelseDTO.tidspunktNyttRegelverk,
                            )
                    },
            )
        }
    }
}

data class BarnepensjonRevurderingRedigerbartUtfallDTO(
    val erEtterbetaling: Boolean,
) : EndringBrevData() {
    companion object {
        fun fra(etterbetaling: EtterbetalingDTO?): BrevData {
            return BarnepensjonRevurderingRedigerbartUtfallDTO(
                erEtterbetaling = etterbetaling != null,
            )
        }
    }
}

data class SoeskenjusteringRevurderingBrevdata(
    val utbetalingsinfo: Utbetalingsinfo,
    val grunnForJustering: BarnepensjonSoeskenjusteringGrunn,
) : EndringBrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            utbetalingsinfo: Utbetalingsinfo,
        ): SoeskenjusteringRevurderingBrevdata {
            val revurderingsinfo =
                valider<RevurderingInfo.Soeskenjustering>(
                    generellBrevData.revurderingsaarsak,
                    generellBrevData.forenkletVedtak?.revurderingInfo,
                    Revurderingaarsak.SOESKENJUSTERING,
                )

            return SoeskenjusteringRevurderingBrevdata(
                utbetalingsinfo = utbetalingsinfo,
                grunnForJustering = revurderingsinfo.grunnForSoeskenjustering,
            )
        }
    }
}
