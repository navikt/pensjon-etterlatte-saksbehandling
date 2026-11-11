package no.nav.etterlatte.behandling.vedtaksvurdering.service

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.vedtaksvurdering.Vedtak
import no.nav.etterlatte.behandling.vedtaksvurdering.VedtaksvurderingRepository
import no.nav.etterlatte.behandling.vedtaksvurdering.vedtak
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.VedtakEtteroppgjoerPeriode
import no.nav.etterlatte.libs.common.vedtak.VedtakSamordningDto
import no.nav.etterlatte.libs.common.vedtak.VedtakSamordningPeriode
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.security.SecureRandom
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class VedtakEtteroppgjoerServiceTest {
    private val vedtaksvurderingRepository = mockk<VedtaksvurderingRepository>()
    private val vedtakSamordningService = mockk<VedtakSamordningService>()
    private val vedtakEtteroppgjoerService =
        VedtakEtteroppgjoerService(
            repository = vedtaksvurderingRepository,
            vedtakSamordningService = vedtakSamordningService,
        )

    @AfterEach
    fun tearDown() {
        confirmVerified()
    }

    @Test
    fun `hentVedtakslisteIEtteroppgjoersAar skal bare hente vedtak som paavirker utbetaling`() {
        val sakId = randomSakId()
        val (vedtak1, vedtak2, vedtak3) =
            listOf(
                omsVedtak(
                    sakId = sakId,
                    status = VedtakStatus.ATTESTERT,
                    type = VedtakType.AVSLAG,
                ),
                omsVedtak(
                    sakId = sakId,
                    status = VedtakStatus.IVERKSATT,
                    type = VedtakType.INNVILGELSE,
                ),
                omsVedtak(
                    sakId = sakId,
                    status = VedtakStatus.FATTET_VEDTAK,
                    type = VedtakType.ENDRING,
                ),
            )
        every { vedtaksvurderingRepository.hentVedtakForSak(sakId) } returns listOf(vedtak1, vedtak2, vedtak3)

        val tidslinjeJustertMocked =
            listOf(
                vedtakSamordningDto(
                    vedtak = vedtak1,
                    utbetalingsperioder =
                        listOf(
                            VedtakSamordningPeriode(
                                fom = YearMonth.of(2025, 8),
                                tom = null,
                                ytelseFoerAvkorting = 110,
                                ytelseEtterAvkorting = 100,
                            ),
                        ),
                ),
                vedtakSamordningDto(
                    vedtak = vedtak2,
                    utbetalingsperioder =
                        listOf(
                            VedtakSamordningPeriode(
                                fom = YearMonth.of(2025, 9),
                                tom = null,
                                ytelseFoerAvkorting = 210,
                                ytelseEtterAvkorting = 200,
                            ),
                        ),
                ),
            )
        val etteroppgjoersAarDag1 = LocalDate.of(2025, 1, 1)
        val tilOgMedBehandlingId = UUID.randomUUID()
        every {
            vedtakSamordningService.tidslinjejustertVedtaksliste(any(), tilOgMedBehandlingId, etteroppgjoersAarDag1)
        } returns
            tidslinjeJustertMocked

        val result =
            vedtakEtteroppgjoerService.hentVedtakslisteIEtteroppgjoersAar(
                sakId = sakId,
                etteroppgjoersAar = 2025,
                tilOgMedBehandlingId = tilOgMedBehandlingId,
            )

        // sjekk at mappingen av retur fra samordning er ok
        result.map { it.vedtakId } shouldContainExactlyInAnyOrder listOf(vedtak1.id, vedtak2.id)
        result.forEach { vedtak ->
            val lagretPeriode: VedtakSamordningPeriode =
                tidslinjeJustertMocked.first { it.vedtakId == vedtak.vedtakId }.perioder.single()
            val utledetPeriode: VedtakEtteroppgjoerPeriode = vedtak.perioder.single()
            utledetPeriode.fom shouldBe lagretPeriode.fom
            utledetPeriode.tom shouldBe lagretPeriode.tom
            utledetPeriode.ytelseEtterAvkorting shouldBe lagretPeriode.ytelseEtterAvkorting
        }

        // sjekk at vi baserer oss på iverksatte / attesterte avslag
        verify { vedtaksvurderingRepository.hentVedtakForSak(sakId) }
        val slotVedtakliste = slot<List<Vedtak>>()
        verify { vedtakSamordningService.tidslinjejustertVedtaksliste(capture(slotVedtakliste), any(), any()) }
        slotVedtakliste.captured.map { it.id } shouldContainExactlyInAnyOrder
            listOf(
                vedtak1.id,
                vedtak2.id,
            )
    }

    private fun vedtakSamordningDto(
        vedtak: Vedtak,
        utbetalingsperioder: List<VedtakSamordningPeriode>,
    ): VedtakSamordningDto =
        VedtakSamordningDto(
            vedtakId = vedtak.id,
            fnr = "dontcare",
            status = VedtakStatus.IVERKSATT,
            virkningstidspunkt = YearMonth.now(),
            sak = VedtakSak(ident = "123", SakType.OMSTILLINGSSTOENAD, id = vedtak.sakId),
            behandling = Behandling(id = UUID.randomUUID(), type = BehandlingType.FØRSTEGANGSBEHANDLING),
            type = VedtakType.INNVILGELSE,
            vedtakFattet = null,
            attestasjon = null,
            perioder = utbetalingsperioder,
            beregning = null,
        )

    private fun omsVedtak(
        sakId: SakId,
        status: VedtakStatus,
        type: VedtakType,
    ): Vedtak =
        vedtak(
            id = SecureRandom().nextLong(Long.MAX_VALUE),
            sakId = sakId,
            sakType = SakType.OMSTILLINGSSTOENAD,
            status = status,
            type = type,
        )
}
