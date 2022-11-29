package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import java.math.BigDecimal
import java.time.YearMonth

data class Utbetalingsvedtak(
    val vedtakId: Long,
    val sak: Sak,
    val behandling: Behandling,
    val pensjonTilUtbetaling: List<Utbetalingsperiode>,
    val vedtakFattet: VedtakFattet,
    val attestasjon: Attestasjon
) {
    companion object {
        fun fra(vedtak: Vedtak) =
            Utbetalingsvedtak(
                vedtakId = vedtak.vedtakId,
                sak = Sak(vedtak.sak.ident, vedtak.sak.id, Saktype.fraString(vedtak.sak.sakType)),
                behandling = Behandling(type = vedtak.behandling.type, id = vedtak.behandling.id),
                pensjonTilUtbetaling = vedtak.pensjonTilUtbetaling!!.map {
                    Utbetalingsperiode(
                        id = it.id,
                        periode = Periode(
                            fom = it.periode.fom,
                            tom = it.periode.tom
                        ),
                        beloep = it.beloep,
                        type = it.type.toUtbetalingsperiodeType()
                    )
                },
                vedtakFattet = VedtakFattet(vedtak.vedtakFattet!!.ansvarligSaksbehandler),
                attestasjon = Attestasjon(vedtak.attestasjon!!.attestant)
            )
    }
}

data class Sak(val ident: String, val id: Long, val sakType: Saktype)
enum class Saktype {
    BARNEPENSJON, OMSTILLINGSSTOENAD;

    companion object {
        fun fraString(str: String) =
            when (str) {
                "BARNEPENSJON" -> BARNEPENSJON
                "OMSTILLINGSSTOENAD" -> OMSTILLINGSSTOENAD
                else -> throw IllegalArgumentException("$str er ikke en kjent sakstype.")
            }
    }
}

data class Attestasjon(val attestant: String)

data class Utbetalingsperiode(
    val id: Long,
    val periode: Periode,
    val beloep: BigDecimal?,
    val type: UtbetalingsperiodeType
)

data class VedtakFattet(val ansvarligSaksbehandler: String)
data class Periode(
    val fom: YearMonth,
    val tom: YearMonth?
)

enum class UtbetalingsperiodeType {
    OPPHOER, UTBETALING; // TODO: trenger vi en ENDRING-type her?
}

fun no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType.toUtbetalingsperiodeType() =
    when (this) {
        no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType.UTBETALING -> UtbetalingsperiodeType.UTBETALING
        no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType.OPPHOER -> UtbetalingsperiodeType.OPPHOER
    }