package no.nav.etterlatte.grunnlagsendring

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.KONTANT_FOT
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BrukerService
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.domain.SamsvarMellomKildeOgGrunnlag
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlient
import no.nav.etterlatte.grunnlagsendringshendelseMedSamsvar
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SakidOgRolle
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.opprettNyOppgaveMedReferanseOgSak
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.mockPerson
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.sql.Connection
import java.time.LocalDate
import java.util.UUID.randomUUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnlagsendringshendelseServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val grunnlagshendelsesDao = mockk<GrunnlagsendringshendelseDao>(relaxUnitFun = true)
    private val pdlService = mockk<PdlTjenesterKlientImpl>()
    private val grunnlagKlient = mockk<GrunnlagKlient>()
    private val sakService = mockk<SakService>()
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

    private lateinit var grunnlagsendringshendelseService: GrunnlagsendringshendelseService

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

        grunnlagsendringshendelseService =
            spyk(
                GrunnlagsendringshendelseService(
                    oppgaveService,
                    grunnlagshendelsesDao,
                    behandlingService,
                    pdlService,
                    grunnlagKlient,
                    sakService,
                    brukerService,
                ),
            )
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun `Skal ignorere duplikate hendelser`() {
        val sakId = 1L

        val adresse =
            Adresse(type = AdresseType.VEGADRESSE, aktiv = true, kilde = "FREG", postnr = "2040", adresseLinje1 = "Furukollveien 189")
        val samsvarBostedAdresse =
            SamsvarMellomKildeOgGrunnlag.Adresse(
                samsvar = false,
                fraPdl = listOf(adresse),
                fraGrunnlag = null,
            )
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns
            listOf(
                grunnlagsendringshendelseMedSamsvar(
                    gjelderPerson = KONTANT_FOT.value,
                    samsvarMellomKildeOgGrunnlag = samsvarBostedAdresse,
                ).copy(
                    status = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                    type = GrunnlagsendringsType.BOSTED,
                    hendelseGjelderRolle = Saksrolle.SOESKEN,
                ),
            )

        val erDuplikat =
            grunnlagsendringshendelseService.erDuplikatHendelse(
                sakId,
                KONTANT_FOT.value,
                GrunnlagsendringsType.BOSTED,
                samsvarBostedAdresse,
            )
        assertTrue(erDuplikat)

        val erIkkeDuplikat =
            grunnlagsendringshendelseService.erDuplikatHendelse(
                sakId,
                KONTANT_FOT.value,
                GrunnlagsendringsType.BOSTED,
                samsvarBostedAdresse.copy(
                    fraPdl =
                        listOf(
                            Adresse(
                                type = AdresseType.VEGADRESSE,
                                aktiv = true,
                                kilde = "FREG",
                                postnr = "1359",
                                adresseLinje1 = "Trøgstadbåsta 612",
                            ),
                        ),
                ),
            )
        assertFalse(erIkkeDuplikat)
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
        coEvery { grunnlagKlient.hentGrunnlag(any()) } returns Grunnlag.empty()
        coEvery { grunnlagKlient.hentPersonSakOgRolle(any()) }
            .returns(
                PersonMedSakerOgRoller(
                    fnr,
                    listOf(
                        SakidOgRolle(sakId, Saksrolle.GJENLEVENDE),
                        SakidOgRolle(sakId, Saksrolle.GJENLEVENDE),
                        SakidOgRolle(sakId, Saksrolle.GJENLEVENDE),
                        SakidOgRolle(sakId, Saksrolle.SOESKEN),
                        SakidOgRolle(2L, Saksrolle.AVDOED),
                        SakidOgRolle(2L, Saksrolle.GJENLEVENDE),
                        SakidOgRolle(2L, Saksrolle.SOEKER),
                        SakidOgRolle(3L, Saksrolle.SOESKEN),
                    ),
                ),
            )

        every {
            sakService.finnSak(2L)
        } returns Sak(fnr, SakType.BARNEPENSJON, 2L, Enheter.defaultEnhet.enhetNr)
        every {
            sakService.finnSak(3L)
        } returns Sak(fnr, SakType.BARNEPENSJON, 3L, Enheter.defaultEnhet.enhetNr)

        val grunnlagsendringshendelse =
            grunnlagsendringshendelseMedSamsvar(
                id = randomUUID(),
                sakId = sakId,
                gjelderPerson = fnr,
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
    fun `skal opprette grunnlagsendringshendelser i databasen for doedshendelser - base case`() {
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
                gjelderPerson = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )

        val opprettGrunnlagsendringshendelse = slot<Grunnlagsendringshendelse>()

        coEvery { grunnlagKlient.hentGrunnlag(sakId) } returns Grunnlag.empty()
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

        coEvery { grunnlagKlient.hentPersonSakOgRolle(any()) }
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
            { assertEquals(grunnlagsendringshendelse, lagredeGrunnlagsendringshendelser.first()) },
        )
    }

    @Test
    fun `skal ikke opprette ny doedshendelse dersom en lignende allerede eksisterer - ny hendelse på x med lik info, har behandling`() {
        val sakId = 1L
        val fnr = KONTANT_FOT.value
        val doedsdato = LocalDate.of(2022, 7, 8)
        val doedsfallhendelse =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.DOEDSFALL,
                id = randomUUID(),
                sakId = sakId,
                gjelderPerson = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )

        every { sakService.finnSak(sakId) } returns Sak(KONTANT_FOT.value, SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr)

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any())
        } returns doedsfallhendelse
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList() andThen
            listOf(
                doedsfallhendelse,
            )
        coEvery {
            grunnlagKlient.hentPersonSakOgRolle(any())
        } returns PersonMedSakerOgRoller(fnr, listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)))
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave

        every { behandlingService.hentBehandlingerForSak(sakId) } returns
            listOf(
                foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.BEREGNET),
            )
        // Så hendelse ikke blir forkastet i oppdaterHendelseSjekket og da er ikke grunnlag null

        every { pdlService.hentPdlModell(any(), any(), any()) } returns
            mockPerson()
                .copy(doedsdato = OpplysningDTO(doedsdato, "doedsdato"))

        coEvery { grunnlagKlient.hentGrunnlag(sakId) } returns Grunnlag.empty()
        val lagredeGrunnlagsendringshendelser1 =
            grunnlagsendringshendelseService.opprettDoedshendelse(
                Doedshendelse(
                    hendelseId = "1",
                    fnr = fnr,
                    doedsdato = doedsdato,
                    endringstype = Endringstype.OPPRETTET,
                ),
            )
        verify(exactly = 1) { grunnlagsendringshendelseService.oppdaterHendelseSjekket(any(), any()) }
        verify(exactly = 0) { grunnlagsendringshendelseService.forkastHendelse(any(), any()) }

        assertEquals(listOf(doedsfallhendelse), lagredeGrunnlagsendringshendelser1)

        mockkStatic(Grunnlag::doedsdato)
        val grunnlagMock = mockk<Grunnlag>()
        coEvery { grunnlagKlient.hentGrunnlag(sakId) } returns grunnlagMock
        with(grunnlagMock) {
            every { doedsdato(any(), any()) } returns Opplysning.Konstant(randomUUID(), kilde, doedsdato)
        }

        grunnlagsendringshendelseService.opprettDoedshendelse(
            Doedshendelse(
                hendelseId = "1",
                fnr = fnr,
                doedsdato = doedsdato,
                endringstype = Endringstype.ANNULLERT,
            ),
        )

        verify(exactly = 1) { grunnlagsendringshendelseService.forkastHendelse(any(), any()) }
    }

    @Test
    fun `skal opprette ny doedshendelse med ny info dersom en allerede eksisterer - ny hendelse på x med ulik info, har behandling`() {
        val sakId = 1L
        val fnr = KONTANT_FOT.value
        val doedsdato = LocalDate.of(2022, 7, 8)
        val doedsfallhendelse =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.DOEDSFALL,
                id = randomUUID(),
                sakId = sakId,
                gjelderPerson = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )

        every { sakService.finnSak(sakId) } returns Sak(KONTANT_FOT.value, SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr)

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any())
        } returns doedsfallhendelse
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList() andThen
            listOf(
                doedsfallhendelse,
            )
        coEvery {
            grunnlagKlient.hentPersonSakOgRolle(any())
        } returns PersonMedSakerOgRoller(fnr, listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)))
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave

        every { behandlingService.hentBehandlingerForSak(sakId) } returns
            listOf(
                foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.BEREGNET),
            )
        // Så hendelse ikke blir forkastet i oppdaterHendelseSjekket og da er ikke grunnlag null

        every { pdlService.hentPdlModell(any(), any(), any()) } returns
            mockPerson()
                .copy(doedsdato = OpplysningDTO(doedsdato, "doedsdato"))

        coEvery { grunnlagKlient.hentGrunnlag(sakId) } returns Grunnlag.empty()
        val lagredeGrunnlagsendringshendelser1 =
            grunnlagsendringshendelseService.opprettDoedshendelse(
                Doedshendelse(
                    hendelseId = "1",
                    fnr = fnr,
                    doedsdato = doedsdato,
                    endringstype = Endringstype.OPPRETTET,
                ),
            )
        verify(exactly = 1) { grunnlagsendringshendelseService.oppdaterHendelseSjekket(any(), any()) }
        verify(exactly = 0) { grunnlagsendringshendelseService.forkastHendelse(any(), any()) }

        assertEquals(listOf(doedsfallhendelse), lagredeGrunnlagsendringshendelser1)

        val nyDoedsdato = LocalDate.of(2022, 8, 8)
        mockkStatic(Grunnlag::doedsdato)
        val grunnlagMock = mockk<Grunnlag>()
        coEvery { grunnlagKlient.hentGrunnlag(sakId) } returns grunnlagMock
        with(grunnlagMock) {
            every { doedsdato(any(), any()) } returns Opplysning.Konstant(randomUUID(), kilde, nyDoedsdato)
        }

        grunnlagsendringshendelseService.opprettDoedshendelse(
            Doedshendelse(
                hendelseId = "1",
                fnr = fnr,
                doedsdato = nyDoedsdato,
                endringstype = Endringstype.ANNULLERT,
            ),
        )

        verify(exactly = 0) { grunnlagsendringshendelseService.forkastHendelse(any(), any()) }
        verify(exactly = 2) { grunnlagsendringshendelseService.oppdaterHendelseSjekket(any(), any()) }
    }

    @Test
    fun `skal ikke opprette ny doedshendelse da man ikke har gyldige behandlinger`() {
        val sakId = 1L
        val fnr = KONTANT_FOT.value
        val doedsdato = LocalDate.of(2022, 7, 8)
        val doedsfallhendelse =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.DOEDSFALL,
                id = randomUUID(),
                sakId = sakId,
                gjelderPerson = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )

        every { sakService.finnSak(sakId) } returns Sak(KONTANT_FOT.value, SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr)

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any())
        } returns doedsfallhendelse
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList() andThen
            listOf(
                doedsfallhendelse,
            )
        coEvery {
            grunnlagKlient.hentPersonSakOgRolle(any())
        } returns PersonMedSakerOgRoller(fnr, listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)))
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave

        every { behandlingService.hentBehandlingerForSak(sakId) } returns emptyList()
        // Så hendelse ikke blir forkastet i oppdaterHendelseSjekket

        every { pdlService.hentPdlModell(any(), any(), any()) } returns
            mockPerson()
                .copy(doedsdato = OpplysningDTO(doedsdato, "doedsdato"))

        coEvery { grunnlagKlient.hentGrunnlag(sakId) } returns Grunnlag.empty()

        grunnlagsendringshendelseService.opprettDoedshendelse(
            Doedshendelse(
                hendelseId = "1",
                fnr = fnr,
                doedsdato = doedsdato,
                endringstype = Endringstype.OPPRETTET,
            ),
        )
        verify(exactly = 1) { grunnlagsendringshendelseService.oppdaterHendelseSjekket(any(), any()) }
        verify(exactly = 1) { grunnlagsendringshendelseService.forkastHendelse(any(), any()) }
    }

    @Test
    fun `skal kun opprette ny doedshendelse for saker som finnes med gyldig behandling`() {
        val sakId = 1L
        val fnr = KONTANT_FOT.value
        val doedsdato = LocalDate.of(2022, 7, 8)
        val grunnlagsendringshendelse1 =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.DOEDSFALL,
                id = randomUUID(),
                sakId = sakId,
                gjelderPerson = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )

        val grunnlagsendringshendelse2 =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.UTFLYTTING,
                id = randomUUID(),
                sakId = sakId,
                gjelderPerson = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )

        every { sakService.finnSak(sakId) } returns Sak(KONTANT_FOT.value, SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr)

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
            grunnlagKlient.hentPersonSakOgRolle(any())
        } returns PersonMedSakerOgRoller(fnr, listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)))
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave

        every { behandlingService.hentBehandlingerForSak(sakId) } returns
            listOf(
                foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.BEREGNET),
            )
        // Så hendelse ikke blir forkastet i oppdaterHendelseSjekket

        every { pdlService.hentPdlModell(any(), any(), any()) } returns
            mockPerson()
                .copy(doedsdato = OpplysningDTO(doedsdato, "doedsdato"))

        coEvery { grunnlagKlient.hentGrunnlag(sakId) } returns Grunnlag.empty()

        grunnlagsendringshendelseService.opprettDoedshendelse(
            Doedshendelse(
                hendelseId = "1",
                fnr = fnr,
                doedsdato = doedsdato,
                endringstype = Endringstype.OPPRETTET,
            ),
        )
        verify(exactly = 1) { grunnlagsendringshendelseService.oppdaterHendelseSjekket(any(), any()) }
        verify(exactly = 0) { grunnlagsendringshendelseService.forkastHendelse(any(), any()) }
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

        coEvery { grunnlagKlient.hentAlleSakIder(any()) } returns sakIder
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

        coVerify(exactly = 1) { grunnlagKlient.hentAlleSakIder(adressebeskyttelse.fnr) }

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

        coEvery { grunnlagKlient.hentAlleSakIder(any()) } returns sakIder
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

        coVerify(exactly = 1) { grunnlagKlient.hentAlleSakIder(adressebeskyttelse.fnr) }

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
                gjelderPerson = KONTANT_FOT.value,
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

        coEvery { grunnlagKlient.hentGrunnlag(sakId) } returns Grunnlag.empty()

        grunnlagsendringshendelseService.opprettEndretGrunnbeloepHendelse(sakId)

        assertEquals(hendelseSomLagres.captured.type, GrunnlagsendringsType.GRUNNBELOEP)
        assertEquals(hendelseSomLagres.captured.sakId, sakId)
    }
}
