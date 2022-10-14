package barnepensjon.vilkaar

import GrunnlagTestData
import grunnlag.kilde
import no.nav.etterlatte.barnepensjon.vilkaarBrukerErUnder20
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

class AlderBarnTest {
    private val testDataOver20 = GrunnlagTestData(
        opplysningsmapSoekerOverrides = mapOf(
            Opplysningstype.FOEDSELSDATO to Opplysning.Konstant(
                UUID.randomUUID(),
                kilde,
                LocalDate.of(2000, 3, 23).toJsonNode()
            )
        )
    ).hentOpplysningsgrunnlag()

    private val testDataUnder20 = GrunnlagTestData(
        opplysningsmapSoekerOverrides = mapOf(
            Opplysningstype.FOEDSELSDATO to Opplysning.Konstant(
                UUID.randomUUID(),
                kilde,
                LocalDate.now().minusYears(15).toJsonNode()
            )
        )
    ).hentOpplysningsgrunnlag()

    private val virkningsdataPersonAvdoed =
        YearMonth.from(testDataUnder20.hentAvdoed().hentDoedsdato()!!.verdi).plusMonths(1)

    @Test
    fun vurderAlderErUnder20() {
        val vurderingBarnOver20 = vilkaarBrukerErUnder20(
            testDataOver20.soeker,
            testDataOver20.hentAvdoed(),
            virkningstidspunkt = virkningsdataPersonAvdoed.atDay(1)
        )
        val vurderingBarnUnder20 = vilkaarBrukerErUnder20(
            testDataUnder20.soeker,
            testDataUnder20.hentAvdoed(),
            virkningstidspunkt = virkningsdataPersonAvdoed.atDay(1)
        )

        val vurderingBarnUnder20UtenDoedsdato = vilkaarBrukerErUnder20(
            testDataUnder20.soeker,
            testDataUnder20.hentAvdoed(),
            virkningstidspunkt = null
        )

        Assertions.assertEquals(vurderingBarnOver20.resultat, VurderingsResultat.IKKE_OPPFYLT)
        Assertions.assertEquals(vurderingBarnUnder20.resultat, VurderingsResultat.OPPFYLT)
        Assertions.assertEquals(
            vurderingBarnUnder20UtenDoedsdato.resultat,
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        )
    }
}