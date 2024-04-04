package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BarnepensjonBeregning
import no.nav.etterlatte.brev.model.BarnepensjonBeregningsperiode
import no.nav.etterlatte.brev.model.BarnepensjonEtterbetaling
import no.nav.etterlatte.brev.model.BrevDataFerdigstilling
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.TrygdetidMedBeregningsmetode
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class BarnepensjonInnvilgelse(
    override val innhold: List<Slate.Element>,
    val beregning: BarnepensjonBeregning,
    val etterbetaling: BarnepensjonEtterbetaling?,
    val brukerUnder18Aar: Boolean,
    val bosattUtland: Boolean,
    val kunNyttRegelverk: Boolean,
    val erGjenoppretting: Boolean,
    val harUtbetaling: Boolean,
) : BrevDataFerdigstilling {
    companion object {
        val tidspunktNyttRegelverk: LocalDate = LocalDate.of(2024, 1, 1)

        fun fra(
            innhold: InnholdMedVedlegg,
            utbetalingsinfo: Utbetalingsinfo,
            etterbetaling: EtterbetalingDTO?,
            trygdetid: Trygdetid,
            grunnbeloep: Grunnbeloep,
            utlandstilknytning: UtlandstilknytningType?,
            brevutfall: BrevutfallDto,
            erGjenoppretting: Boolean,
        ): BarnepensjonInnvilgelse {
            val beregningsperioder =
                barnepensjonBeregningsperioder(utbetalingsinfo)

            return BarnepensjonInnvilgelse(
                innhold = innhold.innhold(),
                beregning = barnepensjonBeregning(innhold, utbetalingsinfo, grunnbeloep, beregningsperioder, trygdetid),
                etterbetaling =
                    etterbetaling
                        ?.let { dto -> Etterbetaling.fraBarnepensjonBeregningsperioder(dto, beregningsperioder) },
                brukerUnder18Aar = brevutfall.aldersgruppe == Aldersgruppe.UNDER_18,
                bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
                kunNyttRegelverk =
                    utbetalingsinfo.beregningsperioder.all {
                        it.datoFOM.isAfter(tidspunktNyttRegelverk) || it.datoFOM.isEqual(tidspunktNyttRegelverk)
                    },
                erGjenoppretting = erGjenoppretting,
                harUtbetaling = beregningsperioder.any { it.utbetaltBeloep.value > 0 },
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
    val erGjenoppretting: Boolean,
) : BrevDataRedigerbar {
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
                avdoed =
                    generellBrevData.personerISak.avdoede.minByOrNull { it.doedsdato }
                        ?: throw UgyldigForespoerselException(
                            code = "AVDOED_MED_DOEDSDATO_MANGLER",
                            detail = "Ingen avdød med dødsdato",
                        ),
                sisteBeregningsperiodeDatoFom =
                    beregningsperioder.maxByOrNull { it.datoFOM }?.datoFOM
                        ?: throw UgyldigForespoerselException(
                            code = "INGEN_BEREGNINGSPERIODE_MED_FOM",
                            detail = "Ingen beregningsperiode med dato FOM",
                        ),
                sisteBeregningsperiodeBeloep =
                    beregningsperioder.maxByOrNull { it.datoFOM }?.utbetaltBeloep
                        ?: throw UgyldigForespoerselException(
                            code = "INTET_UTBETALT_BELOEP",
                            detail = "Intet utbetalt beløp i siste beregningsperiode",
                        ),
                erEtterbetaling = etterbetaling != null,
                harFlereUtbetalingsperioder = utbetalingsinfo.beregningsperioder.size > 1,
                erGjenoppretting = generellBrevData.systemkilde == Vedtaksloesning.GJENOPPRETTA,
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
    erForeldreloes: Boolean = false,
    bruktAvdoed: String? = null,
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
    erForeldreloes = erForeldreloes,
    bruktAvdoed = bruktAvdoed,
)

internal fun barnepensjonBeregningsperioder(utbetalingsinfo: Utbetalingsinfo) =
    utbetalingsinfo.beregningsperioder.map {
        BarnepensjonBeregningsperiode(
            datoFOM = it.datoFOM,
            datoTOM = it.datoTOM,
            grunnbeloep = it.grunnbeloep,
            utbetaltBeloep = it.utbetaltBeloep,
            antallBarn = it.antallBarn,
        )
    }
