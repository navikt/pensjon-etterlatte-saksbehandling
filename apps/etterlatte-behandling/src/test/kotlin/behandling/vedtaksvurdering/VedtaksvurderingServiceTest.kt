package no.nav.etterlatte.behandling.vedtaksvurdering

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtaksvurderingService
import no.nav.etterlatte.libs.common.Regelverk
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth
import java.util.UUID

internal class VedtaksvurderingServiceTest {
    private val repository = mockk<VedtaksvurderingRepositoryOperasjoner>()
    private val service = VedtaksvurderingService(repository)

    @Nested
    inner class HentInnvilgedePerioder {
        @Test
        fun `hentInnvilgedePerioder returnerer tom liste naar saken kun har avslag`() {
            val avslagsVedtak =
                lagIverksattVedtak(
                    id = 1,
                    virkningstidspunkt = YearMonth.of(2024, Month.JANUARY),
                    vedtakType = VedtakType.AVSLAG,
                )

            every { repository.hentVedtakForSak(sakId1) } returns listOf(avslagsVedtak)

            val innvilgedePerioder = service.hentInnvilgedePerioder(sakId1)

            innvilgedePerioder shouldBe emptyList()
        }

        @Test
        fun `hentInnvilgedePerioder returnerer innvilget periode naar saken har innvilgelse`() {
            val innvilgelsesVedtak =
                lagIverksattVedtak(
                    id = 1,
                    virkningstidspunkt = YearMonth.of(2024, Month.JANUARY),
                    vedtakType = VedtakType.INNVILGELSE,
                )

            every { repository.hentVedtakForSak(sakId1) } returns listOf(innvilgelsesVedtak)

            val innvilgedePerioder = service.hentInnvilgedePerioder(sakId1)

            innvilgedePerioder.size shouldBe 1
            innvilgedePerioder[0].periode.fom shouldBe YearMonth.of(2024, Month.JANUARY)
        }

        @Test
        fun `hentInnvilgedePerioder ignorerer avslag og returnerer kun innvilgede perioder`() {
            val attestertTidspunkt = Tidspunkt.now()
            val avslagsVedtak =
                lagIverksattVedtak(
                    id = 1,
                    virkningstidspunkt = YearMonth.of(2024, Month.JANUARY),
                    vedtakType = VedtakType.AVSLAG,
                    attestertTidspunkt = attestertTidspunkt,
                )
            val innvilgelsesVedtak =
                lagIverksattVedtak(
                    id = 2,
                    virkningstidspunkt = YearMonth.of(2024, Month.JUNE),
                    vedtakType = VedtakType.INNVILGELSE,
                    attestertTidspunkt = attestertTidspunkt.plus(1, java.time.temporal.ChronoUnit.HOURS),
                )

            every { repository.hentVedtakForSak(sakId1) } returns listOf(avslagsVedtak, innvilgelsesVedtak)

            val innvilgedePerioder = service.hentInnvilgedePerioder(sakId1)

            innvilgedePerioder.size shouldBe 1
            innvilgedePerioder[0].periode.fom shouldBe YearMonth.of(2024, Month.JUNE)
        }

        @Test
        fun `hentInnvilgedePerioder ignorerer avslag og returnerer tom liste`() {
            val attestertTidspunkt = Tidspunkt.now()
            val avslagsVedtak =
                lagIverksattVedtak(
                    id = 1,
                    virkningstidspunkt = YearMonth.of(2024, Month.JANUARY),
                    vedtakType = VedtakType.AVSLAG,
                    attestertTidspunkt = attestertTidspunkt,
                )

            every { repository.hentVedtakForSak(sakId1) } returns listOf(avslagsVedtak)

            val innvilgedePerioder = service.hentInnvilgedePerioder(sakId1)

            innvilgedePerioder.size shouldBe 0
        }
    }

    private fun lagIverksattVedtak(
        id: Long,
        virkningstidspunkt: YearMonth,
        vedtakType: VedtakType,
        behandlingId: UUID = UUID.randomUUID(),
        attestertTidspunkt: Tidspunkt = Tidspunkt.now(),
    ): Vedtak =
        Vedtak(
            id = id,
            sakId = sakId1,
            sakType = SakType.OMSTILLINGSSTOENAD,
            behandlingId = behandlingId,
            soeker = SOEKER_FOEDSELSNUMMER,
            status = VedtakStatus.IVERKSATT,
            type = vedtakType,
            vedtakFattet =
                VedtakFattet(
                    ansvarligSaksbehandler = SAKSBEHANDLER_1,
                    ansvarligEnhet = ENHET_1,
                    tidspunkt = attestertTidspunkt,
                ),
            attestasjon =
                Attestasjon(
                    attestant = SAKSBEHANDLER_2,
                    attesterendeEnhet = ENHET_2,
                    tidspunkt = attestertTidspunkt,
                ),
            innhold =
                VedtakInnhold.Behandling(
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    revurderingAarsak = null,
                    virkningstidspunkt = virkningstidspunkt,
                    beregning = null,
                    avkorting = null,
                    vilkaarsvurdering = null,
                    utbetalingsperioder =
                        if (vedtakType != VedtakType.AVSLAG) {
                            listOf(
                                Utbetalingsperiode(
                                    id = id * 10,
                                    periode = Periode(virkningstidspunkt, null),
                                    beloep = BigDecimal.valueOf(100),
                                    type = UtbetalingsperiodeType.UTBETALING,
                                    regelverk = Regelverk.fraDato(virkningstidspunkt.atDay(1)),
                                ),
                            )
                        } else {
                            emptyList()
                        },
                ),
        )
}
