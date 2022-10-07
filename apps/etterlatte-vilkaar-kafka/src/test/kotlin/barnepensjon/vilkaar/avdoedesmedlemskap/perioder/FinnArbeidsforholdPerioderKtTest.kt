package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import LesVilkaarsmeldingTest.Companion.readFile
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.mockk
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregResponse
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.inntekt.InntektsOpplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class FinnArbeidsforholdPerioderKtTest {
    private fun grunnlag(arbeidOpplysning: String): AvdoedesMedlemskapGrunnlag {
        val arbeidsforhold =
            objectMapper.readValue<Opplysning.Periodisert<AaregResponse>>(readFile(arbeidOpplysning))
        val inntekt = mockk<VilkaarOpplysning<InntektsOpplysning>>()

        @Suppress("UNCHECKED_CAST")
        return AvdoedesMedlemskapGrunnlag(
            inntekt,
            arbeidsforhold as Opplysning.Periodisert<AaregResponse?>,
            null,
            LocalDate.parse("2022-07-01")
        )
    }

    @Test
    fun `Skal returnere en oppfyllt periode naar personen har arbeidet hele perioden`() {
        val grunnlag = grunnlag("/arbeidsforhold100.json")

        val vurdertePerioder = finnArbeidsforholdPerioder(grunnlag)

        assertEquals(1, vurdertePerioder.size)
        assertTrue(vurdertePerioder.first().godkjentPeriode)
    }

    @Test
    fun `Skal returnere en ikke-oppfyllt periode naar personen har arbeidet hele perioden under 80 prosent stilling`() {
        val grunnlag = grunnlag("/arbeidsforhold75.json")

        val vurdertePerioder = finnArbeidsforholdPerioder(grunnlag)

        assertEquals(1, vurdertePerioder.size)
        assertFalse(vurdertePerioder.first().godkjentPeriode)
    }

    @Test
    fun `Skal returnere to oppfyllte periode naar personen har gap i arbeidsforhold`() {
        val grunnlag = grunnlag("/arbeidsforholdMedOpphold.json")

        val vurdertePerioder = finnArbeidsforholdPerioder(grunnlag)

        assertEquals(2, vurdertePerioder.size)
        assertTrue(vurdertePerioder.all { it.godkjentPeriode })
    }

    @Test
    fun `Skal returnere en oppfyllt og en ikke oppfylt periode naar et av arbeidsforholdene er under 80 prosent`() {
        val grunnlag = grunnlag("/arbeidsforholdOverUnder80.json")

        val vurdertePerioder = finnArbeidsforholdPerioder(grunnlag)

        assertEquals(2, vurdertePerioder.size)
        assertEquals(1, vurdertePerioder.count { it.godkjentPeriode })
        assertEquals(1, vurdertePerioder.count { !it.godkjentPeriode })
    }
}