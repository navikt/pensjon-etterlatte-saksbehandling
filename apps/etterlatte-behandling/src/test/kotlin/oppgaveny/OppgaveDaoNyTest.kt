package no.nav.etterlatte.oppgaveny

import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OppgaveDaoNyTest {

    private lateinit var dataSource: DataSource
    private lateinit var oppgaveDaoNy: OppgaveDaoNy

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource = DataSourceBuilder.createDataSource(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        ).apply { migrate() }

        val connection = dataSource.connection
        oppgaveDaoNy = OppgaveDaoNy { connection }
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE sak CASCADE;").execute()
        }
    }

    @Test
    fun `legg til oppgaver og hent oppgaver`() {
        val oppgaveNy = lagNyOppgave()
        oppgaveDaoNy.lagreOppgave(oppgaveNy)
        oppgaveDaoNy.lagreOppgave(lagNyOppgave())
        oppgaveDaoNy.lagreOppgave(lagNyOppgave())

        val hentOppgaver = oppgaveDaoNy.hentOppgaver()
        assertEquals(3, hentOppgaver.size)
    }

    fun lagNyOppgave() = OppgaveNy(
        UUID.randomUUID(),
        Status.NY,
        Enheter.AALESUND.enhetNr,
        1L,
        "Z1234",
        "referanse",
        "merknad",
        Tidspunkt.now()
    )
}