package opplysninger.kilde.pdl

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.InnflyttingTilNorge
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.opplysninger.kilde.pdl.OpplysningsByggerService
import no.nav.etterlatte.opplysninger.kilde.pdl.Pdl
import no.nav.etterlatte.opplysninger.kilde.pdl.lagOpplysning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException
import java.time.Instant
import java.time.LocalDate


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OpplysningsByggerServiceTest {

    companion object {
        const val GYLDIG_FNR_SOEKER = "19040550081"
        const val GYLDIG_FNR_AVDOED = "01018100157"
        const val GYLDIG_FNR_GJENLEVENDE_FORELDER = "07081177656"
        const val KILDE_PDL = "pdl"
        val mockedFamilierelasjon =
            FamilieRelasjon(null, listOf(Foedselsnummer.of("01018100157"), Foedselsnummer.of("07081177656")), null)
        val mockedInnflyttingsliste =
            listOf(
                InnflyttingTilNorge("Sverige", LocalDate.parse("2020-05-08")),
                InnflyttingTilNorge("Singapore", LocalDate.parse("2012-12-23"))
            )
        val mockedUtflyttingsliste =
            listOf(
                UtflyttingFraNorge("Sverige", LocalDate.parse("2018-02-01")),
                UtflyttingFraNorge("Tyskland", LocalDate.parse("2005-09-18"))
            )
        val mockedUtland = Utland(mockedInnflyttingsliste, mockedUtflyttingsliste)
        val soekerPdlMock = mockPerson(GYLDIG_FNR_SOEKER, familieRelasjon = mockedFamilierelasjon)
        val avdoedPdlMock =
            mockPerson(GYLDIG_FNR_AVDOED, doedsdato = LocalDate.parse("2022-01-02"), utland = mockedUtland)
        val gjenlevendeForelderPdlMock = mockPerson(GYLDIG_FNR_GJENLEVENDE_FORELDER)
        val opplysningsByggerService = OpplysningsByggerService()
        val fullstendigBarnepensjonssoknad = readSoknadAsBarnepensjon("/fullstendig_barnepensjonssoknad.json")
        val ufullstendigBarnepensjonssoknad = readSoknadAsBarnepensjon("/ufullstendig_barnepensjonssoknad.json")

        /* metode fra etterlatte-fordeler */
        fun mockPerson(
            fnr: String = "11057523044",
            foedselsaar: Int = 2010,
            foedselsdato: LocalDate? = LocalDate.parse("2010-04-19"),
            doedsdato: LocalDate? = null,
            adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.UGRADERT,
            statsborgerskap: String = "NOR",
            foedeland: String = "NOR",
            sivilstatus: Sivilstatus = Sivilstatus.UGIFT,
            utland: Utland? = null,
            bostedsadresse: Adresse? = null,
            familieRelasjon: FamilieRelasjon? = null,
        ) = Person(fornavn = "Ola",
            etternavn = "Nordmann",
            foedselsnummer = Foedselsnummer.of(fnr),
            foedselsaar = foedselsaar,
            foedselsdato = foedselsdato,
            doedsdato = doedsdato,
            adressebeskyttelse = adressebeskyttelse,
            bostedsadresse = bostedsadresse?.let { listOf(bostedsadresse) } ?: emptyList(),
            deltBostedsadresse = emptyList(),
            oppholdsadresse = emptyList(),
            kontaktadresse = emptyList(),
            statsborgerskap = statsborgerskap,
            foedeland = foedeland,
            sivilstatus = sivilstatus,
            utland = utland,
            familieRelasjon = familieRelasjon)

        fun readSoknadAsBarnepensjon(file: String): Barnepensjon {
            val skjemaInfo = objectMapper.writeValueAsString(
                objectMapper.readTree(readFile(file)).get("@skjema_info")
            )
            return objectMapper.readValue(skjemaInfo, Barnepensjon::class.java)
        }

        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }

    @Test
    fun `skal hente avdoedes foedselsnummer`() {
        val avdoedesFnr = opplysningsByggerService.hentAvdoedFnr(fullstendigBarnepensjonssoknad)
        assertEquals("22128202440", avdoedesFnr)
    }

    @Test
    fun `skal kaste exception ved forsoek paa henting av avdoedes foedselsnummer`() {
        assertThrows<Exception> { opplysningsByggerService.hentAvdoedFnr(ufullstendigBarnepensjonssoknad) }
    }

    @Test
    fun `skal hente gjenlevende forelders foedselsnummer`() {
        val gjenlevendeForelderFnr = opplysningsByggerService.hentGjenlevendeForelderFnr(fullstendigBarnepensjonssoknad)
        assertEquals("03108718357", gjenlevendeForelderFnr)
    }

    @Test
    fun `skal kaste exception ved forsoek paa henting av gjenlevendes foedselsnummer`() {
        assertThrows<Exception> { opplysningsByggerService.hentGjenlevendeForelderFnr(ufullstendigBarnepensjonssoknad) }
    }

    @Test
    fun `byggOpplysninger skal kun inneholde de rette opplysningstypene`() {
        val pdlMock = mockk<Pdl>()
        every {
            pdlMock.hentPdlModell(
                "12101376212",
                PersonRolle.BARN
            )
        } returns mockPerson(GYLDIG_FNR_SOEKER)
        every {
            pdlMock.hentPdlModell(
                "22128202440",
                PersonRolle.AVDOED,
            )
        } returns mockPerson(GYLDIG_FNR_AVDOED, doedsdato = LocalDate.parse("2022-01-02"))
        every {
            pdlMock.hentPdlModell(
                "03108718357",
                PersonRolle.GJENLEVENDE
            )
        } returns mockPerson(GYLDIG_FNR_GJENLEVENDE_FORELDER)

        val opplysninger = opplysningsByggerService.byggOpplysninger(fullstendigBarnepensjonssoknad, pdlMock)

        val opplysningstyper = listOf(
            Opplysningstyper.SOEKER_PDL_V1,
            Opplysningstyper.AVDOED_PDL_V1,
            Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1,
        )

        assertTrue(opplysninger.map {
            it.opplysningType
        }.containsAll(opplysningstyper))
    }

    @Test
    fun `lagOpplysning skal lag Behandlingsopplysning på rett format`() {
        val behandlingsopplysning = lagOpplysning(Opplysningstyper.AVDOED_PDL_V1, mockk<Person>())
        behandlingsopplysning.apply {
            assertEquals(36, id.toString().length)
            assertEquals(Behandlingsopplysning.Pdl::class.java, kilde.javaClass)
            assertEquals(KILDE_PDL, (kilde as Behandlingsopplysning.Pdl).navn)
            assertTrue(
                (kilde as Behandlingsopplysning.Pdl).tidspunktForInnhenting in Instant.now()
                    .minusSeconds(10)..Instant.now().plusSeconds(1)
            )
            assertTrue(meta.asText().isEmpty())
            assertEquals(Person::class.java, opplysning.javaClass)
        }
    }

    @Test
    fun `skal lage opplysning om gjenlevende forelder`() {
        val opplysningGjenlevendeForelder = opplysningsByggerService.personOpplysning(
            gjenlevendeForelderPdlMock, Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1
        )
        opplysningGjenlevendeForelder.apply {
            assertEquals(Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1, opplysningType)
            assertEquals(Person::class.java, opplysning.javaClass)
            assertEquals(gjenlevendeForelderPdlMock.fornavn, opplysning.fornavn)
            assertEquals(gjenlevendeForelderPdlMock.etternavn, opplysning.etternavn)
            assertEquals(Foedselsnummer.of(GYLDIG_FNR_GJENLEVENDE_FORELDER), opplysning.foedselsnummer)
        }
    }

    @Test
    fun `skal lage personopplysning om avdød`() {
        val personopplysningAvdoed =
            opplysningsByggerService.personOpplysning(avdoedPdlMock, Opplysningstyper.AVDOED_PDL_V1)

        personopplysningAvdoed.apply {
            assertEquals(Opplysningstyper.AVDOED_PDL_V1, opplysningType)
            assertEquals(Person::class.java, opplysning.javaClass)
            assertEquals(avdoedPdlMock.fornavn, opplysning.fornavn)
            assertEquals(avdoedPdlMock.etternavn, opplysning.etternavn)
            assertEquals(Foedselsnummer.of(GYLDIG_FNR_AVDOED), opplysning.foedselsnummer)
            assertEquals(avdoedPdlMock.doedsdato, opplysning.doedsdato)
            assertEquals(avdoedPdlMock.utland, opplysning.utland )
        }
    }

    @Test
    fun `skal lage opplysning om soeker`() {
        val soekerFoedselsdato =
            opplysningsByggerService.personOpplysning(soekerPdlMock, Opplysningstyper.SOEKER_PDL_V1)
        soekerFoedselsdato.apply {
            assertEquals(Opplysningstyper.SOEKER_PDL_V1, opplysningType)
            assertEquals(Person::class.java, opplysning.javaClass)
            assertEquals(soekerPdlMock.fornavn, opplysning.fornavn)
            assertEquals(soekerPdlMock.etternavn, opplysning.etternavn)
            assertEquals(Foedselsnummer.of(GYLDIG_FNR_SOEKER), opplysning.foedselsnummer)
            assertEquals(soekerPdlMock.foedselsdato, opplysning.foedselsdato)
        }
    }
}