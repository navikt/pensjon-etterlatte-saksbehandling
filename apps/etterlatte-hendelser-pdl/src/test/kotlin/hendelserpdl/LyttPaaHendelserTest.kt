package no.nav.etterlatte.hendelserpdl

import io.mockk.called
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.hendelserpdl.leesah.ILivetErEnStroemAvHendelser
import no.nav.etterlatte.hendelserpdl.pdl.PdlService
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.FolkeregisterIdent
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.forelderbarnrelasjon.ForelderBarnRelasjon
import org.junit.jupiter.api.Test

internal class LyttPaaHendelserTest {

    @Test
    fun hendelserSomIkkeLyttesPaa() {
        val pdlMock = mockk<PdlService>()
        val livshendelserRapid = mockk<IPostLivsHendelserPaaRapid>()
        val subject =
            LyttPaaHendelser(
                LeesahMock(listOf(Personhendelse().apply { put("opplysningstype", "Ikke dodsmelding") })),
                livshendelserRapid,
                pdlMock
            )
        subject.stream()

        verify { livshendelserRapid wasNot called }
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
            coEvery { hentFolkeregisterIdentifikator("123") } returns FolkeregisterIdent(
                Foedselsnummer.of("70078749472")
            )
        }
        val iPostLivsHendelserPaaRapid = mockk<IPostLivsHendelserPaaRapid>() {
            every { personErDod(any(), any(), any()) } just runs
            every { forelderBarnRelasjon(any(), any(), any(), any(), any(), any()) } just runs
            every { personUtflyttingFraNorge(any(), any(), any(), any(), any()) } just runs
        }
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
                    },
                    Personhendelse().apply {
                        put("opplysningstype", "FORELDERBARNRELASJON_V1")
                        put("personidenter", listOf("123"))
                        put(
                            "forelderBarnRelasjon",
                            ForelderBarnRelasjon(
                                "12345678911",
                                "FAR",
                                "BARN",
                                null
                            )
                        )
                        put("endringstype", no.nav.person.pdl.leesah.Endringstype.valueOf("OPPRETTET"))
                    }
                )
            ),
            iPostLivsHendelserPaaRapid,
            pdlMock
        )
        subject.stream()

        verify(exactly = 1) { iPostLivsHendelserPaaRapid.personErDod(any(), any(), any()) }
        verify(exactly = 1) {
            iPostLivsHendelserPaaRapid.forelderBarnRelasjon(any(), any(), any(), any(), any(), any())
        }
        verify(exactly = 1) {
            iPostLivsHendelserPaaRapid.personUtflyttingFraNorge(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    class LeesahMock(val mockData: List<Personhendelse>) : ILivetErEnStroemAvHendelser {
        override fun poll(consumePersonHendelse: (Personhendelse) -> Unit): Int {
            mockData.forEach(consumePersonHendelse)
            return mockData.size
        }

        override fun fraStart() {
        }
    }
}