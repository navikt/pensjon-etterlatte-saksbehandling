package no.nav.etterlatte.oppgaveny

import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveNy
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveType
import no.nav.etterlatte.libs.common.oppgaveNy.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.oppgaveNy.Status
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
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
        oppgaveDaoNy = OppgaveDaoNy { connection }
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE oppgave CASCADE;").execute()
        }
    }

    @Test
    fun `legg til oppgaver og hent oppgaver`() {
        val sakAalesund = Sak("1231244", SakType.BARNEPENSJON, 1L, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDaoNy.lagreOppgave(oppgaveNy)
        oppgaveDaoNy.lagreOppgave(lagNyOppgave(sakAalesund))
        oppgaveDaoNy.lagreOppgave(lagNyOppgave(sakAalesund))

        val hentOppgaver = oppgaveDaoNy.hentOppgaver()
        assertEquals(3, hentOppgaver.size)
    }

    @Test
    fun `opprett oppgave av type ATTESTERING`() {
        val sakAalesund = Sak("1231244", SakType.BARNEPENSJON, 1L, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund, OppgaveType.ATTESTERING)
        oppgaveDaoNy.lagreOppgave(oppgaveNy)

        val hentOppgaver = oppgaveDaoNy.hentOppgaver()
        assertEquals(1, hentOppgaver.size)
        assertEquals(OppgaveType.ATTESTERING, hentOppgaver[0].type)
    }

    @Test
    fun `kan tildelesaksbehandler`() {
        val sakAalesund = Sak("1231244", SakType.BARNEPENSJON, 1L, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDaoNy.lagreOppgave(oppgaveNy)

        val nySaksbehandler = "nysaksbehandler"
        oppgaveDaoNy.settNySaksbehandler(SaksbehandlerEndringDto(oppgaveNy.id, nySaksbehandler))
        val hentOppgave = oppgaveDaoNy.hentOppgave(oppgaveNy.id)
        assertEquals(nySaksbehandler, hentOppgave?.saksbehandler)
    }

    fun lagNyOppgave(sak: Sak, oppgaveType: OppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING) = OppgaveNy(
        UUID.randomUUID(),
        Status.NY,
        Enheter.AALESUND.enhetNr,
        1L,
        type = oppgaveType,
        null,
        "referanse",
        "merknad",
        Tidspunkt.now(),
        sakType = sak.sakType,
        fnr = sak.ident,
        frist = null
    )
}