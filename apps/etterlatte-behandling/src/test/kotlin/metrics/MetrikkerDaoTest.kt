package metrics

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveNy
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveType
import no.nav.etterlatte.libs.common.oppgaveNy.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.metrics.OppgaveMetrikkerDao
import no.nav.etterlatte.oppgaveny.OppgaveDaoNy
import no.nav.etterlatte.oppgaveny.OppgaveDaoNyImpl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.*
import javax.sql.DataSource


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MetrikkerDaoTest {
    private lateinit var dataSource: DataSource
    private lateinit var oppgaveDaoNy: OppgaveDaoNy
    private lateinit var metrikkerDao: OppgaveMetrikkerDao

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

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
        oppgaveDaoNy = OppgaveDaoNyImpl { connection }
        metrikkerDao = OppgaveMetrikkerDao(dataSource)
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE oppgave CASCADE;").execute()
        }
    }

    @Test
    fun `Skal returnere antall for totalt, aktive og inaktive oppgaver`() {
        val oppgaver = listOf(
            lagNyOppgave(status = Status.NY),
            lagNyOppgave(status = Status.UNDER_BEHANDLING),
            lagNyOppgave(status = Status.AVBRUTT),
            lagNyOppgave(status = Status.FERDIGSTILT),
            lagNyOppgave(status = Status.FEILREGISTRERT),
        )
        oppgaver.forEach {
            oppgaveDaoNy.lagreOppgave(it)
        }

        metrikkerDao.hentOppgaveAntall().asClue {
            it.totalt shouldBe 5
            it.aktive shouldBe 2
            it.avsluttet shouldBe 3
        }
    }

    fun lagNyOppgave(
        sakId: Long = 123L,
        sakType: SakType = SakType.BARNEPENSJON,
        oppgaveKilde: OppgaveKilde = OppgaveKilde.BEHANDLING,
        oppgaveType: OppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
        status: Status = Status.NY,
    ) = OppgaveNy(
        id = UUID.randomUUID(),
        status = status,
        enhet = Enheter.AALESUND.enhetNr,
        sakId = sakId,
        kilde = oppgaveKilde,
        referanse = "referanse",
        merknad = "merknad",
        opprettet = Tidspunkt.now(),
        sakType = sakType,
        fnr = "",
        frist = null,
        type = oppgaveType
    )
}