package vedtaksvurdering

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.beInstanceOf
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.vedtaksvurdering.VedtakBehandlingInnhold
import no.nav.etterlatte.vedtaksvurdering.VedtakTilbakekrevingInnhold
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VedtaksvurderingRepositoryTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
    private lateinit var dataSource: DataSource
    private lateinit var repository: VedtaksvurderingRepository

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()

        dataSource =
            DataSourceBuilder.createDataSource(
                postgreSQLContainer.jdbcUrl,
                postgreSQLContainer.username,
                postgreSQLContainer.password,
            ).also { it.migrate() }

        repository = VedtaksvurderingRepository(dataSource)
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `skal opprette vedtak for behandling`() {
        val nyttVedtak = opprettVedtak()

        val vedtak = repository.opprettVedtak(nyttVedtak)

        with(vedtak) {
            vedtak shouldNotBe null
            id shouldNotBe null
            status shouldBe VedtakStatus.OPPRETTET
            soeker shouldBe SOEKER_FOEDSELSNUMMER
            sakId shouldBe 1L
            sakType shouldBe SakType.BARNEPENSJON
            behandlingId shouldNotBe null
            type shouldBe VedtakType.INNVILGELSE
            innhold should beInstanceOf<VedtakBehandlingInnhold>()
            (innhold as VedtakBehandlingInnhold).let {
                it.behandlingType shouldBe BehandlingType.FØRSTEGANGSBEHANDLING
                it.virkningstidspunkt shouldBe YearMonth.of(2023, Month.JANUARY)
                it.vilkaarsvurdering shouldBe objectMapper.createObjectNode()
                it.beregning shouldBe objectMapper.createObjectNode()
                it.avkorting shouldBe objectMapper.createObjectNode()
                it.utbetalingsperioder.first().let { utbetalingsperiode ->
                    utbetalingsperiode.id shouldNotBe null
                    utbetalingsperiode.periode shouldBe Periode(YearMonth.of(2023, Month.JANUARY), null)
                    utbetalingsperiode.beloep shouldBe BigDecimal.valueOf(100L)
                    utbetalingsperiode.type shouldBe UtbetalingsperiodeType.UTBETALING
                }
            }
        }
    }

    @Test
    fun `skal opprette vedtak for tilbakekreving`() {
        val nyttVedtak = opprettVedtakTilbakekreving()

        val vedtak = repository.opprettVedtak(nyttVedtak)

        with(vedtak) {
            vedtak shouldNotBe null
            id shouldNotBe null
            status shouldBe VedtakStatus.OPPRETTET
            soeker shouldBe SOEKER_FOEDSELSNUMMER
            sakId shouldBe 1L
            sakType shouldBe SakType.BARNEPENSJON
            behandlingId shouldNotBe null
            type shouldBe VedtakType.TILBAKEKREVING
            innhold should beInstanceOf<VedtakTilbakekrevingInnhold>()
            (innhold as VedtakTilbakekrevingInnhold).tilbakekreving shouldBe objectMapper.createObjectNode()
        }
    }

    @Test
    fun `skal oppdatere vedtak for behandling`() {
        val nyttVedtak = opprettVedtak()

        val vedtak = repository.opprettVedtak(nyttVedtak)

        val nyttVirkningstidspunkt = YearMonth.of(2023, Month.MARCH)

        val oppdatertVedtak =
            repository.oppdaterVedtak(
                vedtak.copy(
                    type = VedtakType.OPPHOER,
                    innhold =
                        (vedtak.innhold as VedtakBehandlingInnhold).copy(
                            virkningstidspunkt = nyttVirkningstidspunkt,
                            utbetalingsperioder =
                                listOf(
                                    Utbetalingsperiode(
                                        periode = Periode(nyttVirkningstidspunkt, null),
                                        beloep = null,
                                        type = UtbetalingsperiodeType.OPPHOER,
                                    ),
                                ),
                        ),
                ),
            )

        oppdatertVedtak shouldNotBe null
        oppdatertVedtak.type shouldBe VedtakType.OPPHOER
        oppdatertVedtak.innhold should beInstanceOf<VedtakBehandlingInnhold>()
        (oppdatertVedtak.innhold as VedtakBehandlingInnhold).let { innhold ->
            innhold.virkningstidspunkt shouldBe nyttVirkningstidspunkt
            innhold.utbetalingsperioder.first().let {
                it.id!! shouldBeGreaterThan 0
                it.periode shouldBe Periode(nyttVirkningstidspunkt, null)
                it.beloep shouldBe null
                it.type shouldBe UtbetalingsperiodeType.OPPHOER
            }
        }
    }

    @Test
    fun `skal oppdatere vedtak for tilbakekreving`() {
        val nyttVedtak = opprettVedtakTilbakekreving()
        val vedtak = repository.opprettVedtak(nyttVedtak)

        val oppdatertTilbakekreving = objectMapper.createObjectNode()
        oppdatertTilbakekreving.replace("endret", objectMapper.createObjectNode())

        val oppdatertVedtak =
            repository.oppdaterVedtak(
                vedtak.copy(
                    innhold =
                        (vedtak.innhold as VedtakTilbakekrevingInnhold).copy(
                            tilbakekreving = oppdatertTilbakekreving,
                        ),
                ),
            )

        oppdatertVedtak shouldNotBe null
        oppdatertVedtak.type shouldBe VedtakType.TILBAKEKREVING
        oppdatertVedtak.innhold should beInstanceOf<VedtakTilbakekrevingInnhold>()
        (oppdatertVedtak.innhold as VedtakTilbakekrevingInnhold).tilbakekreving shouldBe oppdatertTilbakekreving
    }

    @Test
    fun `skal opprette og hente vedtak`() {
        val nyttVedtak = opprettVedtak()

        repository.opprettVedtak(nyttVedtak)

        val vedtak = repository.hentVedtak(nyttVedtak.behandlingId)

        vedtak shouldNotBe null
    }

    @Test
    fun `skal fatte vedtak`() {
        val nyttVedtak = opprettVedtak()

        val vedtak = repository.opprettVedtak(nyttVedtak)

        val fattetVedtak =
            repository.fattVedtak(
                vedtak.behandlingId,
                VedtakFattet(SAKSBEHANDLER_1, ENHET_1, Tidspunkt.now()),
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
        val nyttVedtak = opprettVedtak()

        val vedtak = repository.opprettVedtak(nyttVedtak)

        repository.fattVedtak(
            vedtak.behandlingId,
            VedtakFattet(SAKSBEHANDLER_1, ENHET_1, Tidspunkt.now()),
        )

        val attestertVedtak =
            repository.attesterVedtak(
                vedtak.behandlingId,
                Attestasjon(SAKSBEHANDLER_2, ENHET_2, Tidspunkt.now()),
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
        val nyttVedtak = opprettVedtak()

        val vedtak = repository.opprettVedtak(nyttVedtak)

        repository.fattVedtak(
            vedtak.behandlingId,
            VedtakFattet(SAKSBEHANDLER_1, ENHET_1, Tidspunkt.now()),
        )

        repository.attesterVedtak(
            vedtak.behandlingId,
            Attestasjon(SAKSBEHANDLER_2, ENHET_2, Tidspunkt.now()),
        )

        val iverksattVedtak = repository.iverksattVedtak(vedtak.behandlingId)

        iverksattVedtak shouldNotBe null
        iverksattVedtak.status shouldBe VedtakStatus.IVERKSATT
    }

    @Test
    fun `skal underkjenne vedtak`() {
        val nyttVedtak = opprettVedtak()

        val vedtak = repository.opprettVedtak(nyttVedtak)

        repository.fattVedtak(
            vedtak.behandlingId,
            VedtakFattet(SAKSBEHANDLER_1, ENHET_1, Tidspunkt.now()),
        )

        val underkjentVedtak =
            repository.underkjennVedtak(
                vedtak.behandlingId,
            )

        underkjentVedtak shouldNotBe null
        underkjentVedtak.vedtakFattet shouldBe null
        underkjentVedtak.attestasjon shouldBe null
        underkjentVedtak.status shouldBe VedtakStatus.RETURNERT
    }

    @Test
    fun `skal hente vedtak for sak`() {
        val sakId = 1L
        val nyeVedtak =
            listOf(
                opprettVedtak(sakId = sakId),
                opprettVedtak(sakId = 2),
                opprettVedtak(sakId = sakId),
                opprettVedtak(sakId = sakId),
            )
        nyeVedtak.forEach { repository.opprettVedtak(it) }

        val vedtakForSak = repository.hentVedtakForSak(sakId)

        vedtakForSak.size shouldBeExactly 3
        vedtakForSak.forEach { it.sakId shouldBe sakId }
    }

    @Test
    fun `skal hente vedtak for fnr og fra-og-med angitt dato`() {
        val soeker1 = SOEKER_FOEDSELSNUMMER
        val soeker2 = Folkeregisteridentifikator.of(FNR_2)

        listOf(
            opprettVedtak(
                sakId = 1,
                soeker = soeker1,
                virkningstidspunkt = YearMonth.of(2024, Month.JANUARY),
                status = VedtakStatus.IVERKSATT,
            ),
            opprettVedtak(
                sakId = 2,
                soeker = soeker2,
                virkningstidspunkt = YearMonth.of(2024, Month.JANUARY),
                status = VedtakStatus.IVERKSATT,
            ),
            opprettVedtak(
                sakId = 2,
                soeker = soeker2,
                virkningstidspunkt = YearMonth.of(2024, Month.MARCH),
                status = VedtakStatus.SAMORDNET,
            ),
            opprettVedtak(
                sakId = 1,
                soeker = soeker1,
                virkningstidspunkt = YearMonth.of(2024, Month.APRIL),
                status = VedtakStatus.IVERKSATT,
            ),
            opprettVedtak(
                sakId = 2,
                soeker = soeker2,
                virkningstidspunkt = YearMonth.of(2024, Month.JUNE),
                status = VedtakStatus.TIL_SAMORDNING,
            ),
        )
            .map { repository.opprettVedtak(it) }
            .forEach { repository.iverksattVedtak(it.behandlingId) }

        val results = repository.hentFerdigstilteVedtak(soeker2, LocalDate.of(2024, Month.MARCH, 1))

        results.size shouldBeExactly 2
        results.forEach {
            it.soeker shouldBe soeker2
            (it.innhold as VedtakBehandlingInnhold).virkningstidspunkt shouldBeGreaterThanOrEqualTo
                YearMonth.of(
                    2024,
                    Month.MARCH,
                )
        }
    }
}
