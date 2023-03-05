package vedtaksvurdering

import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.VedtakStatus
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VedtaksvurderingRepositoryTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")
    private lateinit var dataSource: DataSource
    private lateinit var repository: VedtaksvurderingRepository

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()

        dataSource = DataSourceBuilder.createDataSource(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password
        ).also { it.migrate() }

        repository = VedtaksvurderingRepository(dataSource)
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `skal opprette vedtak`() {
        val nyttVedtak = nyttVedtak()

        val vedtak = repository.opprettVedtak(nyttVedtak)

        with(vedtak) {
            vedtak shouldNotBe null
            id shouldNotBe null
            status shouldBe VedtakStatus.OPPRETTET
            soeker shouldBe Foedselsnummer.of(FNR_1)
            sakId shouldBe 1L
            sakType shouldBe SakType.BARNEPENSJON
            behandlingId shouldNotBe null
            behandlingType shouldBe BehandlingType.FØRSTEGANGSBEHANDLING
            vedtakType shouldBe VedtakType.INNVILGELSE
            virkningstidspunkt shouldBe YearMonth.of(2023, Month.JANUARY)
            vilkaarsvurdering shouldBe objectMapper.createObjectNode()
            beregning shouldBe objectMapper.createObjectNode()
            utbetalingsperioder.first().let { utbetalingsperiode ->
                utbetalingsperiode.id shouldNotBe null
                utbetalingsperiode.periode shouldBe Periode(YearMonth.of(2023, Month.JANUARY), null)
                utbetalingsperiode.beloep shouldBe BigDecimal.valueOf(100L)
                utbetalingsperiode.type shouldBe UtbetalingsperiodeType.UTBETALING
            }
        }
    }

    @Test
    fun `skal oppdatere vedtak`() {
        val nyttVedtak = nyttVedtak()

        val vedtak = repository.opprettVedtak(nyttVedtak)

        val nyttVirkningstidspunkt = YearMonth.of(2023, Month.MARCH)
        val oppdatertVedtak = vedtak.copy(virkningstidspunkt = nyttVirkningstidspunkt)
        repository.oppdaterVedtak(vedtak)

        oppdatertVedtak shouldNotBe null
        oppdatertVedtak.virkningstidspunkt shouldBe nyttVirkningstidspunkt
    }

    @Test
    fun `skal opprette og hente vedtak`() {
        val nyttVedtak = nyttVedtak()

        repository.opprettVedtak(nyttVedtak)

        val vedtak = repository.hentVedtak(nyttVedtak.behandlingId)

        vedtak shouldNotBe null
    }

    @Test
    fun `skal fatte vedtak`() {
        val nyttVedtak = nyttVedtak()

        val vedtak = repository.opprettVedtak(nyttVedtak)

        val fattetVedtak = repository.fattVedtak(
            vedtak.behandlingId,
            VedtakFattet(SAKSBEHANDLER_1, ENHET_1, Tidspunkt.now().toNorskTid())
        )

        fattetVedtak.vedtakFattet shouldNotBe null
        fattetVedtak.status shouldBe VedtakStatus.FATTET_VEDTAK
        with(fattetVedtak.vedtakFattet!!) {
            ansvarligSaksbehandler shouldBe SAKSBEHANDLER_1
            ansvarligEnhet shouldBe ENHET_1
            tidspunkt shouldNotBe null
        }
    }

    @Test
    fun `skal attestere vedtak`() {
        val nyttVedtak = nyttVedtak()

        val vedtak = repository.opprettVedtak(nyttVedtak)

        repository.fattVedtak(
            vedtak.behandlingId,
            VedtakFattet(SAKSBEHANDLER_1, ENHET_1, Tidspunkt.now().toNorskTid())
        )

        val attestertVedtak = repository.attesterVedtak(
            vedtak.behandlingId,
            Attestasjon(SAKSBEHANDLER_2, ENHET_2, Tidspunkt.now().toNorskTid())
        )

        attestertVedtak.attestasjon shouldNotBe null
        attestertVedtak.status shouldBe VedtakStatus.ATTESTERT
        with(attestertVedtak.attestasjon!!) {
            attestant shouldBe SAKSBEHANDLER_2
            attesterendeEnhet shouldBe ENHET_2
            tidspunkt shouldNotBe null
        }
    }

    @Test
    fun `skal sette vedtak iverksatt`() {
        val nyttVedtak = nyttVedtak()

        val vedtak = repository.opprettVedtak(nyttVedtak)

        repository.fattVedtak(
            vedtak.behandlingId,
            VedtakFattet(SAKSBEHANDLER_1, ENHET_1, Tidspunkt.now().toNorskTid())
        )

        repository.attesterVedtak(
            vedtak.behandlingId,
            Attestasjon(SAKSBEHANDLER_2, ENHET_2, Tidspunkt.now().toNorskTid())
        )

        val iverksattVedtak = repository.iverksattVedtak(vedtak.behandlingId)

        iverksattVedtak shouldNotBe null
        iverksattVedtak.status shouldBe VedtakStatus.IVERKSATT
    }

    @Test
    fun `skal underkjenne vedtak`() {
        val nyttVedtak = nyttVedtak()

        val vedtak = repository.opprettVedtak(nyttVedtak)

        repository.fattVedtak(
            vedtak.behandlingId,
            VedtakFattet(SAKSBEHANDLER_1, ENHET_1, Tidspunkt.now().toNorskTid())
        )

        val underkjentVedtak = repository.underkjennVedtak(
            vedtak.behandlingId
        )

        underkjentVedtak shouldNotBe null
        underkjentVedtak.vedtakFattet shouldBe null
        underkjentVedtak.attestasjon shouldBe null
        underkjentVedtak.status shouldBe VedtakStatus.RETURNERT
    }

    @Test
    fun `skal hente vedtak for sak`() {
        val sakId = 1L
        val nyeVedtak = listOf(
            nyttVedtak(sakId = sakId),
            nyttVedtak(sakId = 2),
            nyttVedtak(sakId = sakId),
            nyttVedtak(sakId = sakId)
        )
        nyeVedtak.forEach { repository.opprettVedtak(it) }

        val vedtakForSak = repository.hentVedtakForSak(sakId)

        vedtakForSak.size shouldBeExactly 3
        vedtakForSak.forEach { it.sakId shouldBe sakId }
    }
}