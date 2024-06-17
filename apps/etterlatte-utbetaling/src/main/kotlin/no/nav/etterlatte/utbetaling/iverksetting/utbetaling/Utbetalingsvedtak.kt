package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.mq.Prioritet
import no.nav.etterlatte.utbetaling.common.OppdragDefaults.SAKSBEHANDLER_ID_SYSTEM_ETTERLATTEYTELSER
import java.math.BigDecimal
import java.time.YearMonth

data class Utbetalingsvedtak(
    val vedtakId: Long,
    val sak: Sak,
    val behandling: Behandling,
    val pensjonTilUtbetaling: List<Utbetalingsperiode>,
    val vedtakFattet: VedtakFattet,
    val attestasjon: Attestasjon,
) {
    companion object {
        fun fra(
            vedtak: VedtakDto,
            vedtakFattet: VedtakFattet? = null,
            attestasjon: Attestasjon? = null,
        ): Utbetalingsvedtak {
            val innhold = (vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto)
            return Utbetalingsvedtak(
                vedtakId = vedtak.id,
                sak = Sak(vedtak.sak.ident, vedtak.sak.id, Saktype.valueOf(vedtak.sak.sakType.toString())),
                behandling =
                    Behandling(
                        type = innhold.behandling.type,
                        id = innhold.behandling.id,
                        revurderingsaarsak = innhold.behandling.revurderingsaarsak,
                        revurderingInfo = innhold.behandling.revurderingInfo,
                    ),
                pensjonTilUtbetaling =
                    innhold.utbetalingsperioder.map {
                        Utbetalingsperiode(
                            id = it.id!!,
                            periode =
                                Periode(
                                    fom = it.periode.fom,
                                    tom = it.periode.tom,
                                ),
                            beloep = it.beloep,
                            type = it.type.toUtbetalingsperiodeType(),
                        )
                    },
                vedtakFattet =
                    vedtakFattet ?: vedtak.vedtakFattet?.let {
                        VedtakFattet(
                            ansvarligSaksbehandler = it.ansvarligSaksbehandler,
                            ansvarligEnhet = it.ansvarligEnhet,
                        )
                    } ?: throw Exception("Mangler saksbehandler og enhet på vedtak"),
                attestasjon =
                    attestasjon ?: vedtak.attestasjon?.let {
                        Attestasjon(
                            attestant = it.attestant,
                            attesterendeEnhet = it.attesterendeEnhet,
                        )
                    } ?: throw Exception("Mangler attestant på vedtak"),
            )
        }
    }

    fun finnPrioritet(): Prioritet =
        if (SAKSBEHANDLER_ID_SYSTEM_ETTERLATTEYTELSER == vedtakFattet.ansvarligSaksbehandler &&
            behandling.revurderingsaarsak?.equals(Revurderingaarsak.REGULERING) == true
        ) {
            Prioritet.LAV
        } else {
            Prioritet.NORMAL
        }
}

data class Sak(
    val ident: String,
    val id: Long,
    val sakType: Saktype,
)

enum class Saktype {
    BARNEPENSJON,
    OMSTILLINGSSTOENAD,
}

data class Attestasjon(
    val attestant: String,
    val attesterendeEnhet: String? = null,
)

data class Utbetalingsperiode(
    val id: Long,
    val periode: Periode,
    val beloep: BigDecimal?,
    val type: UtbetalingsperiodeType,
)

data class VedtakFattet(
    val ansvarligSaksbehandler: String,
    val ansvarligEnhet: String? = null,
)

data class Periode(
    val fom: YearMonth,
    val tom: YearMonth?,
)

enum class UtbetalingsperiodeType {
    OPPHOER,
    UTBETALING,
}

fun no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType.toUtbetalingsperiodeType() =
    when (this) {
        no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType.UTBETALING -> UtbetalingsperiodeType.UTBETALING
        no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType.OPPHOER -> UtbetalingsperiodeType.OPPHOER
    }
