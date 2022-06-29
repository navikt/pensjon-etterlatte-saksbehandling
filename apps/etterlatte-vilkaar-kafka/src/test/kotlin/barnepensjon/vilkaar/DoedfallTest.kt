package barnepensjon.vilkaar

import lagMockPersonPdl
import mapTilVilkaarstypePerson
import no.nav.etterlatte.barnepensjon.vilkaarDoedsfallErRegistrert
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DoedfallTest {

    @Test
    fun vurderDoedsdatoErRegistrert() {
        val avdoedIngenDoedsdato = lagMockPersonPdl(null, fnrAvdoed, null, null, null)
        val avdoedRegistrertDoedsdato = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, null, null)
        val barnAvdoedErForeldre = lagMockPersonPdl(null, fnrBarn, null, null, avdoedErForeldre)
        val barnAvdoedErIkkeForeldre = lagMockPersonPdl(null, fnrBarn, null, null, avdoedErIkkeForeldre)

        val doedsdatoIkkeIPdl =
            vilkaarDoedsfallErRegistrert(
                Vilkaartyper.DOEDSFALL_ER_REGISTRERT,
                mapTilVilkaarstypePerson(avdoedIngenDoedsdato),
                mapTilVilkaarstypePerson(barnAvdoedErForeldre)
            )

        val avdoedErForelder =
            vilkaarDoedsfallErRegistrert(
                Vilkaartyper.DOEDSFALL_ER_REGISTRERT,
                mapTilVilkaarstypePerson(avdoedRegistrertDoedsdato),
                mapTilVilkaarstypePerson(barnAvdoedErForeldre)
            )

        val avdoedIkkeForelder =
            vilkaarDoedsfallErRegistrert(
                Vilkaartyper.DOEDSFALL_ER_REGISTRERT,
                mapTilVilkaarstypePerson(avdoedRegistrertDoedsdato),
                mapTilVilkaarstypePerson(barnAvdoedErIkkeForeldre)
            )

        Assertions.assertEquals(doedsdatoIkkeIPdl.resultat, VurderingsResultat.IKKE_OPPFYLT)
        Assertions.assertEquals(avdoedErForelder.resultat, VurderingsResultat.OPPFYLT)
        Assertions.assertEquals(avdoedIkkeForelder.resultat, VurderingsResultat.IKKE_OPPFYLT)

    }

    companion object {
        val fnrAvdoed = Foedselsnummer.of("19078504903")
        val doedsdatoPdl = LocalDate.parse("2021-01-25")
        val fnrBarn = Foedselsnummer.of("19040550081")
        val avdoedErForeldre = listOf(Foedselsnummer.of("19078504903"))
        val avdoedErIkkeForeldre = listOf(Foedselsnummer.of("11057523044"))
    }
}