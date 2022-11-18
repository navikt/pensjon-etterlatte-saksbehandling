package nav.no.etterlatte.model

import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import java.time.LocalDateTime
import java.util.*

data class Beregning(
    val beregningId: UUID,
    val behandlingId: UUID,
    val beregningsperioder: List<Beregningsperiode>,
    val beregnetDato: LocalDateTime,
    val grunnlagMetadata: Metadata
)