package metrics

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.metrics.BehandlingMetrics
import no.nav.etterlatte.metrics.BehandlingMetrikkerDao
import no.nav.etterlatte.metrics.OppgaveMetrikkerDao
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingMetricsTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
    private lateinit var behandlingMetrikkerDao: BehandlingMetrikkerDao
    private lateinit var oppgaveDao: OppgaveMetrikkerDao
    private lateinit var behandlingRepo: BehandlingDao
    private lateinit var sakRepo: SakDao
    private lateinit var behandlingMetrics: BehandlingMetrics

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()

        val ds =
            DataSourceBuilder.createDataSource(
                postgreSQLContainer.jdbcUrl,
                postgreSQLContainer.username,
                postgreSQLContainer.password,
            ).also { it.migrate() }
        val connection = ds.connection

        sakRepo = SakDao { connection }
        behandlingRepo =
            BehandlingDao(
                kommerBarnetTilGodeDao = mockk(),
                revurderingDao = mockk(),
                connection = { connection },
            )

        opprettBehandlinger()

        behandlingMetrikkerDao = BehandlingMetrikkerDao(ds)
        oppgaveDao = OppgaveMetrikkerDao(ds)
        behandlingMetrics = BehandlingMetrics(oppgaveDao, behandlingMetrikkerDao)

        behandlingMetrics.run()
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `Metrikker for behandlinger skal ha labels i riktig rekkefoelge`() {
        val metrikker = behandlingMetrics.behandlinger.collect().first().samples
        metrikker.first().labelNames shouldContainExactly
            listOf(
                "behandlingstype", "status", "revurdering_aarsak", "kilde", "automatiskMigrering",
            )
    }

    @Test
    fun `Henter riktig antall totalt`() {
        val metrikker = behandlingMetrics.behandlinger.collect().first().samples
        metrikker.size shouldBe 4
    }

    @Test
    fun `Henter riktig antall for behandlingstyper`() {
        val metrikker = behandlingMetrics.behandlinger.collect().first().samples
        metrikker.filter { it.labelValues[0] == BehandlingType.FØRSTEGANGSBEHANDLING.name }.size shouldBe 3
        metrikker.filter { it.labelValues[0] == BehandlingType.REVURDERING.name }.size shouldBe 1
    }

    @Test
    fun `Henter riktig antall for status`() {
        val metrikker = behandlingMetrics.behandlinger.collect().first().samples
        metrikker.filter { it.labelValues[1] == BehandlingStatus.IVERKSATT.name }.size shouldBe 2
        metrikker.filter { it.labelValues[1] == BehandlingStatus.OPPRETTET.name }.size shouldBe 2
    }

    @Test
    fun `Henter riktig antall for revuderingsaarsak`() {
        val metrikker = behandlingMetrics.behandlinger.collect().first().samples
        metrikker.filter { it.labelValues[2] == "null" }.size shouldBe 3 // Førstegangsbehandling
        metrikker.filter { it.labelValues[2] == Revurderingaarsak.REGULERING.name }.size shouldBe 1
    }

    @Test
    fun `Henter riktig antall for kilde`() {
        val metrikker = behandlingMetrics.behandlinger.collect().first().samples
        metrikker.filter { it.labelValues[3] == Vedtaksloesning.GJENNY.name }.size shouldBe 2
        metrikker.filter { it.labelValues[3] == Vedtaksloesning.PESYS.name }.size shouldBe 2
    }

    @Test
    fun `Henter riktig antall for automatiskMigrert`() {
        val metrikker = behandlingMetrics.behandlinger.collect().first().samples
        metrikker.filter { it.labelValues[4] == "true" }.size shouldBe 1
        metrikker.filter { it.labelValues[4] == "false" }.size shouldBe 3
    }

    // TODO SakType...

    private fun opprettBehandlinger() {
        sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).let {
            behandlingRepo.opprettBehandling(
                opprettBehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.IVERKSATT,
                    sakId = it.id,
                ),
            )
            behandlingRepo.opprettBehandling(
                opprettBehandling(
                    type = BehandlingType.REVURDERING,
                    revurderingAarsak = Revurderingaarsak.REGULERING,
                    sakId = it.id,
                ),
            )
        }

        // Automatisk migrert fra pesys
        sakRepo.opprettSak("111", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).let {
            behandlingRepo.opprettBehandling(
                opprettBehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.IVERKSATT,
                    kilde = Vedtaksloesning.PESYS,
                    virkningstidspunkt =
                        Virkningstidspunkt(
                            dato = YearMonth.now(),
                            kilde = Grunnlagsopplysning.Saksbehandler("PESYS", Tidspunkt.now()),
                            begrunnelse = "",
                        ),
                    sakId = it.id,
                ),
            )
        }

        // Manuelt migrert fra pesys
        sakRepo.opprettSak("222", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).let {
            behandlingRepo.opprettBehandling(
                opprettBehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.OPPRETTET,
                    kilde = Vedtaksloesning.PESYS,
                    virkningstidspunkt =
                        Virkningstidspunkt(
                            dato = YearMonth.now(),
                            kilde = Grunnlagsopplysning.Saksbehandler("saksbehandler", Tidspunkt.now()),
                            begrunnelse = "",
                        ),
                    sakId = it.id,
                ),
            )
        }
    }
}
