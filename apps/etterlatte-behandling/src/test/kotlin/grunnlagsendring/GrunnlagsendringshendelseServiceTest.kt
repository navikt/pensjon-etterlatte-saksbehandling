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
import no.nav.etterlatte.behandling.EnhetService
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlKlientImpl
import no.nav.etterlatte.common.klienter.hentDoedsdato
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlient
import no.nav.etterlatte.grunnlagsendringshendelseMedSamsvar
import no.nav.etterlatte.grunnlagsinformasjonDoedshendelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.SakOgRolle
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning.Konstant
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.oppgave.opprettNyOppgaveMedReferanseOgSak
import no.nav.etterlatte.libs.common.pdl.PersonDTO
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
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED2_FOEDSELSNUMMER
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.LocalDate
import java.util.UUID
import java.util.UUID.randomUUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

internal class GrunnlagsendringshendelseServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val grunnlagshendelsesDao = mockk<GrunnlagsendringshendelseDao>(relaxUnitFun = true)
    private val pdlService = mockk<PdlKlientImpl>()
    private val grunnlagClient = mockk<GrunnlagKlient>(relaxed = true, relaxUnitFun = true)
    private val sakService = mockk<SakService>(relaxed = true)
    private val oppgaveService = mockk<OppgaveService>()
    private val enhetService = mockk<EnhetService>()
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
            enhetService,
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
    fun `skal opprette grunnlagsendringshendelser i databasen for doedshendelser`() {
        val sakId = 1L
        val fnr = "Soeker"
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

        every {
            grunnlagshendelsesDao.oppdaterGrunnlagsendringStatusOgSamsvar(any(), any(), any(), any())
        } returns Unit
        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(capture(opprettGrunnlagsendringshendelse))
        } returns grunnlagsendringshendelse
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList()

        every { behandlingService.hentBehandlingerForSak(1L) } returns foerstegangsbehandlinger

        coEvery { grunnlagClient.hentPersonSakOgRolle(any()) }
            .returns(PersonMedSakerOgRoller(fnr, listOf(SakOgRolle(sakId, Saksrolle.SOEKER))))
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave

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
            { assertEquals(1, lagredeGrunnlagsendringshendelser.size) },
            { assertEquals(grunnlagsendringshendelse, lagredeGrunnlagsendringshendelser.first()) },
        )
    }

    @Test
    fun `skal opprette grunnlagsendringshendelser i databasen for utflytting og forelder-barn`() {
        val sakId = 1L
        val fnr = "Soeker"
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
        } returns PersonMedSakerOgRoller(fnr, listOf(SakOgRolle(sakId, Saksrolle.SOEKER)))
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave
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
        val fnr = "Soeker"
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
        } returns PersonMedSakerOgRoller(fnr, listOf(SakOgRolle(sakId, Saksrolle.SOEKER)))
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave
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
        } returns PersonMedSakerOgRoller(fnr, listOf(SakOgRolle(sakId, Saksrolle.SOEKER)))
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave
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
        } returns PersonMedSakerOgRoller("Soeker", listOf(SakOgRolle(sakId, Saksrolle.SOEKER)))
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
                listOf(SakOgRolle(sakSomFinnes, Saksrolle.SOEKER), SakOgRolle(sakSomIkkeFinnes, Saksrolle.SOEKER)),
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
        } returns PersonMedSakerOgRoller("Soeker", listOf(SakOgRolle(sakId, Saksrolle.SOEKER)))

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
    fun `skal sette status til SJEKKET_AV_JOBB og opprette oppgave, for hendelser som er sjekket av jobb`() {
        val minutter = 60L
        val avdoedFnr = AVDOED2_FOEDSELSNUMMER.value
        val sakId = 1L
        val grlgId = randomUUID()
        val doedsdato = LocalDate.of(2022, 3, 13)
        val rolle = Saksrolle.SOEKER
        val personRolle = rolle.toPersonrolle(SakType.BARNEPENSJON)
        val grunnlagsendringshendelser =
            listOf(
                grunnlagsendringshendelseMedSamsvar(
                    id = grlgId,
                    sakId = sakId,
                    opprettet = Tidspunkt.now().toLocalDatetimeUTC().minusHours(1),
                    fnr = avdoedFnr,
                    hendelseGjelderRolle = rolle,
                    samsvarMellomKildeOgGrunnlag = null,
                ),
            )

        val idArg = slot<UUID>()
        every {
            grunnlagshendelsesDao.hentIkkeVurderteGrunnlagsendringshendelserEldreEnn(
                minutter,
            )
        } returns grunnlagsendringshendelser
        every { sakService.finnSak(any()) } returns
            Sak(
                avdoedFnr,
                SakType.BARNEPENSJON,
                sakId,
                enhet = Enheter.defaultEnhet.enhetNr,
            )
        every {
            grunnlagshendelsesDao.oppdaterGrunnlagsendringStatusOgSamsvar(
                capture(idArg),
                GrunnlagsendringStatus.VENTER_PAA_JOBB,
                GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                any(),
            )
        } returns Unit
        val mockPdlModel =
            mockk<PersonDTO> {
                every { hentDoedsdato() } returns doedsdato
            }
        every { pdlService.hentPdlModell(avdoedFnr, personRolle, SakType.BARNEPENSJON) } returns mockPdlModel

        every { behandlingService.hentBehandlingerForSak(sakId) } returns
            listOf(
                mockk {
                    every { status } returns BehandlingStatus.VILKAARSVURDERT
                    every { id } returns randomUUID()
                    every { type } returns BehandlingType.FØRSTEGANGSBEHANDLING
                },
            )

        coEvery { grunnlagClient.hentGrunnlag(any()) } returns null

        grunnlagsendringshendelseService.sjekkKlareGrunnlagsendringshendelser(minutter)

        assertEquals(grlgId, idArg.captured)
        verify(exactly = grunnlagsendringshendelser.size) {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                any(),
                any(),
                OppgaveKilde.HENDELSE,
                OppgaveType.VURDER_KONSEKVENS,
                any(),
            )
        }
    }

    @Test
    fun `Hendelse forkastes når den nye informasjonen samsvarer med PDL-data`() {
        val hendelseId = randomUUID()
        val minutter = Random.nextLong()
        val avdoedFnr = AVDOED2_FOEDSELSNUMMER.value
        val sakId = Random.nextLong()
        val rolle = Saksrolle.SOEKER
        val doedsdato = LocalDate.now()
        val personRolle = rolle.toPersonrolle(SakType.BARNEPENSJON)

        val grunnlagsendringshendelser =
            listOf(
                grunnlagsendringshendelseMedSamsvar(
                    id = hendelseId,
                    sakId = sakId,
                    opprettet = Tidspunkt.now().toLocalDatetimeUTC().minusHours(1),
                    fnr = avdoedFnr,
                    hendelseGjelderRolle = rolle,
                    samsvarMellomKildeOgGrunnlag = null,
                ),
            )

        val kilde = Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, "opplysningsId1")
        val grunnlag =
            Grunnlag(
                soeker =
                    mapOf(
                        Opplysningstype.DOEDSDATO to Konstant(randomUUID(), kilde, doedsdato.toJsonNode()),
                    ),
                familie = emptyList(),
                sak = emptyMap(),
                metadata = no.nav.etterlatte.libs.common.grunnlag.Metadata(sakId, 1),
            )
        coEvery { grunnlagClient.hentGrunnlag(sakId) } returns grunnlag
        every { grunnlagshendelsesDao.hentIkkeVurderteGrunnlagsendringshendelserEldreEnn(any()) } returns grunnlagsendringshendelser
        every { sakService.finnSak(any()) }
            .returns(Sak(avdoedFnr, SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr))

        every { pdlService.hentPdlModell(any(), any(), any()) }
            .returns(mockk<PersonDTO> { every { hentDoedsdato() } returns doedsdato })

        grunnlagsendringshendelseService.sjekkKlareGrunnlagsendringshendelser(minutter)

        verify {
            grunnlagshendelsesDao.hentIkkeVurderteGrunnlagsendringshendelserEldreEnn(minutter)
            sakService.finnSak(sakId)
            pdlService.hentPdlModell(avdoedFnr, personRolle, SakType.BARNEPENSJON)
            grunnlagshendelsesDao.oppdaterGrunnlagsendringStatusOgSamsvar(
                hendelseId = hendelseId,
                foerStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
                etterStatus = GrunnlagsendringStatus.FORKASTET,
                samsvarMellomKildeOgGrunnlag = any(),
            )
        }
        coVerify {
            grunnlagClient.hentGrunnlag(sakId)
        }
    }

    @Test
    fun `Skal kunne sette adresse og faa oppdatert enhet`() {
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
        val bostedsadresse = Bostedsadresse("1", Endringstype.OPPRETTET, fnr)
        coEvery { grunnlagClient.hentAlleSakIder(any()) } returns sakIder
        every { sakService.oppdaterAdressebeskyttelse(any(), any()) } returns 1
        every { sakService.finnSaker(fnr) } returns saker
        every { oppgaveService.oppdaterEnhetForRelaterteOppgaver(any()) } returns Unit
        every { oppgaveService.hentOppgaverForSak(any()) } returns emptyList()
        every {
            enhetService.finnEnhetForPersonOgTema(any(), any(), any())
        } returns ArbeidsFordelingEnhet(Enheter.STEINKJER.navn, Enheter.STEINKJER.enhetNr)
        every { sakService.oppdaterEnhetForSaker(any()) } just runs
        runBlocking {
            grunnlagsendringshendelseService.oppdaterAdresseHendelse(bostedsadresse)
        }
        coVerify(exactly = 1) { sakService.finnSaker(bostedsadresse.fnr) }

        verify(exactly = 6) {
            sakService.oppdaterEnhetForSaker(
                any(),
            )
        }
        verify(exactly = 6) {
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(
                any(),
            )
        }
    }

    @Test
    fun `Oppretter ny bostedshendelse hvis det finnes en oppgave under behandling for sak`() {
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
                    SakOgRolle(1L, Saksrolle.UKJENT),
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
            enhetService.finnEnhetForPersonOgTema(any(), any(), any())
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
        runBlocking {
            grunnlagsendringshendelseService.oppdaterAdresseHendelse(bostedsadresse)
        }
        coVerify(exactly = 1) { sakService.finnSaker(bostedsadresse.fnr) }
        coVerify(exactly = 1) { grunnlagClient.hentPersonSakOgRolle(KONTANT_FOT.value) }
        verify(exactly = 1) {
            sakService.oppdaterEnhetForSaker(
                any(),
            )
        }
        verify(exactly = 1) {
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(
                any(),
            )
        }
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
            enhetService.finnEnhetForPersonOgTema(any(), any(), any())
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
            enhetService.finnEnhetForPersonOgTema(any(), any(), any())
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
                fnr = "test-fnr",
                samsvarMellomKildeOgGrunnlag = null,
            )

        val hendelseSomLagres = slot<Grunnlagsendringshendelse>()

        every { sakService.finnSak(sakId) } returns
            Sak(
                "test-fnr",
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
        grunnlagsendringshendelseService.opprettEndretGrunnbeloepHendelse(sakId)

        assertEquals(hendelseSomLagres.captured.type, GrunnlagsendringsType.GRUNNBELOEP)
        assertEquals(hendelseSomLagres.captured.sakId, sakId)
    }
}
