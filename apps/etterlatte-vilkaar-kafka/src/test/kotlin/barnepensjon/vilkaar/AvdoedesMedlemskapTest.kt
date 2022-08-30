package barnepensjon.vilkaar

import adresseDanmarkPdl
import adresseUtlandFoerFemAar
import adresserNorgePdl
import barnepensjon.vilkaar.avdoedesmedlemskap.MedlemskapsPeriode
import barnepensjon.vilkaar.avdoedesmedlemskap.kriterieHarHatt80prosentStillingSisteFemAar
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
import no.nav.etterlatte.barnepensjon.Periode
import no.nav.etterlatte.libs.common.arbeidsforhold.ArbeidsforholdOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Utenlandsopphold
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.UtenlandsoppholdOpplysninger
import no.nav.etterlatte.libs.common.inntekt.InntektsOpplysning
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.time.LocalDate

class AvdoedesMedlemskapTest {

    @Test
    fun `Oppfyller kriteriet for pensjon-ufoere siste 5 aar hvis det er utbetalinger i hele perioden`() {
        val file = readFile("/inntektsopplysning.json")
        val opplysning = objectMapper.readValue<VilkaarOpplysning<InntektsOpplysning>>(file)

        kriterieHarMottattPensjonEllerTrygdSisteFemAar(LocalDate.parse("2022-07-01"), opplysning).let {
            assertEquals(it.resultat, VurderingsResultat.OPPFYLT)
        }
    }

    @Test
    fun `Oppfyller ikke kriteriet for pensjon-ufoere siste 5 aar hvis det er gaps i perioden`() {
        val file = readFile("/inntektsopplysning.json")
        val inntektsOpplysning = objectMapper.readValue<VilkaarOpplysning<InntektsOpplysning>>(file)

        kriterieHarMottattPensjonEllerTrygdSisteFemAar(LocalDate.parse("2021-07-01"), inntektsOpplysning).let {
            assertEquals(it.resultat, VurderingsResultat.IKKE_OPPFYLT)
            val opplysning = it.basertPaaOpplysninger[0].opplysning as MedlemskapsPeriode
            assertEquals(1, opplysning.gaps?.size)
        }
    }

    @Test
    fun `Arbeidsforhold siste fem aar oppfylt naar man har arbeidsforhold hele perioden`() {
        val inntekter =
            objectMapper.readValue<VilkaarOpplysning<InntektsOpplysning>>(readFile("/inntektsopplysning.json"))
        val arbeidsforhold =
            objectMapper.readValue<VilkaarOpplysning<ArbeidsforholdOpplysning>>(readFile("/arbeidsforhold100.json"))

        val kriterie = kriterieHarHatt80prosentStillingSisteFemAar(
            doedsdato = LocalDate.parse("2022-07-01"),
            arbeidsforholdOpplysning = arbeidsforhold,
            inntektsOpplysning = inntekter
        )

        assertEquals(VurderingsResultat.OPPFYLT, kriterie.resultat)
    }

    @Test
    fun `Arbeidsforhold siste fem aar ikke oppfylt dersom man har under 80 prosent stilling`() {
        val inntekter =
            objectMapper.readValue<VilkaarOpplysning<InntektsOpplysning>>(readFile("/inntektsopplysning.json"))
        val arbeidsforhold =
            objectMapper.readValue<VilkaarOpplysning<ArbeidsforholdOpplysning>>(readFile("/arbeidsforhold75.json"))

        val kriterie = kriterieHarHatt80prosentStillingSisteFemAar(
            doedsdato = LocalDate.parse("2022-07-01"),
            arbeidsforholdOpplysning = arbeidsforhold,
            inntektsOpplysning = inntekter
        )

        assertEquals(VurderingsResultat.IKKE_OPPFYLT, kriterie.resultat)
    }

    @Test
    fun `Arbeidsforhold siste fem aar ikke oppfylt dersom man har opphold i arbeidsforhold`() {
        val inntekter =
            objectMapper.readValue<VilkaarOpplysning<InntektsOpplysning>>(readFile("/inntektsopplysning.json"))
        val arbeidsforhold = objectMapper.readValue<VilkaarOpplysning<ArbeidsforholdOpplysning>>(
            readFile("/arbeidsforholdMedOpphold.json")
        )

        val kriterie = kriterieHarHatt80prosentStillingSisteFemAar(
            doedsdato = LocalDate.parse("2022-07-01"),
            arbeidsforholdOpplysning = arbeidsforhold,
            inntektsOpplysning = inntekter
        )

        assertEquals(VurderingsResultat.IKKE_OPPFYLT, kriterie.resultat)
        kriterie.basertPaaOpplysninger.let {
            val opplysning = it[0].opplysning as MedlemskapsPeriode
            assertEquals(2, opplysning.arbeidsforhold?.size)
            assertEquals(1, opplysning.gaps?.size)
            assertEquals(Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)), opplysning.gaps?.first())
        }
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

    private fun readFile(file: String) = AvdoedesMedlemskapTest::class.java.getResource(file)?.readText()
        ?: throw FileNotFoundException("Fant ikke filen $file")
}