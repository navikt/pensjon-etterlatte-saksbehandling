package no.nav.etterlatte.tilbakekreving.domene

import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.util.*

data class Kravgrunnlag(
    val sakId: SakId, //fagsystem-id
    val kravgrunnlagId: KravgrunnlagId,
    val vedtakId: VedtakId,
    val kontrollFelt: Kontrollfelt,
    val status: KravgrunnlagStatus,
    val saksbehandler: NavIdent,
    val sisteUtbetalingslinjeId: UUID30,
    val grunnlagsperioder: List<Grunnlagsperiode>
) {

    data class SakId(val value: Long)
    data class KravgrunnlagId(val value: BigInteger)
    data class VedtakId(val value: BigInteger)
    data class Kontrollfelt(val value: String)
    data class NavIdent(val value: String)
    data class UUID30(val value: String)
    enum class KravgrunnlagStatus {
        ANNU, ANOM, AVSL, BEHA, ENDR, FEIL, MANU, NY, SPER;
    }

    data class Grunnlagsperiode(
        val periode: Periode,
        val beloepSkattMnd: BigDecimal,
        val grunnlagsbeloep: List<Grunnlagsbeloep>,
    ) {

        data class Periode(val fraOgMed: LocalDate, val tilOgMed: LocalDate)

        data class Grunnlagsbeloep(
            val kode: KlasseKode,
            val type: KlasseType,
            val beloepTidligereUtbetaling: BigDecimal,
            val beloepNyUtbetaling: BigDecimal,
            val beloepSkalTilbakekreves: BigDecimal,
            val beloepSkalIkkeTilbakekreves: BigDecimal,
            val skatteProsent: BigDecimal,
        ) {
            data class KlasseKode(val value: String) // TODO: finne ut hvilke verdier klassekodene kan ha
            enum class KlasseType {
                YTEL, SKAT, FEIL,
            }
        }

        fun UUID.toUUID30() = this.toString().replace("-", "").substring(0, 30).let { UUID30(it) }
    }
}





