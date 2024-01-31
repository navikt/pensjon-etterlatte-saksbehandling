package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Beregningsperiode
import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BarnepensjonBeregningsperiode
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.pensjon.brevbaker.api.model.Kroner
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

internal class BarnepensjonInnvilgetDTOTest {
    private val grunnbeloep = 118_620
    private val grunnbeloepPerMaaned = 9885

    @Test
    fun `setter skille for etterbetaling`() {
        val barnepensjonInnvilgelse =
            BarnepensjonInnvilgelse.fra(
                utbetalingsinfo =
                    Utbetalingsinfo(
                        antallBarn = 1,
                        beloep = Kroner(1234),
                        virkningsdato = LocalDate.of(2023, Month.JANUARY, 1),
                        soeskenjustering = false,
                        beregningsperioder =
                            listOf(
                                beregningsperiodeJanuar2022(),
                                beregningsperiodeFebruar2022(),
                                beregningsperiodeMarsApril2022(),
                                beregningsperiodeAprilDesember2022(),
                                beregningsperiode2023OgUtover(),
                            ),
                    ),
                etterbetaling =
                    EtterbetalingDTO(
                        datoFom = LocalDate.of(2022, Month.JANUARY, 1),
                        datoTom = LocalDate.of(2022, Month.MARCH, 31),
                    ),
                trygdetid =
                    Trygdetid(
                        aarTrygdetid = 10,
                        maanederTrygdetid = 0,
                        perioder = listOf(),
                        overstyrt = false,
                        prorataBroek = null,
                        mindreEnnFireFemtedelerAvOpptjeningstiden = false,
                    ),
                grunnbeloep =
                    Grunnbeloep(
                        dato = YearMonth.of(2023, Month.JANUARY),
                        grunnbeloep = grunnbeloep,
                        grunnbeloepPerMaaned = grunnbeloepPerMaaned,
                        omregningsfaktor = null,
                    ),
                innhold = lagInnholdMedVedlegg(),
                utlandstilknytning = null,
                brevutfall = BrevutfallDto(UUID.randomUUID(), Aldersgruppe.UNDER_18, null, null),
            )

        Assertions.assertEquals(
            listOf(
                beregningsperiodeMars2022().toBarnepensjonBeregningsperiode(),
                beregningsperiodeFebruar2022().toBarnepensjonBeregningsperiode(),
                beregningsperiodeJanuar2022().toBarnepensjonBeregningsperiode(),
            ),
            barnepensjonInnvilgelse.etterbetaling!!.etterbetalingsperioder,
        )
        Assertions.assertEquals(
            listOf(
                beregningsperiodeJanuar2022().toBarnepensjonBeregningsperiode(),
                beregningsperiodeFebruar2022().toBarnepensjonBeregningsperiode(),
                beregningsperiodeMarsApril2022().toBarnepensjonBeregningsperiode(),
                beregningsperiodeAprilDesember2022().toBarnepensjonBeregningsperiode(),
                beregningsperiode2023OgUtover().toBarnepensjonBeregningsperiode(),
            ),
            barnepensjonInnvilgelse.beregning.beregningsperioder,
        )
    }

    private fun beregningsperiodeJanuar2022() =
        beregningsperiode(
            LocalDate.of(2022, Month.JANUARY, 1),
            LocalDate.of(2022, Month.JANUARY, 31),
        )

    private fun beregningsperiodeFebruar2022() =
        beregningsperiode(
            LocalDate.of(2022, Month.FEBRUARY, 1),
            LocalDate.of(2022, Month.FEBRUARY, 28),
        )

    private fun beregningsperiodeMarsApril2022() =
        beregningsperiode(
            LocalDate.of(2022, Month.MARCH, 1),
            LocalDate.of(2022, Month.APRIL, 30),
        )

    private fun beregningsperiodeMars2022() =
        beregningsperiode(
            LocalDate.of(2022, Month.MARCH, 1),
            LocalDate.of(2022, Month.MARCH, 31),
        )

    private fun beregningsperiodeAprilDesember2022() =
        beregningsperiode(
            LocalDate.of(2022, Month.APRIL, 1),
            LocalDate.of(2022, Month.DECEMBER, 31),
        )

    private fun beregningsperiode2023OgUtover() =
        beregningsperiode(
            datoFOM = LocalDate.of(2023, Month.JANUARY, 1),
            datoTOM = null,
        )

    private fun beregningsperiode(
        datoFOM: LocalDate,
        datoTOM: LocalDate?,
    ) = Beregningsperiode(
        datoFOM = datoFOM,
        datoTOM = datoTOM,
        grunnbeloep = Kroner(grunnbeloep),
        antallBarn = 1,
        utbetaltBeloep = Kroner(grunnbeloepPerMaaned),
        trygdetid = 10,
        prorataBroek = null,
        institusjon = false,
        beregningsMetodeAnvendt = BeregningsMetode.NASJONAL,
        beregningsMetodeFraGrunnlag = BeregningsMetode.BEST,
    )

    private fun lagInnholdMedVedlegg() =
        InnholdMedVedlegg({ listOf() }, {
            listOf(
                BrevInnholdVedlegg(
                    tittel = "Vedlegget",
                    key = BrevVedleggKey.BP_BEREGNING_TRYGDETID,
                    payload = Slate(),
                ),
            )
        })

    private fun Beregningsperiode.toBarnepensjonBeregningsperiode() =
        BarnepensjonBeregningsperiode(
            datoFOM = datoFOM,
            datoTOM = datoTOM,
            grunnbeloep = grunnbeloep,
            antallBarn = 1,
            utbetaltBeloep = utbetaltBeloep,
        )
}
