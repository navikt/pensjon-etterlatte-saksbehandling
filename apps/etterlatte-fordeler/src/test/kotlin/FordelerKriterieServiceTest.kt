package no.nav.etterlatte.prosess

import no.nav.etterlatte.FordelerKriterie
import no.nav.etterlatte.FordelerKriterierService
import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.person.EyBostedsadresse
import no.nav.etterlatte.libs.common.person.EyFamilieRelasjon
import no.nav.etterlatte.libs.common.person.EyForeldreAnsvar
import no.nav.etterlatte.libs.common.person.EyVegadresse
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.Rolle
import no.nav.etterlatte.libs.common.person.eyAdresse
import no.nav.etterlatte.libs.common.person.eyUtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.eyUtland
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
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
            rolle = Rolle.BARN,
            adresse = mockNorskAdresse()
        )
        val avdoed = mockPerson(
            rolle = Rolle.AVDOED,
            doedsdato = "2022-01-01",
            adresse = mockNorskAdresse()
        )
        val gjenlevende = mockPerson(
            rolle = Rolle.ETTERLATT,
            adresse = mockNorskAdresse()
        )

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.isEmpty())
    }

    @Test
    fun `barn som er for gammelt er ikke en gyldig kandidat`() {
        val barn = mockPerson(
            rolle = Rolle.BARN,
            foedselsaar = now().year - 15,
            foedselsdato = now().minusYears(15).format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
        val avdoed = mockPerson(Rolle.AVDOED)
        val gjenlevende = mockPerson(Rolle.ETTERLATT)

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.BARN_ER_FOR_GAMMELT))
    }

    @Test
    fun `barn som ikke er norsk statsborger er ikke en gyldig kandidat`() {
        val barn = mockPerson(Rolle.BARN, statsborgerskap = "SWE")
        val avdoed = mockPerson(Rolle.AVDOED)
        val gjenlevende = mockPerson(Rolle.ETTERLATT)

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.BARN_ER_IKKE_NORSK_STATSBORGER))
    }

    @Test
    fun `barn som har adressebeskyttelse er ikke en gyldig kandidat`() {
        val barn = mockPerson(Rolle.BARN, adressebeskyttelse = true)
        val avdoed = mockPerson(Rolle.AVDOED)
        val gjenlevende = mockPerson(Rolle.ETTERLATT)

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.BARN_HAR_ADRESSEBESKYTTELSE))
    }

    @Test
    fun `barn som ikke er fodt i Norge er ikke en gyldig kandidat`() {
        val barn = mockPerson(Rolle.BARN, foedeland = "SWE")
        val avdoed = mockPerson(Rolle.AVDOED)
        val gjenlevende = mockPerson(Rolle.ETTERLATT)

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.BARN_ER_IKKE_FOEDT_I_NORGE))
    }

    @Test
    fun `barn som har utvandret er ikke en gyldig kandidat`() {
        val barn = mockPerson(
            rolle = Rolle.BARN,
            utland = eyUtland(
                utflyttingFraNorge = listOf(eyUtflyttingFraNorge(
                    tilflyttingsland = "SWE",
                    dato = "2010-01-01"
                )),
                innflyttingTilNorge = null
            )
        )
        val avdoed = mockPerson(Rolle.AVDOED)
        val gjenlevende = mockPerson(Rolle.ETTERLATT)

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.BARN_HAR_UTVANDRING))
    }

    @Test
    fun `barn som har verge er ikke en gyldig kandidat`() {
        val barn = mockPerson(Rolle.BARN)
        val avdoed = mockPerson(Rolle.AVDOED)
        val gjenlevende = mockPerson(Rolle.ETTERLATT)

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD_VERGE)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.BARN_HAR_VERGE))
    }

    @Test
    fun `barn uten bostedsadresse i Norge er ikke en gyldig kandidat`() {
        val barn = mockPerson(
            rolle = Rolle.BARN,
            adresse = mockUgyldigNorskAdresse()
        )
        val avdoed = mockPerson(Rolle.AVDOED)
        val gjenlevende = mockPerson(Rolle.ETTERLATT)

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.BARN_ER_IKKE_BOSATT_I_NORGE))
    }

    @Test
    fun `avdod som har utvandret er ikke en gyldig kandidat`() {
        val barn = mockPerson(Rolle.BARN)
        val avdoed = mockPerson(
            rolle = Rolle.AVDOED,
            utland = eyUtland(
                utflyttingFraNorge = listOf(eyUtflyttingFraNorge(
                    tilflyttingsland = "SWE",
                    dato = "2010-01-01"
                )),
                innflyttingTilNorge = null
            )
        )
        val gjenlevende = mockPerson(Rolle.ETTERLATT)

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.AVDOED_HAR_UTVANDRING))
    }

    @Test
    fun `avdod som har yrkesskade i soknad er ikke en gyldig kandidat`() {
        val barn = mockPerson(Rolle.BARN)
        val avdoed = mockPerson(Rolle.AVDOED)
        val gjenlevende = mockPerson(Rolle.ETTERLATT)

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD_YRKESSKADE)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.AVDOED_HAR_YRKESSKADE))
    }

    @Test
    fun `avdod som ikke er registrert som dod er ikke en gyldig kandidat`() {
        val barn = mockPerson(Rolle.BARN)
        val avdoed = mockPerson(
            rolle = Rolle.AVDOED,
            doedsdato = null,
        )
        val gjenlevende = mockPerson(Rolle.ETTERLATT)

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.AVDOED_ER_IKKE_REGISTRERT_SOM_DOED))
    }

    @Test
    fun `avdod hvor det er huket av for utlandsopphold er ikke en gyldig kandidat`() {
        val barn = mockPerson(Rolle.BARN)
        val avdoed = mockPerson(Rolle.AVDOED)
        val gjenlevende = mockPerson(Rolle.ETTERLATT)

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD_HUKET_AV_UTLAND)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.AVDOED_HAR_HATT_UTLANDSOPPHOLD))
    }

    @Test
    fun `avdod uten bostedsadresse i Norge er ikke en gyldig kandidat`() {
        val barn = mockPerson(Rolle.BARN)
        val avdoed = mockPerson(
            rolle = Rolle.AVDOED,
            adresse = mockUgyldigNorskAdresse()
        )
        val gjenlevende = mockPerson(Rolle.ETTERLATT)

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.AVDOED_VAR_IKKE_BOSATT_I_NORGE))
    }

    @Test
    fun `innsender som ikke er forelder er ikke en gyldig kandidat`() {
        val barn = mockPerson(Rolle.BARN)
        val avdoed = mockPerson(Rolle.AVDOED)
        val gjenlevende = mockPerson(Rolle.ETTERLATT)

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD_INNSENDER_IKKE_FORELDER)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.INNSENDER_ER_IKKE_FORELDER))
    }

    @Test
    fun `gjenlevende uten bostedsadresse i Norge er ikke en gyldig kandidat`() {
        val barn = mockPerson(Rolle.BARN)
        val avdoed = mockPerson(Rolle.AVDOED)
        val gjenlevende = mockPerson(
            rolle = Rolle.ETTERLATT,
            adresse = mockUgyldigNorskAdresse()
        )

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.GJENLEVENDE_ER_IKKE_BOSATT_I_NORGE))
    }

    @Test
    fun `gjenlevende uten foreldreansvar er ikke en gyldig kandidat`() {
        val barn = mockPerson(
            rolle = Rolle.BARN,
            familieRelasjon = EyFamilieRelasjon(
                ansvarligeForeldre = listOf(EyForeldreAnsvar(Foedselsnummer.of("09018701453"))),
                foreldre = null,
                barn = null,
            )
        )
        val avdoed = mockPerson(Rolle.AVDOED)
        val gjenlevende = mockPerson(Rolle.ETTERLATT)

        val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, BARNEPENSJON_SOKNAD)

        assertTrue(fordelerResultat.forklaring.contains(FordelerKriterie.GJENLEVENDE_HAR_IKKE_FORELDREANSVAR))
    }

    private fun mockPerson(
        rolle: Rolle,
        fnr: String = "11057523044",
        foedselsaar: Int = 2010,
        foedselsdato: String? = "2010-04-19",
        doedsdato: String? = null,
        adressebeskyttelse: Boolean = false,
        statsborgerskap: String = "NOR",
        foedeland: String = "NOR",
        sivilstatus: String = "ugift",
        utland: eyUtland? = null,
        adresse: eyAdresse? = null,
        familieRelasjon: EyFamilieRelasjon? = null,
    ) = Person(
        fornavn = "Ola",
        etternavn = "Nordmann",
        foedselsnummer = Foedselsnummer.of(fnr),
        foedselsaar = foedselsaar,
        foedselsdato = foedselsdato,
        doedsdato = doedsdato,
        adressebeskyttelse = adressebeskyttelse,
        adresse = adresse,
        statsborgerskap = statsborgerskap,
        foedeland = foedeland,
        sivilstatus = sivilstatus,
        utland = utland,
        familieRelasjon = familieRelasjon,
        rolle = rolle
    )

    private fun mockNorskAdresse() = eyAdresse(
        bostedsadresse = EyBostedsadresse(
            vegadresse = EyVegadresse("Testveien", "4", null, "1234")
        ),
        kontaktadresse = null,
        oppholdsadresse = null,
    )

    private fun mockUgyldigNorskAdresse() = eyAdresse(
        bostedsadresse = null,
        kontaktadresse = null,
        oppholdsadresse = null,
    )

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