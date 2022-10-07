package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import GrunnlagTestData
import grunnlag.medlemskap
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.hentMedlemskapsperiode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class FinnSaksbehandlerMedlemsPerioderKtTest {

    @Test
    fun `Skal returnere to vurderte perioder, ett godkjent og ett ikke godkjent pga stillingsprosent`() {
        val medlemskap = GrunnlagTestData(
            opplysningsmapAvdødOverrides = mapOf(
                Opplysningstyper.MEDLEMSKAPSPERIODE to Opplysning.Periodisert(medlemskap)
            )
        ).hentOpplysningsgrunnlag().hentAvdoed().hentMedlemskapsperiode()!!

        val vurdertePerioder = finnSaksbehandlerMedlemsPerioder(medlemskap)

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