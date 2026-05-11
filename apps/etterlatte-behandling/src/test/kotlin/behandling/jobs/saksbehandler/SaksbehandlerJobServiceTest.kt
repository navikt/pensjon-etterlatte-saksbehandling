package no.nav.etterlatte.behandling.jobs.saksbehandler

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.klienter.EntraProxyKlient
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.PingResultUp
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import no.nav.etterlatte.saksbehandler.SaksbehandlerEnhet
import no.nav.etterlatte.saksbehandler.SaksbehandlerInfoDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class SaksbehandlerJobServiceTest(
    private val ds: DataSource,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val exceptionHandler = CoroutineExceptionHandler { _, e -> logger.error("Uventet feil i koroutine", e) }
    private val entraProxyKlient = mockk<EntraProxyKlient>()

    private lateinit var dao: SaksbehandlerInfoDao
    private lateinit var sakSkrivDao: SakSkrivDao
    private lateinit var oppgaveDao: OppgaveDaoImpl

    private val pingOppe =
        PingResultUp(
            serviceName = "EntraProxy",
            endpoint = "http://entra",
            description = "test",
        )

    @BeforeAll
    fun beforeAll() {
        dao = SaksbehandlerInfoDao(ConnectionAutoclosingTest(ds))
        sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(ds)))
        oppgaveDao = OppgaveDaoImpl(ConnectionAutoclosingTest(ds))
    }

    @AfterEach
    fun afterEach() {
        ds.connection.use {
            it.prepareStatement("TRUNCATE saksbehandler_info CASCADE; TRUNCATE sak CASCADE;").execute()
        }
    }

    @Test
    fun `saksbehandler med enheter faar enheter lagret`() {
        val ident = "A123456"
        val enhet = SaksbehandlerEnhet(Enhetsnummer("4817"), "NAV Familie- og pensjonsytelser")
        opprettOppgaveMedSaksbehandler(ident)
        dao.upsertSaksbehandlerNavn(SaksbehandlerInfo(ident, "Navn Navnesen"))

        coEvery { entraProxyKlient.ping() } returns pingOppe
        coEvery { entraProxyKlient.hentEnheterForIdent(ident) } returns listOf(enhet)

        runBlocking {
            oppdaterSaksbehandlerEnhet(logger, dao, entraProxyKlient, exceptionHandler)
        }

        dao.hentSaksbehandlerEnheter(ident) shouldBe listOf(enhet)
    }

    @Test
    fun `saksbehandler som ikke finnes i entra får enheter nullet ut i databasen`() {
        val ident = "A123456"
        val gammelEnhet = SaksbehandlerEnhet(Enheter.PORSGRUNN.enhetNr, Enheter.PORSGRUNN.navn)
        opprettOppgaveMedSaksbehandler(ident)
        dao.upsertSaksbehandlerNavn(SaksbehandlerInfo(ident, "Navn Navnesen"))
        dao.upsertSaksbehandlerEnheter(ident to listOf(gammelEnhet))

        coEvery { entraProxyKlient.ping() } returns pingOppe
        coEvery { entraProxyKlient.hentEnheterForIdent(ident) } returns emptyList()

        runBlocking {
            oppdaterSaksbehandlerEnhet(logger, dao, entraProxyKlient, exceptionHandler)
        }

        dao.hentSaksbehandlerEnheter(ident) shouldBe emptyList()
    }

    @Test
    fun `feil ved henting av enheter for en ident stopper ikke oppdatering av andre`() {
        val feilendeIdent = "A111111"
        val fungerendeIdent = "B222222"
        val enhet = SaksbehandlerEnhet(Enhetsnummer("4817"), "NAV Familie- og pensjonsytelser")
        opprettOppgaveMedSaksbehandler(feilendeIdent)
        opprettOppgaveMedSaksbehandler(fungerendeIdent)
        dao.upsertSaksbehandlerNavn(SaksbehandlerInfo(fungerendeIdent, "Fungerende Navnesen"))

        coEvery { entraProxyKlient.ping() } returns pingOppe
        coEvery { entraProxyKlient.hentEnheterForIdent(feilendeIdent) } throws RuntimeException("Noe gikk galt")
        coEvery { entraProxyKlient.hentEnheterForIdent(fungerendeIdent) } returns listOf(enhet)

        runBlocking {
            oppdaterSaksbehandlerEnhet(logger, dao, entraProxyKlient, exceptionHandler)
        }

        dao.hentSaksbehandlerEnheter(fungerendeIdent) shouldBe listOf(enhet)
    }

    private fun opprettOppgaveMedSaksbehandler(ident: String) {
        val sak = sakSkrivDao.opprettSak("fnr_$ident", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        oppgaveDao.opprettOppgave(lagOppgave(sak, ident))
    }

    private fun lagOppgave(
        sak: Sak,
        ident: String,
    ) = OppgaveIntern(
        id = UUID.randomUUID(),
        status = Status.NY,
        saksbehandler = OppgaveSaksbehandler(ident),
        enhet = sak.enhet,
        sakId = sak.id,
        kilde = OppgaveKilde.BEHANDLING,
        referanse = "referanse",
        gruppeId = null,
        merknad = null,
        opprettet = Tidspunkt.now(),
        sakType = sak.sakType,
        fnr = sak.ident,
        frist = null,
        type = OppgaveType.FOERSTEGANGSBEHANDLING,
    )
}
