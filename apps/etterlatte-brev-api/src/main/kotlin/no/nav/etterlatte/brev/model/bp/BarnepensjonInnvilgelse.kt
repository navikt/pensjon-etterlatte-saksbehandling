package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BarnepensjonBeregning
import no.nav.etterlatte.brev.model.BarnepensjonBeregningsperiode
import no.nav.etterlatte.brev.model.BarnepensjonEtterbetaling
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.TrygdetidMedBeregningsmetode
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class BarnepensjonInnvilgelse(
    val innhold: List<Slate.Element>,
    val beregning: BarnepensjonBeregning,
    val etterbetaling: BarnepensjonEtterbetaling?,
    val brukerUnder18Aar: Boolean,
    val bosattUtland: Boolean,
    val kunNyttRegelverk: Boolean,
) : BrevData() {
    companion object {
        val tidspunktNyttRegelverk: LocalDate = LocalDate.of(2024, 1, 1)

        fun fra(
            utbetalingsinfo: Utbetalingsinfo,
            etterbetaling: EtterbetalingDTO?,
            trygdetid: Trygdetid,
            grunnbeloep: Grunnbeloep,
            utlandstilknytning: UtlandstilknytningType?,
            innhold: InnholdMedVedlegg,
            brukerUnder18Aar: Boolean,
        ): BarnepensjonInnvilgelse {
            val beregningsperioder =
                barnepensjonBeregningsperiodes(utbetalingsinfo)

            return BarnepensjonInnvilgelse(
                innhold = innhold.innhold(),
                beregning = barnepensjonBeregning(innhold, utbetalingsinfo, grunnbeloep, beregningsperioder, trygdetid),
                etterbetaling =
                    etterbetaling
                        ?.let { dto -> Etterbetaling.fraBarnepensjonBeregningsperioder(dto, beregningsperioder) },
                brukerUnder18Aar = brukerUnder18Aar,
                bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
                kunNyttRegelverk =
                    utbetalingsinfo.beregningsperioder.all {
                        it.datoFOM.isAfter(tidspunktNyttRegelverk) || it.datoFOM.isEqual(tidspunktNyttRegelverk)
                    },
            )
        }
    }
}

data class BarnepensjonInnvilgelseRedigerbartUtfall(
    val virkningsdato: LocalDate,
    val avdoed: Avdoed,
    val sisteBeregningsperiodeDatoFom: LocalDate,
    val sisteBeregningsperiodeBeloep: Kroner,
    val erEtterbetaling: Boolean,
    val harFlereUtbetalingsperioder: Boolean,
) : BrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            utbetalingsinfo: Utbetalingsinfo,
            etterbetaling: EtterbetalingDTO?,
        ): BarnepensjonInnvilgelseRedigerbartUtfall {
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

            return BarnepensjonInnvilgelseRedigerbartUtfall(
                virkningsdato = utbetalingsinfo.virkningsdato,
                avdoed = generellBrevData.personerISak.avdoede.minBy { it.doedsdato },
                sisteBeregningsperiodeDatoFom = beregningsperioder.maxBy { it.datoFOM }.datoFOM,
                sisteBeregningsperiodeBeloep = beregningsperioder.maxBy { it.datoFOM }.utbetaltBeloep,
                erEtterbetaling = etterbetaling != null,
                harFlereUtbetalingsperioder = utbetalingsinfo.beregningsperioder.size > 1,
            )
        }
    }
}

internal fun barnepensjonBeregning(
    innhold: InnholdMedVedlegg,
    utbetalingsinfo: Utbetalingsinfo,
    grunnbeloep: Grunnbeloep,
    beregningsperioder: List<BarnepensjonBeregningsperiode>,
    trygdetid: Trygdetid,
) = BarnepensjonBeregning(
    innhold = innhold.finnVedlegg(BrevVedleggKey.BP_BEREGNING_TRYGDETID),
    antallBarn = utbetalingsinfo.antallBarn,
    virkningsdato = utbetalingsinfo.virkningsdato,
    grunnbeloep = Kroner(grunnbeloep.grunnbeloep),
    beregningsperioder = beregningsperioder,
    sisteBeregningsperiode = beregningsperioder.maxBy { it.datoFOM },
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
)

internal fun barnepensjonBeregningsperiodes(utbetalingsinfo: Utbetalingsinfo) =
    utbetalingsinfo.beregningsperioder.map {
        BarnepensjonBeregningsperiode(
            datoFOM = it.datoFOM,
            datoTOM = it.datoTOM,
            grunnbeloep = it.grunnbeloep,
            utbetaltBeloep = it.utbetaltBeloep,
            antallBarn = it.antallBarn,
        )
    }
