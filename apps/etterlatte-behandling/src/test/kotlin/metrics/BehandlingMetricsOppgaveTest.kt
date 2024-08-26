package no.nav.etterlatte.metrics

import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Gauge
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class BehandlingMetricsOppgaveTest(
    private val ds: DataSource,
) {
    private lateinit var oppgaveDao: OppgaveDaoImpl
    private lateinit var sakDao: SakDao

    private lateinit var behandlingMetrikkerDao: BehandlingMetrikkerDao
    private lateinit var oppgaveMetrikkerDao: OppgaveMetrikkerDao
    private lateinit var gjenopprettingDao: GjenopprettingMetrikkerDao
    private lateinit var behandlingMetrics: BehandlingMetrics

    private val testreg = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    @BeforeAll
    fun beforeAll() {
        oppgaveDao = OppgaveDaoImpl(ConnectionAutoclosingTest(ds))
        sakDao = SakDao(ConnectionAutoclosingTest(ds))

        behandlingMetrikkerDao = BehandlingMetrikkerDao(ds)
        oppgaveMetrikkerDao = OppgaveMetrikkerDao(ds)
        gjenopprettingDao = GjenopprettingMetrikkerDao(ds)
        behandlingMetrics = BehandlingMetrics(oppgaveMetrikkerDao, behandlingMetrikkerDao, gjenopprettingDao, testreg)

        opprettOppgaver()
        behandlingMetrics.run()
    }

    @Nested
    inner class Oppgaver {
        @Test
        fun `Metrikker for oppgaver totalt`() {
            testreg.get("etterlatte oppgaver").gauges().size shouldBe 4
        }

        @Test
        fun `Metrikker for oppgaver status`() {
            val metrikker = metrikker("etterlatte oppgaver")
            val tag = "status"
            assertEquals(4, metrikker.size)
            assertEquals(1, hentVerdi(metrikker, tag, Status.NY.name))
            assertEquals(1, hentVerdi(metrikker, tag, Status.UNDER_BEHANDLING.name))
            assertEquals(1, hentVerdi(metrikker, tag, Status.FEILREGISTRERT.name))
            assertEquals(1, hentVerdi(metrikker, tag, Status.FERDIGSTILT.name))
        }

        @Test
        fun `Metrikker for oppgaver enhet`() {
            val tag = "enhet"
            val metrikker = metrikker("etterlatte oppgaver")
            assertEquals(2, hentVerdi(metrikker, tag, Enheter.AALESUND.enhetNr))
            assertEquals(2, hentVerdi(metrikker, tag, Enheter.PORSGRUNN.enhetNr))
        }

        @Test
        fun `Metrikker for oppgaver saktype`() {
            val tag = "saktype"
            val metrikker = metrikker("etterlatte oppgaver")
            assertEquals(2, hentVerdi(metrikker, tag, SakType.BARNEPENSJON.name))
            assertEquals(2, hentVerdi(metrikker, tag, SakType.OMSTILLINGSSTOENAD.name))
        }
    }

    private fun hentVerdi(
        metrikker: Collection<Gauge>,
        tag: String,
        verdi: String,
    ) = metrikker
        .filter {
            it.id.getTag(tag) == verdi
        }.sumOf { it.value() }
        .toInt()

    private fun metrikker(metrikk: String) = testreg.get(metrikk).gauges()

    @Nested
    inner class Saksbehandler {
        @Test
        fun `Metrikker for oppgaver totalt og per enhet`() {
            val metrikker = metrikker("etterlatte_oppgaver_saksbehandler")
            val tag = "enhet"
            assertEquals(2, hentVerdi(metrikker, tag, Enheter.AALESUND.enhetNr))
            assertEquals(2, hentVerdi(metrikker, tag, Enheter.PORSGRUNN.enhetNr))
            assertEquals(3, hentVerdi(metrikker, tag, "Totalt"))
        }
    }

    private fun opprettOppgaver() {
        sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr).let {
            oppgaveDao.opprettOppgave(lagNyOppgave(it, Status.NY, "saksbehandler1"))
        }
        sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr).let {
            oppgaveDao.opprettOppgave(lagNyOppgave(it, Status.UNDER_BEHANDLING, "saksbehandler2"))
        }
        sakDao.opprettSak("fnr", SakType.OMSTILLINGSSTOENAD, Enheter.PORSGRUNN.enhetNr).let {
            oppgaveDao.opprettOppgave(lagNyOppgave(it, Status.FEILREGISTRERT, "saksbehandler1"))
        }
        sakDao.opprettSak("fnr", SakType.OMSTILLINGSSTOENAD, Enheter.PORSGRUNN.enhetNr).let {
            oppgaveDao.opprettOppgave(lagNyOppgave(it, Status.FERDIGSTILT, "saksbehandler3"))
        }
    }

    fun lagNyOppgave(
        sak: Sak,
        status: Status,
        ident: String,
    ) = OppgaveIntern(
        id = UUID.randomUUID(),
        status = status,
        saksbehandler = OppgaveSaksbehandler(ident),
        enhet = sak.enhet,
        sakId = sak.id,
        kilde = OppgaveKilde.BEHANDLING,
        referanse = "referanse",
        merknad = "merknad",
        opprettet = Tidspunkt.now(),
        sakType = sak.sakType,
        fnr = sak.ident,
        frist = null,
        type = OppgaveType.FOERSTEGANGSBEHANDLING,
    )
}
