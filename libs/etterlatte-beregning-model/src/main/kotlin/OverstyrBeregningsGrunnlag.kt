package no.nav.etterlatte.beregning.grunnlag

import java.time.LocalDate
import java.util.UUID

data class OverstyrBeregningGrunnlagDao(
    val id: UUID,
    val behandlingId: UUID,
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val utbetaltBeloep: Long,
    val trygdetid: Long,
    val sakId: Long,
)

data class OverstyrBeregningGrunnlag(
    val utbetaltBeloep: Long,
    val trygdetid: Long,
)

data class OverstyrBeregningGrunnlagDTO(
    val perioder: List<GrunnlagMedPeriode<OverstyrBeregningGrunnlag>>,
)
