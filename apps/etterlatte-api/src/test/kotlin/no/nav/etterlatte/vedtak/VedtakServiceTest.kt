package no.nav.etterlatte.vedtak

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.Regelverk
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class VedtakServiceTest {
    private val klient = mockk<VedtaksvurderingKlient>()
    private val service = VedtakService(klient)

    @Test
    fun `skal mappe vedtak til dto`() {
        val fnr = Folkeregisteridentifikator.of("16888697822")

        coEvery { klient.hentVedtak(fnr) } returns
            listOf(
                vedtak(
                    VedtakType.INNVILGELSE,
                    listOf(
                        utbetalingsperiode(
                            fom = YearMonth.of(2024, 6),
                            tom = YearMonth.of(2024, 12),
                            BigDecimal(1000),
                        ),
                        utbetalingsperiode(
                            fom = YearMonth.of(2025, 1),
                            tom = null,
                            BigDecimal(2000),
                        ),
                    ),
                ),
            )

        val result =
            runBlocking {
                service.hentVedtak(fnr)
            }

        result shouldBe
            VedtakTilPerson(
                listOf(
                    Vedtak(
                        sakId = 123L,
                        sakType = SakType.OMSTILLINGSSTOENAD.name,
                        virkningstidspunkt = LocalDate.of(2024, 6, 1),
                        type = no.nav.etterlatte.vedtak.VedtakType.INNVILGELSE,
                        utbetaling =
                            listOf(
                                VedtakUtbetaling(
                                    fraOgMed = LocalDate.of(2024, 6, 1),
                                    tilOgMed = LocalDate.of(2024, 12, 31),
                                    beloep = BigDecimal(1000),
                                ),
                                VedtakUtbetaling(
                                    fraOgMed = LocalDate.of(2025, 1, 1),
                                    tilOgMed = null,
                                    beloep = BigDecimal(2000),
                                ),
                            ),
                    ),
                ),
            )
    }

    @Test
    fun `skal filtrere ut behandlinger med utbetaling`() {
        val fnr = Folkeregisteridentifikator.of("16888697822")

        coEvery { klient.hentVedtak(fnr) } returns
            listOf(
                vedtak(VedtakType.INNVILGELSE, emptyList()),
                vedtak(VedtakType.AVSLAG, emptyList()),
                vedtak(VedtakType.ENDRING, emptyList()),
                vedtak(VedtakType.OPPHOER, emptyList()),
                vedtak(VedtakType.AVVIST_KLAGE, emptyList()),
                vedtak(VedtakType.TILBAKEKREVING, emptyList()),
            )

        val result =
            runBlocking {
                service.hentVedtak(fnr)
            }

        result.vedtak.size shouldBe 4
        result.vedtak.map { it.type }.let {
            it shouldContain no.nav.etterlatte.vedtak.VedtakType.INNVILGELSE
            it shouldContain no.nav.etterlatte.vedtak.VedtakType.AVSLAG
            it shouldContain no.nav.etterlatte.vedtak.VedtakType.ENDRING
            it shouldContain no.nav.etterlatte.vedtak.VedtakType.OPPHOER
        }
    }

    companion object {
        fun vedtak(
            vedtakstype: VedtakType,
            utbetalingsperioder: List<Utbetalingsperiode>,
        ): VedtakDto {
            val behandlingId = UUID.randomUUID()
            return VedtakDto(
                id = 123L,
                behandlingId = behandlingId,
                status = VedtakStatus.IVERKSATT,
                type = vedtakstype,
                sak =
                    VedtakSak(
                        id = SakId(123L),
                        ident = "",
                        sakType = SakType.OMSTILLINGSSTOENAD,
                    ),
                vedtakFattet = null,
                attestasjon = null,
                innhold =
                    VedtakInnholdDto.VedtakBehandlingDto(
                        virkningstidspunkt = YearMonth.of(2024, 6),
                        behandling =
                            Behandling(
                                id = behandlingId,
                                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                revurderingsaarsak = null,
                            ),
                        utbetalingsperioder = utbetalingsperioder,
                        opphoerFraOgMed = null,
                    ),
            )
        }

        fun utbetalingsperiode(
            fom: YearMonth,
            tom: YearMonth?,
            beloep: BigDecimal = BigDecimal(100),
        ): Utbetalingsperiode =
            Utbetalingsperiode(
                periode = Periode(fom = fom, tom = tom),
                beloep = beloep,
                type = UtbetalingsperiodeType.UTBETALING,
                regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
            )
    }
}
