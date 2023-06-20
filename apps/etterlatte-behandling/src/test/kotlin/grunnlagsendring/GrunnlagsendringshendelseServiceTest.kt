package no.nav.etterlatte.grunnlagsendring

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
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.TRIVIELL_MIDTPUNKT
import no.nav.etterlatte.behandling.GenerellBehandlingService
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
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.SakOgRolle
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sak.SakTilgangDao
import no.nav.etterlatte.sak.TilgangServiceImpl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.LocalDate
import java.util.*

internal class GrunnlagsendringshendelseServiceTest {

    private val generellBehandlingService = mockk<GenerellBehandlingService>()
    private val grunnlagshendelsesDao = mockk<GrunnlagsendringshendelseDao>()
    private val pdlService = mockk<PdlKlientImpl>()
    private val grunnlagClient = mockk<GrunnlagKlient>(relaxed = true, relaxUnitFun = true)
    private val adressebeskyttelseDaoMock = mockk<SakTilgangDao>()
    private val tilgangServiceImpl = TilgangServiceImpl(adressebeskyttelseDaoMock)
    private val sakService = mockk<SakService>()

    private val grunnlagsendringshendelseService = GrunnlagsendringshendelseService(
        grunnlagshendelsesDao,
        generellBehandlingService,
        pdlService,
        grunnlagClient,
        tilgangServiceImpl,
        sakService
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
            samsvarMellomKildeOgGrunnlag = null
        )

        val opprettGrunnlagsendringshendelse = slot<Grunnlagsendringshendelse>()

