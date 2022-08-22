package barnepensjon.vilkaar

import adresseDanmarkPdl
import adresserNorgePdl
import lagMockPersonPdl
import lagMockPersonSoekerSoeknad
import mapTilVilkaarstypePerson
import mapTilVilkaarstypeSoekerSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.UtenlandsadresseBarn
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.vilkaar.barnepensjon.vilkaarBarnetsMedlemskap
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BarnetsMedlemskapTest {
    @Test
    fun vuderBarnetsMedlemskap() {
        val foedselsdatoBarnUnder20 = LocalDate.parse("2020-06-10")
        val fnrAvdoed = Foedselsnummer.of("19078504903")
        val fnrGjenlevende = Foedselsnummer.of("07081177656")
        val doedsdatoPdl = LocalDate.parse("2021-01-25")
        val fnrBarn = Foedselsnummer.of("19040550081")
        val avdoedErForeldre = listOf(Foedselsnummer.of("19078504903"))

        val avdoedRegistrertDoedsdato = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, null, null)
        val gjenlevendePdlNorge = lagMockPersonPdl(null, fnrGjenlevende, null, adresserNorgePdl(), null)
        val gjenlevendePdlDanmark = lagMockPersonPdl(null, fnrGjenlevende, null, adresseDanmarkPdl(), null)

        val barnPdlNorge = lagMockPersonPdl(
            foedselsdatoBarnUnder20,
            fnrBarn,
            null,
            adresserNorgePdl(),
            avdoedErForeldre
        )
        val barnPdlDanmark =
            lagMockPersonPdl(foedselsdatoBarnUnder20, fnrBarn, null, adresseDanmarkPdl(), avdoedErForeldre)
        val barnSoeknadNorge = lagMockPersonSoekerSoeknad(UtenlandsadresseBarn(JaNeiVetIkke.NEI, null, null))
        val barnSoeknadDanmark = lagMockPersonSoekerSoeknad(UtenlandsadresseBarn(JaNeiVetIkke.JA, null, null))

        val ingenUtenlandsAdresser = vilkaarBarnetsMedlemskap(
            mapTilVilkaarstypePerson(barnPdlNorge),
            mapTilVilkaarstypeSoekerSoeknad(barnSoeknadNorge),
            mapTilVilkaarstypePerson(gjenlevendePdlNorge),
            mapTilVilkaarstypePerson(avdoedRegistrertDoedsdato)
        )

        val barnUtenlandsAdresserPdl = vilkaarBarnetsMedlemskap(
            mapTilVilkaarstypePerson(barnPdlDanmark),
            mapTilVilkaarstypeSoekerSoeknad(barnSoeknadNorge),
            mapTilVilkaarstypePerson(gjenlevendePdlNorge),
            mapTilVilkaarstypePerson(avdoedRegistrertDoedsdato)
        )

        val barnUtenlandsAdresserSoeknad = vilkaarBarnetsMedlemskap(
            mapTilVilkaarstypePerson(barnPdlNorge),
            mapTilVilkaarstypeSoekerSoeknad(barnSoeknadDanmark),
            mapTilVilkaarstypePerson(gjenlevendePdlNorge),
            mapTilVilkaarstypePerson(avdoedRegistrertDoedsdato)
        )

        val gjenlevendeUtenlandsAdresserPdl = vilkaarBarnetsMedlemskap(
            mapTilVilkaarstypePerson(barnPdlNorge),
            mapTilVilkaarstypeSoekerSoeknad(barnSoeknadNorge),
            mapTilVilkaarstypePerson(gjenlevendePdlDanmark),
            mapTilVilkaarstypePerson(avdoedRegistrertDoedsdato)
        )

        Assertions.assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsAdresser.resultat)
        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, barnUtenlandsAdresserPdl.resultat)
        Assertions.assertEquals(
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            gjenlevendeUtenlandsAdresserPdl.resultat
        )
        Assertions.assertEquals(
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            barnUtenlandsAdresserSoeknad.resultat
        )
    }
}