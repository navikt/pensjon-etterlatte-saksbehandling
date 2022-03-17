package barnepensjon

import no.nav.etterlatte.barnepensjon.kriterieIngenUtenlandsoppholdSisteFemAar
import no.nav.etterlatte.barnepensjon.setVikaarVurderingsResultat
import no.nav.etterlatte.barnepensjon.vilkaarBrukerErUnder20
import no.nav.etterlatte.barnepensjon.vilkaarDoedsfallErRegistrert
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.UtenlandsadresseBarn
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Utenlandsopphold
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.UtenlandsoppholdOpplysninger
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.OppholdUtlandType
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VilkaarVurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

internal class BarnepensjonVilkaarTest {

    @Test
    fun vurderAlderErUnder20() {
        val personBarnOver20 = lagMockPersonPdl(foedselsdatoBarnOver20, null, null, Bosted.NORGE, null)
        val personBarnUnder20 = lagMockPersonPdl(foedselsdatoBarnUnder20, null, null, Bosted.NORGE, null)
        val personAvdoedMedDoedsdato = lagMockPersonPdl(foedselsdatoBarnUnder20, null, doedsdatoPdl, Bosted.NORGE, null)
        val personAvdoedUtenDoedsdato = lagMockPersonPdl(foedselsdatoBarnUnder20, null, null, Bosted.NORGE, null)

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

        assertEquals(vurderingBarnOver20.resultat, VilkaarVurderingsResultat.IKKE_OPPFYLT)
        assertEquals(vurderingBarnUnder20.resultat, VilkaarVurderingsResultat.OPPFYLT)
        assertEquals(
            vurderingBarnUnder20UtenDoedsdato.resultat,
            VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        )

    }

    @Test
    fun vurderDoedsdatoErRegistrert() {
        val avdoedIngenDoedsdato = lagMockPersonPdl(null, fnrAvdoed, null, null, null)
        val avdoedRegistrertDoedsdato = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, null, null)
        val barnAvdoedErForeldre = lagMockPersonPdl(null, null, null, null, avdoedErForeldre)
        val barnAvdoedErIkkeForeldre = lagMockPersonPdl(null, null, null, null, avdoedErIkkeForeldre)

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

