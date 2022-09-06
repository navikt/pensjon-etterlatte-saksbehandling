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

internal class LyttPaaHendelserTest {

    @Test
    fun hendelserSomIkkeLyttesPaa() {
        val pdlMock = mockk<PdlService>()
        val subject =
            LyttPaaHendelser(
                LeesahMock(listOf(Personhendelse().apply { put("opplysningstype", "Ikke dodsmelding") })),
                DodsMock { _ -> fail() },
                pdlMock
            )
        subject.stream()
    }

    @Test
    fun hendelserSomLyttesPaa() {
        val pdlMock = mockk<PdlService>() {
            coEvery { hentFolkeregisterIdentifikator("123") } returns FolkeregisterIdent(
                Foedselsnummer.of("70078749472")
            )
            coEvery { hentFolkeregisterIdentifikator("321") } returns FolkeregisterIdent(
                Foedselsnummer.of("12345678911")
            )
        }
        val hendelser = mutableListOf<Pair<String, String>>()
        val subject = LyttPaaHendelser(
            LeesahMock(
                listOf(
                    Personhendelse().apply {
                        put("opplysningstype", "DOEDSFALL_V1")
                        put("personidenter", listOf("123"))
                        put("endringstype", no.nav.person.pdl.leesah.Endringstype.valueOf("OPPRETTET"))
                    },
                    Personhendelse().apply {
                        put("opplysningstype", "UTFLYTTING_FRA_NORGE")
                        put("personidenter", listOf("321"))
                        put("endringstype", no.nav.person.pdl.leesah.Endringstype.valueOf("OPPRETTET"))
                    }
                )
            ),
            DodsMock { hendelser += it },
            pdlMock
        )
        subject.stream()
        assertEquals(2, hendelser.size)
        assertEquals("70078749472", hendelser[0].first)
        assertEquals("person er doed", hendelser[0].second)
        assertEquals("12345678911", hendelser[1].first)
        assertEquals("person flyttet ut", hendelser[1].second)
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

class DodsMock(val c: (Pair<String, String>) -> Unit) : ILivsHendelser {
    override fun personErDod(fnr: String, doedsdato: String?, endringstype: Endringstype) =
        c(Pair(fnr, "person er doed"))

    override fun personUtflyttingFraNorge(
        fnr: String,
        tilflyttingsLand: String?,
        tilflyttingsstedIUtlandet: String?,
        utflyttingsdato: String?,
        endringstype: Endringstype
    ) {
        c(Pair(fnr, "person flyttet ut"))
    }
}