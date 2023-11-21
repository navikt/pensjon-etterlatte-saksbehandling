package no.nav.etterlatte.samordning.vedtak

import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.vedtak.VedtakSamordningDto
import no.nav.etterlatte.libs.common.vedtak.VedtakSamordningPeriode
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.slf4j.LoggerFactory
import java.time.LocalDate

class SamordningVedtakService(
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
    private val tjenestepensjonKlient: TjenestepensjonKlient,
) {
    private val logger = LoggerFactory.getLogger(SamordningVedtakService::class.java)

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
            logger.info("Avslår forespørsel, manglende/ikke gyldig TP-ytelse")
            throw TjenestepensjonManglendeTilgangException("Ikke gyldig tpYtelse")
        }

        if (vedtak.sak.sakType != SakType.OMSTILLINGSSTOENAD) {
            throw VedtakFeilSakstypeException()
        }

        return vedtak.mapSamordningsvedtak()
    }

    suspend fun hentVedtaksliste(
        fomDato: LocalDate,
        fnr: Folkeregisteridentifikator,
        context: CallerContext,
    ): List<SamordningVedtakDto> {
        if (context is MaskinportenTpContext && !tjenestepensjonKlient.harTpForholdByDate(fnr.value, context.tpnr, fomDato)
        ) {
            logger.info("Avslår forespørsel, manglende/ikke gyldig TP-forhold")
            throw TjenestepensjonManglendeTilgangException("Ikke gyldig tpforhold")
        }

        return vedtaksvurderingKlient.hentVedtaksliste(
            fomDato = fomDato,
            fnr = fnr.value,
            callerContext = context,
        )
            .map { it.mapSamordningsvedtak() }
    }

    private fun VedtakSamordningDto.mapSamordningsvedtak(): SamordningVedtakDto {
        val beregning = beregning?.let { deserialize<BeregningDTO>(it.toString()) }

        return SamordningVedtakDto(
            vedtakId = vedtakId,
            sakstype = "OMS",
            virkningsdato = virkningstidspunkt.atStartOfMonth(),
            opphoersdato = virkningstidspunkt.takeIf { type == VedtakType.OPPHOER }?.atStartOfMonth(),
            type = type.toSamordningsvedtakType(),
            aarsak = behandling.revurderingsaarsak?.toSamordningsvedtakAarsak(),
            anvendtTrygdetid = beregning?.beregningsperioder?.first()?.trygdetid ?: 0,
            perioder =
                perioder
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
            Revurderingaarsak.DOEDSFALL -> SamordningVedtakAarsak.DOEDSFALL
            else -> SamordningVedtakAarsak.ANNET
        }.name
    }

    private fun VedtakSamordningPeriode.toSamordningVedtakPeriode(): SamordningVedtakPeriode {
        return SamordningVedtakPeriode(
            fom = fom.atStartOfMonth(),
            tom = tom?.atEndOfMonth(),
            omstillingsstoenadBrutto = ytelseFoerAvkorting,
            omstillingsstoenadNetto = ytelseEtterAvkorting,
        )
    }
}
