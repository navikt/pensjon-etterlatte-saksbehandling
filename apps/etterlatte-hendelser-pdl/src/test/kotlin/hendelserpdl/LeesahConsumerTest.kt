package no.nav.etterlatte.hendelserpdl

internal class LeesahConsumerTest {

    /*
    @Test
    fun hendelserSomIkkeLyttesPaa() {
        val pdlMock = mockk<PdlKlient>()
        val livshendelserRapid = mockk<ILivsHendelserRapid>()
        val personHendelseFordeler = PersonHendelseFordeler(livshendelserRapid, pdlMock)
        runBlocking {
            personHendelseFordeler.haandterHendelse(
                Personhendelse().apply {
                    put("opplysningstype", "Ikke dodsmelding")
                }
            )
        }

        coVerify(exactly = 0) { pdlMock.hentPdlIdentifikator(any()) }
    }

    @Test
    fun hendelserSomLyttesPaa() {
        val pdlMock = mockk<PdlKlient>() {
            coEvery { hentPdlIdentifikator("123") } returns PdlIdentifikator.FolkeregisterIdent(
                Folkeregisteridentifikator.of("70078749472")
            )
            coEvery { hentPdlIdentifikator("321") } returns PdlIdentifikator.FolkeregisterIdent(
                Folkeregisteridentifikator.of("12345678911")
            )
            coEvery { hentPdlIdentifikator("123") } returns PdlIdentifikator.FolkeregisterIdent(
                Folkeregisteridentifikator.of("70078749472")
            )
        }
        val iLivsHendelserRapid = mockk<ILivsHendelserRapid>() {
            every { personErDod(any(), any(), any(), any()) } just runs
            every { forelderBarnRelasjon(any(), any(), any(), any(), any(), any(), any()) } just runs
            every { personUtflyttingFraNorge(any(), any(), any(), any(), any(), any()) } just runs
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
        runBlocking {
            gyldigeHendelser.forEach {
                personHendelseFordeler.haandterHendelse(it)
            }
        }

        verify(exactly = 1) { iLivsHendelserRapid.personErDod(any(), any(), any(), any()) }
        verify(exactly = 1) {
            iLivsHendelserRapid.forelderBarnRelasjon(any(), any(), any(), any(), any(), any(), any())
        }
        verify(exactly = 1) {
            iLivsHendelserRapid.personUtflyttingFraNorge(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }*/
}