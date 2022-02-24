package no.nav.etterlatte.fordeler

import no.nav.etterlatte.FordelerKriterie
import no.nav.etterlatte.FordelerKriterierService
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.ForeldreAnsvar
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.mockPerson
import no.nav.etterlatte.mockNorskAdresse
import no.nav.etterlatte.mockUgyldigNorskAdresse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.time.LocalDate.now
import java.time.format.DateTimeFormatter

class FordelerKriterieServiceTest {

    private val fordelerKriterierService = FordelerKriterierService()

    @Test
    fun `soeknad er en gyldig kandidat for fordeling`() {
        val barn = mockPerson(
            adresse = mockNorskAdresse()
        )
        val avdoed = mockPerson(
            doedsdato = "2022-01-01",
            adresse = mockNorskAdresse()
        )
        val gjenlevende = mockPerson(
            adresse = mockNorskAdresse()
        )

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.isEmpty())
    }

    @Test
    fun `barn som er for gammelt er ikke en gyldig kandidat`() {
        val barn = mockPerson(
            foedselsaar = now().year - 15,
            foedselsdato = now().minusYears(15).format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
        val avdoed = mockPerson()
        val gjenlevende = mockPerson()

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.BARN_ER_FOR_GAMMELT))
    }

    @Test
    fun `barn som ikke er norsk statsborger er ikke en gyldig kandidat`() {
        val barn = mockPerson(statsborgerskap = "SWE")
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
    fun `barn som ikke er fodt i Norge er ikke en gyldig kandidat`() {
        val barn = mockPerson(foedeland = "SWE")
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
                    tilflyttingsland = "SWE",
                    dato = "2010-01-01"
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
            adresse = mockUgyldigNorskAdresse()
        )
        val avdoed = mockPerson()
        val gjenlevende = mockPerson()

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.BARN_ER_IKKE_BOSATT_I_NORGE))
    }

    @Test
    fun `avdod som har utvandret er ikke en gyldig kandidat`() {
        val barn = mockPerson()
        val avdoed = mockPerson(
            utland = Utland(
                utflyttingFraNorge = listOf(UtflyttingFraNorge(
                    tilflyttingsland = "SWE",
                    dato = "2010-01-01"
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
            adresse = mockUgyldigNorskAdresse()
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
            adresse = mockUgyldigNorskAdresse()
        )

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.GJENLEVENDE_ER_IKKE_BOSATT_I_NORGE))
    }

    @Test
    fun `gjenlevende uten foreldreansvar er ikke en gyldig kandidat`() {
        val barn = mockPerson(
            familieRelasjon = FamilieRelasjon(
                ansvarligeForeldre = listOf(ForeldreAnsvar(Foedselsnummer.of("09018701453"))),
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
        val BARNEPENSJON_SOKNAD = readSoknad("/NyBarnePensjon.json")
        val BARNEPENSJON_SOKNAD_YRKESSKADE = readSoknad("/yrkesskade.json")
        val BARNEPENSJON_SOKNAD_VERGE = readSoknad("/verge.json")
        val BARNEPENSJON_SOKNAD_HUKET_AV_UTLAND = readSoknad("/huketAvForUtland.json")
        val BARNEPENSJON_SOKNAD_INNSENDER_IKKE_FORELDER = readSoknad("/innsenderIkkeForelder.json")

        private fun readSoknad(file: String) =
            JsonMessage(readFile(file), MessageProblems("")).apply { requireKey("@skjema_info") }

        private fun readSoknadAsBarnepensjon(file: String): Barnepensjon {
            val skjemaInfo = objectMapper.writeValueAsString(objectMapper.readTree(readFile(file)).get("@skjema_info"))
            return objectMapper.readValue(skjemaInfo, Barnepensjon::class.java)
        }

        private fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }

}