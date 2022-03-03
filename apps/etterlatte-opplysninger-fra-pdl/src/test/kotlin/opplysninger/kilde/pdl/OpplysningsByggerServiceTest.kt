package opplysninger.kilde.pdl

import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.PersonInfo
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.opplysninger.kilde.pdl.OpplysningsByggerService
import no.nav.etterlatte.opplysninger.kilde.pdl.lagOpplysning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.io.FileNotFoundException
import java.time.Instant

import java.time.LocalDate
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OpplysningsByggerServiceTest {

    companion object {
        const val GYLDIG_FNR_SOEKER = "19040550081"
        const val GYLDIG_FNR_AVDOED = "01018100157"
        const val GYLDIG_FNR_GJENLEVENDE_FORELDER = "07081177656"
        const val KILDE_PDL = "pdl"
        val soekerPdlMock = mockPerson(GYLDIG_FNR_SOEKER)
        val avdoedPdlMock = mockPerson(GYLDIG_FNR_AVDOED, doedsdato = LocalDate.parse("2022-01-02"))
        val gjenlevendeForelderPdlMock = mockPerson(GYLDIG_FNR_GJENLEVENDE_FORELDER)
        val opplysningsByggerService = OpplysningsByggerService()
        val komplettBarnepensjonssoknad = readSoknadAsBarnepensjon("/fullMessage2.json")
        val ukomplettBarnepensjonssoknad = readSoknadAsBarnepensjon("/fullMessage5.json")

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


        fun mockNorskAdresse() = Adresse(
            type = AdresseType.VEGADRESSE,
            aktiv = true,
            coAdresseNavn = null,
            adresseLinje1 = "Testveien 4",
            adresseLinje2 = null,
            adresseLinje3 = null,
            postnr = "1234",
            poststed = null,
            land = null,
            kilde = "FREG",
            gyldigFraOgMed = LocalDateTime.now().minusYears(1),
            gyldigTilOgMed = null
        )

        fun mockUgyldigAdresse() = Adresse(
            type = AdresseType.UKJENT_BOSTED,
            aktiv = true,
            coAdresseNavn = null,
            adresseLinje1 = "Tull",
            adresseLinje2 = null,
            adresseLinje3 = null,
            postnr = null,
            poststed = null,
            land = null,
            kilde = "FREG",
            gyldigFraOgMed = LocalDateTime.now().minusYears(1),
            gyldigTilOgMed = null
        )

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
    fun byggOpplysninger() {

    }

    @Test
    fun `lagOpplysning skal lag Behandlingsopplysning på rett format`() {
        val behandlingsopplysning = lagOpplysning("opplysningstype", mockk<PersonInfo>())
        behandlingsopplysning.apply {
            assertEquals(36, id.toString().length)
            assertEquals(Behandlingsopplysning.Pdl::class.java, kilde.javaClass)
            assertEquals(KILDE_PDL, (kilde as Behandlingsopplysning.Pdl).navn)
            assertTrue(
                (kilde as Behandlingsopplysning.Pdl).tidspunktForInnhenting in Instant.now()
                    .minusSeconds(10)..Instant.now().plusSeconds(1)
            )
            assertTrue(meta.asText().isEmpty())
            assertEquals(PersonInfo::class.java, opplysning.javaClass)
        }
    }

    @Test
    fun `skal lage opplysning om gjenlevende forelder`() {
        val opplysningGjenlevendeForelder = opplysningsByggerService.gjenlevendeForelderOpplysning(
            gjenlevendeForelderPdlMock, Opplysningstyper.GJENLEVENDE_FORELDER_PERSONINFO_V1.value
        )
        opplysningGjenlevendeForelder.apply {
            assertEquals(Opplysningstyper.GJENLEVENDE_FORELDER_PERSONINFO_V1.value, opplysningType)

            assertEquals(PersonInfo::class.java, opplysning.javaClass)
            assertEquals(gjenlevendeForelderPdlMock.fornavn, opplysning.fornavn)
            assertEquals(gjenlevendeForelderPdlMock.etternavn, opplysning.etternavn)
            assertEquals(Foedselsnummer.of(GYLDIG_FNR_GJENLEVENDE_FORELDER), opplysning.foedselsnummer)
            // TODO SKriv test for adresse når adresseformat er på plass
            //assertEquals(gjenlevendeForelderPdlMock.adresse, opplysning.adresse)
            assertEquals(PersonType.GJENLEVENDE_FORELDER, opplysning.type)
        }
    }

    @Test
    fun `skal lage personopplysning om avdød`() {
        val personopplysningAvdoed =
            opplysningsByggerService.personOpplysning(
                avdoedPdlMock, Opplysningstyper.AVDOED_PERSONINFO_V1.value, PersonType.AVDOED
            )

        personopplysningAvdoed.apply {
            assertEquals(Opplysningstyper.AVDOED_PERSONINFO_V1.value, opplysningType)

            assertEquals(PersonInfo::class.java, opplysning.javaClass)
            assertEquals(avdoedPdlMock.fornavn, opplysning.fornavn)
            assertEquals(avdoedPdlMock.etternavn, opplysning.etternavn)
            assertEquals(Foedselsnummer.of(GYLDIG_FNR_AVDOED), opplysning.foedselsnummer)
            // TODO SKriv test for adresse når adresseformat er på plass
            //assertEquals(gjenlevendeForelderPdlMock.adresse, opplysning.adresse)
            assertEquals(PersonType.AVDOED, opplysning.type)
        }
    }

    @Test
    fun `skal lage personopplysning med persontype BARN for søker`() {
        val personopplysningSoeker =
            opplysningsByggerService.personOpplysning(
                soekerPdlMock, Opplysningstyper.SOEKER_PERSONINFO_V1.value, PersonType.BARN
            )
        personopplysningSoeker.apply {
            assertEquals(PersonType.BARN, opplysning.type)
        }
    }

    @Test
    @Disabled
    //TODO avklare om metode er klar for testing
    fun avdoedDodsdato() {
        val avdoedDoedsdato =
            opplysningsByggerService.avdoedDodsdato(avdoedPdlMock, Opplysningstyper.AVDOED_DOEDSFALL_V1.value)

    }

    //TODO avklare om metode er klar for testing
    @Test
    fun soekerFoedselsdato() {
    }


    //TODO avklare om metode er klar for testing
    @Test
    fun soekerRelasjonForeldre() {

    }

    @Test
    fun `skal hente avdoedes foedselsnummer`() {
        val avdoedesFnr = opplysningsByggerService.hentAvdoedFnr(komplettBarnepensjonssoknad)
        assertEquals("22128202440", avdoedesFnr)
    }

    @Test
    fun `skal kaste exception ved forsoek paa henting av avdoedes foedselsnummer`() {
        assertThrows<Exception> { opplysningsByggerService.hentAvdoedFnr(ukomplettBarnepensjonssoknad) }
    }

    @Test
    fun `skal hente gjenlevende forelders foedselsnummer`() {
        val gjenlevendeForelderFnr = opplysningsByggerService.hentGjenlevendeForelderFnr(komplettBarnepensjonssoknad)
        assertEquals("03108718357", gjenlevendeForelderFnr)
    }

    @Test
    fun `skal kaste exception ved forsoek paa henting av gjenlevendes foedselsnummer`() {
        assertThrows<Exception> { opplysningsByggerService.hentGjenlevendeForelderFnr(ukomplettBarnepensjonssoknad) }
    }

}