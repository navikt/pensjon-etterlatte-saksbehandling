package no.nav.etterlatte.hendelserpdl

import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.hendelserpdl.leesah.ILivsHendelserRapid
import no.nav.etterlatte.hendelserpdl.leesah.PersonHendelseFordeler
import no.nav.etterlatte.hendelserpdl.pdl.PdlService
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.FolkeregisterIdent
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.forelderbarnrelasjon.ForelderBarnRelasjon
import org.junit.jupiter.api.Test

internal class KafkaConsumerHendelserPdlTest {

    @Test
    fun hendelserSomIkkeLyttesPaa() {
        val pdlMock = mockk<PdlService>()
        val livshendelserRapid = mockk<ILivsHendelserRapid>()
        val personHendelseFordeler = PersonHendelseFordeler(livshendelserRapid, pdlMock)
        personHendelseFordeler.haandterHendelse(Personhendelse().apply { put("opplysningstype", "Ikke dodsmelding") })

        verify(exactly = 0) { pdlMock.hentFolkeregisterIdentifikator(any()) }
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
        val iLivsHendelserRapid = mockk<ILivsHendelserRapid>() {
            every { personErDod(any(), any(), any()) } just runs
            every { forelderBarnRelasjon(any(), any(), any(), any(), any(), any()) } just runs
            every { personUtflyttingFraNorge(any(), any(), any(), any(), any()) } just runs
        }

        val gyldigeHendelser = listOf(
            Personhendelse().apply {
                put("opplysningstype", "DOEDSFALL_V1")
                put("personidenter", listOf("123"))
                put("endringstype", Endringstype.valueOf("OPPRETTET"))
            },
            Personhendelse().apply {
                put("opplysningstype", "UTFLYTTING_FRA_NORGE")
                put("personidenter", listOf("321"))
                put("endringstype", Endringstype.valueOf("OPPRETTET"))
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
                put("endringstype", Endringstype.valueOf("OPPRETTET"))
            }
        )
        val personHendelseFordeler = PersonHendelseFordeler(iLivsHendelserRapid, pdlMock)

        gyldigeHendelser.forEach {
            personHendelseFordeler.haandterHendelse(it)
        }

        verify(exactly = 1) { iLivsHendelserRapid.personErDod(any(), any(), any()) }
        verify(exactly = 1) {
            iLivsHendelserRapid.forelderBarnRelasjon(any(), any(), any(), any(), any(), any())
        }
        verify(exactly = 1) {
            iLivsHendelserRapid.personUtflyttingFraNorge(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }
}