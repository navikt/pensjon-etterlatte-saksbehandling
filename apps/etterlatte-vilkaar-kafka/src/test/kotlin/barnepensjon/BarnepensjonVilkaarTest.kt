package barnepensjon

import no.nav.etterlatte.barnepensjon.kriterieIngenUtenlandsoppholdSisteFemAar
import no.nav.etterlatte.barnepensjon.setVikaarVurderingFraKriterier
import no.nav.etterlatte.barnepensjon.vilkaarBrukerErUnder20
import no.nav.etterlatte.barnepensjon.vilkaarDoedsfallErRegistrert
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Forelder
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.SoekerBarnSoeknad
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.UtenlandsadresseBarn
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Utenlandsopphold
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.UtenlandsoppholdOpplysninger
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Verge
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.OppholdUtlandType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.vilkaar.barnepensjon.vilkaarBarnetsMedlemskap
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

internal class BarnepensjonVilkaarTest {
    @Test
    fun vurderAlderErUnder20() {
        val personBarnOver20 = lagMockPersonPdl(foedselsdatoBarnOver20, fnrBarn, null, adresserNorgePdl, null)
        val personBarnUnder20 = lagMockPersonPdl(foedselsdatoBarnUnder20, fnrBarn, null, adresserNorgePdl, null)
        val personAvdoedMedDoedsdato =
            lagMockPersonPdl(foedselsdatoBarnUnder20, fnrAvdoed, doedsdatoPdl, adresserNorgePdl, null)
        val personAvdoedUtenDoedsdato =
            lagMockPersonPdl(foedselsdatoBarnUnder20, fnrAvdoed, null, adresserNorgePdl, null)

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

        assertEquals(vurderingBarnOver20.resultat, VurderingsResultat.IKKE_OPPFYLT)
        assertEquals(vurderingBarnUnder20.resultat, VurderingsResultat.OPPFYLT)
        assertEquals(
            vurderingBarnUnder20UtenDoedsdato.resultat,
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        )

    }

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

