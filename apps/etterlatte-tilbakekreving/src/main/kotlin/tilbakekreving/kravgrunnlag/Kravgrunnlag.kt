package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import java.math.BigDecimal
import java.time.LocalDate

data class KravgrunnlagId(val value: Long)
data class SakId(val value: Long)
data class VedtakId(val value: Long)
data class Kontrollfelt(val value: String)
data class NavIdent(val value: String)
data class UUID30(val value: String)
data class KlasseKode(val value: String)
data class Periode(val fraOgMed: LocalDate, val tilOgMed: LocalDate)

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
    val grunnlagsperioder: List<Grunnlagsperiode>
)

data class Grunnlagsperiode(
    val periode: Periode,
    val beloepSkattMnd: BigDecimal,
    val grunnlagsbeloep: List<Grunnlagsbeloep>
)

data class Grunnlagsbeloep(
    val kode: KlasseKode,
    val type: KlasseType,
    val beloepTidligereUtbetaling: BigDecimal,
    val beloepNyUtbetaling: BigDecimal,
    val beloepSkalTilbakekreves: BigDecimal,
    val beloepSkalIkkeTilbakekreves: BigDecimal,
    val skatteProsent: BigDecimal
)