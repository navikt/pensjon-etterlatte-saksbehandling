package barnepensjon.kommerbarnettilgode

import adresseDanmarkPdl
import adresserNorgePdl
import lagMockPersonPdl
import mapTilVilkaarstypePerson
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import vilkaar.barnepensjon.barnOgForelderSammeBostedsadresse
import java.time.LocalDate

class BarnGjenlevendeSammeAdresseTest {
    @Test
    fun vuderBarnGjenlevndeSammeBostedsadresse() {
        val barnPdlNorge = lagMockPersonPdl(foedselsdatoBarnUnder20, fnrBarn, null, adresserNorgePdl(), avdoedErForeldre)
        val gjenlevendePdlNorge = lagMockPersonPdl(null, fnrGjenlevende, null, adresserNorgePdl(), null)
        val gjenlevendePdlDanmark = lagMockPersonPdl(null, fnrGjenlevende, null, adresseDanmarkPdl(), null)

        val sammeAdresse = barnOgForelderSammeBostedsadresse(
            mapTilVilkaarstypePerson(barnPdlNorge),
            mapTilVilkaarstypePerson(gjenlevendePdlNorge)
        )

        val ulikeAdresse = barnOgForelderSammeBostedsadresse(
            mapTilVilkaarstypePerson(barnPdlNorge),
            mapTilVilkaarstypePerson(gjenlevendePdlDanmark)
        )
        Assertions.assertEquals(VurderingsResultat.OPPFYLT, sammeAdresse.resultat)
        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, ulikeAdresse.resultat)

    }

    companion object {
        val foedselsdatoBarnUnder20 = LocalDate.parse("2020-06-10")
        val fnrBarn = Foedselsnummer.of("19040550081")
        val fnrGjenlevende = Foedselsnummer.of("07081177656")
        val avdoedErForeldre = listOf(Foedselsnummer.of("19078504903"))
    }
}