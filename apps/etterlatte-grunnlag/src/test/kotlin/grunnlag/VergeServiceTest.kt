package no.nav.etterlatte.grunnlag

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.grunnlag.adresse.PersondataAdresse
import no.nav.etterlatte.grunnlag.adresse.REGOPPSLAG_ADRESSE
import no.nav.etterlatte.grunnlag.klienter.PersondataKlient
import no.nav.etterlatte.libs.common.person.BrevMottaker
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.VergeEllerFullmektig
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import org.junit.jupiter.api.Test

class VergeServiceTest {
    private val mockPersondataKlient = mockk<PersondataKlient>()
    private val mockPerson = mockk<Person>()
    private val vergeService = VergeService(mockPersondataKlient)

    @Test
    fun `Hvis vergemaal ikke har vergens fnr kan ikke adressen hentes`() {
        val vergemaalUtenFnr = vergemaalUtenFnr()

        every { mockPerson.vergemaalEllerFremtidsfullmakt } returns
            listOf(vergemaalUtenFnr)
        every { mockPerson.foedselsnummer } returns Folkeregisteridentifikator.of("09498230323")

        val vergesAdresse = vergeService.hentGrunnlagsopplysningVergesAdresse(mockPerson)
        vergesAdresse shouldBe null
    }

    @Test
    fun `Hvis vergemaal har vergens fnr skal adressen hentes`() {
        val vergemaalMedFnr = vergemaalMedFnr()
        val persondataAdresse = mockk<PersondataAdresse>()
        val brevMottaker = brevMottaker()

        every { persondataAdresse.tilFrittstaendeBrevMottaker(any()) } returns brevMottaker
        every {
            mockPersondataKlient.hentAdresseGittFnr(vergemaalMedFnr.vergeEllerFullmektig.motpartsPersonident!!.value)
        } returns persondataAdresse

        every { mockPerson.vergemaalEllerFremtidsfullmakt } returns
            listOf(vergemaalMedFnr())
        every { mockPerson.foedselsnummer } returns Folkeregisteridentifikator.of("09498230323")

        val vergesAdresse = vergeService.hentGrunnlagsopplysningVergesAdresse(mockPerson)
        vergesAdresse!!.opplysning shouldBeEqual brevMottaker
    }

    private fun brevMottaker() = BrevMottaker("Johnny Cash", mockk(), mockk(), REGOPPSLAG_ADRESSE)

    private fun vergemaalUtenFnr() =
        VergemaalEllerFremtidsfullmakt(
            embete = "",
            type = "",
            vergeEllerFullmektig =
                VergeEllerFullmektig(
                    motpartsPersonident = null,
                    navn = "John",
                    tjenesteomraade = "",
                    omfangetErInnenPersonligOmraade = true,
                    omfang = "",
                ),
        )

    private fun vergemaalMedFnr() =
        VergemaalEllerFremtidsfullmakt(
            embete = "",
            type = "",
            vergeEllerFullmektig =
                VergeEllerFullmektig(
                    motpartsPersonident = Folkeregisteridentifikator.of("10418305857"),
                    navn = "John",
                    tjenesteomraade = "",
                    omfangetErInnenPersonligOmraade = true,
                    omfang = "",
                ),
        )
}
