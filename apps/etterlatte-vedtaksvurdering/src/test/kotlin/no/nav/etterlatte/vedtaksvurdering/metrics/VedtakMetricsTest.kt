package no.nav.etterlatte.vedtaksvurdering.metrics

import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Gauge
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.behandling.sakId2
import no.nav.etterlatte.behandling.sakId3
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.metrics.VedtakMetrics
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.metrics.VedtakMetrikkerDao
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository
import no.nav.etterlatte.vedtaksvurdering.database.DatabaseExtension
import no.nav.etterlatte.vedtaksvurdering.opprettVedtak
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class VedtakMetricsTest(
    private val dataSource: DataSource,
) {
    private lateinit var vedtakRepo: VedtaksvurderingRepository

    private lateinit var vedtakMetrikkerDao: VedtakMetrikkerDao
    private lateinit var vedtakMetrics: VedtakMetrics

    private val testreg = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    @BeforeAll
    fun beforeAll() {
        vedtakRepo = VedtaksvurderingRepository.using(dataSource)
        opprettLoependeVedtak()

        vedtakMetrikkerDao = VedtakMetrikkerDao.using(dataSource)
        vedtakMetrics = VedtakMetrics(vedtakMetrikkerDao, testreg)

        vedtakMetrics.run()
    }

    @Test
    fun `Metrikker for loepende vedtak skal ha riktig antall`() {
        val metrikker = testreg.get("etterlatte_vedtak_loepende").gauges()
        hentVerdi(metrikker, "saktype", SakType.BARNEPENSJON.name) shouldBe 2
        hentVerdi(metrikker, "saktype", SakType.OMSTILLINGSSTOENAD.name) shouldBe 1
    }

    private fun opprettLoependeVedtak() {
        vedtakRepo.opprettVedtak(opprettVedtak(sakId = sakId1, status = VedtakStatus.IVERKSATT))
        vedtakRepo.opprettVedtak(opprettVedtak(sakId = sakId2, status = VedtakStatus.IVERKSATT))
        vedtakRepo.opprettVedtak(
            opprettVedtak(
                sakId = sakId3,
                status = VedtakStatus.IVERKSATT,
                sakType = SakType.OMSTILLINGSSTOENAD,
            ),
        )
        vedtakRepo.opprettVedtak(
            opprettVedtak(
                sakId = SakId(4),
                status = VedtakStatus.IVERKSATT,
                sakType = SakType.OMSTILLINGSSTOENAD,
            ),
        )
        vedtakRepo.opprettVedtak(
            opprettVedtak(
                sakId = SakId(4),
                status = VedtakStatus.IVERKSATT,
                sakType = SakType.OMSTILLINGSSTOENAD,
                type = VedtakType.OPPHOER,
            ),
        )
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
}
