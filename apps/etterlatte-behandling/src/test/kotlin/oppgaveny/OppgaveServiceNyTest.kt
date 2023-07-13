package no.nav.etterlatte.oppgaveny

import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OppgaveServiceNyTest {

    private lateinit var dataSource: DataSource
    private lateinit var sakDao: SakDao
    private lateinit var oppgaveDaoNy: OppgaveDaoNy
    private lateinit var oppgaveServiceNy: OppgaveServiceNy

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
        sakDao = SakDao { connection }
        oppgaveDaoNy = OppgaveDaoNy { connection }
        oppgaveServiceNy = OppgaveServiceNy(oppgaveDaoNy, sakDao)
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE oppgave CASCADE;").execute()
        }
    }

    @Test
    fun `skal kunne tildele oppgave uten saksbehandler`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveType.FOERSTEGANGSBEHANDLING
        )
        oppgaveServiceNy.tildelSaksbehandler(NySaksbehandlerDto(nyOppgave?.id!!, "nysaksbehandler"))
    }

    @Test
    fun `skal ikke kunne tildele oppgave med saksbehandler`() {
    }

    @Test
    fun `skal ikke kunne tildele hvis oppgave ikke finnes`() {
    }

    @Test
    fun `skal kunne bytte oppgave med saksbehandler`() {
    }
}