package no.nav.etterlatte.metrics

import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Gauge
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
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
import no.nav.etterlatte.sak.SakDao
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
    private lateinit var sakRepo: SakDao
    private lateinit var behandlingMetrics: BehandlingMetrics

    private val testreg = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    @BeforeAll
    fun beforeAll() {
        sakRepo = SakDao(ConnectionAutoclosingTest(ds))
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
    fun `Henter riktig antall totalt`() {
        val metrikker = metrikker("etterlatte_behandlinger")
        metrikker.size shouldBe 5
    }

    @Test
    fun `Henter riktig antall for saktype`() {
        val metrikker = metrikker("etterlatte_behandlinger")
        hentVerdi(metrikker, "saktype", SakType.BARNEPENSJON.name) shouldBe 4
        hentVerdi(metrikker, "saktype", SakType.OMSTILLINGSSTOENAD.name) shouldBe 1
    }

    @Test
    fun `Henter riktig antall for behandlingstyper`() {
        val metrikker = metrikker("etterlatte_behandlinger")
        hentVerdi(metrikker, "behandlingstype", BehandlingType.FØRSTEGANGSBEHANDLING.name) shouldBe 4
        hentVerdi(metrikker, "behandlingstype", BehandlingType.REVURDERING.name) shouldBe 1
    }

    @Test
    fun `Henter riktig antall for status`() {
        val metrikker = metrikker("etterlatte_behandlinger")
        hentVerdi(metrikker, "status", BehandlingStatus.IVERKSATT.name) shouldBe 2
        hentVerdi(metrikker, "status", BehandlingStatus.OPPRETTET.name) shouldBe 3
    }

    @Test
    fun `Henter riktig antall for revuderingsaarsak`() {
        val metrikker = metrikker("etterlatte_behandlinger")
        hentVerdi(metrikker, "revurdering_aarsak", "null") shouldBe 4
        hentVerdi(metrikker, "revurdering_aarsak", Revurderingaarsak.REGULERING.name) shouldBe 1
    }

    @Test
    fun `Henter riktig antall for kilde`() {
        val metrikker = metrikker("etterlatte_behandlinger")
        hentVerdi(metrikker, "kilde", Vedtaksloesning.GJENNY.name) shouldBe 3
        hentVerdi(metrikker, "kilde", Vedtaksloesning.PESYS.name) shouldBe 2
    }

    @Test
    fun `Henter riktig antall for automatiskMigrert`() {
        val metrikker = metrikker("etterlatte_behandlinger")
        hentVerdi(metrikker, "automatiskMigrering", "true") shouldBe 1
        hentVerdi(metrikker, "automatiskMigrering", "false") shouldBe 4
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
}
