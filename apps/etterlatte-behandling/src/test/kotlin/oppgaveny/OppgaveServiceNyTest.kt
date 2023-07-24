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
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
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

                    override fun <T> inTransaction(gjenbruk: Boolean, block: () -> T): T {
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

        val oppgaveMedNySaksbehandler = oppgaveServiceNy.hentOppgave(nyOppgave.id)
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
        Assertions.assertTrue(err.message!!.startsWith("Oppgaven har allerede en saksbehandler"))
    }

    @Test
    fun `skal ikke kunne tildele hvis oppgaven ikke finnes`() {
        val nysaksbehandler = "nysaksbehandler"
        val err = assertThrows<NotFoundException> {
            oppgaveServiceNy.tildelSaksbehandler(SaksbehandlerEndringDto(UUID.randomUUID(), nysaksbehandler))
        }
        Assertions.assertTrue(err.message!!.startsWith("Oppgaven finnes ikke"))
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

        val oppgaveMedNySaksbehandler = oppgaveServiceNy.hentOppgave(nyOppgave.id)
        Assertions.assertEquals(nysaksbehandler, oppgaveMedNySaksbehandler?.saksbehandler)
    }

    @Test
    fun `skal ikke kunne bytte saksbehandler på en ikke eksisterende sak`() {
        val nysaksbehandler = "nysaksbehandler"
        val err = assertThrows<NotFoundException> {
            oppgaveServiceNy.byttSaksbehandler(SaksbehandlerEndringDto(UUID.randomUUID(), nysaksbehandler))
        }
        Assertions.assertTrue(err.message!!.startsWith("Oppgaven finnes ikke"))
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
        oppgaveServiceNy.fjernSaksbehandler(FjernSaksbehandlerRequest(nyOppgave.id))
        val oppgaveUtenSaksbehandler = oppgaveServiceNy.hentOppgave(nyOppgave.id)
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
            oppgaveServiceNy.fjernSaksbehandler(FjernSaksbehandlerRequest(nyOppgave.id))
        }
        Assertions.assertTrue(err.message!!.startsWith("Oppgaven har ingen saksbehandler"))
    }

    @Test
    fun `kan redigere frist`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveType.FOERSTEGANGSBEHANDLING
        )
        oppgaveServiceNy.tildelSaksbehandler(SaksbehandlerEndringDto(nyOppgave.id, "nysaksbehandler"))
        val nyFrist = Tidspunkt.now().toLocalDatetimeUTC().plusMonths(4L).toTidspunkt()
        oppgaveServiceNy.redigerFrist(RedigerFristRequest(nyOppgave.id, nyFrist))
        val oppgaveMedNyFrist = oppgaveServiceNy.hentOppgave(nyOppgave.id)
        Assertions.assertEquals(nyFrist, oppgaveMedNyFrist?.frist)
    }

    @Test
    fun `kan ikke redigere frist tilbake i tid`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveType.FOERSTEGANGSBEHANDLING
        )
        oppgaveServiceNy.tildelSaksbehandler(SaksbehandlerEndringDto(nyOppgave.id, "nysaksbehandler"))
        val nyFrist = Tidspunkt.now().toLocalDatetimeUTC().minusMonths(1L).toTidspunkt()

        val err = assertThrows<BadRequestException> {
            oppgaveServiceNy.redigerFrist(RedigerFristRequest(nyOppgave.id, nyFrist))
        }

        Assertions.assertTrue(err.message!!.startsWith("Tidspunkt tilbake i tid id: "))
    }

    @Test
    fun `kan ikke fjerne saksbehandler hvis oppgaven ikke finnes`() {
        val err = assertThrows<NotFoundException> {
            oppgaveServiceNy.fjernSaksbehandler(FjernSaksbehandlerRequest(UUID.randomUUID()))
        }
        Assertions.assertTrue(err.message!!.startsWith("Oppgaven finnes ikke"))
    }
}