        assertEquals(doedsdatoIkkeIPdl.resultat, VilkaarVurderingsResultat.IKKE_OPPFYLT)
        assertEquals(avdoedErForelder.resultat, VilkaarVurderingsResultat.OPPFYLT)
        assertEquals(avdoedIkkeForelder.resultat, VilkaarVurderingsResultat.IKKE_OPPFYLT)

    }

    @Test
    fun vurderAvdoedesForutgaaendeMeldemskap() {
        val avdoedRegistrertDoedsdato = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, null, null)

        val utenlandsopphold =
            kriterieIngenUtenlandsoppholdSisteFemAar(
                listOf(mapTilVilkaarstypeUtenlandsopphold(utenlandsoppholdSoeknad)),
                mapTilVilkaarstypePerson(avdoedRegistrertDoedsdato)
            )

        val ingenUtenlandsopphold =
            kriterieIngenUtenlandsoppholdSisteFemAar(
                listOf(
                    mapTilVilkaarstypeUtenlandsopphold(
                        ingenUtenlandsoppholdSoeknad
                    )
                ), mapTilVilkaarstypePerson(avdoedRegistrertDoedsdato)
            )

        assertEquals(utenlandsopphold.resultat, VilkaarVurderingsResultat.IKKE_OPPFYLT)
        assertEquals(ingenUtenlandsopphold.resultat, VilkaarVurderingsResultat.OPPFYLT)
    }

    /*
    @Test
    fun vuderBarnetsMedlemskap() {
        val test = lagMockPersonPdl(foedselsdatoBarnOver20)

        val ingenOppgittUtenlandsadresse =
            kriterieHarIkkeOppgittAdresseIUtlandet(listOf(ingenUtenlandsadresseBarnVilkaarOpplysning))
        val harOppgittUtenlandsadresse =
            kriterieHarIkkeOppgittAdresseIUtlandet(listOf(harUtenlandsadresseBarnVilkaarOpplysning))

        val ingenUtenlandskBostedadresseEtterDoedsdato =
            kriterieHarIkkeBostedsadresseIUtlandet(
                listOf(ingenUtenlandsBostedadresseVilkaarOpplysning),
                listOf(doedsdatoForelderPdl)
            )

        val harUtenlandskBostedadresseEtterDoedsdato =
            kriterieHarIkkeBostedsadresseIUtlandet(
                listOf(harUtenlandsBostedadresseVilkaarOpplysning),
                listOf(doedsdatoForelderPdl)
            )

        val ingenBostedAdresser = kriterieHarIkkeBostedsadresseIUtlandet(
            listOf(ingenBostedadresseVilkaarOpplysning),
            listOf(doedsdatoForelderPdl)
        )

        val ingenUtenlandskOppholdadresseEtterDoedsdato =
            kriterieHarIkkeOppholddsadresseIUtlandet(
                listOf(ingenUtenlandsOppholdadresseVilkaarOpplysning),
                listOf(doedsdatoForelderPdl)
            )

        val harUtenlandskOppholdadresseEtterDoedsdato =
            kriterieHarIkkeOppholddsadresseIUtlandet(
                listOf(harUtenlandsOppholdadresseVilkaarOpplysning),
                listOf(doedsdatoForelderPdl)
            )

        assertEquals(VilkaarVurderingsResultat.OPPFYLT, ingenOppgittUtenlandsadresse.resultat)
        assertEquals(VilkaarVurderingsResultat.IKKE_OPPFYLT, harOppgittUtenlandsadresse.resultat)
        assertEquals(VilkaarVurderingsResultat.OPPFYLT, ingenUtenlandskBostedadresseEtterDoedsdato.resultat)
        assertEquals(VilkaarVurderingsResultat.IKKE_OPPFYLT, harUtenlandskBostedadresseEtterDoedsdato.resultat)
        assertEquals(VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, ingenBostedAdresser.resultat)
        assertEquals(VilkaarVurderingsResultat.OPPFYLT, ingenUtenlandskOppholdadresseEtterDoedsdato.resultat)
        assertEquals(VilkaarVurderingsResultat.IKKE_OPPFYLT, harUtenlandskOppholdadresseEtterDoedsdato.resultat)

    }
    */



    @Test
    fun vurderVilkaarsVurdering() {
        val kriterieOppfylt =
            Kriterie(Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO, VilkaarVurderingsResultat.OPPFYLT, listOf())
        val kriterieIkkeOppfylt =
            Kriterie(
                Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
                VilkaarVurderingsResultat.IKKE_OPPFYLT,
                listOf()
            )
        val kriterieKanIkkeVurdere = Kriterie(
            Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
            VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            listOf()
        )

        val vilkaarKriterierOppfylt = setVikaarVurderingsResultat(listOf(kriterieOppfylt, kriterieOppfylt))
        val vilkaarEtKriterieIkkeOppfylt =
            setVikaarVurderingsResultat(listOf(kriterieOppfylt, kriterieIkkeOppfylt, kriterieKanIkkeVurdere))
        val vilkaarKriterierOppfyltOgKanIkkeHentesUt =
            setVikaarVurderingsResultat(listOf(kriterieOppfylt, kriterieKanIkkeVurdere, kriterieOppfylt))

        assertEquals(VilkaarVurderingsResultat.OPPFYLT, vilkaarKriterierOppfylt)
        assertEquals(VilkaarVurderingsResultat.IKKE_OPPFYLT, vilkaarEtKriterieIkkeOppfylt)
        assertEquals(
            VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            vilkaarKriterierOppfyltOgKanIkkeHentesUt
        )
    }

    fun mapTilVilkaarstypeUtenlandsopphold(opphold: Utenlandsopphold): VilkaarOpplysning<Utenlandsopphold> {
        return VilkaarOpplysning(
            Opplysningstyper.SOEKER_PDL_V1,
            Behandlingsopplysning.Privatperson("", Instant.now()),
            opphold
        )
    }

    fun mapTilVilkaarstypePerson(person: Person): VilkaarOpplysning<Person> {
        return VilkaarOpplysning(
            Opplysningstyper.SOEKER_PDL_V1,
            Behandlingsopplysning.Pdl("pdl", Instant.now(), null),
            person
        )
    }


    val foedselsdatoBarnOver20 = LocalDate.parse("2000-08-29")
    val foedselsdatoBarnUnder20 = LocalDate.parse("2020-06-10")
    val doedsdatoPdl = LocalDate.parse("2022-01-25")

    val fnrAvdoed = Foedselsnummer.of("19078504903")
    val avdoedErForeldre = listOf(Foedselsnummer.of("19078504903"))
    val avdoedErIkkeForeldre = listOf(Foedselsnummer.of("11057523044"))


    val utenlandsoppholdSoeknad = Utenlandsopphold(
        "JA",
        listOf(
            UtenlandsoppholdOpplysninger(
                "Danmark",
                LocalDate.parse("2010-01-25"),
                LocalDate.parse("2022-01-25"),
                listOf(OppholdUtlandType.ARBEIDET),
                "JA",
                null
            ),
            UtenlandsoppholdOpplysninger(
                "Costa Rica",
                LocalDate.parse("2000-01-25"),
                LocalDate.parse("2007-01-25"),
                listOf(OppholdUtlandType.ARBEIDET),
                "NEI",
                null
            ),

            ),
        "19078504903"
    )


    val ingenUtenlandsoppholdSoeknad = Utenlandsopphold(
        "NEI",
        listOf(),
        "19078504903"
    )

    val ingenUtenlandsadresseBarnVilkaarOpplysning =
        UtenlandsadresseBarn("NEI", null, null, null)

    val harUtenlandsadresseBarnVilkaarOpplysning = UtenlandsadresseBarn("JA", null, null, null)
    val ingenBostedadresseVilkaarOpplysning = null

    enum class Bosted {
        NORGE,
        UTLAND,
        FINNESIKKE
    }

    fun lagMockPersonPdl(
        foedselsdato: LocalDate?,
        foedselsnummer: Foedselsnummer?,
        doedsdato: LocalDate?,
        bosted: Bosted?,
        foreldre: List<Foedselsnummer>?
    ): Person {
        val foedselsdato = foedselsdato ?: LocalDate.parse("2020-06-10")
        val foedselsnummer = if (foedselsnummer == null) Foedselsnummer.of("19078504903") else foedselsnummer
        val adresse = if (bosted == Bosted.NORGE) {
            adresserNorge
        } else if (bosted == Bosted.UTLAND) {
            adresseDanmark
        } else {
            null
        }

        return Person(
            "Test",
            "Testulfsen",
            foedselsnummer,
            foedselsdato,
            1920,
            null,
            doedsdato,
            Adressebeskyttelse.UGRADERT,
            adresse,
            null,
            adresse,
            adresse,
            null,
            null,
            null,
            FamilieRelasjon(null, foreldre, null)
        )
    }


    val adresserNorge = listOf(
        Adresse(
            AdresseType.VEGADRESSE,
            true,
            null,
            null,
            null,
            null,
            null,
            null,
            "NOR",
            "kilde",
            LocalDateTime.parse("2025-01-26T00:00:00"),
            null
        ),
        Adresse(
            AdresseType.VEGADRESSE,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            "NOR",
            "kilde",
            LocalDateTime.parse("2010-01-25T00:00:00"),
            LocalDateTime.parse("2025-01-30T00:00:00"),
        )
    )

    val adresseDanmark = listOf(
        Adresse(
            AdresseType.UTENLANDSKADRESSE,
            true,
            null,
            null,
            null,
            null,
            null,
            null,
            "DAN",
            "kilde",
            LocalDateTime.parse("2022-01-25T00:00:00"),
            null
        )
    )

}