        every { grunnlagshendelsesDao.oppdaterGrunnlagsendringStatusOgSamsvar(any(), any(), any(), any()) } returns Unit
        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(capture(opprettGrunnlagsendringshendelse))
        } returns grunnlagsendringshendelse
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList()

        every { generellBehandlingService.hentBehandlingerISak(1L) } returns foerstegangsbehandlinger

        coEvery { grunnlagClient.hentPersonSakOgRolle(any()) }
            .returns(PersonMedSakerOgRoller(fnr, listOf(SakOgRolle(sakId, Saksrolle.SOEKER))))

        val lagredeGrunnlagsendringshendelser = grunnlagsendringshendelseService.opprettDoedshendelse(
            Doedshendelse(
                hendelseId = "1",
                fnr = fnr,
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
            samsvarMellomKildeOgGrunnlag = null
        )
        val grlagEndringForelderBarn = grunnlagsendringshendelseMedSamsvar(
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = fnr,
            samsvarMellomKildeOgGrunnlag = null
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
                hendelseId = "1",
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
                hendelseId = "1",
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
            samsvarMellomKildeOgGrunnlag = null
        )

        val grunnlagsendringshendelse2 = grunnlagsendringshendelseMedSamsvar(
            type = GrunnlagsendringsType.UTFLYTTING,
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = fnr,
            samsvarMellomKildeOgGrunnlag = null
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
                hendelseId = "1",
                fnr = fnr,
                doedsdato = doedsdato,
                endringstype = Endringstype.OPPRETTET
            )
        )
        // siden dette er en hendelse av en annen type skal det ikke påvirke filtreringen
        grunnlagsendringshendelseService.opprettUtflyttingshendelse(
            UtflyttingsHendelse(
                hendelseId = "1",
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
                hendelseId = "1",
                fnr = fnr,
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
            samsvarMellomKildeOgGrunnlag = null
        )
        val grunnlagsendringshendelse2 = grunnlagsendringshendelseMedSamsvar(
            type = GrunnlagsendringsType.DOEDSFALL,
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = fnr,
            samsvarMellomKildeOgGrunnlag = null
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
                hendelseId = "1",
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
                hendelseId = "1",
                fnr = fnr,
                doedsdato = utflyttingsdato,
                endringstype = Endringstype.OPPRETTET
            )
        )

        // denne skal ikke opprette en utflyttingshendelse, siden den allerede eksisterer
        val lagredeGrunnlagsendringshendelser3 = grunnlagsendringshendelseService.opprettUtflyttingshendelse(
            UtflyttingsHendelse(
                hendelseId = "1",
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
            samsvarMellomKildeOgGrunnlag = null
        )
        val grunnlagsendringshendelse2 = grunnlagsendringshendelseMedSamsvar(
            type = GrunnlagsendringsType.DOEDSFALL,
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = "Soeker",
            samsvarMellomKildeOgGrunnlag = null
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
                hendelseId = "1",
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
                hendelseId = "1",
                fnr = "Soeker",
                doedsdato = LocalDate.of(2022, 1, 1),
                endringstype = Endringstype.OPPRETTET
            )
        )

        // denne skal ikke opprette en utflyttingshendelse, siden den allerede eksisterer
        val lagredeGrunnlagsendringshendelser3 = grunnlagsendringshendelseService.opprettForelderBarnRelasjonHendelse(
            ForelderBarnRelasjonHendelse(
                hendelseId = "1",
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
                samsvarMellomKildeOgGrunnlag = null
            )
        )

        val idArg = slot<UUID>()
        every {
            grunnlagshendelsesDao.hentIkkeVurderteGrunnlagsendringshendelserEldreEnn(
                minutter
            )
        } returns grunnlagsendringshendelser
        every { sakService.finnSak(any()) } returns Sak(
            avdoedFnr,
            SakType.BARNEPENSJON,
            sakId,
            enhet = Enheter.defaultEnhet.enhetNr
        )
        every {
            grunnlagshendelsesDao.oppdaterGrunnlagsendringStatusOgSamsvar(
                capture(idArg),
                GrunnlagsendringStatus.VENTER_PAA_JOBB,
                GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                any()
            )
        } returns Unit
        val mockPdlModel = mockk<PersonDTO> {
            every { hentDoedsdato() } returns doedsdato
        }
        every { pdlService.hentPdlModell(avdoedFnr, personRolle, SakType.BARNEPENSJON) } returns mockPdlModel

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
    fun `Skal kunne sette adressebeskyttelse strengt fortrolig og sette enhet`() {
        val sakIder: Set<Long> = setOf(1, 2, 3, 4, 5, 6)
        val saker = sakIder.map {
            Sak(
                id = it,
                ident = TRIVIELL_MIDTPUNKT.value,
                sakType = SakType.BARNEPENSJON,
                enhet = Enheter.PORSGRUNN.enhetNr
            )
        }
        val fnr = "16017919184"
        val adressebeskyttelse =
            Adressebeskyttelse("1", Endringstype.OPPRETTET, fnr, AdressebeskyttelseGradering.STRENGT_FORTROLIG)

        coEvery { grunnlagClient.hentAlleSakIder(any()) } returns sakIder
        every { adressebeskyttelseDaoMock.oppdaterAdresseBeskyttelse(any(), any()) } returns 1
        every { sakService.finnSaker(fnr) } returns saker
        every {
            sakService.finnEnhetForPersonOgTema(any(), any(), any())
        } returns ArbeidsFordelingEnhet("NAV Familie- og pensjonsytelser Steinkjer", "4817")
        every { sakService.oppdaterEnhetForSaker(any()) } just runs
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

    @Test
    fun `Skal kunne sette fortrolig og sette enhet`() {
        val sakIder: Set<Long> = setOf(1, 2, 3, 4, 5, 6)
        val saker = sakIder.map {
            Sak(
                id = it,
                ident = TRIVIELL_MIDTPUNKT.value,
                sakType = SakType.BARNEPENSJON,
                enhet = Enheter.PORSGRUNN.enhetNr
            )
        }
        val fnr = "16017919184"
        val adressebeskyttelse =
            Adressebeskyttelse("1", Endringstype.OPPRETTET, fnr, AdressebeskyttelseGradering.FORTROLIG)

        coEvery { grunnlagClient.hentAlleSakIder(any()) } returns sakIder
        every { adressebeskyttelseDaoMock.oppdaterAdresseBeskyttelse(any(), any()) } returns 1
        every { sakService.finnSaker(fnr) } returns saker
        every {
            sakService.finnEnhetForPersonOgTema(any(), any(), any())
        } returns ArbeidsFordelingEnhet("NAV Familie- og pensjonsytelser Steinkjer", "4817")
        every { sakService.oppdaterEnhetForSaker(any()) } just runs
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

    @Test
    fun `skal kunne opprette hendelser som følge av feilet regulering`() {
        val sakId = 1L
        val lagretHendelse = grunnlagsendringshendelseMedSamsvar(
            type = GrunnlagsendringsType.GRUNNBELOEP,
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = "test-fnr",
            samsvarMellomKildeOgGrunnlag = null
        )

        val hendelseSomLagres = slot<Grunnlagsendringshendelse>()

        every { sakService.finnSak(sakId) } returns Sak(
            "test-fnr",
            SakType.BARNEPENSJON,
            sakId,
            Enheter.defaultEnhet.enhetNr
        )

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(capture(hendelseSomLagres))
        } returns lagretHendelse
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(sakId, any())
        } returns emptyList()

        grunnlagsendringshendelseService.opprettEndretGrunnbeloepHendelse(sakId)

        assertEquals(hendelseSomLagres.captured.type, GrunnlagsendringsType.GRUNNBELOEP)
        assertEquals(hendelseSomLagres.captured.sakId, sakId)
    }
}