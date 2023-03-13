package no.nav.etterlatte.statistikk.database

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.statistikk.domain.SoeknadStatistikk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SoeknadStatistikkRepositoryTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var dataSource: DataSource

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource = DataSourceBuilder.createDataSource(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        )

        dataSource.migrate(gcp = false)
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.prepareStatement("""TRUNCATE TABLE soeknad_statistikk""")
            .executeUpdate()
    }

    @Test
    fun `lagrer ned soeknadStatistikk`() {
        val repo = SoeknadStatistikkRepository.using(dataSource)

        Assertions.assertEquals(0L, repo.hentAntallSoeknader())

        repo.lagreNedSoeknadStatistikk(
            soeknadStatistikk = SoeknadStatistikk(
                soeknadId = 1337,
                gyldigForBehandling = true,
                sakType = SakType.BARNEPENSJON,
                kriterierForIngenBehandling = listOf()
            )
        )

        Assertions.assertEquals(1L, repo.hentAntallSoeknader())
    }

    @Test
    fun `duplikate lagringer på samme søknadId samles og oppdateres av siste lagring`() {
        val repo = SoeknadStatistikkRepository.using(dataSource)

        Assertions.assertEquals(0L, repo.hentAntallSoeknader())
        repo.lagreNedSoeknadStatistikk(
            soeknadStatistikk = SoeknadStatistikk(
                soeknadId = 2,
                gyldigForBehandling = false,
                sakType = SakType.OMSTILLINGSSTOENAD,
                kriterierForIngenBehandling = listOf("Jo")
            )
        )
        Assertions.assertEquals(0, repo.hentAntallSoeknaderGyldigForBehandling())
        Assertions.assertEquals(1L, repo.hentAntallSoeknaderIkkeGyldigForBehandling())
        Assertions.assertEquals(1L, repo.hentAntallSoeknader())
        repo.lagreNedSoeknadStatistikk(
            soeknadStatistikk = SoeknadStatistikk(
                soeknadId = 2,
                gyldigForBehandling = true,
                sakType = SakType.BARNEPENSJON,
                kriterierForIngenBehandling = listOf()
            )
        )
        Assertions.assertEquals(1L, repo.hentAntallSoeknaderGyldigForBehandling())
        Assertions.assertEquals(0, repo.hentAntallSoeknaderIkkeGyldigForBehandling())
        Assertions.assertEquals(1L, repo.hentAntallSoeknader())
    }
}