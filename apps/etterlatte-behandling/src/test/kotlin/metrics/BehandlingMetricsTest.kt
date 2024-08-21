package no.nav.etterlatte.metrics

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.prometheus.client.CollectorRegistry
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
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
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakSkrivDao
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.YearMonth
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class BehandlingMetricsTest(
    private val ds: DataSource,
) {
    private lateinit var behandlingMetrikkerDao: BehandlingMetrikkerDao
    private lateinit var oppgaveDao: OppgaveMetrikkerDao
    private lateinit var behandlingRepo: BehandlingDao
    private lateinit var gjenopprettingDao: GjenopprettingMetrikkerDao
    private lateinit var sakRepo: SakSkrivDao
    private lateinit var behandlingMetrics: BehandlingMetrics

    private val testreg = CollectorRegistry(true)

    @BeforeAll
    fun beforeAll() {
        sakRepo = SakSkrivDao(ConnectionAutoclosingTest(ds))
        behandlingRepo =
            BehandlingDao(
                kommerBarnetTilGodeDao = mockk(),
                revurderingDao = mockk(),
                ConnectionAutoclosingTest(ds),
            )

        opprettBehandlinger()

        behandlingMetrikkerDao = BehandlingMetrikkerDao(ds)
        oppgaveDao = OppgaveMetrikkerDao(ds)
        gjenopprettingDao = GjenopprettingMetrikkerDao(ds)
        behandlingMetrics = BehandlingMetrics(oppgaveDao, behandlingMetrikkerDao, gjenopprettingDao, testreg)

        behandlingMetrics.run()
    }

    @Test
    fun `Metrikker for behandlinger skal ha labels i riktig rekkefoelge`() {
        val metrikker =
            behandlingMetrics.behandlinger
                .collect()
                .first()
                .samples
        metrikker.first().labelNames shouldContainExactly
            listOf(
                "saktype",
                "behandlingstype",
                "status",
                "revurdering_aarsak",
                "kilde",
                "automatiskMigrering",
            )
    }

    @Test
    fun `Henter riktig antall totalt`() {
        val metrikker =
            behandlingMetrics.behandlinger
                .collect()
                .first()
                .samples
        metrikker.size shouldBe 5
    }

    @Test
    fun `Henter riktig antall for saktype`() {
        val metrikker =
            behandlingMetrics.behandlinger
                .collect()
                .first()
                .samples
        metrikker.filter { it.labelValues[0] == SakType.BARNEPENSJON.name }.size shouldBe 4
        metrikker.filter { it.labelValues[0] == SakType.OMSTILLINGSSTOENAD.name }.size shouldBe 1
    }

    @Test
    fun `Henter riktig antall for behandlingstyper`() {
        val metrikker =
            behandlingMetrics.behandlinger
                .collect()
                .first()
                .samples
        metrikker.filter { it.labelValues[1] == BehandlingType.FØRSTEGANGSBEHANDLING.name }.size shouldBe 4
        metrikker.filter { it.labelValues[1] == BehandlingType.REVURDERING.name }.size shouldBe 1
    }

    @Test
    fun `Henter riktig antall for status`() {
        val metrikker =
            behandlingMetrics.behandlinger
                .collect()
                .first()
                .samples
        metrikker.filter { it.labelValues[2] == BehandlingStatus.IVERKSATT.name }.size shouldBe 2
        metrikker.filter { it.labelValues[2] == BehandlingStatus.OPPRETTET.name }.size shouldBe 3
    }

    @Test
    fun `Henter riktig antall for revuderingsaarsak`() {
        val metrikker =
            behandlingMetrics.behandlinger
                .collect()
                .first()
                .samples
        metrikker.filter { it.labelValues[3] == "null" }.size shouldBe 4 // Førstegangsbehandling
        metrikker.filter { it.labelValues[3] == Revurderingaarsak.REGULERING.name }.size shouldBe 1
    }

    @Test
    fun `Henter riktig antall for kilde`() {
        val metrikker =
            behandlingMetrics.behandlinger
                .collect()
                .first()
                .samples
        metrikker.filter { it.labelValues[4] == Vedtaksloesning.GJENNY.name }.size shouldBe 3
        metrikker.filter { it.labelValues[4] == Vedtaksloesning.PESYS.name }.size shouldBe 2
    }

    @Test
    fun `Henter riktig antall for automatiskMigrert`() {
        val metrikker =
            behandlingMetrics.behandlinger
                .collect()
                .first()
                .samples
        metrikker.filter { it.labelValues[5] == "true" }.size shouldBe 1
        metrikker.filter { it.labelValues[5] == "false" }.size shouldBe 4
    }

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

        sakRepo.opprettSak("321", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr).let {
            behandlingRepo.opprettBehandling(
                opprettBehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
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
