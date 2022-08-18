package no.nav.etterlatte.hendelserpdl

import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.hendelserpdl.leesah.ILivetErEnStroemAvHendelser
import no.nav.etterlatte.hendelserpdl.pdl.PdlService
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.FolkeregisterIdent
import no.nav.person.pdl.leesah.Personhendelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

internal class FinnDodsmeldingerTest {

    @Test
    fun ikkeDodsmelding() {
        val pdlMock = mockk<PdlService>()
        val subject =
            FinnDodsmeldinger(
                LeesahMock(listOf(Personhendelse().apply { put("opplysningstype", "Ikke dodsmelding") })),
                DodsMock { _, _ -> fail() },
                pdlMock
            )
        subject.stream()
    }

    @Test
    fun dodsmelding() {
        val pdlMock = mockk<PdlService>() {
            coEvery { hentFolkeregisterIdentifikator("123") } returns FolkeregisterIdent(
                Foedselsnummer.of("70078749472")
            )
        }
        val dodsmeldinger = mutableListOf<String>()
        val subject = FinnDodsmeldinger(
            LeesahMock(
                listOf(
                    Personhendelse().apply {
                        put("opplysningstype", "DOEDSFALL_V1")
                        put("personidenter", listOf("123"))
                        put("endringstype", no.nav.person.pdl.leesah.Endringstype.valueOf("OPPRETTET"))
                    }
                )
            ),
            DodsMock { it, _ -> dodsmeldinger += it },
            pdlMock
        )
        subject.stream()
        assertEquals(1, dodsmeldinger.size)
        assertEquals("70078749472", dodsmeldinger[0])
    }
}

class LeesahMock(val mockData: List<Personhendelse>) : ILivetErEnStroemAvHendelser {
    override fun poll(c: (Personhendelse) -> Unit): Int {
        mockData.forEach(c)
        return 1
    }

    override fun fraStart() {
    }
}

class DodsMock(val c: (String, String?) -> Unit) : IDodsmeldinger {
    override fun personErDod(ident: String, doedsdato: String?, endringstype: Endringstype) = c(ident, doedsdato)
}