package no.nav.etterlatte.behandling.generellbehandling

import io.mockk.mockk
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.oppgave.OppgaveDao
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.oppgave.OppgaveDaoMedEndringssporingImpl
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.token.BrukerTokenInfo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenerellBehandlingServiceTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

    private lateinit var dataSource: DataSource
    private lateinit var dao: GenerellBehandlingDao
    private lateinit var oppgaveDao: OppgaveDao
    private lateinit var oppgaveService: OppgaveService
    private lateinit var sakRepo: SakDao
    private lateinit var service: GenerellBehandlingService

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
        dao = GenerellBehandlingDao { connection }
        oppgaveDao = OppgaveDaoImpl { connection }
        sakRepo = SakDao { connection }

        oppgaveService = OppgaveService(OppgaveDaoMedEndringssporingImpl(oppgaveDao) { connection }, sakRepo, DummyFeatureToggleService())
        service = GenerellBehandlingService(dao, oppgaveService)
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE generellbehandling CASCADE; TRUNCATE oppgave CASCADE").execute()
        }
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    private val user = mockk<SaksbehandlerMedEnheterOgRoller>()

    @BeforeEach
    fun before() {
        Kontekst.set(
            Context(
                user,
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

    @Test
    fun kanOppretteBehandlingOgFaarDaOgsaaEnOppgaveManuellOpprettelseUtenTildelingAvSaksbehandler() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val manueltOpprettetBehandling = GenerellBehandling.opprettFraType(GenerellBehandling.GenerellBehandlingType.UTLAND, sak.id)
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling)
        Assertions.assertEquals(GenerellBehandling.GenerellBehandlingType.UTLAND, opprettBehandling.type)
        Assertions.assertEquals(sak.id, opprettBehandling.sakId)
        Assertions.assertNotNull(opprettBehandling.opprettet)
        Assertions.assertNull(opprettBehandling.innhold)

        val oppgaverforGenerellBehandling = oppgaveService.hentOppgaverForReferanse(manueltOpprettetBehandling.id.toString())
        Assertions.assertEquals(1, oppgaverforGenerellBehandling.size)
        val manuellBehandlingOppgave = oppgaverforGenerellBehandling[0]
        Assertions.assertEquals(manueltOpprettetBehandling.id.toString(), manuellBehandlingOppgave.referanse)
        Assertions.assertEquals(OppgaveKilde.GENERELL_BEHANDLING, manuellBehandlingOppgave.kilde)
        Assertions.assertEquals(OppgaveType.UTLAND, manuellBehandlingOppgave.type)
        Assertions.assertEquals(sak.id, manuellBehandlingOppgave.sakId)
        Assertions.assertNull(manuellBehandlingOppgave.saksbehandler)
    }

    @Test
    fun kanOppretteBehandlingOgFaarDaOgsaaEnOppgaveManuellOpprettelseMedTildelingAvSaksbehandler() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID()
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettUtland(
                GenerellBehandling.GenerellBehandlingType.UTLAND,
                sak.id,
                behandlingId,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling)
        Assertions.assertEquals(GenerellBehandling.GenerellBehandlingType.UTLAND, opprettBehandling.type)
        Assertions.assertEquals(sak.id, opprettBehandling.sakId)
        Assertions.assertNotNull(opprettBehandling.opprettet)
        Assertions.assertNull(opprettBehandling.innhold)

        val oppgaveForFoerstegangsBehandling =
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(behandlingId.toString(), sak.id)
        val saksbehandler = "saksbehandler"
        oppgaveService.tildelSaksbehandler(oppgaveForFoerstegangsBehandling.id, saksbehandler)

        val tokenInfo = BrukerTokenInfo.of(UUID.randomUUID().toString(), saksbehandler, null, null, null)
        oppgaveService.ferdigStillOppgaveUnderBehandling(behandlingId.toString(), tokenInfo)

        val oppgaverforGenerellBehandling = oppgaveService.hentOppgaverForReferanse(manueltOpprettetBehandling.id.toString())
        Assertions.assertEquals(1, oppgaverforGenerellBehandling.size)
        val manuellBehandlingOppgave = oppgaverforGenerellBehandling[0]
        Assertions.assertEquals(manueltOpprettetBehandling.id.toString(), manuellBehandlingOppgave.referanse)
        Assertions.assertEquals(OppgaveKilde.GENERELL_BEHANDLING, manuellBehandlingOppgave.kilde)
        Assertions.assertEquals(OppgaveType.UTLAND, manuellBehandlingOppgave.type)
        Assertions.assertEquals(sak.id, manuellBehandlingOppgave.sakId)
        Assertions.assertNull(manuellBehandlingOppgave.saksbehandler)
    }
}
