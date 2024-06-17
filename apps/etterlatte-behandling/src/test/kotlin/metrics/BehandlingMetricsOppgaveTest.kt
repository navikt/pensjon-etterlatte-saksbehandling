package no.nav.etterlatte.metrics

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.prometheus.client.CollectorRegistry
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

    private val testreg = CollectorRegistry(true)

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
        fun `Metrikker for oppgaver labels i riktig rekkefoelge`() {
            val metrikker =
                behandlingMetrics.oppgaver
                    .collect()
                    .first()
                    .samples
            metrikker.first().labelNames shouldContainExactly listOf("status", "enhet", "saktype")
        }

        @Test
        fun `Metrikker for oppgaver totalt`() {
            val metrikker =
                behandlingMetrics.oppgaver
                    .collect()
                    .first()
                    .samples
            metrikker.size shouldBe 4
        }

        @Test
        fun `Metrikker for oppgaver status`() {
            val metrikker =
                behandlingMetrics.oppgaver
                    .collect()
                    .first()
                    .samples
            metrikker.filter { it.labelValues[0] == Status.NY.name }.size shouldBe 1
            metrikker.filter { it.labelValues[0] == Status.UNDER_BEHANDLING.name }.size shouldBe 1
            metrikker.filter { it.labelValues[0] == Status.FEILREGISTRERT.name }.size shouldBe 1
            metrikker.filter { it.labelValues[0] == Status.FERDIGSTILT.name }.size shouldBe 1
        }

        @Test
        fun `Metrikker for oppgaver enhet`() {
            val metrikker =
                behandlingMetrics.oppgaver
                    .collect()
                    .first()
                    .samples
            metrikker.filter { it.labelValues[1] == Enheter.AALESUND.enhetNr }.size shouldBe 2
            metrikker.filter { it.labelValues[1] == Enheter.PORSGRUNN.enhetNr }.size shouldBe 2
        }

        @Test
        fun `Metrikker for oppgaver saktype`() {
            val metrikker =
                behandlingMetrics.oppgaver
                    .collect()
                    .first()
                    .samples
            metrikker.filter { it.labelValues[2] == SakType.BARNEPENSJON.name }.size shouldBe 2
            metrikker.filter { it.labelValues[2] == SakType.OMSTILLINGSSTOENAD.name }.size shouldBe 2
        }
    }

    @Nested
    inner class Saksbehandler {
        @Test
        fun `Metrikker for saksbehandler labels i riktig rekkefoelge`() {
            val metrikker =
                behandlingMetrics.saksbehandler
                    .collect()
                    .first()
                    .samples
            metrikker.first().labelNames shouldContainExactly listOf("enhet")
        }

        @Test
        fun `Metrikker for oppgaver totalt og per enhet`() {
            val metrikker =
                behandlingMetrics.saksbehandler
                    .collect()
                    .first()
                    .samples
            metrikker.first { it.labelValues[0] == Enheter.AALESUND.enhetNr }.value shouldBe 2
            metrikker.first { it.labelValues[0] == Enheter.PORSGRUNN.enhetNr }.value shouldBe 2
            metrikker.first { it.labelValues[0] == "Totalt" }.value shouldBe 3
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
