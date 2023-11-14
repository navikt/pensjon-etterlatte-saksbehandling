package no.nav.etterlatte.pdl.mapper

import no.nav.etterlatte.libs.common.pdl.AkseptererIkkePersonerUtenIdentException
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.mockResponse
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlPersonResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class FamilierelasjonMapperTest {
    private val personRelasjonUtenIdentResponse: PdlPersonResponse =
        mockResponse("/pdl/hentPersonRelasjonUtenIdent.json")
    private val hentPersonRelasjonUtenIdent: PdlHentPerson = personRelasjonUtenIdentResponse.data?.hentPerson!!
    private val personResponse: PdlPersonResponse = mockResponse("/pdl/person.json")
    private val hentPerson: PdlHentPerson = personResponse.data?.hentPerson!!

    @Test
    fun `mapFamilierelasjon kaster feil hvis vi mangler ident på relasjon og ikke aksepeterer det`() {
        Assertions.assertThrows(AkseptererIkkePersonerUtenIdentException::class.java) {
            FamilieRelasjonMapper.mapFamilieRelasjon(
                hentPerson = hentPersonRelasjonUtenIdent,
                personRolle = PersonRolle.BARN,
                aksepterPersonerUtenIdent = false,
            )
        }
    }

    @Test
    fun `mapFamilierelasjon kaster ikke feil hvis vi ikke mangler ident på relasjon`() {
        Assertions.assertDoesNotThrow {
            FamilieRelasjonMapper.mapFamilieRelasjon(
                hentPerson = hentPerson,
                personRolle = PersonRolle.BARN,
                aksepterPersonerUtenIdent = false,
            )
        }
        Assertions.assertDoesNotThrow {
            FamilieRelasjonMapper.mapFamilieRelasjon(
                hentPerson = hentPerson,
                personRolle = PersonRolle.BARN,
                aksepterPersonerUtenIdent = true,
            )
        }
    }

    @Test
    fun `mapFamilierelasjon henter ut personer som mangler identer hvis vi aksepterer det`() {
        val personer =
            FamilieRelasjonMapper.mapFamilieRelasjon(
                hentPerson = hentPersonRelasjonUtenIdent,
                personRolle = PersonRolle.BARN,
                aksepterPersonerUtenIdent = true,
            )
        Assertions.assertNotNull(personer.personerUtenIdent)
        Assertions.assertEquals(2, personer.personerUtenIdent?.size)
    }
}
