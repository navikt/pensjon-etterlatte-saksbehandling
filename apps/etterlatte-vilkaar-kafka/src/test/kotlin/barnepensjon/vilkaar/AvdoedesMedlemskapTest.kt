package barnepensjon.vilkaar

import adresseDanmarkPdl
import adresseUtlandFoerFemAar
import adresserNorgePdl
import barnepensjon.vilkaar.avdoedesmedlemskap.kriterieHarMottattPensjonEllerTrygdSisteFemAar
import barnepensjon.vilkaar.avdoedesmedlemskap.kriterieIngenInnUtvandring
import barnepensjon.vilkaar.avdoedesmedlemskap.kriterieIngenUtenlandsoppholdFraSoeknad
import barnepensjon.vilkaar.avdoedesmedlemskap.kriterieKunNorskeBostedsadresserSisteFemAar
import barnepensjon.vilkaar.avdoedesmedlemskap.kriterieKunNorskeKontaktadresserSisteFemAar
import barnepensjon.vilkaar.avdoedesmedlemskap.kriterieKunNorskeOppholdsadresserSisteFemAar
import barnepensjon.vilkaar.avdoedesmedlemskap.kriterieNorskStatsborger
import barnepensjon.vilkaar.avdoedesmedlemskap.kriterieSammenhengendeAdresserINorgeSisteFemAar
import com.fasterxml.jackson.module.kotlin.readValue
import lagMockPersonAvdoedSoeknad
import lagMockPersonPdl
import mapTilVilkaarstypeAvdoedSoeknad
import mapTilVilkaarstypePerson
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Utenlandsopphold
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.UtenlandsoppholdOpplysninger
import no.nav.etterlatte.libs.common.inntekt.PensjonUforeOpplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.InnflyttingTilNorge
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.OppholdUtlandType
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.time.LocalDate

class AvdoedesMedlemskapTest {

    @Test
    fun vurderMottattPensjonUforeSisteFemAar() {
        val file = readFile("/inntektsopplysning.json")
        val opplysning = objectMapper.readValue<VilkaarOpplysning<PensjonUforeOpplysning>>(file)
        val pensjonufore = kriterieHarMottattPensjonEllerTrygdSisteFemAar(opplysning)
        //print(pensjonufore)
        assert(true)
    }

