package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Beregningsperiode
import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.pensjon.brevbaker.api.model.Kroner
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

internal class InnvilgetHovedmalBrevDataTest {
    private val grunnbeloep = 118_620
    private val grunnbeloepPerMaaned = 9885

    @Test
    fun `setter skille for etterbetaling`() {
        val brevdata =
            InnvilgetHovedmalBrevData.fra(
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
                avkortingsinfo = null,
                etterbetalingDTO =
                    EtterbetalingDTO(
                        fraDato = LocalDate.of(2022, Month.JANUARY, 1),
                        tilDato = LocalDate.of(2022, Month.MARCH, 30),
                    ),
                trygdetid =
                    Trygdetid(
                        aarTrygdetid = 10,
                        maanederTrygdetid = 0,
                        perioder = listOf(),
                        overstyrt = false,
                    ),
                grunnbeloep =
                    Grunnbeloep(
                        dato = YearMonth.of(2023, Month.JANUARY),
                        grunnbeloep = grunnbeloep,
                        grunnbeloepPerMaaned = grunnbeloepPerMaaned,
                        omregningsfaktor = null,
                    ),
                innhold = lagInnholdMedVedlegg(),
            )

        Assertions.assertEquals(
            brevdata.etterbetaling!!.etterbetalingsperioder,
            listOf(
                beregningsperiodeJanuar2022(),
                beregningsperiodeFebruar2022(),
                beregningsperiodeMarsApril2022(),
            ),
        )
        Assertions.assertEquals(
            brevdata.utbetalingsinfo.beregningsperioder,
            listOf(
                beregningsperiodeAprilDesember2022(),
                beregningsperiode2023OgUtover(),
            ),
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
        institusjon = false,
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
}
