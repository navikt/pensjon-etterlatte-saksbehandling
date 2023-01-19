package no.nav.etterlatte.libs.common.beregning

import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.YearMonth
import java.util.*

enum class DelytelseId {
    BP
}

enum class Beregningstyper {
    GP,
    GBBP,
    BPGP,
    BIPIDYBOPIDY
}

enum class Endringskode {
    NY,
    REVURDERING
}

enum class BeregningsResultatType {
    BEREGNET, FAARIKKEPAENG
}

data class BeregningDTO(
    val beregningId: UUID,
    val behandlingId: UUID,
    val beregningsperioder: List<Beregningsperiode>,
    val beregnetDato: Tidspunkt,
    val grunnlagMetadata: Metadata
)

data class Beregningsperiode(
    val delytelsesId: DelytelseId,
    val type: Beregningstyper,
    val datoFOM: YearMonth,
    val datoTOM: YearMonth?,
    val utbetaltBeloep: Int,
    val soeskenFlokk: List<String>?,
    val grunnbelopMnd: Int,
    val grunnbelop: Int,
    val trygdetid: Int
)