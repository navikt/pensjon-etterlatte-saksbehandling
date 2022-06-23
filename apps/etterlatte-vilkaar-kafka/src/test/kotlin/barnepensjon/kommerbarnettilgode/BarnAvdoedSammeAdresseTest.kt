package barnepensjon.kommerbarnettilgode

import adresseDanmarkPdl
import adresserNorgePdl
import lagMockPersonPdl
import mapTilVilkaarstypePerson
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import vilkaar.barnepensjon.barnOgAvdoedSammeBostedsadresse
import java.time.LocalDate

class BarnAvdoedSammeAdresseTest {
    @Test
    fun vurderBarnOgAvdoedSammeAdresse() {
        val barnPdlNorge = lagMockPersonPdl(foedselsdatoBarnUnder20, fnrBarn, null, adresserNorgePdl(), avdoedErForeldre)
        val avdoedPdlNorge = lagMockPersonPdl(null, fnrAvdoed, null, adresserNorgePdl(), null)
        val avdoedPdlDanmark = lagMockPersonPdl(null, fnrAvdoed, null, adresseDanmarkPdl(), null)

        val sammeAdresse = barnOgAvdoedSammeBostedsadresse(
            Vilkaartyper.BARN_BOR_PAA_AVDOEDES_ADRESSE,
            mapTilVilkaarstypePerson(barnPdlNorge),
            mapTilVilkaarstypePerson(avdoedPdlNorge)
        )

        val ulikeAdresse = barnOgAvdoedSammeBostedsadresse(
            Vilkaartyper.BARN_BOR_PAA_AVDOEDES_ADRESSE,
            mapTilVilkaarstypePerson(barnPdlNorge),
            mapTilVilkaarstypePerson(avdoedPdlDanmark)
        )
        Assertions.assertEquals(VurderingsResultat.OPPFYLT, sammeAdresse.resultat)
        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, ulikeAdresse.resultat)

    }

    companion object {
        val foedselsdatoBarnUnder20 = LocalDate.parse("2020-06-10")
        val fnrBarn = Foedselsnummer.of("19040550081")
        val fnrAvdoed = Foedselsnummer.of("19078504903")
        val avdoedErForeldre = listOf(Foedselsnummer.of("19078504903"))

    }



}