        assertEquals(doedsdatoIkkeIPdl.resultat, VurderingsResultat.IKKE_OPPFYLT)
        assertEquals(avdoedErForelder.resultat, VurderingsResultat.OPPFYLT)
        assertEquals(avdoedIkkeForelder.resultat, VurderingsResultat.IKKE_OPPFYLT)

    }

    @Test
    fun vurderAvdoedesForutgaaendeMeldemskap() {
        val avdoedRegistrertDoedsdato = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, null, null)
        val avdoedSoknadMedAdresse = lagMockPersonAvdoedSoeknad(utenlandsoppholdAvdoedSoeknad)
        val avdoedSoeknadUtenUtland = lagMockPersonAvdoedSoeknad(ingenUtenlandsoppholdAvdoedSoeknad)

        val utenlandsopphold =
            kriterieIngenUtenlandsoppholdSisteFemAar(
                mapTilVilkaarstypeAvdoedSoeknad(avdoedSoknadMedAdresse),
                mapTilVilkaarstypePerson(avdoedRegistrertDoedsdato)
            )

        val ingenUtenlandsopphold =
            kriterieIngenUtenlandsoppholdSisteFemAar(
                mapTilVilkaarstypeAvdoedSoeknad(avdoedSoeknadUtenUtland),
                mapTilVilkaarstypePerson(avdoedRegistrertDoedsdato)
            )

        assertEquals(utenlandsopphold.resultat, VurderingsResultat.IKKE_OPPFYLT)
        assertEquals(ingenUtenlandsopphold.resultat, VurderingsResultat.OPPFYLT)
    }


    @Test
    fun vuderBarnetsMedlemskap() {
        val avdoedRegistrertDoedsdato = lagMockPersonPdl(null, fnrAvdoed, doedsdatoPdl, null, null)
        val gjenlevendePdlNorge = lagMockPersonPdl(null, fnrGjenlevende, null, adresserNorgePdl, null)
        val gjenlevendePdlDanmark = lagMockPersonPdl(null, fnrGjenlevende, null, adresseDanmarkPdl, null)

        val barnPdlNorge = lagMockPersonPdl(foedselsdatoBarnUnder20, fnrBarn, null, adresserNorgePdl, avdoedErForeldre)
        val barnPdlDanmark =
            lagMockPersonPdl(foedselsdatoBarnUnder20, fnrBarn, null, adresseDanmarkPdl, avdoedErForeldre)
        val barnSoeknadNorge = lagMockPersonSoekerSoeknad(UtenlandsadresseBarn(JaNeiVetIkke.NEI, null, null))
        val barnSoeknadDanmark = lagMockPersonSoekerSoeknad(UtenlandsadresseBarn(JaNeiVetIkke.JA, null, null))

        val ingenUtenlandsAdresser = vilkaarBarnetsMedlemskap(
            Vilkaartyper.BARNETS_MEDLEMSKAP,
            mapTilVilkaarstypePerson(barnPdlNorge),
            mapTilVilkaarstypeSoekerSoeknad(barnSoeknadNorge),
            mapTilVilkaarstypePerson(gjenlevendePdlNorge),
            mapTilVilkaarstypePerson(avdoedRegistrertDoedsdato),
        )

        val barnUtenlandsAdresserPdl = vilkaarBarnetsMedlemskap(
            Vilkaartyper.BARNETS_MEDLEMSKAP,
            mapTilVilkaarstypePerson(barnPdlDanmark),
            mapTilVilkaarstypeSoekerSoeknad(barnSoeknadNorge),
            mapTilVilkaarstypePerson(gjenlevendePdlNorge),
            mapTilVilkaarstypePerson(avdoedRegistrertDoedsdato),
        )

        val barnUtenlandsAdresserSoeknad = vilkaarBarnetsMedlemskap(
            Vilkaartyper.BARNETS_MEDLEMSKAP,
            mapTilVilkaarstypePerson(barnPdlNorge),
            mapTilVilkaarstypeSoekerSoeknad(barnSoeknadDanmark),
            mapTilVilkaarstypePerson(gjenlevendePdlNorge),
            mapTilVilkaarstypePerson(avdoedRegistrertDoedsdato),
        )

        val gjenlevendeUtenlandsAdresserPdl = vilkaarBarnetsMedlemskap(
            Vilkaartyper.BARNETS_MEDLEMSKAP,
            mapTilVilkaarstypePerson(barnPdlNorge),
            mapTilVilkaarstypeSoekerSoeknad(barnSoeknadNorge),
            mapTilVilkaarstypePerson(gjenlevendePdlDanmark),
            mapTilVilkaarstypePerson(avdoedRegistrertDoedsdato),
        )

        assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsAdresser.resultat)
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, barnUtenlandsAdresserPdl.resultat)
        assertEquals(
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            gjenlevendeUtenlandsAdresserPdl.resultat
        )
        assertEquals(
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            barnUtenlandsAdresserSoeknad.resultat
        )
    }


    @Test
    fun vurderVilkaarsVurdering() {
        val kriterieOppfylt =
            Kriterie(Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO, VurderingsResultat.OPPFYLT, listOf())
        val kriterieIkkeOppfylt =
            Kriterie(
                Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
                VurderingsResultat.IKKE_OPPFYLT,
                listOf()
            )
        val kriterieKanIkkeVurdere = Kriterie(
            Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            listOf()
        )

        val vilkaarKriterierOppfylt = setVikaarVurderingFraKriterier(listOf(kriterieOppfylt, kriterieOppfylt))
        val vilkaarEtKriterieIkkeOppfylt =
            setVikaarVurderingFraKriterier(listOf(kriterieOppfylt, kriterieIkkeOppfylt, kriterieKanIkkeVurdere))
        val vilkaarKriterierOppfyltOgKanIkkeHentesUt =
            setVikaarVurderingFraKriterier(listOf(kriterieOppfylt, kriterieKanIkkeVurdere, kriterieOppfylt))

        assertEquals(VurderingsResultat.OPPFYLT, vilkaarKriterierOppfylt)
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, vilkaarEtKriterieIkkeOppfylt)
        assertEquals(
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            vilkaarKriterierOppfyltOgKanIkkeHentesUt
        )
    }

    companion object {
        val foedselsdatoBarnOver20 = LocalDate.parse("2000-08-29")
        val foedselsdatoBarnUnder20 = LocalDate.parse("2020-06-10")
        val doedsdatoPdl = LocalDate.parse("2021-01-25")
        val fnrBarn = Foedselsnummer.of("19040550081")
        val fnrAvdoed = Foedselsnummer.of("19078504903")
        val fnrGjenlevende = Foedselsnummer.of("07081177656")
        val avdoedErForeldre = listOf(Foedselsnummer.of("19078504903"))
        val avdoedErIkkeForeldre = listOf(Foedselsnummer.of("11057523044"))

        fun mapTilVilkaarstypeAvdoedSoeknad(person: AvdoedSoeknad): VilkaarOpplysning<AvdoedSoeknad> {
            return VilkaarOpplysning(
                Opplysningstyper.AVDOED_SOEKNAD_V1,
                Behandlingsopplysning.Privatperson("", Instant.now()),
                person
            )
        }

        fun mapTilVilkaarstypeSoekerSoeknad(person: SoekerBarnSoeknad): VilkaarOpplysning<SoekerBarnSoeknad> {
            return VilkaarOpplysning(
                Opplysningstyper.SOEKER_PDL_V1,
                Behandlingsopplysning.Privatperson("", Instant.now()),
                person
            )
        }

        fun mapTilVilkaarstypePerson(person: Person): VilkaarOpplysning<Person> {
            return VilkaarOpplysning(
                Opplysningstyper.SOEKER_PDL_V1,
                Behandlingsopplysning.Pdl("pdl", Instant.now(), null),
                person
            )
        }
    }

    fun lagMockPersonAvdoedSoeknad(utenlandsopphold: Utenlandsopphold): AvdoedSoeknad {
        return AvdoedSoeknad(
            PersonType.AVDOED,
            "Fornavn",
            "Etternavn",
            Foedselsnummer.of("19078504903"),
            LocalDate.parse("2020-06-10"),
            "Norge",
            utenlandsopphold,
            JaNeiVetIkke.NEI
        )
    }

    fun lagMockPersonSoekerSoeknad(utland: UtenlandsadresseBarn): SoekerBarnSoeknad {
        return SoekerBarnSoeknad(
            PersonType.BARN,
            "Fornavn",
            "Etternavn",
            fnrBarn,
            "Norge",
            utland,
            listOf(Forelder(PersonType.AVDOED, "fornavn", "etternavn", fnrAvdoed)),
            Verge(null, null, null, null),
            null
        )
    }

    fun lagMockPersonPdl(
        foedselsdato: LocalDate? = LocalDate.parse("2020-06-10"),
        foedselsnummer: Foedselsnummer = Foedselsnummer.of("19078504903"),
        doedsdato: LocalDate?,
        adresse: List<Adresse>?,
        foreldre: List<Foedselsnummer>?
    ) = Person(
        fornavn = "Test",
        etternavn = "Testulfsen",
        foedselsnummer = foedselsnummer,
        foedselsdato = foedselsdato,
        foedselsaar = 1985,
        foedeland = null,
        doedsdato = doedsdato,
        adressebeskyttelse = Adressebeskyttelse.UGRADERT,
        bostedsadresse = adresse,
        deltBostedsadresse = null,
        kontaktadresse = adresse,
        oppholdsadresse = adresse,
        sivilstatus = null,
        statsborgerskap = null,
        utland = null,
        familieRelasjon = FamilieRelasjon(null, foreldre, null)
    )

    val adresserNorgePdl = listOf(
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

    val adresseDanmarkPdl = listOf(
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