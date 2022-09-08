package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import LesVilkaarsmeldingTest.Companion.readFile
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.mockk
import no.nav.etterlatte.libs.common.arbeidsforhold.ArbeidsforholdOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.inntekt.InntektsOpplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class FinnPensjonEllerTrygdePerioderKtTest {
    private fun grunnlag(arbeidOpplysning: String): AvdoedesMedlemskapGrunnlag {
        val inntekter =
            objectMapper.readValue<VilkaarOpplysning<InntektsOpplysning>>(readFile(arbeidOpplysning))
        val arbeidsforhold = mockk<VilkaarOpplysning<ArbeidsforholdOpplysning>>()

        return AvdoedesMedlemskapGrunnlag(
            inntekter,
            arbeidsforhold,
            null,
            LocalDate.parse("2022-07-01")
        )
    }

    @Test
    fun `Skal returnere med en kombienert periode naar bruker har sammenhengende utbetalinger`() {
        val grunnlag = grunnlag("/inntektsopplysning.json")

        val vurdertePerioder = finnPensjonEllerTrygdePerioder(grunnlag, PeriodeType.ALDERSPENSJON, "alderspensjon")

        assertEquals(1, vurdertePerioder.size)
        vurdertePerioder.first().let {
            assertTrue(it.godkjentPeriode)
            assertEquals(PeriodeType.ALDERSPENSJON, it.periodeType)
            assertEquals(it.fraDato, LocalDate.of(2017, 7, 1))
            assertEquals(it.tilDato, LocalDate.of(2022, 6, 30))
        }
    }

    @Test
    fun `Skal returnere med to perioder naar bruker har opphold i utbetalinger`() {
        val grunnlag = grunnlag("/inntektsopplysningOpphold.json")

        val vurdertePerioder = finnPensjonEllerTrygdePerioder(grunnlag, PeriodeType.ALDERSPENSJON, "alderspensjon")

        assertEquals(2, vurdertePerioder.size)
    }
}