    @Test
    fun vurderNorskStatsborgerskap() {
        val avdoedPdlNorsk = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, adresserNorgePdl(), null, "NOR")
        val avdoedPdlDansk = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, adresserNorgePdl(), null, "DAN")
        val avdoedPdlMangler = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, adresserNorgePdl(), null, null)

        val norsk =
            kriterieNorskStatsborger(mapTilVilkaarstypePerson(avdoedPdlNorsk), Kriterietyper.AVDOED_NORSK_STATSBORGER)
        val dansk =
            kriterieNorskStatsborger(mapTilVilkaarstypePerson(avdoedPdlDansk), Kriterietyper.AVDOED_NORSK_STATSBORGER)
        val mangler =
            kriterieNorskStatsborger(mapTilVilkaarstypePerson(avdoedPdlMangler), Kriterietyper.AVDOED_NORSK_STATSBORGER)

        Assertions.assertEquals(VurderingsResultat.OPPFYLT, norsk.resultat)
        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, dansk.resultat)
        Assertions.assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, mangler.resultat)
    }

    @Test
    fun vurderInnOgUtvandring() {
        val avdoedPdlIngen = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, adresserNorgePdl(), null)
        val avdoedPdlInnvandring = lagMockPersonPdl(
            null, fnrAvdoed, doedsdatoPdl, adresserNorgePdl(), null, utland = Utland(
                innflyttingTilNorge = listOf(InnflyttingTilNorge("DAN", LocalDate.now())),
                utflyttingFraNorge = listOf(UtflyttingFraNorge("USA", LocalDate.now()))
            )
        )

        val ingenInnOgUtvandring =
            kriterieIngenInnUtvandring(mapTilVilkaarstypePerson(avdoedPdlIngen), Kriterietyper.AVDOED_NORSK_STATSBORGER)
        val harInnOgUtvandring =
            kriterieIngenInnUtvandring(
                mapTilVilkaarstypePerson(avdoedPdlInnvandring),
                Kriterietyper.AVDOED_NORSK_STATSBORGER
            )

        Assertions.assertEquals(VurderingsResultat.OPPFYLT, ingenInnOgUtvandring.resultat)
        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, harInnOgUtvandring.resultat)
    }

    @Test
    fun vurderIngenUtelandsoppholdISoeknad() {
        val avdoedSoknadMedUtland = lagMockPersonAvdoedSoeknad(utenlandsoppholdAvdoedSoeknad)
        val avdoedSoeknadUtenUtland = lagMockPersonAvdoedSoeknad(ingenUtenlandsoppholdAvdoedSoeknad)

        val utenlandsopphold =
            kriterieIngenUtenlandsoppholdFraSoeknad(
                mapTilVilkaarstypeAvdoedSoeknad(avdoedSoknadMedUtland),
                Kriterietyper.AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD
            )

        val ingenUtenlandsopphold =
            kriterieIngenUtenlandsoppholdFraSoeknad(
                mapTilVilkaarstypeAvdoedSoeknad(avdoedSoeknadUtenUtland),
                Kriterietyper.AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD
            )

        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, utenlandsopphold.resultat)
        Assertions.assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsopphold.resultat)
    }

    @Test
    fun kunNorskeAdresserSisteFemAar() {
        val avdoedPdlUtenAdresse = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, null, null)
        val avdoedPdlMedUtland = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, adresseDanmarkPdl(), null)
        val avdoedPdlUtenUtland = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, adresserNorgePdl(), null)
        val avdoedPdlUtlandFoerFemAar = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, adresseUtlandFoerFemAar(), null)

        val utenlandsoppholdBosted =
            kriterieKunNorskeBostedsadresserSisteFemAar(
                mapTilVilkaarstypePerson(avdoedPdlMedUtland),
                Kriterietyper.AVDOED_KUN_NORSKE_BOSTEDSADRESSER
            )
        val utenlandsoppholdOpphold =
            kriterieKunNorskeOppholdsadresserSisteFemAar(
                mapTilVilkaarstypePerson(avdoedPdlMedUtland),
                Kriterietyper.AVDOED_KUN_NORSKE_OPPHOLDSSADRESSER
            )

        val utenlandsoppholdKontakt =
            kriterieKunNorskeKontaktadresserSisteFemAar(
                mapTilVilkaarstypePerson(avdoedPdlMedUtland),
                Kriterietyper.AVDOED_KUN_NORSKE_KONTAKTADRESSER
            )

        val ingenUtenlandsoppholdBosted =
            kriterieKunNorskeBostedsadresserSisteFemAar(
                mapTilVilkaarstypePerson(avdoedPdlUtenUtland),
                Kriterietyper.AVDOED_KUN_NORSKE_BOSTEDSADRESSER
            )

        val ingenUtenlandsoppholdOpphold =
            kriterieKunNorskeOppholdsadresserSisteFemAar(
                mapTilVilkaarstypePerson(avdoedPdlUtenUtland),
                Kriterietyper.AVDOED_KUN_NORSKE_OPPHOLDSSADRESSER
            )

        val ingenUtenlandsoppholdKontakt =
            kriterieKunNorskeKontaktadresserSisteFemAar(
                mapTilVilkaarstypePerson(avdoedPdlUtenUtland),
                Kriterietyper.AVDOED_KUN_NORSKE_KONTAKTADRESSER
            )

        val utenlandsoppholdFoerFemAar =
            kriterieKunNorskeBostedsadresserSisteFemAar(
                mapTilVilkaarstypePerson(avdoedPdlUtlandFoerFemAar),
                Kriterietyper.AVDOED_KUN_NORSKE_BOSTEDSADRESSER
            )


        val ingenAdresser =
            kriterieKunNorskeBostedsadresserSisteFemAar(
                mapTilVilkaarstypePerson(avdoedPdlUtenAdresse),
                Kriterietyper.AVDOED_KUN_NORSKE_BOSTEDSADRESSER
            )

        Assertions.assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsoppholdBosted.resultat)
        Assertions.assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsoppholdOpphold.resultat)
        Assertions.assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsoppholdKontakt.resultat)

        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, utenlandsoppholdBosted.resultat)
        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, utenlandsoppholdOpphold.resultat)
        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, utenlandsoppholdKontakt.resultat)

        Assertions.assertEquals(VurderingsResultat.OPPFYLT, utenlandsoppholdFoerFemAar.resultat)
        Assertions.assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, ingenAdresser.resultat)

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
                Kriterietyper.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR
            )

        val ingenUtenlandsopphold =
            kriterieSammenhengendeAdresserINorgeSisteFemAar(
                mapTilVilkaarstypePerson(avdoedPdlUtenUtland),
                Kriterietyper.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR
            )

        val utenlandsoppholdFoerFemAar =
            kriterieSammenhengendeAdresserINorgeSisteFemAar(
                mapTilVilkaarstypePerson(avdoedPdlUtlandFoerFemAar),
                Kriterietyper.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR
            )


        val ingenAdresser =
            kriterieSammenhengendeAdresserINorgeSisteFemAar(
                mapTilVilkaarstypePerson(avdoedPdlUtenAdresse),
                Kriterietyper.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR
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

        val ingenUtenlandsoppholdAvdoedSoeknad = Utenlandsopphold(
            JaNeiVetIkke.NEI,
            listOf()
        )
    }


    fun readFile(file: String) = AvdoedesMedlemskapTest::class.java.getResource(file)?.readText()
        ?: throw FileNotFoundException("Fant ikke filen $file")

}