package no.nav.etterlatte.libs.common.tilbakekreving

import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.UUID30
import java.math.BigDecimal
import java.time.YearMonth

data class KravgrunnlagId(
    val value: Long,
)

data class SakId(
    val value: Long,
)

data class VedtakId(
    val value: Long,
)

data class Kontrollfelt(
    val value: String,
)

data class NavIdent(
    val value: String,
)

data class KlasseKode(
    val value: String,
)

data class Periode(
    val fraOgMed: YearMonth,
    val tilOgMed: YearMonth,
)

enum class KravgrunnlagStatus { ANNU, ANOM, AVSL, BEHA, ENDR, FEIL, MANU, NY, SPER }

enum class KlasseType { YTEL, SKAT, FEIL, JUST, RENT, TREK }

data class Kravgrunnlag(
    val kravgrunnlagId: KravgrunnlagId,
    val sakId: SakId,
    val vedtakId: VedtakId,
    val kontrollFelt: Kontrollfelt,
    val status: KravgrunnlagStatus,
    val saksbehandler: NavIdent,
    val referanse: UUID30,
    val perioder: List<KravgrunnlagPeriode>,
)

data class KravgrunnlagPeriode(
    val periode: Periode,
    val skatt: BigDecimal,
    val grunnlagsbeloep: List<Grunnlagsbeloep>,
)

data class Grunnlagsbeloep(
    val klasseKode: KlasseKode,
    val klasseType: KlasseType,
    val bruttoUtbetaling: BigDecimal,
    val nyBruttoUtbetaling: BigDecimal,
    val bruttoTilbakekreving: BigDecimal,
    val beloepSkalIkkeTilbakekreves: BigDecimal,
    val skatteProsent: BigDecimal,
    val resultat: String?,
    val skyld: String?,
    val aarsak: String?,
)

data class KravOgVedtakstatus(
    val sakId: SakId,
    val vedtakId: VedtakId,
    val status: KravgrunnlagStatus,
    val referanse: UUID30,
)

data class HentOmgjoeringKravgrunnlagRequest(
    val saksbehandler: String,
    val enhet: Enhetsnummer,
    val kravgrunnlagId: Long,
)
