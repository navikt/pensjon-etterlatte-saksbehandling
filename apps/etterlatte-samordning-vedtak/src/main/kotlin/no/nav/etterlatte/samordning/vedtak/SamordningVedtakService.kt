package no.nav.etterlatte.samordning.vedtak

import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.vedtak.VedtakSamordningDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.LocalDate

class SamordningVedtakService(
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
    private val tjenestepensjonKlient: TjenestepensjonKlient,
) {
    suspend fun hentVedtak(
        vedtakId: Long,
        callerContext: CallerContext,
    ): SamordningVedtakDto {
        val vedtak = vedtaksvurderingKlient.hentVedtak(vedtakId, callerContext)

        if (callerContext is MaskinportenTpContext &&
            !tjenestepensjonKlient.harTpYtelseOnDate(
                fnr = vedtak.fnr,
                tpnr = callerContext.tpnr,
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
        fnr: Folkeregisteridentifikator,
        context: CallerContext,
    ): List<SamordningVedtakDto> {
        if (context is MaskinportenTpContext && !tjenestepensjonKlient.harTpForholdByDate(fnr.value, context.tpnr, virkFom)
        ) {
            throw TjenestepensjonManglendeTilgangException("Ikke gyldig tpforhold")
        }

        return vedtaksvurderingKlient.hentVedtaksliste(
            virkFom = virkFom,
            fnr = fnr.value,
            callerContext = context,
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

    private fun Revurderingaarsak.toSamordningsvedtakAarsak(): String {
        return when (this) {
            Revurderingaarsak.INNTEKTSENDRING -> SamordningVedtakAarsak.INNTEKT
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
