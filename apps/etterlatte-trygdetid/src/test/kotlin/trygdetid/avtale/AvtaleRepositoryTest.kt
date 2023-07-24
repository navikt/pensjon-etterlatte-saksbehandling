package no.nav.etterlatte.trygdetid

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.trygdetid.avtale.AvtaleRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import trygdetid.trygdeavtale
import java.util.*
import java.util.UUID.randomUUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AvtaleRepositoryTest {

    @Container
    private val postgres = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
    private lateinit var repository: AvtaleRepository
    private lateinit var dataSource: DataSource

    @BeforeAll
    fun beforeAll() {
        postgres.start()
        dataSource = DataSourceBuilder.createDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
        repository = AvtaleRepository(dataSource.apply { migrate() })
    }

    @AfterAll
    fun afterAll() {
        postgres.stop()
    }

    @AfterEach
    fun afterEach() {
        cleanDatabase()
    }

    @Test
    fun `skal opprette og hente avtaler`() {
        val behandling = behandlingMock()
        val avtale = trygdeavtale(behandling.id, "EOS_NOR", "EOS2010", "YRK_MEDL")

        repository.opprettAvtale(avtale)

        repository.hentAvtale(behandling.id) shouldBe avtale
    }

    @Test
    fun `skal oppdatere avtaler`() {
        val behandling = behandlingMock()
        val avtale = trygdeavtale(behandling.id, "EOS_NOR", "EOS2010", "YRK_MEDL")

        repository.opprettAvtale(avtale)

        val oppdatertAvtale = repository.hentAvtale(behandling.id)!!.copy(
            avtaleKode = "ISR",
            avtaleDatoKode = null,
            avtaleKriteriaKode = "YRK_TRYGD"
        )

        repository.lagreAvtale(oppdatertAvtale)

        repository.hentAvtale(behandling.id) shouldBe oppdatertAvtale
    }

    private fun behandlingMock() =
        mockk<DetaljertBehandling>().apply {
            every { id } returns randomUUID()
            every { sak } returns 123L
        }

    private fun cleanDatabase() {
        dataSource.connection.use { it.prepareStatement("TRUNCATE trygdeavtale").apply { execute() } }
    }
}