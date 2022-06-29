package barnepensjon.vilkaar

import adresseDanmarkPdl
import adresseUtlandFoerFemAar
import adresserNorgePdl
import lagMockPersonAvdoedSoeknad
import lagMockPersonPdl
import mapTilVilkaarstypeAvdoedSoeknad
import mapTilVilkaarstypePerson
import no.nav.etterlatte.barnepensjon.kriterieIngenUtenlandsoppholdFraSoeknadSisteFemAar
import no.nav.etterlatte.barnepensjon.kriterieSammenhengendeAdresserINorgeSisteFemAar
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Utenlandsopphold
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.UtenlandsoppholdOpplysninger
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.OppholdUtlandType
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvdoedesMedlemskapTest {

    @Test
    fun vurderIngenUtelandsoppholdISoeknad() {
        val avdoedRegistrertDoedsdato = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, null, null)
        val avdoedSoknadMedUtland = lagMockPersonAvdoedSoeknad(utenlandsoppholdAvdoedSoeknad)
        val avdoedSoknadMedUtlandFoerFemAar = lagMockPersonAvdoedSoeknad(utenlandsoppholdAvdoedSoeknadFoerFemAar)
        val avdoedSoeknadUtenUtland = lagMockPersonAvdoedSoeknad(ingenUtenlandsoppholdAvdoedSoeknad)

        val utenlandsopphold =
            kriterieIngenUtenlandsoppholdFraSoeknadSisteFemAar(
                mapTilVilkaarstypeAvdoedSoeknad(avdoedSoknadMedUtland),
                mapTilVilkaarstypePerson(avdoedRegistrertDoedsdato),
                Kriterietyper.AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD
            )

        val utenlandsoppholdFoerFemAar =
            kriterieIngenUtenlandsoppholdFraSoeknadSisteFemAar(
                mapTilVilkaarstypeAvdoedSoeknad(avdoedSoknadMedUtlandFoerFemAar),
                mapTilVilkaarstypePerson(avdoedRegistrertDoedsdato),
                Kriterietyper.AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD
            )

        val ingenUtenlandsopphold =
            kriterieIngenUtenlandsoppholdFraSoeknadSisteFemAar(
                mapTilVilkaarstypeAvdoedSoeknad(avdoedSoeknadUtenUtland),
                mapTilVilkaarstypePerson(avdoedRegistrertDoedsdato),
                Kriterietyper.AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD
            )

        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, utenlandsopphold.resultat)
        Assertions.assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsopphold.resultat)
        Assertions.assertEquals(VurderingsResultat.OPPFYLT, utenlandsoppholdFoerFemAar.resultat)
    }

    @Test
    fun vurderSammenhengendeAdresserINorgeSisteFemAar() {
        val avdoedPdlUtenAdresse = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, null, null)
        val avdoedPdlMedUtland = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, adresseDanmarkPdl(), null)
        val avdoedPdlUtenUtland = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, adresserNorgePdl(), null)
        val avdoedPdlUtlandFoerFemAar = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, adresseUtlandFoerFemAar(), null)

        val utenlandsopphold =
            kriterieSammenhengendeAdresserINorgeSisteFemAar(
                mapTilVilkaarstypePerson(avdoedPdlMedUtland),
                Kriterietyper.AVDOED_SAMMENHENGENDE_ADRESSE_NORGE_SISTE_FEM_AAR
            )

        val ingenUtenlandsopphold =
            kriterieSammenhengendeAdresserINorgeSisteFemAar(
                mapTilVilkaarstypePerson(avdoedPdlUtenUtland),
                Kriterietyper.AVDOED_SAMMENHENGENDE_ADRESSE_NORGE_SISTE_FEM_AAR
            )

        val utenlandsoppholdFoerFemAar =
            kriterieSammenhengendeAdresserINorgeSisteFemAar(
                mapTilVilkaarstypePerson(avdoedPdlUtlandFoerFemAar),
                Kriterietyper.AVDOED_SAMMENHENGENDE_ADRESSE_NORGE_SISTE_FEM_AAR
            )


        val ingenAdresser =
            kriterieSammenhengendeAdresserINorgeSisteFemAar(
                mapTilVilkaarstypePerson(avdoedPdlUtenAdresse),
                Kriterietyper.AVDOED_SAMMENHENGENDE_ADRESSE_NORGE_SISTE_FEM_AAR
            )

        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, utenlandsopphold.resultat)
        Assertions.assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsopphold.resultat)
        Assertions.assertEquals(VurderingsResultat.OPPFYLT, utenlandsoppholdFoerFemAar.resultat)
        Assertions.assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, ingenAdresser.resultat)
    }

    companion object {
        val fnrAvdoed = Foedselsnummer.of("19078504903")
        val doedsdatoPdl = LocalDate.parse("2022-03-25")

        val utenlandsoppholdAvdoedSoeknad = Utenlandsopphold(
            JaNeiVetIkke.JA,
            listOf(
                UtenlandsoppholdOpplysninger(
                    "Danmark",
                    LocalDate.parse("2010-01-25"),
                    LocalDate.parse("2022-01-25"),
                    listOf(OppholdUtlandType.ARBEIDET),
                    JaNeiVetIkke.JA,
                    null
                ),
                UtenlandsoppholdOpplysninger(
                    "Costa Rica",
                    LocalDate.parse("2000-01-25"),
                    LocalDate.parse("2007-01-25"),
                    listOf(OppholdUtlandType.ARBEIDET),
                    JaNeiVetIkke.NEI,
                    null
                ),
            )
        )

        val utenlandsoppholdAvdoedSoeknadFoerFemAar = Utenlandsopphold(
            JaNeiVetIkke.JA,
            listOf(
                UtenlandsoppholdOpplysninger(
                    "Danmark",
                    LocalDate.parse("2010-01-25"),
                    LocalDate.parse("2012-01-25"),
                    listOf(OppholdUtlandType.ARBEIDET),
                    JaNeiVetIkke.JA,
                    null
                )
            )
        )

        val ingenUtenlandsoppholdAvdoedSoeknad = Utenlandsopphold(
            JaNeiVetIkke.NEI,
            listOf()
        )
    }

}