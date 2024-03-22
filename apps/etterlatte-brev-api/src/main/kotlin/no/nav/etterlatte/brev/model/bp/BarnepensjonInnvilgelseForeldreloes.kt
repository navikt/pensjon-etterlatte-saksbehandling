package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BarnepensjonBeregning
import no.nav.etterlatte.brev.model.BarnepensjonEtterbetaling
import no.nav.etterlatte.brev.model.BrevDataFerdigstilling
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import java.time.LocalDate

data class BarnepensjonInnvilgelseForeldreloes(
    override val innhold: List<Slate.Element>,
    val beregning: BarnepensjonBeregning,
    val etterbetaling: BarnepensjonEtterbetaling?,
    val brukerUnder18Aar: Boolean,
    val bosattUtland: Boolean,
    val kunNyttRegelverk: Boolean,
    val bareEnPeriode: Boolean,
    val flerePerioder: Boolean,
    val ingenUtbetaling: Boolean,
    val vedtattIPesys: Boolean,
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
            vedtattIPesys: Boolean,
            avdoede: List<Avdoed>,
        ): BarnepensjonInnvilgelseForeldreloes {
            val beregningsperioder =
                barnepensjonBeregningsperioder(utbetalingsinfo)

            return BarnepensjonInnvilgelseForeldreloes(
                innhold = innhold.innhold(),
                beregning =
                    barnepensjonBeregning(
                        innhold,
                        utbetalingsinfo,
                        grunnbeloep,
                        beregningsperioder,
                        trygdetid,
                        erForeldreloes = true,
                        bruktAvdoed =
                            avdoede.find {
                                val fnr =
                                    utbetalingsinfo.beregningsperioder.last().trygdetidForIdent
                                        ?: throw ManglerAvdoedBruktTilTrygdetidExceoption()
                                it.fnr.value == fnr
                            }?.navn ?: throw ManglerAvdoedBruktTilTrygdetidExceoption(),
                    ),
                etterbetaling =
                    etterbetaling
                        ?.let { dto -> Etterbetaling.fraBarnepensjonBeregningsperioder(dto, beregningsperioder) },
                brukerUnder18Aar = brevutfall.aldersgruppe == Aldersgruppe.UNDER_18,
                bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
                kunNyttRegelverk =
                    utbetalingsinfo.beregningsperioder.all {
                        it.datoFOM.isAfter(tidspunktNyttRegelverk) || it.datoFOM.isEqual(tidspunktNyttRegelverk)
                    },
                bareEnPeriode = utbetalingsinfo.beregningsperioder.size < 2,
                flerePerioder = utbetalingsinfo.beregningsperioder.size > 1,
                ingenUtbetaling = utbetalingsinfo.beregningsperioder.none { it.utbetaltBeloep.value > 0 },
                vedtattIPesys = vedtattIPesys,
            )
        }
    }
}

data class BarnepensjonForeldreloesRedigerbar(
    val erEtterbetaling: Boolean,
    val vedtattIPesys: Boolean,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            etterbetaling: EtterbetalingDTO?,
        ): BarnepensjonForeldreloesRedigerbar =
            BarnepensjonForeldreloesRedigerbar(
                erEtterbetaling = etterbetaling != null,
                vedtattIPesys = generellBrevData.loependeIPesys(),
            )
    }
}

class ManglerAvdoedBruktTilTrygdetidExceoption : UgyldigForespoerselException(
    code = "MANGLER_AVDOED_INFO_I_BEREGNING",
    detail = "Det er mangler i beregning. Utfør beregning på nytt og prøv igjen.",
)
