package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import io.mockk.mockk
import no.nav.etterlatte.libs.common.arbeidsforhold.ArbeidsforholdOpplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapsperiode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.inntekt.InntektsOpplysning
import no.nav.etterlatte.libs.common.vikaar.Metakriterie
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class FinnSaksbehandlerMedlemsPerioderKtTest {
    private fun grunnlag(): AvdoedesMedlemskapGrunnlag {
        val arbeidsforhold = mockk<VilkaarOpplysning<ArbeidsforholdOpplysning>>()
        val inntekt = mockk<VilkaarOpplysning<InntektsOpplysning>>()
        val bosattNorge = mockk<Metakriterie>()

        return AvdoedesMedlemskapGrunnlag(
            inntekt,
            arbeidsforhold,
            listOf(
                VilkaarOpplysning(
                    id = UUID.randomUUID(),
                    kilde = Grunnlagsopplysning.Vilkaarskomponenten("vilkaar"),
                    opplysningType = Opplysningstyper.SAKSBEHANDLER_AVDOED_MEDLEMSKAPS_PERIODE,
                    opplysning = AvdoedesMedlemskapsperiode(
                        periodeType = PeriodeType.DAGPENGER,
                        fraDato = LocalDate.of(2021, 1, 1),
                        tilDato = LocalDate.of(2022, 1, 1),
                        stillingsprosent = null,
                        arbeidsgiver = null,
                        begrunnelse = "Sykdom",
                        kilde = "NAV"
                    )
                ),
                VilkaarOpplysning(
                    id = UUID.randomUUID(),
                    kilde = Grunnlagsopplysning.Vilkaarskomponenten("vilkaar"),
                    opplysningType = Opplysningstyper.SAKSBEHANDLER_AVDOED_MEDLEMSKAPS_PERIODE,
                    opplysning = AvdoedesMedlemskapsperiode(
                        periodeType = PeriodeType.ARBEIDSPERIODE,
                        fraDato = LocalDate.of(2021, 1, 1),
                        tilDato = LocalDate.of(2022, 1, 1),
                        stillingsprosent = "70.0",
                        arbeidsgiver = null,
                        begrunnelse = "Sykdom",
                        kilde = "NAV"
                    )
                )
            ),
            LocalDate.parse("2022-07-01"),
            bosattNorge
        )
    }

    @Test
    fun `Skal returnere to vurderte perioder, ett godkjent og ett ikke godkjent pga stillingsprosent`() {
        val grunnlag = grunnlag()

        val vurdertePerioder = finnSaksbehandlerMedlemsPerioder(grunnlag)

        assertEquals(2, vurdertePerioder.size)
        vurdertePerioder.first().let {
            assertTrue(it.godkjentPeriode)
            assertEquals(PeriodeType.DAGPENGER, it.periodeType)
        }
        vurdertePerioder.last().let {
            assertFalse(it.godkjentPeriode)
            assertEquals(PeriodeType.ARBEIDSPERIODE, it.periodeType)
        }
    }
}