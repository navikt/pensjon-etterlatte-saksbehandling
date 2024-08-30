package no.nav.etterlatte.brev.model.bp

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Beregningsperiode
import no.nav.etterlatte.brev.beregningsperiode
import no.nav.etterlatte.brev.model.ForskjelligAvdoedPeriode
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class FlereAvdoedePerioderHelperKtTest {
    @Test
    fun `finnEventuellForskjelligAvdoedPeriode skal returnere null hvis vi har bare en beregningsperiode`() {
        val test =
            finnEventuellForskjelligAvdoedPeriode(
                mockk(),
                mockk {
                    every { beregningsperioder } returns
                        listOf(
                            mockk {
                                every { datoFOM } returns YearMonth.now().minusYears(1).atDay(1)
                                every { avdoedeForeldre } returns listOf(AVDOED_FOEDSELSNUMMER.value)
                            },
                        )
                },
            )
        Assertions.assertNull(test)
    }

    @Test
    fun `finnEventuellForskjelligAvdoedPeriode skal returnere null hvis vi har samme avdøde i alle perioder`() {
        val avdoedEn =
            Avdoed(
                fnr = Foedselsnummer(AVDOED_FOEDSELSNUMMER.value),
                navn = "Avdød 1",
                doedsdato = LocalDate.now().minusMonths(6),
            )
        val avdoedTo =
            Avdoed(
                fnr = Foedselsnummer(AVDOED2_FOEDSELSNUMMER.value),
                navn = "Avdød 2",
                doedsdato = LocalDate.now().minusMonths(6),
            )

        val perioder: List<Beregningsperiode> =
            listOf(
                beregningsperiode(
                    datoFOM = YearMonth.now().minusMonths(5).atDay(1),
                    datoTOM = YearMonth.now().atEndOfMonth(),
                    avdoedeForeldre = listOf(AVDOED2_FOEDSELSNUMMER.value, AVDOED_FOEDSELSNUMMER.value),
                ),
                beregningsperiode(
                    datoFOM = YearMonth.now().plusMonths(1).atDay(1),
                    datoTOM = null,
                    avdoedeForeldre = listOf(AVDOED_FOEDSELSNUMMER.value, AVDOED2_FOEDSELSNUMMER.value),
                ),
            )
        val forskjelligAvdoedPeriode =
            finnEventuellForskjelligAvdoedPeriode(
                listOf(avdoedEn, avdoedTo),
                mockk {
                    every {
                        beregningsperioder
                    } returns perioder
                },
            )
        Assertions.assertNull(forskjelligAvdoedPeriode)
    }

    @Test
    fun `finnEventuellForskjelligAvdoedPeriode skal returnere en periode hvis vi har en og så to avdøde`() {
        val avdoedEn =
            Avdoed(
                fnr = Foedselsnummer(AVDOED_FOEDSELSNUMMER.value),
                navn = "Avdød 1",
                doedsdato = LocalDate.now().minusMonths(6),
            )
        val avdoedTo =
            Avdoed(
                fnr = Foedselsnummer(AVDOED2_FOEDSELSNUMMER.value),
                navn = "Avdød 2",
                doedsdato = LocalDate.now(),
            )

        val perioder: List<Beregningsperiode> =
            listOf(
                beregningsperiode(
                    datoFOM = YearMonth.now().minusMonths(5).atDay(1),
                    datoTOM = YearMonth.now().atEndOfMonth(),
                    avdoedeForeldre = listOf(AVDOED_FOEDSELSNUMMER.value),
                ),
                beregningsperiode(
                    datoFOM = YearMonth.now().plusMonths(1).atDay(1),
                    datoTOM = null,
                    avdoedeForeldre = listOf(AVDOED_FOEDSELSNUMMER.value, AVDOED2_FOEDSELSNUMMER.value),
                ),
            )

        val forskjelligAvdoedPeriode =
            finnEventuellForskjelligAvdoedPeriode(
                listOf(avdoedEn, avdoedTo),
                mockk {
                    every {
                        beregningsperioder
                    } returns perioder
                },
            )
        val forventet =
            ForskjelligAvdoedPeriode(
                foersteAvdoed = avdoedEn,
                senereAvdoed = avdoedTo,
                senereVirkningsdato = perioder.last().datoFOM,
            )
        Assertions.assertEquals(forventet, forskjelligAvdoedPeriode)
    }

    @Test
    fun `finnEventuellForskjelligAvdoedPeriode kaster feil hvis vi har to og så en avdød`() {
        val avdoedEn =
            Avdoed(
                fnr = Foedselsnummer(AVDOED_FOEDSELSNUMMER.value),
                navn = "Avdød 1",
                doedsdato = LocalDate.now().minusMonths(6),
            )
        val avdoedTo =
            Avdoed(
                fnr = Foedselsnummer(AVDOED2_FOEDSELSNUMMER.value),
                navn = "Avdød 2",
                doedsdato = LocalDate.now(),
            )

        val perioder: List<Beregningsperiode> =
            listOf(
                beregningsperiode(
                    datoFOM = YearMonth.now().minusMonths(5).atDay(1),
                    datoTOM = YearMonth.now().atEndOfMonth(),
                    avdoedeForeldre = listOf(AVDOED_FOEDSELSNUMMER.value, AVDOED2_FOEDSELSNUMMER.value),
                ),
                beregningsperiode(
                    datoFOM = YearMonth.now().plusMonths(1).atDay(1),
                    datoTOM = null,
                    avdoedeForeldre = listOf(AVDOED_FOEDSELSNUMMER.value),
                ),
            )
        assertThrows<Exception> {
            finnEventuellForskjelligAvdoedPeriode(listOf(avdoedEn, avdoedTo), mockk { every { beregningsperioder } returns perioder })
        }
    }
}
