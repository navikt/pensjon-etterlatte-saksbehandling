package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BarnepensjonBeregning
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.ForskjelligAvdoedPeriode
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.kodeverk.LandDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class BarnepensjonInnvilgelseForeldreloes(
    override val innhold: List<Slate.Element>,
    val beregning: BarnepensjonBeregning,
    val brukerUnder18Aar: Boolean,
    val frivilligSkattetrekk: Boolean,
    val bosattUtland: Boolean,
    val kunNyttRegelverk: Boolean,
    val harUtbetaling: Boolean,
    val erGjenoppretting: Boolean,
    val vedtattIPesys: Boolean,
    val erMigrertYrkesskade: Boolean,
    val erSluttbehandling: Boolean,
    val erEtterbetaling: Boolean,
    val datoVedtakOmgjoering: LocalDate?,
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
            erSluttbehandling: Boolean,
            landKodeverk: List<LandDto>,
            klage: Klage?,
        ): BarnepensjonInnvilgelseForeldreloes =
            BarnepensjonInnvilgelseForeldreloes(
                innhold = innhold.innhold(),
                beregning =
                    barnepensjonBeregning(
                        innhold,
                        avdoede,
                        utbetalingsinfo,
                        grunnbeloep,
                        trygdetid,
                        erForeldreloes = true,
                        landKodeverk,
                    ),
                bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
                brukerUnder18Aar = brevutfall.aldersgruppe == Aldersgruppe.UNDER_18,
                erGjenoppretting = erGjenoppretting,
                erMigrertYrkesskade = erMigrertYrkesskade,
                frivilligSkattetrekk = brevutfall.frivilligSkattetrekk ?: false,
                harUtbetaling = utbetalingsinfo.beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                kunNyttRegelverk =
                    utbetalingsinfo.beregningsperioder.all {
                        it.datoFOM.isAfter(tidspunktNyttRegelverk) || it.datoFOM.isEqual(tidspunktNyttRegelverk)
                    },
                vedtattIPesys = vedtattIPesys,
                erSluttbehandling = erSluttbehandling,
                erEtterbetaling = etterbetaling != null,
                datoVedtakOmgjoering = klage?.datoVedtakOmgjoering(),
            )
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
            val forskjelligAvdoedPeriode = finnEventuellForskjelligAvdoedPeriode(avdoede, utbetalingsinfo)

            return BarnepensjonForeldreloesRedigerbar(
                virkningsdato = utbetalingsinfo.virkningsdato,
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
                flerePerioder = utbetalingsinfo.beregningsperioder.size > 1,
                harUtbetaling = utbetalingsinfo.beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                erGjenoppretting = vedtaksloesning == Vedtaksloesning.GJENOPPRETTA,
                vedtattIPesys = loependeIPesys,
                forskjelligAvdoedPeriode = forskjelligAvdoedPeriode,
            )
        }
    }
}
