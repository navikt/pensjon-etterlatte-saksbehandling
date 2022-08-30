package barnepensjon.vilkaar

import adresserNorgePdl
import barnepensjon.vilkaarFormaalForYtelsen
import lagMockPersonPdl
import mapTilVilkaarstypePerson
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class FormaalTest {

    private val personBarnUnder20 = lagMockPersonPdl(
        AlderBarnTest.foedselsdatoBarnUnder20,
        AlderBarnTest.fnrBarn,
        null,
        adresserNorgePdl(),
        null
    )

    @Test
    fun `søker som er i live på virkningstidspunkt oppfyller vilkaar`() {
        val personMedDoedsdato = personBarnUnder20.copy(
            doedsdato = YearMonth.from(AlderBarnTest.doedsdatoPdl).plusMonths(2).atEndOfMonth()
        )
        val virkningstidspunkt = YearMonth.from(AlderBarnTest.doedsdatoPdl).plusMonths(1).atDay(1)
        val personUtenDoedsdato = personBarnUnder20.copy()
        val vurderingMedDoedsdato = vilkaarFormaalForYtelsen(
            mapTilVilkaarstypePerson(personMedDoedsdato),
            virkningstidspunkt
        )
        val vurderingUtenDoedsdato = vilkaarFormaalForYtelsen(
            mapTilVilkaarstypePerson(personUtenDoedsdato),
            virkningstidspunkt
        )

        Assertions.assertEquals(VurderingsResultat.OPPFYLT, vurderingMedDoedsdato.resultat)
        Assertions.assertEquals(VurderingsResultat.OPPFYLT, vurderingUtenDoedsdato.resultat)
    }

    @Test
    fun `ukjent data for søker gir må avklares for vilkår`() {
        val personUtenDoedsdato = personBarnUnder20.copy()
        val vurderingUkjentVirk = vilkaarFormaalForYtelsen(mapTilVilkaarstypePerson(personUtenDoedsdato), null)
        val vurderingUkjentSoeker = vilkaarFormaalForYtelsen(
            null,
            YearMonth.from(AlderBarnTest.doedsdatoPdl).plusMonths(1).atDay(1)
        )
        val vurderingUkjentBegge = vilkaarFormaalForYtelsen(null, null)

        Assertions.assertEquals(
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            vurderingUkjentBegge.resultat
        )
        Assertions.assertEquals(
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            vurderingUkjentVirk.resultat
        )
        Assertions.assertEquals(
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            vurderingUkjentSoeker.resultat
        )
    }

    @Test
    fun `søker med dødsdato tidligere enn virkningsdato oppfyller ikke vurdering`() {
        val personMedDødsdato = personBarnUnder20.copy(
            doedsdato = YearMonth.from(AlderBarnTest.doedsdatoPdl).atEndOfMonth()
        )
        val vurdering = vilkaarFormaalForYtelsen(
            mapTilVilkaarstypePerson(personMedDødsdato),
            virkningstidspunkt = YearMonth.from(AlderBarnTest.doedsdatoPdl).plusMonths(1).atDay(1)
        )

        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, vurdering.resultat)
    }
}