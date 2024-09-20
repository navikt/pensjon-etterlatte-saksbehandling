package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BarnepensjonBeregning
import no.nav.etterlatte.brev.model.BarnepensjonBeregningsperiode
import no.nav.etterlatte.brev.model.BarnepensjonEtterbetaling
import no.nav.etterlatte.brev.model.BrevDataFerdigstilling
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
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
        ): BarnepensjonInnvilgelse {
            val beregningsperioder = barnepensjonBeregningsperioder(utbetalingsinfo)
            return BarnepensjonInnvilgelse(
                innhold = innhold.innhold(),
                beregning =
                    barnepensjonBeregning(innhold, avdoede, utbetalingsinfo, grunnbeloep, beregningsperioder, trygdetid),
                bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
                brukerUnder18Aar = brevutfall.aldersgruppe == Aldersgruppe.UNDER_18,
                erGjenoppretting = erGjenoppretting,
                erMigrertYrkesskade = erMigrertYrkesskade,
                etterbetaling = etterbetaling?.let { dto -> Etterbetaling.fraBarnepensjonDTO(dto) },
                frivilligSkattetrekk =
                    brevutfall.frivilligSkattetrekk
                        ?: throw InternfeilException(
                            "Behandling ${brevutfall.behandlingId} mangler informasjon om frivillig skattetrekk, som er påkrevd for barnepensjon. Du kan legge til dette i Valg av utfall i brev.",
                        ),
                harUtbetaling = beregningsperioder.any { it.utbetaltBeloep.value > 0 },
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
            val beregningsperioder =
                utbetalingsinfo.beregningsperioder.map {
                    BarnepensjonBeregningsperiode.fra(it)
                }

            val foersteAvdoed =
                avdoede.minByOrNull { it.doedsdato }
                    ?: throw UgyldigForespoerselException(
                        code = "AVDOED_MED_DOEDSDATO_MANGLER",
                        detail = "Ingen avdød med dødsdato",
                    )
            val senereAvdoed = avdoede.find { it.fnr != foersteAvdoed.fnr }

            return BarnepensjonInnvilgelseRedigerbartUtfall(
                virkningsdato = utbetalingsinfo.virkningsdato,
                avdoed = foersteAvdoed,
                senereAvdoed = senereAvdoed,
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
                erGjenoppretting = systemkilde == Vedtaksloesning.GJENOPPRETTA,
                harUtbetaling = beregningsperioder.any { it.utbetaltBeloep.value > 0 },
            )
        }
    }
}
