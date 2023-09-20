package no.nav.etterlatte.libs.common.tilbakekreving

import java.math.BigDecimal
import java.time.YearMonth

data class KravgrunnlagId(val value: Long)

data class SakId(val value: Long)

data class VedtakId(val value: Long)

data class Kontrollfelt(val value: String)

data class NavIdent(val value: String)

data class UUID30(val value: String)

data class KlasseKode(val value: String)

data class Periode(val fraOgMed: YearMonth, val tilOgMed: YearMonth)

enum class KravgrunnlagStatus { ANNU, ANOM, AVSL, BEHA, ENDR, FEIL, MANU, NY, SPER }

enum class KlasseType { YTEL, SKAT, FEIL, JUST }

data class Kravgrunnlag(
    val kravgrunnlagId: KravgrunnlagId,
    val sakId: SakId,
    val vedtakId: VedtakId,
    val kontrollFelt: Kontrollfelt,
    val status: KravgrunnlagStatus,
    val saksbehandler: NavIdent,
    val sisteUtbetalingslinjeId: UUID30,
    val perioder: List<KravgrunnlagPeriode>,
)

data class KravgrunnlagPeriode(
    val periode: Periode, // TODO EY-2661 endre til kun en yearmonth?
    val skatt: BigDecimal,
    val grunnlagsbeloep: List<Grunnlagsbeloep>,
)

data class Grunnlagsbeloep(
    val kode: KlasseKode,
    val type: KlasseType,
    val bruttoUtbetaling: BigDecimal,
    val beregnetNyBrutto: BigDecimal,
    val bruttoTilbakekreving: BigDecimal,
    val nettoTilbakekreving: BigDecimal, // TODO EY-2661 hvordan fylles denne ??
    val beloepSkalIkkeTilbakekreves: BigDecimal, // TODO EY-2661 samme som beregnetFeilutbetaling i frontend ??
    val skatteProsent: BigDecimal,
    val resultat: String?,
    val skyld: String?,
    val aarsak: String?,
)
