package no.nav.etterlatte.libs.common.vedtak

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import java.math.BigDecimal
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.*
import java.util.Objects.isNull

data class Vedtak(
    val vedtakId: Long, // Løpenummer (BIGSERIAL)
    val virk: Periode,
    val sak: Sak,
    val behandling: Behandling,
    val type: VedtakType,
    val grunnlag: List<Grunnlagsopplysning<ObjectNode>>,
    val vilkaarsvurdering: VilkaarsvurderingDto?,
    val beregning: BilagMedSammendrag<List<Beregningsperiode>>?,
    val avkorting: BilagMedSammendrag<List<Beregningsperiode>>?,
    val pensjonTilUtbetaling: List<Utbetalingsperiode>?,
    val vedtakFattet: VedtakFattet?,
    val attestasjon: Attestasjon?
)

data class Behandling(
    val type: BehandlingType,
    val id: UUID
)

data class Sak(val ident: String, val sakType: String, val id: Long)

data class Periode(
    val fom: YearMonth,
    val tom: YearMonth?
) {
    init {
        require(isNull(tom) || fom == tom || fom.isBefore(tom))
    }
}

data class BilagMedSammendrag<T>(
    val bilag: ObjectNode,
    val sammendrag: T
)

data class VedtakFattet(
    val ansvarligSaksbehandler: String,
    val ansvarligEnhet: String,
    val tidspunkt: ZonedDateTime
)

data class Beregningsperiode(
    val periode: Periode,
    val beloep: BigDecimal
)

data class Utbetalingsperiode(
    val id: Long,
    val periode: Periode,
    val beloep: BigDecimal?,
    val type: UtbetalingsperiodeType
)

enum class UtbetalingsperiodeType {
    OPPHOER, UTBETALING
}

data class Attestasjon(
    val attestant: String,
    val attesterendeEnhet: String, // aktuell?
    val tidspunkt: ZonedDateTime
)

enum class BehandlingAarsak {
    SOEKNAD
}

enum class VedtakType {
    INNVILGELSE, OPPHOER, AVSLAG, ENDRING
}