package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BarnepensjonBeregning
import no.nav.etterlatte.brev.model.BarnepensjonEtterbetaling
import no.nav.etterlatte.brev.model.BrevDataFerdigstilling
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.ForskjelligAvdoedPeriode
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.ManglerFrivilligSkattetrekk
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class BarnepensjonInnvilgelseForeldreloes(
    override val innhold: List<Slate.Element>,
    val beregning: BarnepensjonBeregning,
    val etterbetaling: BarnepensjonEtterbetaling?,
    val brukerUnder18Aar: Boolean,
    val frivilligSkattetrekk: Boolean,
    val bosattUtland: Boolean,
    val kunNyttRegelverk: Boolean,
    val harUtbetaling: Boolean,
    val erGjenoppretting: Boolean,
    val vedtattIPesys: Boolean,
    val erMigrertYrkesskade: Boolean,
) : BrevDataFerdigstilling {
    companion object {
        val tidspunktNyttRegelverk: LocalDate = LocalDate.of(2024, 1, 1)

        fun fra(
            innhold: InnholdMedVedlegg,
            utbetalingsinfo: Utbetalingsinfo,
            etterbetaling: EtterbetalingDTO?,
            trygdetid: List<TrygdetidDto>,
            grunnbeloep: Grunnbeloep,
            utlandstilknytning: UtlandstilknytningType?,
            brevutfall: BrevutfallDto,
            vedtattIPesys: Boolean,
            avdoede: List<Avdoed>,
            erGjenoppretting: Boolean,
            erMigrertYrkesskade: Boolean,
        ): BarnepensjonInnvilgelseForeldreloes {
            val beregningsperioder =
                barnepensjonBeregningsperioder(utbetalingsinfo)

            return BarnepensjonInnvilgelseForeldreloes(
                innhold = innhold.innhold(),
                beregning =
                    barnepensjonBeregning(
                        innhold,
                        avdoede,
                        utbetalingsinfo,
                        grunnbeloep,
                        beregningsperioder,
                        trygdetid,
                        erForeldreloes = true,
                    ),
                bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
                brukerUnder18Aar = brevutfall.aldersgruppe == Aldersgruppe.UNDER_18,
                erGjenoppretting = erGjenoppretting,
                erMigrertYrkesskade = erMigrertYrkesskade,
                etterbetaling = etterbetaling?.let { dto -> Etterbetaling.fraBarnepensjonDTO(dto) },
                frivilligSkattetrekk =
                    brevutfall.frivilligSkattetrekk ?: throw ManglerFrivilligSkattetrekk(brevutfall.behandlingId),
                harUtbetaling = utbetalingsinfo.beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                kunNyttRegelverk =
                    utbetalingsinfo.beregningsperioder.all {
                        it.datoFOM.isAfter(tidspunktNyttRegelverk) || it.datoFOM.isEqual(tidspunktNyttRegelverk)
                    },
                vedtattIPesys = vedtattIPesys,
            )
        }
    }
}

data class BarnepensjonForeldreloesRedigerbar(
    val virkningsdato: LocalDate,
    val sisteBeregningsperiodeBeloep: Kroner,
    val sisteBeregningsperiodeDatoFom: LocalDate,
    val erEtterbetaling: Boolean,
    val flerePerioder: Boolean,
    val harUtbetaling: Boolean,
    val erGjenoppretting: Boolean,
    val vedtattIPesys: Boolean,
    val forskjelligAvdoedPeriode: ForskjelligAvdoedPeriode?,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            etterbetaling: EtterbetalingDTO?,
            utbetalingsinfo: Utbetalingsinfo,
            avdoede: List<Avdoed>,
            vedtaksloesning: Vedtaksloesning,
            loependeIPesys: Boolean,
        ): BarnepensjonForeldreloesRedigerbar {
            val beregningsperioder = barnepensjonBeregningsperioder(utbetalingsinfo)

            val forskjelligAvdoedPeriode = finnEventuellForskjelligAvdoedPeriode(avdoede, utbetalingsinfo)

            return BarnepensjonForeldreloesRedigerbar(
                virkningsdato = utbetalingsinfo.virkningsdato,
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
                flerePerioder = utbetalingsinfo.beregningsperioder.size > 1,
                harUtbetaling = beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                erGjenoppretting = vedtaksloesning == Vedtaksloesning.GJENOPPRETTA,
                vedtattIPesys = loependeIPesys,
                forskjelligAvdoedPeriode = forskjelligAvdoedPeriode,
            )
        }
    }
}
