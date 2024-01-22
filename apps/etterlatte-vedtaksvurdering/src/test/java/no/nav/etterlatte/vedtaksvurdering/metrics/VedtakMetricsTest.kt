package no.nav.etterlatte.vedtaksvurdering.metrics

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.prometheus.client.CollectorRegistry
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.metrics.VedtakMetrics
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.metrics.VedtakMetrikkerDao
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository
import no.nav.etterlatte.vedtaksvurdering.opprettVedtak
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VedtakMetricsTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

    private lateinit var vedtakRepo: VedtaksvurderingRepository

    private lateinit var vedtakMetrikkerDao: VedtakMetrikkerDao
    private lateinit var vedtakMetrics: VedtakMetrics

    private val testreg = CollectorRegistry(true)

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()

        val ds =
            DataSourceBuilder.createDataSource(
                postgreSQLContainer.jdbcUrl,
                postgreSQLContainer.username,
                postgreSQLContainer.password,
            ).also { it.migrate() }

        vedtakRepo = VedtaksvurderingRepository.using(ds)
        opprettLoependeVedtak()

        vedtakMetrikkerDao = VedtakMetrikkerDao.using(ds)
        vedtakMetrics = VedtakMetrics(vedtakMetrikkerDao, testreg)

        vedtakMetrics.run()
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `Metrikker for loepende vedtak skal ha labels`() {
        val metrikker = vedtakMetrics.loependeVedtak.collect().first().samples
        metrikker.first().labelNames shouldContainExactly listOf("saktype")
    }

    @Test
    fun `Metrikker for loepende vedtak skal ha riktig antall`() {
        val metrikker = vedtakMetrics.loependeVedtak.collect().first().samples
        metrikker.first { it.labelValues[0] == SakType.BARNEPENSJON.name }.value shouldBe 2
        metrikker.first { it.labelValues[0] == SakType.OMSTILLINGSSTOENAD.name }.value shouldBe 1
    }

    private fun opprettLoependeVedtak() {
        vedtakRepo.opprettVedtak(opprettVedtak(sakId = 1, status = VedtakStatus.IVERKSATT))
        vedtakRepo.opprettVedtak(opprettVedtak(sakId = 2, status = VedtakStatus.IVERKSATT))
        vedtakRepo.opprettVedtak(
            opprettVedtak(
                sakId = 3,
                status = VedtakStatus.IVERKSATT,
                sakType = SakType.OMSTILLINGSSTOENAD,
            ),
        )
        vedtakRepo.opprettVedtak(
            opprettVedtak(
                sakId = 4,
                status = VedtakStatus.IVERKSATT,
                sakType = SakType.OMSTILLINGSSTOENAD,
            ),
        )
        vedtakRepo.opprettVedtak(
            opprettVedtak(
                sakId = 4,
                status = VedtakStatus.IVERKSATT,
                sakType = SakType.OMSTILLINGSSTOENAD,
                type = VedtakType.OPPHOER,
            ),
        )
    }
}
