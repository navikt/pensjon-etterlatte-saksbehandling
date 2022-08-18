package opplysninger.kilde.pdl

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.rapidsandrivers.behovNameKey
import no.nav.etterlatte.opplysninger.kilde.pdl.BesvarOpplysningsbehov
import no.nav.etterlatte.opplysninger.kilde.pdl.Pdl
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.time.LocalDate

class BesvarOpplysningsbehovTest {
    companion object {
        val melding = readFile("/behov.json")

        val pdlMock = mockk<Pdl>()
        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")

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
            familieRelasjon: FamilieRelasjon? = null
        ) = Person(
            fornavn = "Ola",
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
            familieRelasjon = familieRelasjon,
            avdoedesBarn = null,
            vergemaalEllerFremtidsfullmakt = null
        )
    }

    private val inspector = TestRapid().apply { BesvarOpplysningsbehov(this, pdlMock) }

    @Test
    fun `skal lese melding om opplysningsbehov og returnere info fra PDL`() {
        every { pdlMock.hentPdlModell(any(), any()) } answers { mockPerson(fnr = firstArg()) }
        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        Assertions.assertEquals(Opplysningstyper.SOEKER_PDL_V1.name, inspector.message(0).get(behovNameKey).asText())
        // TODO fikse litt på denne
        Assertions.assertEquals(
            "Ola",
            inspector.message(0).get("opplysning").first().get("opplysning").get("fornavn").asText()
        )
        Assertions.assertEquals(1, inspector.size)
    }
}