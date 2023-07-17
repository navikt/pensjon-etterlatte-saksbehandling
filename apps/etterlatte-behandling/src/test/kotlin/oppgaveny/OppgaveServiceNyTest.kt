package no.nav.etterlatte.oppgaveny

import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.mockk.mockk
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.sql.Connection
import java.util.*
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

    @BeforeEach
    fun beforeEach() {
        val saksbehandler = mockk<SaksbehandlerMedEnheterOgRoller>()
        Kontekst.set(
            Context(
                saksbehandler,
                object : DatabaseKontekst {
                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(block: () -> T): T {
                        return block()
                    }
                }
            )
        )
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
        val nysaksbehandler = "nysaksbehandler"
        oppgaveServiceNy.tildelSaksbehandler(SaksbehandlerEndringDto(nyOppgave.id, nysaksbehandler))

        val oppgaveMedNySaksbehandler = oppgaveServiceNy.hentOppgve(nyOppgave.id)
        Assertions.assertEquals(nysaksbehandler, oppgaveMedNySaksbehandler?.saksbehandler)
    }

    @Test
    fun `skal ikke kunne tildele oppgave med saksbehandler`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveType.FOERSTEGANGSBEHANDLING
        )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveServiceNy.tildelSaksbehandler(SaksbehandlerEndringDto(nyOppgave.id, nysaksbehandler))
        val err = assertThrows<BadRequestException> {
            oppgaveServiceNy.tildelSaksbehandler(SaksbehandlerEndringDto(nyOppgave.id, "enda en"))
        }
        Assertions.assertEquals("Oppgaven har allerede en saksbehandler", err.message)
    }

    @Test
    fun `skal ikke kunne tildele hvis oppgaven ikke finnes`() {
        val nysaksbehandler = "nysaksbehandler"
        val err = assertThrows<NotFoundException> {
            oppgaveServiceNy.tildelSaksbehandler(SaksbehandlerEndringDto(UUID.randomUUID(), nysaksbehandler))
        }
        Assertions.assertEquals("Oppgaven finnes ikke", err.message)
    }

    @Test
    fun `skal kunne bytte oppgave med saksbehandler`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveType.FOERSTEGANGSBEHANDLING
        )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveServiceNy.byttSaksbehandler(SaksbehandlerEndringDto(nyOppgave.id, nysaksbehandler))

        val oppgaveMedNySaksbehandler = oppgaveServiceNy.hentOppgve(nyOppgave.id)
        Assertions.assertEquals(nysaksbehandler, oppgaveMedNySaksbehandler?.saksbehandler)
    }

    @Test
    fun `skal ikke kunne bytte saksbehandler på en ikke eksisterende sak`() {
        val nysaksbehandler = "nysaksbehandler"
        val err = assertThrows<NotFoundException> {
            oppgaveServiceNy.byttSaksbehandler(SaksbehandlerEndringDto(UUID.randomUUID(), nysaksbehandler))
        }
        Assertions.assertEquals("Oppgaven finnes ikke", err.message)
    }

    @Test
    fun `skal kunne fjerne saksbehandler fra oppgave`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveType.FOERSTEGANGSBEHANDLING
        )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveServiceNy.tildelSaksbehandler(SaksbehandlerEndringDto(nyOppgave.id, nysaksbehandler))
        oppgaveServiceNy.fjernSaksbehandler(OppgaveId(nyOppgave.id))
        val oppgaveUtenSaksbehandler = oppgaveServiceNy.hentOppgve(nyOppgave.id)
        Assertions.assertNotNull(oppgaveUtenSaksbehandler?.id)
        Assertions.assertNull(oppgaveUtenSaksbehandler?.saksbehandler)
    }

    @Test
    fun `kan ikke fjerne saksbehandler hvis det ikke er satt på oppgaven`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveType.FOERSTEGANGSBEHANDLING
        )
        val err = assertThrows<BadRequestException> {
            oppgaveServiceNy.fjernSaksbehandler(OppgaveId(nyOppgave.id))
        }
        Assertions.assertEquals("Oppgaven har ingen saksbehandler", err.message)
    }

    @Test
    fun `kan ikke fjerne saksbehandler hvis oppgaven ikke finnes`() {
        val err = assertThrows<NotFoundException> {
            oppgaveServiceNy.fjernSaksbehandler(OppgaveId(UUID.randomUUID()))
        }
        Assertions.assertEquals("Oppgaven finnes ikke", err.message)
    }
}