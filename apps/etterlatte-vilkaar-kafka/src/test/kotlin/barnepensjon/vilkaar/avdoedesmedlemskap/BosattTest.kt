package barnepensjon.vilkaar.avdoedesmedlemskap

import adresseDanmarkPdl
import adresseUtlandFoerFemAar
import adresserNorgePdl
import lagMockPersonAvdoedSoeknad
import lagMockPersonPdl
import mapTilVilkaarstypeAvdoedSoeknad
import mapTilVilkaarstypePerson
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Utenlandsopphold
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.UtenlandsoppholdOpplysninger
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.InnflyttingTilNorge
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.OppholdUtlandType
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BosattTest {

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

        assertEquals(VurderingsResultat.OPPFYLT, norsk.resultat)
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, dansk.resultat)
        assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, mangler.resultat)
    }

    @Test
    fun vurderInnOgUtvandring() {
        val avdoedPdlIngenUtland = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, adresserNorgePdl(), null)
        val avdoedPdlInnvandring = lagMockPersonPdl(
            null,
            fnrAvdoed,
            doedsdatoPdl,
            adresserNorgePdl(),
            null,
            utland = Utland(
                innflyttingTilNorge = listOf(InnflyttingTilNorge("DAN", LocalDate.now())),
                utflyttingFraNorge = listOf(UtflyttingFraNorge("USA", LocalDate.now()))
            )
        )

        val avdoedPdlHarIkke = lagMockPersonPdl(
            null,
            fnrAvdoed,
            doedsdatoPdl,
            adresserNorgePdl(),
            null,
            utland = Utland(
                innflyttingTilNorge = emptyList(),
                utflyttingFraNorge = emptyList()
            )
        )

        val ingenUtland =
            kriterieIngenInnUtvandring(
                mapTilVilkaarstypePerson(avdoedPdlIngenUtland),
                Kriterietyper.AVDOED_NORSK_STATSBORGER
            )
        val ingenInnOgUtvandring =
            kriterieIngenInnUtvandring(
                mapTilVilkaarstypePerson(avdoedPdlHarIkke),
                Kriterietyper.AVDOED_NORSK_STATSBORGER
            )
        val harInnOgUtvandring =
            kriterieIngenInnUtvandring(
                mapTilVilkaarstypePerson(avdoedPdlInnvandring),
                Kriterietyper.AVDOED_NORSK_STATSBORGER
            )

        assertEquals(VurderingsResultat.OPPFYLT, ingenUtland.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, ingenInnOgUtvandring.resultat)
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, harInnOgUtvandring.resultat)
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

        assertEquals(VurderingsResultat.IKKE_OPPFYLT, utenlandsopphold.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsopphold.resultat)
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

        assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsoppholdBosted.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsoppholdOpphold.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsoppholdKontakt.resultat)

        assertEquals(VurderingsResultat.IKKE_OPPFYLT, utenlandsoppholdBosted.resultat)
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, utenlandsoppholdOpphold.resultat)
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, utenlandsoppholdKontakt.resultat)

        assertEquals(VurderingsResultat.OPPFYLT, utenlandsoppholdFoerFemAar.resultat)
        assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, ingenAdresser.resultat)
    }

    @Test
    fun vurderSammenhengendeAdresserINorgeSisteFemAar() {
        val avdoedPdlUtenAdresse = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, null, null)
        val avdoedPdlMedUtland = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, adresseDanmarkPdl(), null)
        val avdoedPdlUtenUtland = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, adresserNorgePdl(), null)
        val avdoedPdlUtlandFoerFemAar = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, adresseUtlandFoerFemAar(), null)

        val utenlandsopphold =
            kriterieSammenhengendeBostedsadresserINorgeSisteFemAar(
                mapTilVilkaarstypePerson(avdoedPdlMedUtland),
                Kriterietyper.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR
            )

        val ingenUtenlandsopphold =
            kriterieSammenhengendeBostedsadresserINorgeSisteFemAar(
                mapTilVilkaarstypePerson(avdoedPdlUtenUtland),
                Kriterietyper.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR
            )

        val utenlandsoppholdFoerFemAar =
            kriterieSammenhengendeBostedsadresserINorgeSisteFemAar(
                mapTilVilkaarstypePerson(avdoedPdlUtlandFoerFemAar),
                Kriterietyper.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR
            )

        val ingenAdresser =
            kriterieSammenhengendeBostedsadresserINorgeSisteFemAar(
                mapTilVilkaarstypePerson(avdoedPdlUtenAdresse),
                Kriterietyper.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR
            )

        assertEquals(VurderingsResultat.IKKE_OPPFYLT, utenlandsopphold.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsopphold.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, utenlandsoppholdFoerFemAar.resultat)
        assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, ingenAdresser.resultat)
    }

    companion object {
        val fnrAvdoed = Foedselsnummer.of("19078504903")
        val doedsdatoPdl: LocalDate = LocalDate.parse("2022-03-25")

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
                )
            )
        )

        val ingenUtenlandsoppholdAvdoedSoeknad = Utenlandsopphold(
            JaNeiVetIkke.NEI,
            listOf()
        )
    }
}