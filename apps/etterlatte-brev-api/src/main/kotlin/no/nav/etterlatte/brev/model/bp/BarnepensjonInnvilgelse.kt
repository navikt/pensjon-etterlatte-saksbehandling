package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BarnepensjonBeregning
import no.nav.etterlatte.brev.model.BarnepensjonEtterbetaling
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.ManglerFrivilligSkattetrekk
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class BarnepensjonInnvilgelse(
    override val innhold: List<Slate.Element>,
    val beregning: BarnepensjonBeregning,
    val etterbetaling: BarnepensjonEtterbetaling?,
    val frivilligSkattetrekk: Boolean,
    val brukerUnder18Aar: Boolean,
    val bosattUtland: Boolean,
    val kunNyttRegelverk: Boolean,
    val erGjenoppretting: Boolean,
    val harUtbetaling: Boolean,
    val erMigrertYrkesskade: Boolean,
    val erSluttbehandling: Boolean,
) : BrevDataFerdigstilling {
    companion object {
        val tidspunktNyttRegelverk: LocalDate = LocalDate.of(2024, 1, 1)

        fun fra(
            innhold: InnholdMedVedlegg,
            avdoede: List<Avdoed>,
            utbetalingsinfo: Utbetalingsinfo,
            etterbetaling: EtterbetalingDTO?,
            trygdetid: List<TrygdetidDto>,
            grunnbeloep: Grunnbeloep,
            utlandstilknytning: UtlandstilknytningType?,
            brevutfall: BrevutfallDto,
            erGjenoppretting: Boolean,
            erMigrertYrkesskade: Boolean,
            erSluttbehandling: Boolean,
        ): BarnepensjonInnvilgelse {
            val frivilligSkattetrekk =
                brevutfall.frivilligSkattetrekk ?: etterbetaling?.frivilligSkattetrekk
                    ?: throw ManglerFrivilligSkattetrekk(brevutfall.behandlingId)

            return BarnepensjonInnvilgelse(
                innhold = innhold.innhold(),
                beregning =
                    barnepensjonBeregning(innhold, avdoede, utbetalingsinfo, grunnbeloep, trygdetid),
                bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
                brukerUnder18Aar = brevutfall.aldersgruppe == Aldersgruppe.UNDER_18,
                erGjenoppretting = erGjenoppretting,
                erMigrertYrkesskade = erMigrertYrkesskade,
                etterbetaling = etterbetaling?.let { dto -> Etterbetaling.fraBarnepensjonDTO(dto) },
                frivilligSkattetrekk = frivilligSkattetrekk,
                harUtbetaling = utbetalingsinfo.beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                kunNyttRegelverk =
                    utbetalingsinfo.beregningsperioder.all {
                        it.datoFOM.isAfter(tidspunktNyttRegelverk) || it.datoFOM.isEqual(tidspunktNyttRegelverk)
                    },
                erSluttbehandling = erSluttbehandling,
            )
        }
    }
}

data class BarnepensjonInnvilgelseRedigerbartUtfall(
    val virkningsdato: LocalDate,
    val avdoed: Avdoed?,
    val senereAvdoed: Avdoed?,
    val sisteBeregningsperiodeDatoFom: LocalDate,
    val sisteBeregningsperiodeBeloep: Kroner,
    val erEtterbetaling: Boolean,
    val harFlereUtbetalingsperioder: Boolean,
    val erGjenoppretting: Boolean,
    val harUtbetaling: Boolean,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            utbetalingsinfo: Utbetalingsinfo,
            etterbetaling: EtterbetalingDTO?,
            avdoede: List<Avdoed>,
            systemkilde: Vedtaksloesning,
        ): BarnepensjonInnvilgelseRedigerbartUtfall {
            val foersteAvdoed =
                avdoede.minByOrNull { it.doedsdato }

            val senereAvdoed = foersteAvdoed?.let { avdoede.find { it.fnr != foersteAvdoed.fnr } }

            return BarnepensjonInnvilgelseRedigerbartUtfall(
                virkningsdato = utbetalingsinfo.virkningsdato,
                avdoed = foersteAvdoed,
                senereAvdoed = senereAvdoed,
                sisteBeregningsperiodeDatoFom =
                    utbetalingsinfo.beregningsperioder.maxByOrNull { it.datoFOM }?.datoFOM
                        ?: throw UgyldigForespoerselException(
                            code = "INGEN_BEREGNINGSPERIODE_MED_FOM",
                            detail = "Ingen beregningsperiode med dato FOM",
                        ),
                sisteBeregningsperiodeBeloep =
                    utbetalingsinfo.beregningsperioder.maxByOrNull { it.datoFOM }?.utbetaltBeloep
                        ?: throw UgyldigForespoerselException(
                            code = "INTET_UTBETALT_BELOEP",
                            detail = "Intet utbetalt beløp i siste beregningsperiode",
                        ),
                erEtterbetaling = etterbetaling != null,
                harFlereUtbetalingsperioder = utbetalingsinfo.beregningsperioder.size > 1,
                erGjenoppretting = systemkilde == Vedtaksloesning.GJENOPPRETTA,
                harUtbetaling = utbetalingsinfo.beregningsperioder.any { it.utbetaltBeloep.value > 0 },
            )
        }
    }
}
