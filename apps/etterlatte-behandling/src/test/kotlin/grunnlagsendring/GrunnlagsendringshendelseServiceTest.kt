package no.nav.etterlatte.grunnlagsendring

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.KONTANT_FOT
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BrukerService
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlient
import no.nav.etterlatte.grunnlagsendringshendelseMedSamsvar
import no.nav.etterlatte.grunnlagsinformasjonDoedshendelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SakidOgRolle
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.oppgave.opprettNyOppgaveMedReferanseOgSak
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Bostedsadresse
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.SivilstandHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED2_FOEDSELSNUMMER
import no.nav.etterlatte.mockPerson
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.sql.Connection
import java.time.LocalDate
import java.util.UUID.randomUUID
import java.util.concurrent.atomic.AtomicBoolean

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnlagsendringshendelseServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val grunnlagshendelsesDao = mockk<GrunnlagsendringshendelseDao>(relaxUnitFun = true)
    private val pdlService = mockk<PdlTjenesterKlientImpl>()
    private val grunnlagClient = mockk<GrunnlagKlient>(relaxed = true, relaxUnitFun = true)
    private val sakService = mockk<SakService>(relaxed = true)
    private val oppgaveService = mockk<OppgaveService>()
    private val brukerService = mockk<BrukerService>()
    private val mockOppgave =
        opprettNyOppgaveMedReferanseOgSak(
            "hendelseid",
            Sak("ident", SakType.BARNEPENSJON, 1L, Enheter.AALESUND.enhetNr),
            OppgaveKilde.HENDELSE,
            OppgaveType.VURDER_KONSEKVENS,
            null,
        )

    private val grunnlagsendringshendelseService =
        GrunnlagsendringshendelseService(
            oppgaveService,
            grunnlagshendelsesDao,
            behandlingService,
            pdlService,
            grunnlagClient,
            sakService,
            brukerService,
        )

    @BeforeEach
    fun before() {
        Kontekst.set(
            Context(
                mockk(),
                object : DatabaseKontekst {
                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(block: () -> T): T {
                        return block()
                    }
                },
            ),
        )
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun `Skal opprette hendelser for hendelse`() {
        val sakId = 1L
        val fnr = KONTANT_FOT.value

        every {
            sakService.finnSak(sakId)
        } returns Sak(fnr, SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr)
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList()
        coEvery { grunnlagClient.hentPersonSakOgRolle(any()) }
            .returns(
                PersonMedSakerOgRoller(
                    fnr,
                    listOf(
                        SakidOgRolle(1L, Saksrolle.GJENLEVENDE),
                        SakidOgRolle(1L, Saksrolle.GJENLEVENDE),
                        SakidOgRolle(1L, Saksrolle.GJENLEVENDE),
                        SakidOgRolle(1L, Saksrolle.SOESKEN),
                        SakidOgRolle(2L, Saksrolle.AVDOED),
                        SakidOgRolle(2L, Saksrolle.GJENLEVENDE),
                        SakidOgRolle(2L, Saksrolle.SOEKER),
                        SakidOgRolle(3L, Saksrolle.SOESKEN),
                    ),
                ),
            )

        val grunnlagsendringshendelse =
            grunnlagsendringshendelseMedSamsvar(
                id = randomUUID(),
                sakId = sakId,
                fnr = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )

        val opprettGrunnlagsendringshendelse = slot<Grunnlagsendringshendelse>()
        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(capture(opprettGrunnlagsendringshendelse))
        } returns grunnlagsendringshendelse

        every { pdlService.hentPdlModell(any(), any(), any()) } returns mockPerson()
        val opprettedeHendelser = grunnlagsendringshendelseService.opprettHendelseAvTypeForPerson(fnr, GrunnlagsendringsType.DOEDSFALL)
        assertEquals(6, opprettedeHendelser.size)
    }

    @Test
    fun `skal opprette grunnlagsendringshendelser i databasen for doedshendelser`() {
        val sakId = 1L
        val fnr = KONTANT_FOT.value
        val foerstegangsbehandlinger =
            listOf(
                foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.IVERKSATT),
                foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.FATTET_VEDTAK),
            )
        val grunnlagsendringshendelse =
            grunnlagsendringshendelseMedSamsvar(
                id = randomUUID(),
                sakId = sakId,
                fnr = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )

        val opprettGrunnlagsendringshendelse = slot<Grunnlagsendringshendelse>()

        every { sakService.finnSak(sakId) } returns Sak(KONTANT_FOT.value, SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr)
        every {
            grunnlagshendelsesDao.oppdaterGrunnlagsendringStatusOgSamsvar(any(), any(), any(), any())
        } returns Unit
        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(capture(opprettGrunnlagsendringshendelse))
        } returns grunnlagsendringshendelse
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList()

        every { behandlingService.hentBehandlingerForSak(sakId) } returns foerstegangsbehandlinger

        coEvery { grunnlagClient.hentPersonSakOgRolle(any()) }
            .returns(PersonMedSakerOgRoller(fnr, listOf(SakidOgRolle(sakId, Saksrolle.SOEKER))))
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave

        every { pdlService.hentPdlModell(any(), any(), any()) } returns
            mockPerson()
                .copy(doedsdato = OpplysningDTO(LocalDate.now(), "doedsdato"))

        val lagredeGrunnlagsendringshendelser =
            grunnlagsendringshendelseService.opprettDoedshendelse(
                Doedshendelse(
                    hendelseId = "1",
                    fnr = fnr,
                    doedsdato = LocalDate.of(2022, 1, 1),
                    endringstype = Endringstype.OPPRETTET,
                ),
            )
        assertAll(
            "oppretter grunnlagshendringer i databasen for doedshendelser",
            { assertEquals(1, lagredeGrunnlagsendringshendelser.size) },
            { assertEquals(sakId, opprettGrunnlagsendringshendelse.captured.sakId) },
            { assertEquals(GrunnlagsendringsType.DOEDSFALL, opprettGrunnlagsendringshendelse.captured.type) },
            {
                assertTrue(
                    opprettGrunnlagsendringshendelse.captured.opprettet >=
                        Tidspunkt.now().toLocalDatetimeUTC().minusSeconds(
                            10,
                        ),
                )
            },
            { assertEquals(grunnlagsendringshendelse, lagredeGrunnlagsendringshendelser.first()) },
        )
    }

    @Test
    fun `skal opprette grunnlagsendringshendelser i databasen for utflytting og forelder-barn`() {
        val sakId = 1L
        val fnr = KONTANT_FOT.value
        val grlagEndringUtflytting =
            grunnlagsendringshendelseMedSamsvar(
                id = randomUUID(),
                sakId = sakId,
                fnr = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )
        val grlagEndringForelderBarn =
            grunnlagsendringshendelseMedSamsvar(
                id = randomUUID(),
                sakId = sakId,
                fnr = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )

        val opprettGrlaghendelseUtflytting = slot<Grunnlagsendringshendelse>()

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(capture(opprettGrlaghendelseUtflytting))
        } returns grlagEndringUtflytting
        every { grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any()) } returns emptyList()
        coEvery {
            grunnlagClient.hentPersonSakOgRolle(any())
        } returns PersonMedSakerOgRoller(fnr, listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)))
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave

        every { pdlService.hentPdlModell(any(), any(), any()) } returns mockPerson()

        grunnlagsendringshendelseService.opprettUtflyttingshendelse(
            utflyttingsHendelse =
                UtflyttingsHendelse(
                    hendelseId = "1",
                    fnr = fnr,
                    endringstype = Endringstype.OPPRETTET,
                    tilflyttingsLand = null,
                    tilflyttingsstedIUtlandet = null,
                    utflyttingsdato = null,
                ),
        )

        val opprettGrlaghendelseForelderBarnRelasjon = slot<Grunnlagsendringshendelse>()
        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(capture(opprettGrlaghendelseForelderBarnRelasjon))
        } returns grlagEndringForelderBarn

        grunnlagsendringshendelseService.opprettForelderBarnRelasjonHendelse(
            forelderBarnRelasjonHendelse =
                ForelderBarnRelasjonHendelse(
                    hendelseId = "1",
                    fnr = fnr,
                    relatertPersonsIdent = null,
                    relatertPersonsRolle = "",
                    minRolleForPerson = "",
                    relatertPersonUtenFolkeregisteridentifikator = null,
                    endringstype = Endringstype.OPPRETTET,
                ),
        )

        assertEquals(
            opprettGrlaghendelseForelderBarnRelasjon.captured.type,
            GrunnlagsendringsType.FORELDER_BARN_RELASJON,
        )
        assertEquals(opprettGrlaghendelseUtflytting.captured.type, GrunnlagsendringsType.UTFLYTTING)
    }

    @Test
    fun `skal ikke opprette ny doedshendelse dersom en lignende allerede eksisterer`() {
        val sakId = 1L
        val fnr = KONTANT_FOT.value
        val doedsdato = LocalDate.of(2022, 7, 8)
        val grunnlagsendringshendelse1 =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.DOEDSFALL,
                id = randomUUID(),
                sakId = sakId,
                fnr = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )

        val grunnlagsendringshendelse2 =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.UTFLYTTING,
                id = randomUUID(),
                sakId = sakId,
                fnr = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )

        every { sakService.finnSak(sakId) } returns Sak(KONTANT_FOT.value, SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr)

        every { pdlService.hentPdlModell(any(), any(), any()) } returns
            mockPerson()
                .copy(doedsdato = OpplysningDTO(doedsdato, "doedsdato"))

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any())
        } returns grunnlagsendringshendelse1
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList() andThen
            listOf(
                grunnlagsendringshendelse1,
                grunnlagsendringshendelse2,
            )
        coEvery {
            grunnlagClient.hentPersonSakOgRolle(any())
        } returns PersonMedSakerOgRoller(fnr, listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)))
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave

        every { behandlingService.hentBehandlingerForSak(sakId) } returns emptyList()
        val lagredeGrunnlagsendringshendelser1 =
            grunnlagsendringshendelseService.opprettDoedshendelse(
                Doedshendelse(
                    hendelseId = "1",
                    fnr = fnr,
                    doedsdato = doedsdato,
                    endringstype = Endringstype.OPPRETTET,
                ),
            )
        // siden dette er en hendelse av en annen type skal det ikke påvirke filtreringen
        grunnlagsendringshendelseService.opprettUtflyttingshendelse(
            UtflyttingsHendelse(
                hendelseId = "1",
                fnr = fnr,
                tilflyttingsLand = null,
                tilflyttingsstedIUtlandet = null,
                utflyttingsdato = null,
                endringstype = Endringstype.OPPRETTET,
            ),
        )

        // denne skal ikke opprette en doedshendelse, siden den allerede eksisterer
        val lagredeGrunnlagsendringshendelser3 =
            grunnlagsendringshendelseService.opprettDoedshendelse(
                Doedshendelse(
                    hendelseId = "1",
                    fnr = fnr,
                    doedsdato = doedsdato,
                    endringstype = Endringstype.ANNULLERT,
                ),
            )

        assertEquals(listOf(grunnlagsendringshendelse1), lagredeGrunnlagsendringshendelser1)
        assertEquals(emptyList<Grunnlagsendringshendelse>(), lagredeGrunnlagsendringshendelser3)
    }

    @Test
    fun `skal ikke opprette ny utflyttingshendelse dersom en lignende allerede eksisterer`() {
        val sakId = 1L
        val fnr = "Soeker"
        val tilflyttingsland = "Sverige"
        val utflyttingsdato = LocalDate.of(2022, 2, 8)
        val grunnlagsendringshendelse1 =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.UTFLYTTING,
                id = randomUUID(),
                sakId = sakId,
                fnr = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )
        val grunnlagsendringshendelse2 =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.DOEDSFALL,
                id = randomUUID(),
                sakId = sakId,
                fnr = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any())
        } returns grunnlagsendringshendelse1
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList() andThen
            listOf(
                grunnlagsendringshendelse1,
                grunnlagsendringshendelse2,
            )
        coEvery {
            grunnlagClient.hentPersonSakOgRolle(any())
        } returns PersonMedSakerOgRoller(fnr, listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)))
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave

        every { pdlService.hentPdlModell(any(), any(), any()) } returns mockPerson()

        val lagredeGrunnlagsendringshendelser1 =
            grunnlagsendringshendelseService.opprettUtflyttingshendelse(
                UtflyttingsHendelse(
                    hendelseId = "1",
                    fnr = fnr,
                    tilflyttingsLand = tilflyttingsland,
                    tilflyttingsstedIUtlandet = null,
                    utflyttingsdato = utflyttingsdato,
                    endringstype = Endringstype.OPPRETTET,
                ),
            )

        // siden dette er en hendelse av en annen type skal det ikke påvirke filtreringen
        grunnlagsendringshendelseService.opprettDoedshendelse(
            Doedshendelse(
                hendelseId = "1",
                fnr = fnr,
                doedsdato = utflyttingsdato,
                endringstype = Endringstype.OPPRETTET,
            ),
        )

        // denne skal ikke opprette en utflyttingshendelse, siden den allerede eksisterer
        val lagredeGrunnlagsendringshendelser3 =
            grunnlagsendringshendelseService.opprettUtflyttingshendelse(
                UtflyttingsHendelse(
                    hendelseId = "1",
                    fnr = fnr,
                    tilflyttingsLand = tilflyttingsland,
                    tilflyttingsstedIUtlandet = null,
                    utflyttingsdato = utflyttingsdato,
                    endringstype = Endringstype.OPPRETTET,
                ),
            )

        assertEquals(listOf(grunnlagsendringshendelse1), lagredeGrunnlagsendringshendelser1)
        assertEquals(emptyList<Grunnlagsendringshendelse>(), lagredeGrunnlagsendringshendelser3)
    }

    @Test
    fun `skal ikke opprette ny forelder-barn-relasjon-hendelse dersom en lignende allerede eksisterer`() {
        val sakId = 1L
        val grunnlagsendringshendelse1 =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.FORELDER_BARN_RELASJON,
                id = randomUUID(),
                sakId = sakId,
                fnr = "Soeker",
                samsvarMellomKildeOgGrunnlag = null,
            )
        val grunnlagsendringshendelse2 =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.DOEDSFALL,
                id = randomUUID(),
                sakId = sakId,
                fnr = "Soeker",
                samsvarMellomKildeOgGrunnlag = null,
            )

        every { pdlService.hentPdlModell(any(), any(), any()) } returns mockPerson()
        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any())
        } returns grunnlagsendringshendelse1
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList() andThen
            listOf(
                grunnlagsendringshendelse1,
                grunnlagsendringshendelse2,
            )
        coEvery {
            grunnlagClient.hentPersonSakOgRolle(any())
        } returns PersonMedSakerOgRoller("Soeker", listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)))
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave

        val lagredeGrunnlagsendringshendelser1 =
            grunnlagsendringshendelseService.opprettForelderBarnRelasjonHendelse(
                ForelderBarnRelasjonHendelse(
                    hendelseId = "1",
                    fnr = "Soeker",
                    relatertPersonsIdent = "Ny forelder",
                    relatertPersonsRolle = null,
                    minRolleForPerson = null,
                    relatertPersonUtenFolkeregisteridentifikator = null,
                    endringstype = Endringstype.OPPRETTET,
                ),
            )

        // siden dette er en hendelse av en annen type skal det ikke påvirke filtreringen
        grunnlagsendringshendelseService.opprettDoedshendelse(
            Doedshendelse(
                hendelseId = "1",
                fnr = "Soeker",
                doedsdato = LocalDate.of(2022, 1, 1),
                endringstype = Endringstype.OPPRETTET,
            ),
        )

        // denne skal ikke opprette en utflyttingshendelse, siden den allerede eksisterer
        val lagredeGrunnlagsendringshendelser3 =
            grunnlagsendringshendelseService.opprettForelderBarnRelasjonHendelse(
                ForelderBarnRelasjonHendelse(
                    hendelseId = "1",
                    fnr = "Soeker",
                    relatertPersonsIdent = "Ny forelder",
                    relatertPersonsRolle = null,
                    minRolleForPerson = null,
                    relatertPersonUtenFolkeregisteridentifikator = null,
                    endringstype = Endringstype.OPPRETTET,
                ),
            )

        assertEquals(listOf(grunnlagsendringshendelse1), lagredeGrunnlagsendringshendelser1)
        assertEquals(emptyList<Grunnlagsendringshendelse>(), lagredeGrunnlagsendringshendelser3)
    }

    @Test
    fun `skal kun opprette ny forelder-barn-relasjon-hendelse for saker som finnes`() {
        val soekerIdent = "ident"
        val relatertIdent = "relatertIdent"

        val sakSomFinnes = 1L
        val sakSomIkkeFinnes = 2L

        val grunnlagsendringshendelse =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.FORELDER_BARN_RELASJON,
                id = randomUUID(),
                sakId = sakSomFinnes,
                fnr = soekerIdent,
                samsvarMellomKildeOgGrunnlag = null,
            )

        every { pdlService.hentPdlModell(any(), any(), any()) } returns mockPerson()
        every {
            sakService.finnSak(sakSomFinnes)
        } returns Sak("Soeker", SakType.BARNEPENSJON, sakSomFinnes, Enheter.defaultEnhet.enhetNr)

        every {
            sakService.finnSak(sakSomIkkeFinnes)
        } returns null

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any())
        } returns grunnlagsendringshendelse

        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(sakSomFinnes, any())
        } returns emptyList()

        coEvery {
            grunnlagClient.hentPersonSakOgRolle(any())
        } returns
            PersonMedSakerOgRoller(
                soekerIdent,
                listOf(SakidOgRolle(sakSomFinnes, Saksrolle.SOEKER), SakidOgRolle(sakSomIkkeFinnes, Saksrolle.SOEKER)),
            )

        val lagredeGrunnlagsendringshendelser =
            grunnlagsendringshendelseService.opprettForelderBarnRelasjonHendelse(
                ForelderBarnRelasjonHendelse(
                    hendelseId = "1",
                    fnr = soekerIdent,
                    relatertPersonsIdent = relatertIdent,
                    relatertPersonsRolle = null,
                    minRolleForPerson = null,
                    relatertPersonUtenFolkeregisteridentifikator = null,
                    endringstype = Endringstype.OPPRETTET,
                ),
            )

        lagredeGrunnlagsendringshendelser.size shouldBe 1
    }

    @Test
    fun `skal ikke opprette ny sivilstand-hendelse dersom en lignende allerede eksisterer`() {
        val sakId = 1L
        val grunnlagsendringshendelse1 =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.SIVILSTAND,
                id = randomUUID(),
                sakId = sakId,
                fnr = "Soeker",
                samsvarMellomKildeOgGrunnlag = null,
            )
        val grunnlagsendringshendelse2 =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.SIVILSTAND,
                id = randomUUID(),
                sakId = sakId,
                fnr = "Soeker",
                samsvarMellomKildeOgGrunnlag = null,
            )

        every { pdlService.hentPdlModell(any(), any(), any()) } returns mockPerson()

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any())
        } returns grunnlagsendringshendelse1
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList() andThen
            listOf(
                grunnlagsendringshendelse1,
                grunnlagsendringshendelse2,
            )
        coEvery {
            grunnlagClient.hentPersonSakOgRolle(any())
        } returns PersonMedSakerOgRoller("Soeker", listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)))

        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave

        val lagredeGrunnlagsendringshendelser1 =
            grunnlagsendringshendelseService.opprettSivilstandHendelse(
                SivilstandHendelse(
                    hendelseId = "1",
                    fnr = "Soeker",
                    type = "GIFT",
                    relatertVedSivilstand = "Ny partner",
                    gyldigFraOgMed = LocalDate.now(),
                    bekreftelsesdato = LocalDate.now(),
                    endringstype = Endringstype.OPPRETTET,
                ),
            )

        // siden dette er en hendelse av en annen type skal det ikke påvirke filtreringen
        grunnlagsendringshendelseService.opprettDoedshendelse(
            Doedshendelse(
                hendelseId = "1",
                fnr = "Soeker",
                doedsdato = LocalDate.of(2022, 1, 1),
                endringstype = Endringstype.OPPRETTET,
            ),
        )

        // denne skal ikke opprette en sivilstandhendelse, siden den allerede eksisterer
        val lagredeGrunnlagsendringshendelser3 =
            grunnlagsendringshendelseService.opprettSivilstandHendelse(
                SivilstandHendelse(
                    hendelseId = "1",
                    fnr = "Soeker",
                    type = "GIFT",
                    relatertVedSivilstand = "Ny partner",
                    gyldigFraOgMed = LocalDate.now(),
                    bekreftelsesdato = LocalDate.now(),
                    endringstype = Endringstype.OPPRETTET,
                ),
            )

        assertEquals(listOf(grunnlagsendringshendelse1), lagredeGrunnlagsendringshendelser1)
        assertEquals(emptyList<Grunnlagsendringshendelse>(), lagredeGrunnlagsendringshendelser3)
    }

    @Test
    fun `Oppretter ny bostedshendelse`() {
        Kontekst.set(
            Context(
                mockk(),
                object : DatabaseKontekst {
                    private val transaktionOpen = AtomicBoolean(false)

                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(block: () -> T): T {
                        if (transaktionOpen.getAndSet(true)) {
                            throw IllegalStateException("Støtter ikke nøstede transactsjoner")
                        }
                        return block()
                    }
                },
            ),
        )

        val sakIder: Set<Long> = setOf(1, 2)
        val saker =
            sakIder.map {
                Sak(
                    id = it,
                    ident = KONTANT_FOT.value,
                    sakType = SakType.BARNEPENSJON,
                    enhet = Enheter.PORSGRUNN.enhetNr,
                )
            }

        val bostedsadresse = Bostedsadresse("1", Endringstype.OPPRETTET, KONTANT_FOT.value)
        coEvery { grunnlagClient.hentAlleSakIder(any()) } returns sakIder
        coEvery { grunnlagClient.hentPersonSakOgRolle(KONTANT_FOT.value) } returns
            PersonMedSakerOgRoller(
                KONTANT_FOT.value,
                listOf(
                    SakidOgRolle(1L, Saksrolle.UKJENT),
                ),
            )
        every { sakService.oppdaterAdressebeskyttelse(any(), any()) } returns 1
        every { sakService.finnSaker(KONTANT_FOT.value) } returns saker
        every { oppgaveService.oppdaterEnhetForRelaterteOppgaver(any()) } returns Unit
        every { oppgaveService.hentOppgaverForSak(2L) } returns
            listOf(
                OppgaveIntern(
                    randomUUID(), Status.FERDIGSTILT,
                    Enheter.PORSGRUNN.enhetNr, 2L, null,
                    OppgaveType.FOERSTEGANGSBEHANDLING, "saksbehandler",
                    "refernase", null, Tidspunkt.now(), SakType.BARNEPENSJON, null, null,
                ),
            )
        every { oppgaveService.hentOppgaverForSak(1L) } returns
            listOf(
                OppgaveIntern(
                    randomUUID(), Status.UNDER_BEHANDLING, Enheter.PORSGRUNN.enhetNr,
                    1L, null, OppgaveType.FOERSTEGANGSBEHANDLING, "saksbehandler",
                    "refernase", null, Tidspunkt.now(), SakType.BARNEPENSJON, null, null,
                ),
            )
        every {
            brukerService.finnEnhetForPersonOgTema(any(), any(), any())
        } returns ArbeidsFordelingEnhet(Enheter.STEINKJER.navn, Enheter.STEINKJER.enhetNr)
        every { sakService.oppdaterEnhetForSaker(any()) } just runs
        every { grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any()) } returns emptyList()
        val grunnlagsendringshendelseMedSamsvar =
            grunnlagsendringshendelseMedSamsvar(
                sakId = 1L,
                opprettet = Tidspunkt.now().toLocalDatetimeUTC().minusMinutes(30),
                fnr = grunnlagsinformasjonDoedshendelse().fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )
        every { grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any()) } returns grunnlagsendringshendelseMedSamsvar

        every { pdlService.hentPdlModell(any(), any(), any()) } returns mockPerson()

        runBlocking {
            grunnlagsendringshendelseService.oppdaterAdresseHendelse(bostedsadresse)
        }
        coVerify(exactly = 1) { grunnlagClient.hentPersonSakOgRolle(KONTANT_FOT.value) }
    }

    @Test
    fun `Skal kunne sette adressebeskyttelse strengt fortrolig og sette enhet`() {
        val sakIder: Set<Long> = setOf(1, 2, 3, 4, 5, 6)
        val saker =
            sakIder.map {
                Sak(
                    id = it,
                    ident = KONTANT_FOT.value,
                    sakType = SakType.BARNEPENSJON,
                    enhet = Enheter.PORSGRUNN.enhetNr,
                )
            }
        val fnr = "16508201382"
        val adressebeskyttelse =
            Adressebeskyttelse("1", Endringstype.OPPRETTET, fnr, AdressebeskyttelseGradering.STRENGT_FORTROLIG)

        coEvery { grunnlagClient.hentAlleSakIder(any()) } returns sakIder
        every { sakService.oppdaterAdressebeskyttelse(any(), any()) } returns 1
        every { sakService.finnSaker(fnr) } returns saker
        every { oppgaveService.oppdaterEnhetForRelaterteOppgaver(any()) } returns Unit
        every {
            brukerService.finnEnhetForPersonOgTema(any(), any(), any())
        } returns ArbeidsFordelingEnhet(Enheter.STEINKJER.navn, Enheter.STEINKJER.enhetNr)
        every { sakService.oppdaterEnhetForSaker(any()) } just runs
        runBlocking {
            grunnlagsendringshendelseService.oppdaterAdressebeskyttelseHendelse(adressebeskyttelse)
        }

        coVerify(exactly = 1) { grunnlagClient.hentAlleSakIder(adressebeskyttelse.fnr) }

        every { pdlService.hentPdlModell(any(), any(), any()) } returns mockPerson()

        sakIder.forEach {
            verify(exactly = 1) {
                sakService.oppdaterAdressebeskyttelse(
                    it,
                    adressebeskyttelse.adressebeskyttelseGradering,
                )
            }
            verify(exactly = 1) {
                oppgaveService.oppdaterEnhetForRelaterteOppgaver(any())
            }
        }
    }

    @Test
    fun `Skal kunne sette fortrolig og sette enhet`() {
        val sakIder: Set<Long> = setOf(1, 2, 3, 4, 5, 6)
        val saker =
            sakIder.map {
                Sak(
                    id = it,
                    ident = KONTANT_FOT.value,
                    sakType = SakType.BARNEPENSJON,
                    enhet = Enheter.PORSGRUNN.enhetNr,
                )
            }
        val fnr = AVDOED2_FOEDSELSNUMMER.value
        val adressebeskyttelse =
            Adressebeskyttelse("1", Endringstype.OPPRETTET, fnr, AdressebeskyttelseGradering.FORTROLIG)

        coEvery { grunnlagClient.hentAlleSakIder(any()) } returns sakIder
        every { sakService.oppdaterAdressebeskyttelse(any(), any()) } returns 1
        every { sakService.finnSaker(fnr) } returns saker
        every { oppgaveService.oppdaterEnhetForRelaterteOppgaver(any()) } returns Unit
        every {
            brukerService.finnEnhetForPersonOgTema(any(), any(), any())
        } returns ArbeidsFordelingEnhet(Enheter.STEINKJER.navn, Enheter.STEINKJER.enhetNr)
        every { sakService.oppdaterEnhetForSaker(any()) } just runs
        runBlocking {
            grunnlagsendringshendelseService.oppdaterAdressebeskyttelseHendelse(adressebeskyttelse)
        }

        coVerify(exactly = 1) { grunnlagClient.hentAlleSakIder(adressebeskyttelse.fnr) }

        sakIder.forEach {
            verify(exactly = 1) {
                sakService.oppdaterAdressebeskyttelse(
                    it,
                    adressebeskyttelse.adressebeskyttelseGradering,
                )
            }
            verify(exactly = 1) {
                oppgaveService.oppdaterEnhetForRelaterteOppgaver(any())
            }
        }
    }

    @Test
    fun `skal kunne opprette hendelser som følge av feilet regulering`() {
        val sakId = 1L
        val lagretHendelse =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.GRUNNBELOEP,
                id = randomUUID(),
                sakId = sakId,
                fnr = KONTANT_FOT.value,
                samsvarMellomKildeOgGrunnlag = null,
            )

        val hendelseSomLagres = slot<Grunnlagsendringshendelse>()

        every { sakService.finnSak(sakId) } returns
            Sak(
                KONTANT_FOT.value,
                SakType.BARNEPENSJON,
                sakId,
                Enheter.defaultEnhet.enhetNr,
            )

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(capture(hendelseSomLagres))
        } returns lagretHendelse
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(sakId, any())
        } returns emptyList()
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave
        every { pdlService.hentPdlModell(any(), any(), any()) } returns mockPerson()
        every { behandlingService.hentBehandlingerForSak(any()) } returns emptyList()

        grunnlagsendringshendelseService.opprettEndretGrunnbeloepHendelse(sakId)

        assertEquals(hendelseSomLagres.captured.type, GrunnlagsendringsType.GRUNNBELOEP)
        assertEquals(hendelseSomLagres.captured.sakId, sakId)
    }
}
