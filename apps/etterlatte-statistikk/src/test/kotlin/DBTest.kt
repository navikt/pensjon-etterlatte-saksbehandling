package no.nav.etterlatte

import no.nav.etterlatte.database.DataSourceBuilder
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.statistikk.StatistikkRepository
import no.nav.etterlatte.statistikk.StoenadRad
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.Instant
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DBTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var dataSource: DataSource

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        val dsb = DataSourceBuilder(mapOf("DB_JDBC_URL" to postgreSQLContainer.jdbcUrl))
        dataSource = dsb.dataSource

        dsb.migrate()
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun testDB() {
        val repo = StatistikkRepository.using(dataSource)
        val rad = repo.lagreStoenadsrad(
            StoenadRad(
                -1,
                "123",
                listOf(Foedselsnummer.of("23427249697"), Foedselsnummer.of("18458822782")),
                listOf(),
                "40",
                "1000",
                "FOLKETRYGD",
                "0,4G",
                UUID.randomUUID(),
                5,
                5,
                Instant.now(),
                "BP",
                "42",
                "Berit Behandler",
                "Arne Attestant",
                LocalDate.now(),
                null
            )
        )
        repo.datapakke().also {
            Assertions.assertEquals(1, it.size)
            val stoenadRad = it.first()
            Assertions.assertEquals(stoenadRad, rad)
            Assertions.assertEquals(5, stoenadRad.sakId)
            Assertions.assertEquals(
                stoenadRad.fnrForeldre,
                listOf(Foedselsnummer.of("23427249697"), Foedselsnummer.of("18458822782"))
            )
        }
    }
}