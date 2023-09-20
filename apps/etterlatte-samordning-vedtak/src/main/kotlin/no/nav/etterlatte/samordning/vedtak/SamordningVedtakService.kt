package no.nav.etterlatte.samordning.vedtak

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.vedtak.VedtakSamordningDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.LocalDate

class SamordningVedtakService(
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
) {
    suspend fun hentVedtak(
        vedtakId: Long,
        organisasjonsnummer: String,
    ): SamordningVedtakDto {
        val vedtak = vedtaksvurderingKlient.hentVedtak(vedtakId, organisasjonsnummer)

        if (vedtak.sak.sakType != SakType.OMSTILLINGSSTOENAD) {
            throw VedtakFeilSakstypeException()
        }

        return vedtak.mapSamordningsvedtak()
    }

    suspend fun hentVedtaksliste(
        virkFom: LocalDate,
        fnr: String,
        organisasjonsnummer: String,
    ): List<SamordningVedtakDto> {
        return vedtaksvurderingKlient.hentVedtaksliste(
            virkFom = virkFom,
            fnr = fnr,
            organisasjonsnummer = organisasjonsnummer,
        )
            .map { it.mapSamordningsvedtak() }
    }

    private fun VedtakSamordningDto.mapSamordningsvedtak(): SamordningVedtakDto {
        val beregning = deserialize<BeregningDTO>(beregning.toString())
        val avkorting = deserialize<AvkortingDto>(avkorting.toString())

        return SamordningVedtakDto(
            vedtakId = vedtakId,
            sakstype = "OMS",
            virkningsdato = virkningstidspunkt.atDay(1),
            opphoersdato = null,
            type = type.toSamordningsvedtakType(),
            aarsak = null,
            anvendtTrygdetid = beregning.beregningsperioder.first().trygdetid,
            perioder =
                avkorting.avkortetYtelse
                    .map { it.toSamordningVedtakPeriode() }
                    .sortedBy { it.fom },
        )
    }

    private fun VedtakType.toSamordningsvedtakType(): SamordningVedtakType {
        return when (this) {
            VedtakType.INNVILGELSE -> SamordningVedtakType.START
            VedtakType.OPPHOER -> SamordningVedtakType.OPPHOER
            VedtakType.ENDRING -> SamordningVedtakType.ENDRING
            VedtakType.AVSLAG -> throw IllegalArgumentException("Vedtak om avslag støttes ikke")
        }
    }

    private fun AvkortetYtelseDto.toSamordningVedtakPeriode(): SamordningVedtakPeriode {
        return SamordningVedtakPeriode(
            fom = fom.atDay(1),
            tom = tom?.atEndOfMonth(),
            omstillingsstoenadBrutto = ytelseFoerAvkorting,
            omstillingsstoenadNetto = ytelseEtterAvkorting,
        )
    }
}
