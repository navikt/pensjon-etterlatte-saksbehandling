package no.nav.etterlatte.migrering.start

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotliquery.queryOf
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.database.transaction
import no.nav.helse.rapids_rivers.RapidsConnection
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StartMigreringTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
    private lateinit var repository: StartMigreringRepository
    private lateinit var dataSource: DataSource

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        dataSource =
            DataSourceBuilder.createDataSource(
                postgreSQLContainer.jdbcUrl,
                postgreSQLContainer.username,
                postgreSQLContainer.password,
            ).also { it.migrate() }
        repository = StartMigreringRepository(dataSource)
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `starter migrering`() {
        dataSource.transaction { tx ->
            queryOf(
                "INSERT INTO ${StartMigreringRepository.Databasetabell.TABELLNAVN}" +
                    " (${StartMigreringRepository.Databasetabell.SAKID})" +
                    "VALUES(123)",
            ).let { query -> tx.run(query.asUpdate) }
        }
        Assertions.assertEquals(1, repository.hentSakerTilMigrering().size)
        val starter =
            StartMigrering(
                repository = repository,
                rapidsConnection = mockk<RapidsConnection>().also { every { it.publish(any(), any()) } just runs },
                featureToggleService = mockk<FeatureToggleService>().also { every { it.isEnabled(any(), any()) } returns true },
            )

        starter.startMigrering()
        Assertions.assertEquals(0, repository.hentSakerTilMigrering().size)
    }
}
