package no.nav.etterlatte.samordning.vedtak

import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
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
    private val tjenestepensjonKlient: TjenestepensjonKlient,
) {
    suspend fun hentVedtak(
        vedtakId: Long,
        tpnr: String,
        organisasjonsnummer: String,
    ): SamordningVedtakDto {
        val vedtak = vedtaksvurderingKlient.hentVedtak(vedtakId, organisasjonsnummer)

        if (!tjenestepensjonKlient.harTpYtelseOnDate(
                fnr = vedtak.fnr,
                tpnr = tpnr,
                fomDato = vedtak.virkningstidspunkt.atStartOfMonth(),
            )
        ) {
            throw TjenestepensjonManglendeTilgangException("Ikke gyldig tpYtelse")
        }

        if (vedtak.sak.sakType != SakType.OMSTILLINGSSTOENAD) {
            throw VedtakFeilSakstypeException()
        }

        return vedtak.mapSamordningsvedtak()
    }

    suspend fun hentVedtaksliste(
        virkFom: LocalDate,
        fnr: String,
        tpnr: String,
        organisasjonsnummer: String,
    ): List<SamordningVedtakDto> {
        if (!tjenestepensjonKlient.harTpForholdByDate(fnr, tpnr, virkFom)) {
            throw TjenestepensjonManglendeTilgangException("Ikke gyldig tpforhold")
        }

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
            virkningsdato = virkningstidspunkt.atStartOfMonth(),
            opphoersdato = virkningstidspunkt.takeIf { type == VedtakType.OPPHOER }?.atStartOfMonth(),
            type = type.toSamordningsvedtakType(),
            aarsak = behandling.revurderingsaarsak?.toSamordningsvedtakAarsak(),
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
            VedtakType.TILBAKEKREVING -> throw IllegalArgumentException("Ikke relevant")
        }
    }

    private fun RevurderingAarsak.toSamordningsvedtakAarsak(): String {
        return when (this) {
            RevurderingAarsak.INNTEKTSENDRING -> SamordningVedtakAarsak.INNTEKT
            else -> SamordningVedtakAarsak.ANNET
        }.name
    }

    private fun AvkortetYtelseDto.toSamordningVedtakPeriode(): SamordningVedtakPeriode {
        return SamordningVedtakPeriode(
            fom = fom.atStartOfMonth(),
            tom = tom?.atEndOfMonth(),
            omstillingsstoenadBrutto = ytelseFoerAvkorting,
            omstillingsstoenadNetto = ytelseEtterAvkorting,
        )
    }
}
