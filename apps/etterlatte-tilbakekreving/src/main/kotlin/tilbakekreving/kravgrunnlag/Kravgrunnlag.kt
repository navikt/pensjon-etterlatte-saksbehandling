package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class SakId(val value: Long)

data class BehandlingId(
    val value: UUID,
    val shortValue: Kravgrunnlag.UUID30
) // trenger muligens bare UUID - TODO: finn ut om denne sendes til TBK
data class KravgrunnlagId(val value: Long)
data class NavIdent(val value: String)

data class Kravgrunnlag(
    val sakId: SakId,
    val kravgrunnlagId: KravgrunnlagId,
    val vedtakId: VedtakId,
    val kontrollFelt: Kontrollfelt,
    val status: KravgrunnlagStatus,
    val saksbehandler: NavIdent,
    val sisteUtbetalingslinjeId: UUID30,
    val grunnlagsperioder: List<Grunnlagsperiode>,
    val mottattKravgrunnlagXml: String

    // val behandlingId: BehandlingId?, denne burde nok heller være koblet på feks en tilbakekreving
    // val vedtakIdGjelder --> trengs denne?
    // val utbetalesTilId --> trengs denne?
    // val vedtakIdOmgjort --> trengs denne?
    /*
    ref. vedtakIdOmgjort: trenger ikke denne per nå. Dette er en referanse til
    et tidligere tilbakekrevingsvedtak dersom en klage og etterfølgende anke
    fører til at en tilbakekreving blir reversert / endret.
     */
) {

    data class VedtakId(val value: Long)
    data class Kontrollfelt(val value: String)
    data class UUID30(val value: String)

    enum class KravgrunnlagStatus {
        ANNU, ANOM, AVSL, BEHA, ENDR, FEIL, MANU, NY, SPER;
    }

    data class Grunnlagsperiode(
        val periode: Periode,
        val beloepSkattMnd: BigDecimal,
        val grunnlagsbeloep: List<Grunnlagsbeloep>
    ) {

        data class Periode(val fraOgMed: LocalDate, val tilOgMed: LocalDate)

        data class Grunnlagsbeloep(
            val kode: KlasseKode,
            val type: KlasseType,
            val beloepTidligereUtbetaling: BigDecimal,
            val beloepNyUtbetaling: BigDecimal,
            val beloepSkalTilbakekreves: BigDecimal,
            val beloepSkalIkkeTilbakekreves: BigDecimal,
            val skatteProsent: BigDecimal
        ) {
            data class KlasseKode(val value: String) // TODO: finne ut hvilke verdier klassekodene kan ha
            enum class KlasseType {
                YTEL, SKAT, FEIL, JUST
            }
        }

        fun UUID.toUUID30() = this.toString().replace("-", "").substring(0, 30).let { UUID30(it) }
    }
}