package opplysninger.kilde.pdl

import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.PersonInfo
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.opplysninger.kilde.pdl.OpplysningsByggerService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant

import java.time.LocalDate

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

        /*
        fun mockNorskAdresse() = Adresse(
            type = AdresseType.VEGADRESSE,
            aktiv = true,
            adresseLinje1 = "Testveien 4",
            adresseLinje2 = null,
            postnr = "1234",
            poststed = null,
            kilde = "FREG",
            gyldigFraOgMed = LocalDateTime.now().minusYears(1),
            gyldigTilOgMed = null
        )

        fun mockUgyldigAdresse() = Adresse(
            type = AdresseType.UKJENT_BOSTED,
            aktiv = true,
            adresseLinje1 = "Tull",
            adresseLinje2 = null,
            postnr = null,
            poststed = null,
            kilde = "FREG",
            gyldigFraOgMed = LocalDateTime.now().minusYears(1),
            gyldigTilOgMed = null
        )

         */

    }

    @Test
    fun byggOpplysninger() {

    }

    @Test
    fun `skal lage opplysning om gjenlevende forelder`() {
        val opplysningGjenlevendeForelder = opplysningsByggerService.gjenlevendeForelderOpplysning(
            gjenlevendeForelderPdlMock, Opplysningstyper.GJENLEVENDE_FORELDER_PERSONINFO_V1.value
        )
        opplysningGjenlevendeForelder.apply {
            assertEquals(36, id.toString().length)
            assertEquals(Behandlingsopplysning.Pdl::class.java, kilde.javaClass)
            assertEquals(KILDE_PDL, (kilde as Behandlingsopplysning.Pdl).navn)
            assertTrue(
                (kilde as Behandlingsopplysning.Pdl).tidspunktForInnhenting in Instant.now()
                    .minusSeconds(10)..Instant.now().plusSeconds(1)
            )
            assertEquals(Opplysningstyper.GJENLEVENDE_FORELDER_PERSONINFO_V1.value, opplysningType)
            assertTrue(meta.asText().isEmpty())

            assertEquals(PersonInfo::class.java, opplysning.javaClass)
            assertEquals(gjenlevendeForelderPdlMock.fornavn, opplysning.fornavn)
            assertEquals(gjenlevendeForelderPdlMock.etternavn, opplysning.etternavn)
            assertEquals(Foedselsnummer.of(GYLDIG_FNR_GJENLEVENDE_FORELDER), opplysning.foedselsnummer)
            //assertEquals(gjenlevendeForelderPdlMock.adresse, opplysning.adresse)
            assertEquals(PersonType.GJENLEVENDE_FORELDER, opplysning.type)
        }
    }

    @Test
    fun personOpplysning() {
    }

    @Test
    fun avdoedDodsdato() {
    }

    @Test
    fun soekerFoedselsdato() {
    }

    @Test
    fun soekerRelasjonForeldre() {
    }

    @Test
    fun hentAvdoedFnr() {
    }

    @Test
    fun hentGjenlevendeForelderFnr() {
    }

    @Test
    fun `skal lage Behandlingsopplysning om opplysningstype`() {

    }
}