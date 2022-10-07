package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import LesVilkaarsmeldingTest.Companion.readFile
import com.fasterxml.jackson.module.kotlin.readValue
import grunnlag.kilde
import io.mockk.mockk
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregResponse
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.inntekt.InntektsOpplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class FinnPensjonEllerTrygdePerioderKtTest {
    private fun grunnlag(arbeidOpplysning: String): AvdoedesMedlemskapGrunnlag {
        val inntekter = objectMapper.readValue<InntektsOpplysning>(readFile(arbeidOpplysning))
        val arbeidsforhold = mockk<Opplysning.Periodisert<AaregResponse>>()

        @Suppress("UNCHECKED_CAST")
        return AvdoedesMedlemskapGrunnlag(
            VilkaarOpplysning(
                id = UUID.randomUUID(),
                opplysningType = Opplysningstyper.INNTEKT,
                kilde = kilde,
                opplysning = inntekter
            ),
            arbeidsforhold as Opplysning.Periodisert<AaregResponse?>,
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