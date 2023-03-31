package no.nav.etterlatte.grunnlagsendring

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.common.klienter.PdlKlientImpl
import no.nav.etterlatte.common.klienter.hentDoedsdato
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlient
import no.nav.etterlatte.grunnlagsendringshendelseMedSamsvar
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsType
import no.nav.etterlatte.libs.common.behandling.Grunnlagsendringshendelse
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.SakOgRolle
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.sak.SakDaoAdressebeskyttelse
import no.nav.etterlatte.sak.SakServiceAdressebeskyttelseImpl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.LocalDate
import java.util.UUID

internal class GrunnlagsendringshendelseServiceTest {

    private val generellBehandlingService = mockk<GenerellBehandlingService>()
    private val grunnlagshendelsesDao = mockk<GrunnlagsendringshendelseDao>()
    private val pdlService = mockk<PdlKlientImpl>()
    private val grunnlagClient = mockk<GrunnlagKlient>(relaxed = true, relaxUnitFun = true)
    private val adressebeskyttelseDaoMock = mockk<SakDaoAdressebeskyttelse>()
    private val sakServiceAdressebeskyttelse = SakServiceAdressebeskyttelseImpl(adressebeskyttelseDaoMock)

    private val grunnlagsendringshendelseService = GrunnlagsendringshendelseService(
        grunnlagshendelsesDao,
        generellBehandlingService,
        pdlService,
        grunnlagClient,
        sakServiceAdressebeskyttelse
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
                }
            )
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
        val foerstegangsbehandlinger = listOf(
            foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.IVERKSATT),
            foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.FATTET_VEDTAK)
        )
        val grunnlagsendringshendelse = grunnlagsendringshendelseMedSamsvar(
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = fnr,
            samsvarMellomPdlOgGrunnlag = null
        )

        val opprettGrunnlagsendringshendelse = slot<Grunnlagsendringshendelse>()

        every { grunnlagshendelsesDao.oppdaterGrunnlagsendringStatus(any(), any(), any(), any()) } returns Unit
        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(capture(opprettGrunnlagsendringshendelse))
        } returns grunnlagsendringshendelse
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList()

        every { generellBehandlingService.hentBehandlingerISak(1L) } returns foerstegangsbehandlinger
        every { generellBehandlingService.alleBehandlingerForSoekerMedFnr(fnr) } returns foerstegangsbehandlinger

        coEvery { grunnlagClient.hentPersonSakOgRolle(any()) }
            .returns(PersonMedSakerOgRoller(fnr, listOf(SakOgRolle(sakId, Saksrolle.SOEKER))))

        val lagredeGrunnlagsendringshendelser = grunnlagsendringshendelseService.opprettDoedshendelse(
            Doedshendelse(
                avdoedFnr = fnr,
                doedsdato = LocalDate.of(2022, 1, 1),
                endringstype = Endringstype.OPPRETTET
            )
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
                            10
                        )
                )
            },
            { assertEquals(1, lagredeGrunnlagsendringshendelser.size) },
            { assertEquals(grunnlagsendringshendelse, lagredeGrunnlagsendringshendelser.first()) }
        )
    }

    @Test
    fun `skal opprette grunnlagsendringshendelser i databasen for utflytting og forelder-barn`() {
        val sakId = 1L
        val fnr = "Soeker"
        val grlagEndringUtflytting = grunnlagsendringshendelseMedSamsvar(
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = fnr,
            samsvarMellomPdlOgGrunnlag = null
        )
        val grlagEndringForelderBarn = grunnlagsendringshendelseMedSamsvar(
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = fnr,
            samsvarMellomPdlOgGrunnlag = null
        )

        val opprettGrlaghendelseUtflytting = slot<Grunnlagsendringshendelse>()

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(capture(opprettGrlaghendelseUtflytting))
        } returns grlagEndringUtflytting
        every { grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any()) } returns emptyList()
        coEvery {
            grunnlagClient.hentPersonSakOgRolle(any())
        } returns PersonMedSakerOgRoller(fnr, listOf(SakOgRolle(sakId, Saksrolle.SOEKER)))

        grunnlagsendringshendelseService.opprettUtflyttingshendelse(
            utflyttingsHendelse = UtflyttingsHendelse(
                fnr = fnr,
                endringstype = Endringstype.OPPRETTET,
                tilflyttingsLand = null,
                tilflyttingsstedIUtlandet = null,
                utflyttingsdato = null
            )
        )

        val opprettGrlaghendelseForelderBarnRelasjon = slot<Grunnlagsendringshendelse>()
        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(capture(opprettGrlaghendelseForelderBarnRelasjon))
        } returns grlagEndringForelderBarn

        grunnlagsendringshendelseService.opprettForelderBarnRelasjonHendelse(
            forelderBarnRelasjonHendelse = ForelderBarnRelasjonHendelse(
                fnr = fnr,
                relatertPersonsIdent = null,
                relatertPersonsRolle = "",
                minRolleForPerson = "",
                relatertPersonUtenFolkeregisteridentifikator = null,
                endringstype = Endringstype.OPPRETTET
            )
        )

        assertEquals(
            opprettGrlaghendelseForelderBarnRelasjon.captured.type,
            GrunnlagsendringsType.FORELDER_BARN_RELASJON
        )
        assertEquals(opprettGrlaghendelseUtflytting.captured.type, GrunnlagsendringsType.UTFLYTTING)
    }

    @Test
    fun `skal ikke opprette ny doedshendelse dersom en lignende allerede eksisterer`() {
        val sakId = 1L
        val fnr = "Soeker"
        val doedsdato = LocalDate.of(2022, 7, 8)
        val grunnlagsendringshendelse1 = grunnlagsendringshendelseMedSamsvar(
            type = GrunnlagsendringsType.DOEDSFALL,
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = fnr,
            samsvarMellomPdlOgGrunnlag = null
        )

        val grunnlagsendringshendelse2 = grunnlagsendringshendelseMedSamsvar(
            type = GrunnlagsendringsType.UTFLYTTING,
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = fnr,
            samsvarMellomPdlOgGrunnlag = null
        )

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any())
        } returns grunnlagsendringshendelse1
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList() andThen listOf(
            grunnlagsendringshendelse1,
            grunnlagsendringshendelse2
        )
        coEvery {
            grunnlagClient.hentPersonSakOgRolle(any())
        } returns PersonMedSakerOgRoller(fnr, listOf(SakOgRolle(sakId, Saksrolle.SOEKER)))

        val lagredeGrunnlagsendringshendelser1 = grunnlagsendringshendelseService.opprettDoedshendelse(
            Doedshendelse(
                avdoedFnr = fnr,
                doedsdato = doedsdato,
                endringstype = Endringstype.OPPRETTET
            )
        )
        // siden dette er en hendelse av en annen type skal det ikke påvirke filtreringen
        grunnlagsendringshendelseService.opprettUtflyttingshendelse(
            UtflyttingsHendelse(
                fnr = fnr,
                tilflyttingsLand = null,
                tilflyttingsstedIUtlandet = null,
                utflyttingsdato = null,
                endringstype = Endringstype.OPPRETTET

            )
        )

        // denne skal ikke opprette en doedshendelse, siden den allerede eksisterer
        val lagredeGrunnlagsendringshendelser3 = grunnlagsendringshendelseService.opprettDoedshendelse(
            Doedshendelse(
                avdoedFnr = fnr,
                doedsdato = doedsdato,
                endringstype = Endringstype.ANNULLERT
            )
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
        val grunnlagsendringshendelse1 = grunnlagsendringshendelseMedSamsvar(
            type = GrunnlagsendringsType.UTFLYTTING,
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = fnr,
            samsvarMellomPdlOgGrunnlag = null
        )
        val grunnlagsendringshendelse2 = grunnlagsendringshendelseMedSamsvar(
            type = GrunnlagsendringsType.DOEDSFALL,
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = fnr,
            samsvarMellomPdlOgGrunnlag = null
        )

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any())
        } returns grunnlagsendringshendelse1
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList() andThen listOf(
            grunnlagsendringshendelse1,
            grunnlagsendringshendelse2
        )
        coEvery {
            grunnlagClient.hentPersonSakOgRolle(any())
        } returns PersonMedSakerOgRoller(fnr, listOf(SakOgRolle(sakId, Saksrolle.SOEKER)))

        val lagredeGrunnlagsendringshendelser1 = grunnlagsendringshendelseService.opprettUtflyttingshendelse(
            UtflyttingsHendelse(
                fnr = fnr,
                tilflyttingsLand = tilflyttingsland,
                tilflyttingsstedIUtlandet = null,
                utflyttingsdato = utflyttingsdato,
                endringstype = Endringstype.OPPRETTET
            )
        )

        // siden dette er en hendelse av en annen type skal det ikke påvirke filtreringen
        grunnlagsendringshendelseService.opprettDoedshendelse(
            Doedshendelse(
                avdoedFnr = fnr,
                doedsdato = utflyttingsdato,
                endringstype = Endringstype.OPPRETTET
            )
        )

        // denne skal ikke opprette en utflyttingshendelse, siden den allerede eksisterer
        val lagredeGrunnlagsendringshendelser3 = grunnlagsendringshendelseService.opprettUtflyttingshendelse(
            UtflyttingsHendelse(
                fnr = fnr,
                tilflyttingsLand = tilflyttingsland,
                tilflyttingsstedIUtlandet = null,
                utflyttingsdato = utflyttingsdato,
                endringstype = Endringstype.OPPRETTET
            )
        )

        assertEquals(listOf(grunnlagsendringshendelse1), lagredeGrunnlagsendringshendelser1)
        assertEquals(emptyList<Grunnlagsendringshendelse>(), lagredeGrunnlagsendringshendelser3)
    }

    @Test
    fun `skal ikke opprette ny forelder-barn-relasjon-hendelse dersom en lignende allerede eksisterer`() {
        val sakId = 1L
        val grunnlagsendringshendelse1 = grunnlagsendringshendelseMedSamsvar(
            type = GrunnlagsendringsType.FORELDER_BARN_RELASJON,
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = "Soeker",
            samsvarMellomPdlOgGrunnlag = null
        )
        val grunnlagsendringshendelse2 = grunnlagsendringshendelseMedSamsvar(
            type = GrunnlagsendringsType.DOEDSFALL,
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = "Soeker",
            samsvarMellomPdlOgGrunnlag = null
        )

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any())
        } returns grunnlagsendringshendelse1
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList() andThen listOf(
            grunnlagsendringshendelse1,
            grunnlagsendringshendelse2
        )
        coEvery {
            grunnlagClient.hentPersonSakOgRolle(any())
        } returns PersonMedSakerOgRoller("Soeker", listOf(SakOgRolle(sakId, Saksrolle.SOEKER)))

        val lagredeGrunnlagsendringshendelser1 = grunnlagsendringshendelseService.opprettForelderBarnRelasjonHendelse(
            ForelderBarnRelasjonHendelse(
                fnr = "Soeker",
                relatertPersonsIdent = "Ny forelder",
                relatertPersonsRolle = null,
                minRolleForPerson = null,
                relatertPersonUtenFolkeregisteridentifikator = null,
                endringstype = Endringstype.OPPRETTET
            )
        )

        // siden dette er en hendelse av en annen type skal det ikke påvirke filtreringen
        grunnlagsendringshendelseService.opprettDoedshendelse(
            Doedshendelse(
                avdoedFnr = "Soeker",
                doedsdato = LocalDate.of(2022, 1, 1),
                endringstype = Endringstype.OPPRETTET
            )
        )

        // denne skal ikke opprette en utflyttingshendelse, siden den allerede eksisterer
        val lagredeGrunnlagsendringshendelser3 = grunnlagsendringshendelseService.opprettForelderBarnRelasjonHendelse(
            ForelderBarnRelasjonHendelse(
                fnr = "Soeker",
                relatertPersonsIdent = "Ny forelder",
                relatertPersonsRolle = null,
                minRolleForPerson = null,
                relatertPersonUtenFolkeregisteridentifikator = null,
                endringstype = Endringstype.OPPRETTET
            )
        )

        assertEquals(listOf(grunnlagsendringshendelse1), lagredeGrunnlagsendringshendelser1)
        assertEquals(emptyList<Grunnlagsendringshendelse>(), lagredeGrunnlagsendringshendelser3)
    }

    @Test
    fun `skal sette status til SJEKKET_AV_JOBB, for hendelser som er sjekket av jobb`() {
        val minutter = 60L
        val avdoedFnr = "16017919184"
        val sakId = 1L
        val grlg_id = UUID.randomUUID()
        val doedsdato = LocalDate.of(2022, 3, 13)
        val rolle = Saksrolle.SOEKER
        val personRolle = rolle.toPersonrolle()
        val grunnlagsendringshendelser = listOf(
            grunnlagsendringshendelseMedSamsvar(
                id = grlg_id,
                sakId = sakId,
                opprettet = Tidspunkt.now().toLocalDatetimeUTC().minusHours(1),
                fnr = avdoedFnr,
                hendelseGjelderRolle = rolle,
                samsvarMellomPdlOgGrunnlag = null
            )
        )

        val idArg = slot<UUID>()
        every {
            grunnlagshendelsesDao.hentIkkeVurderteGrunnlagsendringshendelserEldreEnn(
                minutter
            )
        } returns grunnlagsendringshendelser
        every {
            grunnlagshendelsesDao.oppdaterGrunnlagsendringStatus(
                capture(idArg),
                GrunnlagsendringStatus.VENTER_PAA_JOBB,
                GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                any()
            )
        } returns Unit
        val mockPdlModel = mockk<PersonDTO> {
            every { hentDoedsdato() } returns doedsdato
        }
        every { pdlService.hentPdlModell(avdoedFnr, personRolle) } returns mockPdlModel

        every { generellBehandlingService.hentBehandlingerISak(sakId) } returns listOf(
            mockk {
                every { status } returns BehandlingStatus.VILKAARSVURDERT
                every { id } returns UUID.randomUUID()
                every { type } returns BehandlingType.FØRSTEGANGSBEHANDLING
            }
        )

        coEvery { grunnlagClient.hentGrunnlag(any()) } returns null

        grunnlagsendringshendelseService.sjekkKlareGrunnlagsendringshendelser(minutter)

        assertEquals(grlg_id, idArg.captured)
    }

    @Test
    fun `Sett adressebeskyttelse fungerer`() {
        val sakIder: Set<Long> = setOf(1, 2, 3, 4, 5, 6)
        val adressebeskyttelse =
            Adressebeskyttelse("16017919184", AdressebeskyttelseGradering.STRENGT_FORTROLIG, Endringstype.OPPRETTET)

        coEvery { grunnlagClient.hentAlleSakIder(any()) } returns sakIder
        every { adressebeskyttelseDaoMock.oppdaterAdresseBeskyttelse(any(), any()) } returns 1

        runBlocking {
            grunnlagsendringshendelseService.oppdaterAdressebeskyttelseHendelse(adressebeskyttelse)
        }

        coVerify(exactly = 1) { grunnlagClient.hentAlleSakIder(adressebeskyttelse.fnr) }

        sakIder.forEach {
            verify(exactly = 1) {
                adressebeskyttelseDaoMock.oppdaterAdresseBeskyttelse(
                    it,
                    adressebeskyttelse.adressebeskyttelseGradering
                )
            }
        }
    }
}