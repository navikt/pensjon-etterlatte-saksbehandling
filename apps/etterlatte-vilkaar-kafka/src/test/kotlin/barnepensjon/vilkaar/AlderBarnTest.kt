package barnepensjon.vilkaar

import adresserNorgePdl
import lagMockPersonPdl
import mapTilVilkaarstypePerson
import no.nav.etterlatte.barnepensjon.vilkaarBrukerErUnder20
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class AlderBarnTest {
    private val personBarnOver20 = lagMockPersonPdl(
        foedselsdatoBarnOver20,
        fnrBarn,
        null,
        adresserNorgePdl(),
        null
    )
    private val personBarnUnder20 = lagMockPersonPdl(
        foedselsdatoBarnUnder20,
        fnrBarn,
        null,
        adresserNorgePdl(),
        null
    )
    private val personAvdoedMedDoedsdato =
        lagMockPersonPdl(
            foedselsdatoBarnUnder20,
            fnrAvdoed,
            doedsdatoPdl,
            adresserNorgePdl(),
            null
        )

    private val virkningsdataPersonAvdoed = YearMonth.from(personAvdoedMedDoedsdato.doedsdato).plusMonths(1)

    @Test
    fun vurderAlderErUnder20() {
        val vurderingBarnOver20 = vilkaarBrukerErUnder20(
            mapTilVilkaarstypePerson(personBarnOver20),
            mapTilVilkaarstypePerson(personAvdoedMedDoedsdato),
            virkningstidspunkt = virkningsdataPersonAvdoed.atDay(1)
        )
        val vurderingBarnUnder20 = vilkaarBrukerErUnder20(
            mapTilVilkaarstypePerson(personBarnUnder20),
            mapTilVilkaarstypePerson(personAvdoedMedDoedsdato),
            virkningstidspunkt = virkningsdataPersonAvdoed.atDay(1)
        )

        val vurderingBarnUnder20UtenDoedsdato = vilkaarBrukerErUnder20(
            mapTilVilkaarstypePerson(personBarnUnder20),
            mapTilVilkaarstypePerson(personAvdoedMedDoedsdato),
            virkningstidspunkt = null
        )

        Assertions.assertEquals(vurderingBarnOver20.resultat, VurderingsResultat.IKKE_OPPFYLT)
        Assertions.assertEquals(vurderingBarnUnder20.resultat, VurderingsResultat.OPPFYLT)
        Assertions.assertEquals(
            vurderingBarnUnder20UtenDoedsdato.resultat,
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        )
    }

    @Test
    fun `søker i live oppfyller vurdering`() {
        val vurdering = vilkaarBrukerErUnder20(
            mapTilVilkaarstypePerson(personBarnUnder20),
            mapTilVilkaarstypePerson(personAvdoedMedDoedsdato),
            virkningstidspunkt = virkningsdataPersonAvdoed.atDay(1)
        )

        Assertions.assertEquals(VurderingsResultat.OPPFYLT, vurdering.resultat)
    }

    @Test
    fun `søker med dødsdato må ha dødsdato etter virkningsdato for oppfyllt vurdering`() {
        val personMedDødsdato = personBarnUnder20.copy(
            doedsdato = YearMonth.from(personAvdoedMedDoedsdato.doedsdato).atEndOfMonth()
        )
        val vurdering = vilkaarBrukerErUnder20(
            mapTilVilkaarstypePerson(personMedDødsdato),
            mapTilVilkaarstypePerson(personAvdoedMedDoedsdato),
            virkningstidspunkt = virkningsdataPersonAvdoed.minusMonths(1).atDay(1)
        )

        Assertions.assertEquals(VurderingsResultat.OPPFYLT, vurdering.resultat)
    }

    @Test
    fun `søker med dødsdato tidligere enn virkningsdato oppfyller ikke vurdering`() {
        val personMedDødsdato = personBarnUnder20.copy(
            doedsdato = YearMonth.from(personAvdoedMedDoedsdato.doedsdato).atEndOfMonth()
        )
        val vurdering = vilkaarBrukerErUnder20(
            mapTilVilkaarstypePerson(personMedDødsdato),
            mapTilVilkaarstypePerson(personAvdoedMedDoedsdato),
            virkningstidspunkt = virkningsdataPersonAvdoed.atDay(1)
        )

        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, vurdering.resultat)
    }

    companion object {
        val foedselsdatoBarnOver20 = LocalDate.parse("2000-08-29")
        val foedselsdatoBarnUnder20 = LocalDate.parse("2020-06-10")
        val doedsdatoPdl = LocalDate.parse("2021-01-25")
        val fnrBarn = Foedselsnummer.of("19040550081")
        val fnrAvdoed = Foedselsnummer.of("19078504903")
    }
}