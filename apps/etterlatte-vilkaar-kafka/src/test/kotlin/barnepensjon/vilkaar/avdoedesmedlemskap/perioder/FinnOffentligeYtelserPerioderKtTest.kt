package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import LesVilkaarsmeldingTest.Companion.readFile
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.mockk
import no.nav.etterlatte.libs.common.arbeidsforhold.ArbeidsforholdOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.inntekt.InntektsOpplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.Metakriterie
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class FinnOffentligeYtelserPerioderKtTest {
    private fun grunnlag(arbeidOpplysning: String): AvdoedesMedlemskapGrunnlag {
        val inntekter =
            objectMapper.readValue<VilkaarOpplysning<InntektsOpplysning>>(readFile(arbeidOpplysning))
        val arbeidsforhold = mockk<VilkaarOpplysning<ArbeidsforholdOpplysning>>()
        val bosattNorge = mockk<Metakriterie>()

        return AvdoedesMedlemskapGrunnlag(
            inntekter,
            arbeidsforhold,
            emptyList(),
            LocalDate.parse("2022-07-01"),
            bosattNorge
        )
    }

    @Test
    fun `Skal returnere med kombinert liste av perioder for offentlige ytelser gruppert paa ytelse`() {
        val grunnlag = grunnlag("/inntektsopplysning_offentlig.json")

        val vurdertePerioder = finnOffentligeYtelserPerioder(grunnlag)

        Assertions.assertEquals(2, vurdertePerioder.size)
        vurdertePerioder.first().let {
            Assertions.assertTrue(it.godkjentPeriode)
            Assertions.assertEquals(PeriodeType.OFFENTLIG_YTELSE, it.periodeType)
            Assertions.assertEquals("sykepenger", it.beskrivelse)
            Assertions.assertEquals(it.fraDato, LocalDate.of(2022, 1, 1))
            Assertions.assertEquals(it.tilDato, LocalDate.of(2022, 2, 28))
        }
        vurdertePerioder.last().let {
            Assertions.assertTrue(it.godkjentPeriode)
            Assertions.assertEquals(PeriodeType.OFFENTLIG_YTELSE, it.periodeType)
            Assertions.assertEquals("foreldrepenger", it.beskrivelse)
            Assertions.assertEquals(it.fraDato, LocalDate.of(2022, 5, 1))
            Assertions.assertEquals(it.tilDato, LocalDate.of(2022, 6, 30))
        }
    }
}