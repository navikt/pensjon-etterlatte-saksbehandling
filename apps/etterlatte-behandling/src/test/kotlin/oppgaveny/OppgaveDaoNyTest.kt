package no.nav.etterlatte.oppgaveny

import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveNy
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveType
import no.nav.etterlatte.libs.common.oppgaveNy.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.oppgaveNy.Status
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakTilgangDao
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
    private lateinit var sakDao: SakDao
    private lateinit var saktilgangDao: SakTilgangDao

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
        sakDao = SakDao { connection }
        saktilgangDao = SakTilgangDao(dataSource)
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE oppgave CASCADE;").execute()
        }
    }

    @Test
    fun `legg til oppgaver og hent oppgaver`() {
        val sakAalesund = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDaoNy.lagreOppgave(oppgaveNy)
        oppgaveDaoNy.lagreOppgave(lagNyOppgave(sakAalesund))
        oppgaveDaoNy.lagreOppgave(lagNyOppgave(sakAalesund))

        val hentOppgaver = oppgaveDaoNy.hentOppgaver(OppgaveType.values().toList())
        assertEquals(3, hentOppgaver.size)
    }

    @Test
    fun `opprett oppgave av type ATTESTERING`() {
        val sakAalesund = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund, OppgaveType.ATTESTERING)
        oppgaveDaoNy.lagreOppgave(oppgaveNy)

        val hentOppgaver = oppgaveDaoNy.hentOppgaver(OppgaveType.values().toList())
        assertEquals(1, hentOppgaver.size)
        assertEquals(OppgaveType.ATTESTERING, hentOppgaver[0].type)
    }

    @Test
    fun `kan tildelesaksbehandler`() {
        val sakAalesund = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDaoNy.lagreOppgave(oppgaveNy)

        val nySaksbehandler = "nysaksbehandler"
        oppgaveDaoNy.settNySaksbehandler(SaksbehandlerEndringDto(oppgaveNy.id, nySaksbehandler))
        val hentOppgave = oppgaveDaoNy.hentOppgave(oppgaveNy.id)
        assertEquals(nySaksbehandler, hentOppgave?.saksbehandler)
        assertEquals(Status.UNDER_BEHANDLING, hentOppgave?.status)
    }

    @Test
    fun `Skal ikke kunne hente adressebeskyttede oppgaver fra vanlig hentoppgaver`() {
        val sakAalesund = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDaoNy.lagreOppgave(oppgaveNy)
        saktilgangDao.oppdaterAdresseBeskyttelse(sakAalesund.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)

        val sakutenbeskyttelse = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveUtenbeskyttelse = lagNyOppgave(sakutenbeskyttelse)
        oppgaveDaoNy.lagreOppgave(oppgaveUtenbeskyttelse)

        val hentetOppgave = oppgaveDaoNy.hentOppgaver(OppgaveType.values().toList())
        assertEquals(1, hentetOppgave.size)
    }

    @Test
    fun `Skal kunne hente adressebeskyttede oppgaver fra finnOppgaverForStrengtFortroligOgStrengtFortroligUtland`() {
        val sakAalesund = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDaoNy.lagreOppgave(oppgaveNy)
        saktilgangDao.oppdaterAdresseBeskyttelse(sakAalesund.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)

        val hentetOppgave = oppgaveDaoNy
            .finnOppgaverForStrengtFortroligOgStrengtFortroligUtland(OppgaveType.values().toList())
        assertEquals(1, hentetOppgave.size)
    }

    fun lagNyOppgave(sak: Sak, oppgaveType: OppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING) = OppgaveNy(
        UUID.randomUUID(),
        Status.NY,
        Enheter.AALESUND.enhetNr,
        sak.id,
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