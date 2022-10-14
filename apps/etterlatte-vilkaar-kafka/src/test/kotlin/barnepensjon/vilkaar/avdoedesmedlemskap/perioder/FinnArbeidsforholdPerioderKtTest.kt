package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import GrunnlagTestData
import LesVilkaarsmeldingTest.Companion.readFile
import com.fasterxml.jackson.module.kotlin.readValue
import grunnlag.arbeidsforholdTestData
import io.mockk.mockk
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregResponse
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.hentArbeidsforhold
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.inntekt.InntektsOpplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class FinnArbeidsforholdPerioderKtTest {
    private fun grunnlag(arbeidOpplysning: Opplysning.Periodisert<AaregResponse?>): AvdoedesMedlemskapGrunnlag {
        val inntekt = mockk<VilkaarOpplysning<InntektsOpplysning>>()

        return AvdoedesMedlemskapGrunnlag(
            inntekt,
            arbeidOpplysning,
            LocalDate.parse("2022-07-01")
        )
    }

    @Test
    fun `Skal returnere en oppfyllt periode naar personen har arbeidet hele perioden`() {
        val arbeidsforhold = GrunnlagTestData(
            opplysningsmapAvdoedOverrides = mapOf(
                Opplysningstype.ARBEIDSFORHOLD to Opplysning.Periodisert(arbeidsforholdTestData(100.0))
            )
        ).hentOpplysningsgrunnlag().hentAvdoed().hentArbeidsforhold()!!

        val grunnlag = grunnlag(arbeidsforhold)

        val vurdertePerioder = finnArbeidsforholdPerioder(grunnlag)

        assertEquals(1, vurdertePerioder.size)
        assertTrue(vurdertePerioder.first().godkjentPeriode)
    }

    @Test
    fun `Skal returnere en ikke-oppfyllt periode naar personen har arbeidet hele perioden under 80 prosent stilling`() {
        val arbeidsforhold = GrunnlagTestData(
            opplysningsmapAvdoedOverrides = mapOf(
                Opplysningstype.ARBEIDSFORHOLD to Opplysning.Periodisert(arbeidsforholdTestData(75.0))
            )
        ).hentOpplysningsgrunnlag().hentAvdoed().hentArbeidsforhold()!!

        val grunnlag = grunnlag(arbeidsforhold)

        val vurdertePerioder = finnArbeidsforholdPerioder(grunnlag)

        assertEquals(1, vurdertePerioder.size)
        assertFalse(vurdertePerioder.first().godkjentPeriode)
    }

    @Test
    fun `Skal returnere to oppfyllte periode naar personen har gap i arbeidsforhold`() {
        val arbeidsforhold = objectMapper.readValue<Opplysning.Periodisert<AaregResponse?>>(
            readFile("/arbeidsforholdMedOpphold.json")
        )
        val grunnlag = grunnlag(arbeidsforhold)

        val vurdertePerioder = finnArbeidsforholdPerioder(grunnlag)

        assertEquals(2, vurdertePerioder.size)
        assertTrue(vurdertePerioder.all { it.godkjentPeriode })
    }

    @Test
    fun `Skal returnere en oppfyllt og en ikke oppfylt periode naar et av arbeidsforholdene er under 80 prosent`() {
        val arbeidsforhold = objectMapper.readValue<Opplysning.Periodisert<AaregResponse?>>(
            readFile("/arbeidsforholdOverUnder80.json")
        )
        val grunnlag = grunnlag(arbeidsforhold)

        val vurdertePerioder = finnArbeidsforholdPerioder(grunnlag)

        assertEquals(2, vurdertePerioder.size)
        assertEquals(1, vurdertePerioder.count { it.godkjentPeriode })
        assertEquals(1, vurdertePerioder.count { !it.godkjentPeriode })
    }
}