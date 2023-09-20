package no.nav.etterlatte.samordning.vedtak

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.LocalDate
import java.time.YearMonth

class SamordningVedtakService(
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
) {
    suspend fun hentVedtak(
        vedtakId: Long,
        organisasjonsnummer: String,
    ): SamordningVedtakDto {
        val vedtak = vedtaksvurderingKlient.hentVedtak(vedtakId, organisasjonsnummer)

        if (vedtak.sak.sakType != SakType.OMSTILLINGSSTOENAD) {
            throw IllegalArgumentException("Ikke tilgang til vedtak")
        }

        return with(vedtak) {
            SamordningVedtakDto(
                vedtakId = vedtakId,
                sakstype = "OMS",
                virkningsdato = virkningstidspunkt.toLocalDate(),
                opphoersdato = null,
                type = type.toSamordningsvedtakType(),
                aarsak = null,
                anvendtTrygdetid = 40,
                perioder = utbetalingsperioder.map { it.toSamordningVedtakPeriode() },
            )
        }
    }

    private fun VedtakType.toSamordningsvedtakType(): SamordningVedtakType {
        return when (this) {
            VedtakType.INNVILGELSE -> SamordningVedtakType.START
            VedtakType.OPPHOER -> SamordningVedtakType.OPPHOER
            VedtakType.ENDRING -> SamordningVedtakType.ENDRING
            VedtakType.AVSLAG -> throw IllegalArgumentException("Vedtak om avslag er ikke aktuelt i tjenesten")
        }
    }

    // FIXME beloep
    private fun Utbetalingsperiode.toSamordningVedtakPeriode(): SamordningVedtakPeriode {
        return SamordningVedtakPeriode(
            fom = periode.fom.toLocalDate(),
            tom = periode.tom?.toLocalDate(),
            omstillingsstoenadBrutto = beloep?.toInt() ?: 0,
            omstillingsstoenadNetto = 0,
        )
    }

    private fun YearMonth.toLocalDate(): LocalDate {
        return LocalDate.of(year, month, 1)
    }
}
