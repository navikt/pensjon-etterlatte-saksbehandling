package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import io.mockk.mockk
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregResponse
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SaksbehandlerMedlemskapsperiode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SaksbehandlerMedlemskapsperioder
import no.nav.etterlatte.libs.common.inntekt.InntektsOpplysning
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.*

internal class FinnSaksbehandlerMedlemsPerioderKtTest {
    private fun grunnlag(): AvdoedesMedlemskapGrunnlag {
        val arbeidsforhold = mockk<Opplysning.Periodisert<AaregResponse>>()
        val inntekt = mockk<VilkaarOpplysning<InntektsOpplysning>>()

        @Suppress("UNCHECKED_CAST")
        return AvdoedesMedlemskapGrunnlag(
            inntekt,
            arbeidsforhold as Opplysning.Periodisert<AaregResponse?>,
            VilkaarOpplysning(
                id = UUID.randomUUID(),
                kilde = Grunnlagsopplysning.Vilkaarskomponenten("vilkaar"),
                opplysningType = Opplysningstyper.SAKSBEHANDLER_AVDOED_MEDLEMSKAPS_PERIODE,
                opplysning = SaksbehandlerMedlemskapsperioder(
                    listOf(
                        SaksbehandlerMedlemskapsperiode(
                            periodeType = PeriodeType.DAGPENGER,
                            id = UUID.randomUUID().toString(),
                            kilde = Grunnlagsopplysning.Saksbehandler("zid122", Instant.now()),
                            fraDato = LocalDate.of(2021, 1, 1),
                            tilDato = LocalDate.of(2022, 1, 1),
                            stillingsprosent = null,
                            arbeidsgiver = null,
                            begrunnelse = "Sykdom",
                            oppgittKilde = "NAV"
                        ),
                        SaksbehandlerMedlemskapsperiode(
                            periodeType = PeriodeType.ARBEIDSPERIODE,
                            id = UUID.randomUUID().toString(),
                            kilde = Grunnlagsopplysning.Saksbehandler("zid122", Instant.now()),
                            fraDato = LocalDate.of(2021, 1, 1),
                            tilDato = LocalDate.of(2022, 1, 1),
                            stillingsprosent = "70.0",
                            arbeidsgiver = null,
                            begrunnelse = "Annen jobb",
                            oppgittKilde = "NAV"
                        )
                    )
                )
            ),
            LocalDate.parse("2022-07-01")
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