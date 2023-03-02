package no.nav.etterlatte.opplysninger.kilde.pdl

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.time.LocalDate
import java.util.*

class BesvarOpplysningsbehovTest {
    companion object {
        val melding = readFile("/behov.json")

        val pdlMock = mockk<Pdl>()
        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")

        fun mockPerson(
            fnr: String = "11057523044"
        ) = PersonDTO(
            fornavn = OpplysningDTO(verdi = "Ola", opplysningsid = null),
            etternavn = OpplysningDTO(verdi = "Nordmann", opplysningsid = null),
            foedselsnummer = OpplysningDTO(Foedselsnummer.of(fnr), null),
            foedselsdato = OpplysningDTO(LocalDate.now().minusYears(20), UUID.randomUUID().toString()),
            foedselsaar = OpplysningDTO(verdi = 2000, opplysningsid = null),
            foedeland = OpplysningDTO("Norge", UUID.randomUUID().toString()),
            doedsdato = null,
            adressebeskyttelse = null,
            bostedsadresse = listOf(
                OpplysningDTO(
                    Adresse(
                        type = AdresseType.VEGADRESSE,
                        aktiv = true,
                        coAdresseNavn = "Hos Geir",
                        adresseLinje1 = "Testveien 4",
                        adresseLinje2 = null,
                        adresseLinje3 = null,
                        postnr = "1234",
                        poststed = null,
                        land = "NOR",
                        kilde = "FREG",
                        gyldigFraOgMed = Tidspunkt.now().toLocalDatetimeUTC().minusYears(1),
                        gyldigTilOgMed = null
                    ),
                    UUID.randomUUID().toString()
                )
            ),
            deltBostedsadresse = listOf(),
            kontaktadresse = listOf(),
            oppholdsadresse = listOf(),
            sivilstatus = null,
            statsborgerskap = OpplysningDTO("Norsk", UUID.randomUUID().toString()),
            utland = null,
            familieRelasjon = null,
            avdoedesBarn = null,
            vergemaalEllerFremtidsfullmakt = null
        )
    }

    private val inspector = TestRapid().apply { BesvarOpplysningsbehov(this, pdlMock) }

    @Test
    fun `skal lese melding om opplysningsbehov og returnere info fra PDL`() {
        every { pdlMock.hentPerson(any(), any()) } answers { mockPerson(fnr = firstArg()).tilPerson() }
        every { pdlMock.hentOpplysningsperson(any(), any()) } answers { mockPerson(fnr = firstArg()) }
        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        Assertions.assertEquals(Opplysningstype.SOEKER_PDL_V1.name, inspector.message(0).get(BEHOV_NAME_KEY).asText())
        // TODO fikse litt på denne
        Assertions.assertEquals(
            "Ola",
            inspector.message(0).get("opplysning").first().get("opplysning").get("fornavn").asText()
        )
        Assertions.assertEquals(1, inspector.size)
    }

    @Test
    fun `alle opplysninger har samme tidspunkt for innhenting`() {
        every { pdlMock.hentPerson(any(), any()) } answers { mockPerson(fnr = firstArg()).tilPerson() }
        every { pdlMock.hentOpplysningsperson(any(), any()) } answers { mockPerson(fnr = firstArg()) }
        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        val opplysninger = inspector.message(0).get("opplysning")
        val expectedTidspunkt = opplysninger.first().get("kilde").get("tidspunktForInnhenting")

        Assertions.assertTrue(expectedTidspunkt != null)
        Assertions.assertTrue(opplysninger.all { it.get("kilde").get("tidspunktForInnhenting") == expectedTidspunkt })
    }
}