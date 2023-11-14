package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
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
    val beskrivelse: String,
    val kilde: Grunnlagsopplysning.Saksbehandler,
)

data class OverstyrBeregningGrunnlag(
    val utbetaltBeloep: Long,
    val trygdetid: Long,
    val beskrivelse: String,
)

data class OverstyrBeregningGrunnlagDTO(
    val perioder: List<GrunnlagMedPeriode<OverstyrBeregningGrunnlag>>,
    val kilde: Grunnlagsopplysning.Saksbehandler,
)
