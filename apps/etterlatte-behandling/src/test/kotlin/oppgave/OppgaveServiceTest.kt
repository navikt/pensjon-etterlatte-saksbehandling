package no.nav.etterlatte.oppgave

import com.nimbusds.jwt.JWTClaimsSet
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.SystemUser
import no.nav.etterlatte.User
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.oppgave.VedtakOppgaveDTO
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakTilgangDao
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Saksbehandler
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OppgaveServiceTest {
    private lateinit var dataSource: DataSource
    private lateinit var sakDao: SakDao
    private lateinit var oppgaveDao: OppgaveDao
    private lateinit var oppgaveService: OppgaveService
    private lateinit var saktilgangDao: SakTilgangDao
    private lateinit var oppgaveDaoMedEndringssporing: OppgaveDaoMedEndringssporing
    private lateinit var featureToggleService: FeatureToggleService
    private val saksbehandlerRolleDev = "8bb9b8d1-f46a-4ade-8ee8-5895eccdf8cf"
    private val strengtfortroligDev = "5ef775f2-61f8-4283-bf3d-8d03f428aa14"
    private val attestantRolleDev = "63f46f74-84a8-4d1c-87a8-78532ab3ae60"
    private val saksbehandler = mockk<SaksbehandlerMedEnheterOgRoller>()

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource =
            DataSourceBuilder.createDataSource(
                jdbcUrl = postgreSQLContainer.jdbcUrl,
                username = postgreSQLContainer.username,
                password = postgreSQLContainer.password,
            ).apply { migrate() }

        val connection = dataSource.connection
        featureToggleService =
            DummyFeatureToggleService().also {
                it.settBryter(OppgaveServiceFeatureToggle.EnhetFilterOppgaver, true)
            }
        sakDao = SakDao { connection }
        oppgaveDao = OppgaveDaoImpl { connection }
        oppgaveDaoMedEndringssporing = OppgaveDaoMedEndringssporingImpl(oppgaveDao) { connection }
        oppgaveService = OppgaveService(oppgaveDaoMedEndringssporing, sakDao, featureToggleService)
        saktilgangDao = SakTilgangDao(dataSource)
    }

    val azureGroupToGroupIDMap =
        mapOf(
            AzureGroup.SAKSBEHANDLER to saksbehandlerRolleDev,
            AzureGroup.ATTESTANT to attestantRolleDev,
            AzureGroup.STRENGT_FORTROLIG to strengtfortroligDev,
        )

    private fun generateSaksbehandlerMedRoller(azureGroup: AzureGroup): SaksbehandlerMedRoller {
        val groupId = azureGroupToGroupIDMap[azureGroup]!!
        val jwtclaimsSaksbehandler = JWTClaimsSet.Builder().claim("groups", groupId).build()
        return SaksbehandlerMedRoller(
            Saksbehandler("", azureGroup.name, JwtTokenClaims(jwtclaimsSaksbehandler)),
            mapOf(azureGroup to groupId),
        )
    }

    private fun setNewKontekstWithMockUser(userMock: User) {
        Kontekst.set(
            Context(
                userMock,
                object : DatabaseKontekst {
                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(
                        gjenbruk: Boolean,
                        block: () -> T,
                    ): T {
                        return block()
                    }
                },
            ),
        )
    }

    private fun mockForSaksbehandlerMedRoller(
        userSaksbehandler: SaksbehandlerMedEnheterOgRoller,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ) {
        every { userSaksbehandler.saksbehandlerMedRoller } returns saksbehandlerMedRoller
    }

    @BeforeEach
    fun beforeEach() {
        val saksbehandlerRoller = generateSaksbehandlerMedRoller(AzureGroup.SAKSBEHANDLER)
        every { saksbehandler.enheter() } returns Enheter.nasjonalTilgangEnheter()

        setNewKontekstWithMockUser(saksbehandler)

        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerRoller
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE oppgave CASCADE;").execute()
        }
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `skal kunne tildele oppgave uten saksbehandler`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveService.tildelSaksbehandler(nyOppgave.id, nysaksbehandler)

        val oppgaveMedNySaksbehandler = oppgaveService.hentOppgave(nyOppgave.id)
        Assertions.assertEquals(nysaksbehandler, oppgaveMedNySaksbehandler?.saksbehandler)
    }

    @Test
    fun `skal tildele attesteringsoppgave hvis systembruker og fatte`() {
        val systemBruker = mockk<SystemUser>()
        setNewKontekstWithMockUser(systemBruker)

        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val systembruker = "systembruker"
        val systembrukerTokenInfo = BrukerTokenInfo.of("a", systembruker, "c", "c", null)
        oppgaveService.tildelSaksbehandler(nyOppgave.id, systembruker)

        val vedtakOppgaveDTO =
            oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                VedtakOppgaveDTO(opprettetSak.id, referanse),
                OppgaveType.ATTESTERING,
                null,
                systembrukerTokenInfo,
            )

        oppgaveService.tildelSaksbehandler(vedtakOppgaveDTO.id, systembruker)
        val systembrukerOppgave = oppgaveService.hentOppgave(vedtakOppgaveDTO.id)
        Assertions.assertEquals(systembruker, systembrukerOppgave?.saksbehandler!!)
    }

    @Test
    fun `skal tildele attesteringsoppgave hvis rolle attestering finnes`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val vanligSaksbehandler = saksbehandler.saksbehandlerMedRoller.saksbehandler
        oppgaveService.tildelSaksbehandler(nyOppgave.id, vanligSaksbehandler.ident)

        val vedtakOppgaveDTO =
            oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                VedtakOppgaveDTO(opprettetSak.id, referanse),
                OppgaveType.ATTESTERING,
                null,
                vanligSaksbehandler,
            )

        val attestantSaksbehandler = mockk<SaksbehandlerMedEnheterOgRoller>()
        setNewKontekstWithMockUser(attestantSaksbehandler)
        val attestantmedRoller = generateSaksbehandlerMedRoller(AzureGroup.ATTESTANT)
        mockForSaksbehandlerMedRoller(attestantSaksbehandler, attestantmedRoller)

        oppgaveService.tildelSaksbehandler(vedtakOppgaveDTO.id, attestantmedRoller.saksbehandler.ident)
        val attestantTildeltOppgave = oppgaveService.hentOppgave(vedtakOppgaveDTO.id)
        Assertions.assertEquals(attestantmedRoller.saksbehandler.ident, attestantTildeltOppgave?.saksbehandler!!)
    }

    @Test
    fun `skal ikke tildele attesteringsoppgave hvis rolle saksbehandler`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val vanligSaksbehandler = saksbehandler.saksbehandlerMedRoller.saksbehandler
        oppgaveService.tildelSaksbehandler(nyOppgave.id, vanligSaksbehandler.ident)

        val vedtakOppgaveDTO =
            oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                VedtakOppgaveDTO(opprettetSak.id, referanse),
                OppgaveType.ATTESTERING,
                null,
                vanligSaksbehandler,
            )

        val saksbehandlerto = mockk<SaksbehandlerMedEnheterOgRoller>()
        setNewKontekstWithMockUser(saksbehandlerto)
        val saksbehandlerMedRoller = generateSaksbehandlerMedRoller(AzureGroup.SAKSBEHANDLER)
        mockForSaksbehandlerMedRoller(saksbehandlerto, saksbehandlerMedRoller)

        assertThrows<BrukerManglerAttestantRolleException> {
            oppgaveService.tildelSaksbehandler(vedtakOppgaveDTO.id, saksbehandlerMedRoller.saksbehandler.ident)
        }
    }

    @Test
    fun `skal ikke kunne tildele oppgave med saksbehandler felt satt`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveService.tildelSaksbehandler(nyOppgave.id, nysaksbehandler)
        val err =
            assertThrows<BadRequestException> {
                oppgaveService.tildelSaksbehandler(nyOppgave.id, "enda en")
            }
        Assertions.assertTrue(err.message!!.startsWith("Oppgaven har allerede en saksbehandler"))
    }

    @Test
    fun `skal ikke kunne tildele hvis oppgaven ikke finnes`() {
        val nysaksbehandler = "nysaksbehandler"
        val err =
            assertThrows<NotFoundException> {
                oppgaveService.tildelSaksbehandler(UUID.randomUUID(), nysaksbehandler)
            }
        Assertions.assertTrue(err.message!!.startsWith("Oppgaven finnes ikke"))
    }

    @Test
    fun `skal ikke kunne tildele en lukket oppgave`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        oppgaveDao.endreStatusPaaOppgave(nyOppgave.id, Status.FERDIGSTILT)
        val nysaksbehandler = "nysaksbehandler"
        assertThrows<IllegalStateException> {
            oppgaveService.tildelSaksbehandler(nyOppgave.id, nysaksbehandler)
        }
    }

    @Test
    fun `avbrytAapneOppgaverForBehandling setter alle åpne oppgaver for behandling til avbrutt`() {
        val sak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingsId = UUID.randomUUID().toString()
        val oppgaveBehandling =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse = behandlingsId,
                sakId = sak.id,
                oppgaveKilde = OppgaveKilde.BEHANDLING,
                oppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = null,
            )
        val oppgaveAttestering =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse = behandlingsId,
                sakId = sak.id,
                oppgaveKilde = OppgaveKilde.BEHANDLING,
                oppgaveType = OppgaveType.ATTESTERING,
                merknad = null,
            )
        oppgaveService.tildelSaksbehandler(oppgaveBehandling.id, "saksbehandler")
        oppgaveService.avbrytAapneOppgaverForBehandling(behandlingsId)
        val oppgaveBehandlingEtterAvbryt = oppgaveService.hentOppgave(oppgaveBehandling.id)
        val oppgaveAttesteringEtterAvbryt = oppgaveService.hentOppgave(oppgaveAttestering.id)

        Assertions.assertEquals(Status.AVBRUTT, oppgaveBehandlingEtterAvbryt?.status)
        Assertions.assertEquals(Status.AVBRUTT, oppgaveAttesteringEtterAvbryt?.status)
    }

    @Test
    fun `avbrytAapneOppgaverForBehandling endrer ikke avsluttede oppgaver eller oppgaver til andre behandlinger`() {
        val sak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID().toString()
        val annenBehandlingId = UUID.randomUUID().toString()
        val saksbehandler = Saksbehandler("", "saksbehandler", null)

        val oppgaveFerdigstilt =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse = behandlingId,
                sakId = sak.id,
                oppgaveKilde = OppgaveKilde.BEHANDLING,
                oppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = null,
            )
        oppgaveService.tildelSaksbehandler(oppgaveFerdigstilt.id, saksbehandler.ident)
        oppgaveService.ferdigStillOppgaveUnderBehandling(behandlingId, saksbehandler)

        val annenbehandlingfoerstegangs =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse = annenBehandlingId,
                sakId = sak.id,
                oppgaveKilde = OppgaveKilde.BEHANDLING,
                oppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = null,
            )
        val saksbehandlerforstegangs = Saksbehandler("", "forstegangssaksbehandler", null)
        oppgaveService.tildelSaksbehandler(annenbehandlingfoerstegangs.id, saksbehandlerforstegangs.ident)
        oppgaveService.ferdigStillOppgaveUnderBehandling(annenBehandlingId, saksbehandlerforstegangs)
        val oppgaveUnderBehandlingAnnenBehandling =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse = annenBehandlingId,
                sakId = sak.id,
                oppgaveKilde = OppgaveKilde.BEHANDLING,
                oppgaveType = OppgaveType.ATTESTERING,
                merknad = null,
            )

        val attestantmock = mockk<SaksbehandlerMedEnheterOgRoller>()
        setNewKontekstWithMockUser(attestantmock)
        mockForSaksbehandlerMedRoller(attestantmock, generateSaksbehandlerMedRoller(AzureGroup.ATTESTANT))
        oppgaveService.tildelSaksbehandler(oppgaveUnderBehandlingAnnenBehandling.id, saksbehandler.ident)
        oppgaveService.avbrytAapneOppgaverForBehandling(behandlingId)

        val oppgaveFerdigstiltEtterAvbryt = oppgaveService.hentOppgave(oppgaveFerdigstilt.id)
        val oppgaveUnderBehandlingEtterAvbryt = oppgaveService.hentOppgave(oppgaveUnderBehandlingAnnenBehandling.id)
        Assertions.assertEquals(Status.FERDIGSTILT, oppgaveFerdigstiltEtterAvbryt?.status)
        Assertions.assertEquals(Status.UNDER_BEHANDLING, oppgaveUnderBehandlingEtterAvbryt?.status)
    }

    @Test
    fun `skal kunne bytte oppgave med saksbehandler`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveService.byttSaksbehandler(nyOppgave.id, nysaksbehandler)

        val oppgaveMedNySaksbehandler = oppgaveService.hentOppgave(nyOppgave.id)
        Assertions.assertEquals(nysaksbehandler, oppgaveMedNySaksbehandler?.saksbehandler)
    }

    @Test
    fun `skal ikke kunne bytte saksbehandler på lukket oppgave`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        oppgaveDao.endreStatusPaaOppgave(nyOppgave.id, Status.FERDIGSTILT)
        val nysaksbehandler = "nysaksbehandler"
        assertThrows<IllegalStateException> {
            oppgaveService.byttSaksbehandler(nyOppgave.id, nysaksbehandler)
        }
        val oppgaveMedNySaksbehandler = oppgaveService.hentOppgave(nyOppgave.id)
        Assertions.assertEquals(nyOppgave.saksbehandler, oppgaveMedNySaksbehandler?.saksbehandler)
    }

    @Test
    fun `skal ikke kunne bytte saksbehandler på en ikke eksisterende sak`() {
        val nysaksbehandler = "nysaksbehandler"
        val err =
            assertThrows<NotFoundException> {
                oppgaveService.byttSaksbehandler(UUID.randomUUID(), nysaksbehandler)
            }
        Assertions.assertTrue(err.message!!.startsWith("Oppgaven finnes ikke"))
    }

    @Test
    fun `skal kunne fjerne saksbehandler fra oppgave`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveService.tildelSaksbehandler(nyOppgave.id, nysaksbehandler)
        oppgaveService.fjernSaksbehandler(nyOppgave.id)
        val oppgaveUtenSaksbehandler = oppgaveService.hentOppgave(nyOppgave.id)
        Assertions.assertNotNull(oppgaveUtenSaksbehandler?.id)
        Assertions.assertNull(oppgaveUtenSaksbehandler?.saksbehandler)
        Assertions.assertEquals(Status.NY, oppgaveUtenSaksbehandler?.status)
    }

    @Test
    fun `kan ikke fjerne saksbehandler hvis det ikke er satt på oppgaven`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val err =
            assertThrows<BadRequestException> {
                oppgaveService.fjernSaksbehandler(nyOppgave.id)
            }
        Assertions.assertTrue(err.message!!.startsWith("Oppgaven har ingen saksbehandler"))
    }

    @Test
    fun `skal ikke kunne fjerne saksbehandler på en lukket oppgave`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val saksbehandler = "saksbehandler"
        oppgaveService.tildelSaksbehandler(nyOppgave.id, saksbehandler)
        oppgaveDao.endreStatusPaaOppgave(nyOppgave.id, Status.FERDIGSTILT)
        assertThrows<IllegalStateException> {
            oppgaveService.fjernSaksbehandler(nyOppgave.id)
        }
        val lagretOppgave = oppgaveService.hentOppgave(nyOppgave.id)

        Assertions.assertEquals(lagretOppgave?.saksbehandler, saksbehandler)
    }

    @Test
    fun `kan redigere frist`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        oppgaveService.tildelSaksbehandler(nyOppgave.id, "nysaksbehandler")
        val nyFrist = Tidspunkt.now().toLocalDatetimeUTC().plusMonths(4L).toTidspunkt()
        oppgaveService.redigerFrist(nyOppgave.id, nyFrist)
        val oppgaveMedNyFrist = oppgaveService.hentOppgave(nyOppgave.id)
        Assertions.assertEquals(nyFrist, oppgaveMedNyFrist?.frist)
    }

    @Test
    fun `kan ikke redigere frist tilbake i tid`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        oppgaveService.tildelSaksbehandler(nyOppgave.id, "nysaksbehandler")
        val nyFrist = Tidspunkt.now().toLocalDatetimeUTC().minusMonths(1L).toTidspunkt()

        val err =
            assertThrows<BadRequestException> {
                oppgaveService.redigerFrist(nyOppgave.id, nyFrist)
            }

        Assertions.assertTrue(err.message!!.startsWith("Tidspunkt tilbake i tid id: "))
    }

    @Test
    fun `kan ikke redigere frist på en lukket oppgave`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        oppgaveDao.endreStatusPaaOppgave(nyOppgave.id, Status.FERDIGSTILT)
        assertThrows<IllegalStateException> {
            oppgaveService.redigerFrist(
                oppgaveId = nyOppgave.id,
                frist = Tidspunkt.now().toLocalDatetimeUTC().plusMonths(1L).toTidspunkt(),
            )
        }
        val lagretOppgave = oppgaveService.hentOppgave(nyOppgave.id)
        Assertions.assertEquals(nyOppgave.frist, lagretOppgave?.frist)
    }

    @Test
    fun `Håndtering av vedtaksfatting`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val saksbehandler1 = Saksbehandler("", "saksbehandler", null)
        oppgaveService.tildelSaksbehandler(nyOppgave.id, saksbehandler1.ident)

        val vedtakOppgaveDTO =
            oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                VedtakOppgaveDTO(opprettetSak.id, referanse),
                OppgaveType.ATTESTERING,
                null,
                saksbehandler1,
            )

        val saksbehandlerOppgave = oppgaveService.hentOppgave(nyOppgave.id)
        Assertions.assertEquals(Status.FERDIGSTILT, saksbehandlerOppgave?.status)
        Assertions.assertEquals(OppgaveType.ATTESTERING, vedtakOppgaveDTO.type)
        Assertions.assertEquals(referanse, vedtakOppgaveDTO.referanse)
    }

    @Test
    fun `Kan ikke lukke oppgave hvis man ikke eier oppgaven under behandling`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val saksbehandler1 = "saksbehandler"
        oppgaveService.tildelSaksbehandler(nyOppgave.id, saksbehandler1)
        assertThrows<OppgaveService.FeilSaksbehandlerPaaOppgaveException> {
            oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                VedtakOppgaveDTO(opprettetSak.id, referanse),
                OppgaveType.ATTESTERING,
                null,
                Saksbehandler("", "Feilsaksbehandler", null),
            )
        }
    }

    @Test
    fun `Skal ikke kunne attestere vedtak hvis ingen oppgaver er under behandling altså tildelt en saksbehandler`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            referanse,
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )

        val err =
            assertThrows<BadRequestException> {
                oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                    VedtakOppgaveDTO(opprettetSak.id, referanse),
                    OppgaveType.ATTESTERING,
                    null,
                    Saksbehandler("", "saksbehandler", null),
                )
            }

        Assertions.assertTrue(
            err.message!!.startsWith("Det må finnes en oppgave under behandling, gjelder behandling:"),
        )
    }

    @Test
    fun `kan ikke attestere uten at det finnes en oppgave på behandlingen`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"

        val err =
            assertThrows<BadRequestException> {
                oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                    VedtakOppgaveDTO(opprettetSak.id, referanse),
                    OppgaveType.ATTESTERING,
                    null,
                    Saksbehandler("", "saksbehandler", null),
                )
            }

        Assertions.assertEquals(
            "Må ha en oppgave for å kunne lage attesteringsoppgave",
            err.message!!,
        )
    }

    @Test
    fun `Skal ikke kunne attestere vedtak hvis det finnes flere oppgaver under behandling for behandlingen`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val oppgaveEn =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val oppgaveTo =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val saksbehandler1 = Saksbehandler("", "saksbehandler", null)
        oppgaveService.tildelSaksbehandler(oppgaveEn.id, saksbehandler1.ident)
        oppgaveService.tildelSaksbehandler(oppgaveTo.id, saksbehandler1.ident)

        val err =
            assertThrows<BadRequestException> {
                oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                    VedtakOppgaveDTO(opprettetSak.id, referanse),
                    OppgaveType.ATTESTERING,
                    null,
                    saksbehandler1,
                )
            }

        Assertions.assertTrue(
            err.message!!.startsWith("Skal kun ha en oppgave under behandling, gjelder behandling:"),
        )
    }

    @Test
    fun `kan ikke fjerne saksbehandler hvis oppgaven ikke finnes`() {
        val err =
            assertThrows<NotFoundException> {
                oppgaveService.fjernSaksbehandler(UUID.randomUUID())
            }
        Assertions.assertTrue(err.message!!.startsWith("Oppgaven finnes ikke"))
    }

    @Test
    fun `Skal kun få saker som ikke er adressebeskyttet tilbake hvis saksbehandler ikke har spesialroller`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val adressebeskyttetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            adressebeskyttetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )

        saktilgangDao.oppdaterAdresseBeskyttelse(adressebeskyttetSak.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)

        val saksbehandlerRoller = generateSaksbehandlerMedRoller(AzureGroup.SAKSBEHANDLER)

        val oppgaver = oppgaveService.finnOppgaverForBruker(saksbehandlerRoller)
        Assertions.assertEquals(1, oppgaver.size)
        val oppgaveUtenbeskyttelse = oppgaver[0]
        Assertions.assertEquals(nyOppgave.id, oppgaveUtenbeskyttelse.id)
        Assertions.assertEquals(nyOppgave.sakId, opprettetSak.id)
    }

    @Test
    fun `Skal kunne endre enhet for oppgaver tilknyttet sak`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            referanse = "referanse",
            sakId = opprettetSak.id,
            oppgaveKilde = OppgaveKilde.BEHANDLING,
            oppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
            merknad = null,
        )

        val saksbehandlerMedRoller = generateSaksbehandlerMedRoller(AzureGroup.SAKSBEHANDLER)
        val oppgaverUtenEndring = oppgaveService.finnOppgaverForBruker(saksbehandlerMedRoller)
        Assertions.assertEquals(1, oppgaverUtenEndring.size)
        Assertions.assertEquals(Enheter.AALESUND.enhetNr, oppgaverUtenEndring[0].enhet)

        oppgaveService.endreEnhetForOppgaverTilknyttetSak(opprettetSak.id, Enheter.STEINKJER.enhetNr)
        val oppgaverMedEndring = oppgaveService.finnOppgaverForBruker(saksbehandlerMedRoller)
        Assertions.assertEquals(1, oppgaverMedEndring.size)
        Assertions.assertEquals(Enheter.STEINKJER.enhetNr, oppgaverMedEndring[0].enhet)
    }

    @Test
    fun `Skal kun få saker som  er strengt fotrolig tilbake hvis saksbehandler har rolle strengt fortrolig`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )

        val adressebeskyttetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val adressebeskyttetOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                adressebeskyttetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        saktilgangDao.oppdaterAdresseBeskyttelse(adressebeskyttetSak.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
        val saksbehandlerMedRollerStrengtFortrolig = generateSaksbehandlerMedRoller(AzureGroup.STRENGT_FORTROLIG)
        val oppgaver = oppgaveService.finnOppgaverForBruker(saksbehandlerMedRollerStrengtFortrolig)
        Assertions.assertEquals(1, oppgaver.size)
        val strengtFortroligOppgave = oppgaver[0]
        Assertions.assertEquals(adressebeskyttetOppgave.id, strengtFortroligOppgave.id)
        Assertions.assertEquals(adressebeskyttetOppgave.sakId, adressebeskyttetSak.id)
    }

    @Test
    fun `saksbehandler med attestant rolle skal få attestant oppgaver`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )

        val attestantSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val attestantOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                attestantSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.ATTESTERING,
                null,
            )

        val saksbehandlerMedRollerAttestant = generateSaksbehandlerMedRoller(AzureGroup.ATTESTANT)
        val oppgaver = oppgaveService.finnOppgaverForBruker(saksbehandlerMedRollerAttestant)
        Assertions.assertEquals(1, oppgaver.size)
        val strengtFortroligOppgave = oppgaver[0]
        Assertions.assertEquals(attestantOppgave.id, strengtFortroligOppgave.id)
        Assertions.assertEquals(attestantOppgave.sakId, attestantSak.id)
    }

    @Test
    fun `skal tracke at en tildeling av saksbehandler blir lagret med oppgaveendringer`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveService.tildelSaksbehandler(nyOppgave.id, nysaksbehandler)

        val oppgaveMedNySaksbehandler = oppgaveService.hentOppgave(nyOppgave.id)
        Assertions.assertEquals(nysaksbehandler, oppgaveMedNySaksbehandler?.saksbehandler)

        val hentEndringerForOppgave = oppgaveDaoMedEndringssporing.hentEndringerForOppgave(nyOppgave.id)
        Assertions.assertEquals(1, hentEndringerForOppgave.size)
        val endringPaaOppgave = hentEndringerForOppgave[0]
        Assertions.assertNull(endringPaaOppgave.oppgaveFoer.saksbehandler)
        Assertions.assertEquals("nysaksbehandler", endringPaaOppgave.oppgaveEtter.saksbehandler)
        Assertions.assertEquals(Status.NY, endringPaaOppgave.oppgaveFoer.status)
        Assertions.assertEquals(Status.UNDER_BEHANDLING, endringPaaOppgave.oppgaveEtter.status)
    }

    @Test
    fun `skal ferdigstille en oppgave hivs det finnes kun en som er under behandling`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingsref = UUID.randomUUID().toString()
        val oppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                behandlingsref,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val saksbehandler1 = Saksbehandler("", "saksbehandler01", null)
        oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandler1.ident)
        oppgaveService.ferdigStillOppgaveUnderBehandling(behandlingsref, saksbehandler1)
        val ferdigstiltOppgave = oppgaveService.hentOppgave(oppgave.id)
        Assertions.assertEquals(Status.FERDIGSTILT, ferdigstiltOppgave?.status)
    }

    @Test
    fun `Kan ikke ferdigstille oppgave under behandling om man ikke eier den`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingsref = UUID.randomUUID().toString()
        val oppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                behandlingsref,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val saksbehandler1 = "saksbehandler01"
        oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandler1)
        assertThrows<OppgaveService.FeilSaksbehandlerPaaOppgaveException> {
            oppgaveService.ferdigStillOppgaveUnderBehandling(
                behandlingsref,
                Saksbehandler("", "feilSaksbehandler", null),
            )
        }
    }

    @Test
    fun `skal lukke nye ikke ferdige eller feilregistrerte oppgaver hvis ny søknad kommer inn`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingsref = UUID.randomUUID().toString()
        val oppgaveSomSkalBliAvbrutt =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                behandlingsref,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        oppgaveService.tildelSaksbehandler(oppgaveSomSkalBliAvbrutt.id, "saksbehandler01")

        oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(behandlingsref, opprettetSak.id)

        val alleOppgaver = oppgaveDao.hentOppgaverForBehandling(behandlingsref)
        Assertions.assertEquals(2, alleOppgaver.size)
        val avbruttOppgave = oppgaveDao.hentOppgave(oppgaveSomSkalBliAvbrutt.id)!!
        Assertions.assertEquals(avbruttOppgave.status, Status.AVBRUTT)
    }

    @Test
    fun `Skal filtrere bort oppgaver med annen enhet`() {
        every { saksbehandler.enheter() } returns listOf(Enheter.AALESUND.enhetNr)
        Kontekst.set(
            Context(
                saksbehandler,
                object : DatabaseKontekst {
                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(
                        gjenbruk: Boolean,
                        block: () -> T,
                    ): T {
                        return block()
                    }
                },
            ),
        )

        val aalesundSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingsref = UUID.randomUUID().toString()
        val oppgaveAalesund =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                behandlingsref,
                aalesundSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val saksbehandlerid = "saksbehandler01"
        oppgaveService.tildelSaksbehandler(oppgaveAalesund.id, saksbehandlerid)

        val saksteinskjer = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.STEINKJER.enhetNr)
        val behrefsteinkjer = UUID.randomUUID().toString()
        val oppgavesteinskjer =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                behrefsteinkjer,
                saksteinskjer.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        oppgaveService.tildelSaksbehandler(oppgavesteinskjer.id, saksbehandlerid)

        val saksbehandlerMedRoller = generateSaksbehandlerMedRoller(AzureGroup.SAKSBEHANDLER)
        val finnOppgaverForBruker = oppgaveService.finnOppgaverForBruker(saksbehandlerMedRoller)

        Assertions.assertEquals(1, finnOppgaverForBruker.size)
        val aalesundfunnetOppgave = finnOppgaverForBruker[0]
        Assertions.assertEquals(Enheter.AALESUND.enhetNr, aalesundfunnetOppgave.enhet)
    }

    @Test
    fun `kan hente saksbehandler på en oppgave tilknyttet behandling som er under arbeid`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID().toString()
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                behandlingId,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val saksbehandler = "saksbehandler"

        oppgaveService.tildelSaksbehandler(nyOppgave.id, saksbehandler)

        val saksbehandlerHentet =
            oppgaveService.hentSaksbehandlerForBehandling(UUID.fromString(behandlingId))

        Assertions.assertEquals(saksbehandler, saksbehandlerHentet)
    }

    @Test
    fun `kan hente saksbehandler på en oppgave fra revurdering`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val revurderingId = UUID.randomUUID().toString()
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                revurderingId,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.REVURDERING,
                null,
            )
        val saksbehandler = "saksbehandler"

        oppgaveService.tildelSaksbehandler(nyOppgave.id, saksbehandler)

        val saksbehandlerHentet =
            oppgaveService.hentSaksbehandlerForBehandling(UUID.fromString(revurderingId))

        Assertions.assertEquals(saksbehandler, saksbehandlerHentet)
    }

    @Test
    fun `Skal kunne hente saksbehandler på oppgave for behandling selvom den er ferdigstilt med attestering`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID().toString()
        val foerstegangsbehandling =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                behandlingId,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val saksbehandler = Saksbehandler("", "saksbehandler", null)

        oppgaveService.tildelSaksbehandler(foerstegangsbehandling.id, saksbehandler.ident)
        oppgaveService.ferdigStillOppgaveUnderBehandling(behandlingId, saksbehandler)
        val attestertBehandlingsoppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                behandlingId,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.ATTESTERING,
                null,
            )

        val attestantmock = mockk<SaksbehandlerMedEnheterOgRoller>()
        setNewKontekstWithMockUser(attestantmock)
        mockForSaksbehandlerMedRoller(attestantmock, generateSaksbehandlerMedRoller(AzureGroup.ATTESTANT))
        oppgaveService.tildelSaksbehandler(attestertBehandlingsoppgave.id, "attestant")

        val saksbehandlerHentet =
            oppgaveService.hentSaksbehandlerForBehandling(UUID.fromString(behandlingId))

        Assertions.assertEquals(saksbehandler.ident, saksbehandlerHentet)
    }

    @Test
    fun `Får null saksbehandler ved henting på behandling hvis saksbehandler ikke satt`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID().toString()
        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            behandlingId,
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )
        val saksbehandlerHentet =
            oppgaveService.hentSaksbehandlerForBehandling(UUID.fromString(behandlingId))

        Assertions.assertNull(saksbehandlerHentet)
    }
}
