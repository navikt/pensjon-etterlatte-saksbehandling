package behandling.sjekkliste

import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.sjekkliste.OppdaterSjekklisteItem
import no.nav.etterlatte.behandling.sjekkliste.SjekklisteDao
import no.nav.etterlatte.behandling.sjekkliste.SjekklisteService
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.sql.Connection
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SjekklisteIntegrationTest {
    private val user =
        mockk<SaksbehandlerMedEnheterOgRoller>().apply {
            every { this@apply.name() } returns "Z123456"
        }

    private val behandlingService = mockk<BehandlingService>()
    private lateinit var dataSource: DataSource
    private lateinit var sjekklisteDao: SjekklisteDao
    private lateinit var sjekklisteService: SjekklisteService

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

    @BeforeAll
    fun setup() {
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
        sjekklisteDao = SjekklisteDao { connection }
        sjekklisteService = SjekklisteService(sjekklisteDao, behandlingService)

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

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
        clearAllMocks()
    }

    @Test
    fun `Opprett sjekkliste for BP`() {
        val behandling = foerstegangsbehandling(sakId = 33L)
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        val opprettet = sjekklisteService.opprettSjekkliste(behandling.id)

        opprettet.id shouldBe behandling.id
        opprettet.versjon shouldBe 1

        opprettet.sjekklisteItems shouldHaveAtLeastSize 10
        opprettet.sjekklisteItems.forEach {
            it.avkrysset shouldBe false
            it.versjon shouldBe 1
        }
    }

    @Test
    fun `Hent eksisterende sjekkliste`() {
        val behandling = foerstegangsbehandling(sakId = 33L)
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        sjekklisteService.opprettSjekkliste(behandling.id)

        val sjekkliste = sjekklisteService.hentSjekkliste(behandling.id)!!

        sjekkliste.id shouldBe behandling.id
        sjekkliste.versjon shouldBe 1
        sjekkliste.sjekklisteItems shouldHaveAtLeastSize 10
    }

    @Test
    fun `Oppdatere et sjekkliste-element til avkrysset`() {
        val behandling = foerstegangsbehandling(sakId = 33L)
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        val opprettet = sjekklisteService.opprettSjekkliste(behandling.id)

        val item = opprettet.sjekklisteItems.first()
        item.avkrysset shouldBe false

        with(
            sjekklisteService.oppdaterSjekklisteItem(
                behandling.id,
                item.id,
                oppdatering = OppdaterSjekklisteItem(avkrysset = true),
            ),
        ) {
            this.id shouldBe item.id
            this.avkrysset shouldBe true
            this.versjon shouldBe 2
        }
    }
}
