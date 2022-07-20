package barnepensjon.vilkaar

import adresserNorgePdl
import lagMockPersonPdl
import mapTilVilkaarstypePerson
import no.nav.etterlatte.barnepensjon.hentVirkningsdato
import no.nav.etterlatte.barnepensjon.vilkaarBrukerErUnder20
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AlderBarnTest {
    private val personBarnOver20 = lagMockPersonPdl(
        foedselsdatoBarnOver20,
        fnrBarn, null, adresserNorgePdl(), null
    )
    private val personBarnUnder20 = lagMockPersonPdl(
        foedselsdatoBarnUnder20,
        fnrBarn, null, adresserNorgePdl(), null
    )
    private val personAvdoedMedDoedsdato =
        lagMockPersonPdl(
            foedselsdatoBarnUnder20,
            fnrAvdoed,
            doedsdatoPdl, adresserNorgePdl(), null
        )
    private val personAvdoedUtenDoedsdato =
        lagMockPersonPdl(
            foedselsdatoBarnUnder20,
            fnrAvdoed, null, adresserNorgePdl(), null
        )

    @Test
    fun vurderAlderErUnder20() {
        val vurderingBarnOver20 = vilkaarBrukerErUnder20(
            Vilkaartyper.SOEKER_ER_UNDER_20,
            mapTilVilkaarstypePerson(personBarnOver20),
            mapTilVilkaarstypePerson(personAvdoedMedDoedsdato)
        )
        val vurderingBarnUnder20 = vilkaarBrukerErUnder20(
            Vilkaartyper.SOEKER_ER_UNDER_20,
            mapTilVilkaarstypePerson(personBarnUnder20),
            mapTilVilkaarstypePerson(personAvdoedMedDoedsdato)
        )

        val vurderingBarnUnder20UtenDoedsdato = vilkaarBrukerErUnder20(
            Vilkaartyper.SOEKER_ER_UNDER_20,
            mapTilVilkaarstypePerson(personBarnUnder20),
            mapTilVilkaarstypePerson(personAvdoedUtenDoedsdato)
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
        val vurdering = vilkaarBrukerErUnder20(Vilkaartyper.SOEKER_ER_UNDER_20, mapTilVilkaarstypePerson(personBarnUnder20), mapTilVilkaarstypePerson(personAvdoedMedDoedsdato))

        Assertions.assertEquals(VurderingsResultat.OPPFYLT, vurdering.resultat)
    }

    @Test
    fun `søker med dødsdato må ha dødsdato etter virkningsdato for oppfyllt vurdering`() {
        val personMedDødsdato = personBarnUnder20.copy(doedsdato = hentVirkningsdato(mapTilVilkaarstypePerson(personAvdoedMedDoedsdato)).plusDays(1))
        val vurdering = vilkaarBrukerErUnder20(Vilkaartyper.SOEKER_ER_UNDER_20, mapTilVilkaarstypePerson(personMedDødsdato), mapTilVilkaarstypePerson(personAvdoedMedDoedsdato))

        Assertions.assertEquals(VurderingsResultat.OPPFYLT, vurdering.resultat)
    }

    @Test
    fun `søker med dødsdato tidligere enn virkningsdato oppfyller ikke vurdering`() {
        val personMedDødsdato = personBarnUnder20.copy(doedsdato = hentVirkningsdato(mapTilVilkaarstypePerson(personAvdoedMedDoedsdato)).minusDays(1))
        val vurdering = vilkaarBrukerErUnder20(Vilkaartyper.SOEKER_ER_UNDER_20, mapTilVilkaarstypePerson(personMedDødsdato), mapTilVilkaarstypePerson(personAvdoedMedDoedsdato))

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