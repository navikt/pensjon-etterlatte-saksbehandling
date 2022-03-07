package no.nav.etterlatte.fordeler

import no.nav.etterlatte.FNR_2
import no.nav.etterlatte.FNR_4
import no.nav.etterlatte.FNR_5
import no.nav.etterlatte.SVERIGE
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.mockNorskAdresse
import no.nav.etterlatte.mockPerson
import no.nav.etterlatte.mockUgyldigAdresse
import no.nav.etterlatte.readSoknad
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDateTime

internal class FordelerKriterierServiceTest {

    private val fordelerKriterierService = FordelerKriterierService()

    @Test
    fun `soeknad er en gyldig kandidat for fordeling`() {
        val barn = mockPerson(
            bostedsadresse = mockNorskAdresse(),
            familieRelasjon = FamilieRelasjon(
                barn = null,
                ansvarligeForeldre = null,
                foreldre = listOf(Foedselsnummer.of(FNR_2)),
            )
        )
        val avdoed = mockPerson(
            fnr = FNR_2,
            doedsdato = LocalDate.parse("2022-01-01"),
            bostedsadresse = mockNorskAdresse(
                gyldigTilOgMed = LocalDateTime.parse("2022-01-01T00:00:00")
            ),
            familieRelasjon = FamilieRelasjon(
                barn = listOf(barn.foedselsnummer),
                ansvarligeForeldre = null,
                foreldre = listOf(Foedselsnummer.of(FNR_2)),
            )
        )
        val gjenlevende = mockPerson(
            bostedsadresse = mockNorskAdresse()
        )

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.isEmpty())
    }

    @Test
    fun `barn som er for gammelt er ikke en gyldig kandidat`() {
        val barn = mockPerson(
            foedselsaar = now().year - 15,
            foedselsdato = now().minusYears(15)
        )
        val avdoed = mockPerson()
        val gjenlevende = mockPerson()

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.BARN_ER_FOR_GAMMELT))
    }

    @Test
    fun `barn som ikke er norsk statsborger er ikke en gyldig kandidat`() {
        val barn = mockPerson(statsborgerskap = SVERIGE)
        val avdoed = mockPerson()
        val gjenlevende = mockPerson()

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.BARN_ER_IKKE_NORSK_STATSBORGER))
    }

    @Test
    fun `barn som har adressebeskyttelse er ikke en gyldig kandidat`() {
        val barn = mockPerson(adressebeskyttelse = Adressebeskyttelse.STRENGT_FORTROLIG)
        val avdoed = mockPerson()
        val gjenlevende = mockPerson()

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.BARN_HAR_ADRESSEBESKYTTELSE))
    }

    @Test
    fun `barn som har huket av for utenlandsadresse er ikke en gyldig kandidat`() {
        val barn = mockPerson()
        val avdoed = mockPerson()
        val gjenlevende = mockPerson()

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD_UTENLANDSADRESSE)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.BARN_HAR_HUKET_AV_UTLANDSADRESSE))
    }

    @Test
    fun `barn som ikke er fodt i Norge er ikke en gyldig kandidat`() {
        val barn = mockPerson(foedeland = SVERIGE)
        val avdoed = mockPerson()
        val gjenlevende = mockPerson()

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.BARN_ER_IKKE_FOEDT_I_NORGE))
    }

    @Test
    fun `barn som har utvandret er ikke en gyldig kandidat`() {
        val barn = mockPerson(
            utland = Utland(
                utflyttingFraNorge = listOf(
                    UtflyttingFraNorge(
                    tilflyttingsland = SVERIGE,
                    dato = LocalDate.parse("2010-01-01")
                )
                ),
                innflyttingTilNorge = null
            )
        )
        val avdoed = mockPerson()
        val gjenlevende = mockPerson()

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.BARN_HAR_UTVANDRING))
    }

    @Test
    fun `barn som har verge er ikke en gyldig kandidat`() {
        val barn = mockPerson()
        val avdoed = mockPerson()
        val gjenlevende = mockPerson()

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD_VERGE)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.BARN_HAR_VERGE))
    }

    @Test
    fun `barn uten bostedsadresse i Norge er ikke en gyldig kandidat`() {
        val barn = mockPerson(
            bostedsadresse = mockUgyldigAdresse()
        )
        val avdoed = mockPerson()
        val gjenlevende = mockPerson()

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.BARN_ER_IKKE_BOSATT_I_NORGE))
    }

    @Test
    fun `barn med sosken er ikke en gyldig kandidat`() {
        val barn = mockPerson(
            bostedsadresse = mockNorskAdresse()
        )
        val avdoed = mockPerson(
            bostedsadresse = mockNorskAdresse(),
            familieRelasjon = FamilieRelasjon(
                barn = listOf(Foedselsnummer.of(FNR_4)),
                ansvarligeForeldre = null,
                foreldre = null,
            )
        )
        val gjenlevende = mockPerson(
            bostedsadresse = mockNorskAdresse()
        )

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.BARN_ER_IKKE_ALENEBARN))
    }

    @Test
    fun `avdod som ikke er forelder til soker er ikke en gyldig kandidat`() {
        val barn = mockPerson(
            bostedsadresse = mockNorskAdresse(),
            familieRelasjon = FamilieRelasjon(
                barn = null,
                ansvarligeForeldre = null,
                foreldre = listOf(Foedselsnummer.of(FNR_2)),
            )
        )
        val avdoed = mockPerson(
            bostedsadresse = mockNorskAdresse(),
            doedsdato = now()
        )
        val gjenlevende = mockPerson(
            bostedsadresse = mockNorskAdresse(),
        )

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.AVDOED_ER_IKKE_FORELDER_TIL_BARN))
    }

    @Test
    fun `avdod som har utvandret er ikke en gyldig kandidat`() {
        val barn = mockPerson()
        val avdoed = mockPerson(
            utland = Utland(
                utflyttingFraNorge = listOf(UtflyttingFraNorge(
                    tilflyttingsland = SVERIGE,
                    dato = LocalDate.parse("2010-01-01")
                )),
                innflyttingTilNorge = null
            )
        )
        val gjenlevende = mockPerson()

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.AVDOED_HAR_UTVANDRING))
    }

    @Test
    fun `avdod som har yrkesskade i soknad er ikke en gyldig kandidat`() {
        val barn = mockPerson()
        val avdoed = mockPerson()
        val gjenlevende = mockPerson()

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD_YRKESSKADE)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.AVDOED_HAR_YRKESSKADE))
    }

    @Test
    fun `avdod som ikke er registrert som dod er ikke en gyldig kandidat`() {
        val barn = mockPerson()
        val avdoed = mockPerson(
            doedsdato = null,
        )
        val gjenlevende = mockPerson()

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.AVDOED_ER_IKKE_REGISTRERT_SOM_DOED))
    }

    @Test
    fun `avdod hvor det er huket av for utlandsopphold er ikke en gyldig kandidat`() {
        val barn = mockPerson()
        val avdoed = mockPerson()
        val gjenlevende = mockPerson()

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD_HUKET_AV_UTLAND)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.AVDOED_HAR_HATT_UTLANDSOPPHOLD))
    }

    @Test
    fun `avdod uten bostedsadresse i Norge er ikke en gyldig kandidat`() {
        val barn = mockPerson()
        val avdoed = mockPerson(
            bostedsadresse = mockUgyldigAdresse()
        )
        val gjenlevende = mockPerson()

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.AVDOED_VAR_IKKE_BOSATT_I_NORGE))
    }

    @Test
    fun `avdod med gyldig bostedsadresse i Norge foer doedsdato er ikke en gyldig kandidat`() {
        val barn = mockPerson()
        val avdoed = mockPerson(
            doedsdato = LocalDate.parse("2022-01-03"),
            bostedsadresse = mockUgyldigAdresse(
                type = AdresseType.VEGADRESSE,
                gyldigFraOgMed = LocalDateTime.parse("2022-01-01T00:00:00"),
                gyldigTilOgMed = LocalDateTime.parse("2022-01-02T00:00:00")
            )
        )
        val gjenlevende = mockPerson()

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.AVDOED_VAR_IKKE_BOSATT_I_NORGE))
    }

    @Test
    fun `avdod med gyldig bostedsadresse i Norge etter doedsdato er ikke en gyldig kandidat`() {
        val barn = mockPerson()
        val avdoed = mockPerson(
            doedsdato = LocalDate.parse("2022-01-01"),
            bostedsadresse = mockUgyldigAdresse(
                type = AdresseType.VEGADRESSE,
                gyldigFraOgMed = LocalDateTime.parse("2022-01-02T00:00:00"),
                gyldigTilOgMed = LocalDateTime.parse("2022-01-03T00:00:00")
            )
        )
        val gjenlevende = mockPerson()

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.AVDOED_VAR_IKKE_BOSATT_I_NORGE))
    }

    @Test
    fun `innsender som ikke er forelder er ikke en gyldig kandidat`() {
        val barn = mockPerson()
        val avdoed = mockPerson()
        val gjenlevende = mockPerson()

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD_INNSENDER_IKKE_FORELDER)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.INNSENDER_ER_IKKE_FORELDER))
    }

    @Test
    fun `gjenlevende uten bostedsadresse i Norge er ikke en gyldig kandidat`() {
        val barn = mockPerson()
        val avdoed = mockPerson()
        val gjenlevende = mockPerson(
            bostedsadresse = mockUgyldigAdresse()
        )

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.GJENLEVENDE_ER_IKKE_BOSATT_I_NORGE))
    }

    @Test
    fun `gjenlevende og barn med ulike adresser er ikke en gyldig kandidat`() {
        val barn = mockPerson(
            bostedsadresse = mockNorskAdresse()
        )
        val avdoed = mockPerson(
            bostedsadresse = mockNorskAdresse()
        )
        val gjenlevende = mockPerson(
            bostedsadresse = mockNorskAdresse(adresseLinje1 = "En annen vei 2")
        )

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.GJENLEVENDE_OG_BARN_HAR_IKKE_SAMME_ADRESSE))
    }

    @Test
    fun `gjenlevende uten foreldreansvar er ikke en gyldig kandidat`() {
        val barn = mockPerson(
            familieRelasjon = FamilieRelasjon(
                ansvarligeForeldre = listOf(Foedselsnummer.of(FNR_5)),
                foreldre = null,
                barn = null,
            )
        )
        val avdoed = mockPerson()
        val gjenlevende = mockPerson()

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.GJENLEVENDE_HAR_IKKE_FORELDREANSVAR))
    }

    companion object {
        val BARNEPENSJON_SOKNAD = readSoknad("/fordeler/soknad_barnepensjon.json")
        val BARNEPENSJON_SOKNAD_UTENLANDSADRESSE = readSoknad("/fordeler/soknad_barn_har_utenlandsadresse.json")
        val BARNEPENSJON_SOKNAD_YRKESSKADE = readSoknad("/fordeler/soknad_har_yrkesskade.json")
        val BARNEPENSJON_SOKNAD_VERGE = readSoknad("/fordeler/soknad_har_verge.json")
        val BARNEPENSJON_SOKNAD_HUKET_AV_UTLAND = readSoknad("/fordeler/soknad_huket_av_utland.json")
        val BARNEPENSJON_SOKNAD_INNSENDER_IKKE_FORELDER = readSoknad("/fordeler/soknad_innsender_ikke_forelder.json")
    }

}