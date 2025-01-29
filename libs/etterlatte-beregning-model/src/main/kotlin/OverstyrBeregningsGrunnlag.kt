package no.nav.etterlatte.beregning.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.SakId
import java.time.LocalDate
import java.util.UUID

data class OverstyrBeregningGrunnlagDao(
    val id: UUID,
    val behandlingId: UUID,
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val utbetaltBeloep: Long,
    val foreldreloessats: Boolean?,
    val trygdetid: Long,
    val trygdetidForIdent: String?,
    val prorataBroekTeller: Long?,
    val prorataBroekNevner: Long?,
    val sakId: SakId,
    val beskrivelse: String,
    val aarsak: String?,
    val kilde: Grunnlagsopplysning.Saksbehandler,
    val reguleringRegelresultat: JsonNode? = null,
) {
    fun tilGrunnlagMedPeriode(): GrunnlagMedPeriode<OverstyrBeregningGrunnlagData> =
        GrunnlagMedPeriode(
            data =
                OverstyrBeregningGrunnlagData(
                    utbetaltBeloep = this.utbetaltBeloep,
                    foreldreloessats = this.foreldreloessats,
                    trygdetid = this.trygdetid,
                    trygdetidForIdent = this.trygdetidForIdent,
                    prorataBroekTeller = this.prorataBroekTeller,
                    prorataBroekNevner = this.prorataBroekNevner,
                    beskrivelse = this.beskrivelse,
                    aarsak = this.aarsak,
                ),
            fom = this.datoFOM,
            tom = this.datoTOM,
        )
}

data class OverstyrBeregningGrunnlagData(
    val utbetaltBeloep: Long,
    val foreldreloessats: Boolean?,
    val trygdetid: Long,
    val trygdetidForIdent: String?,
    val prorataBroekTeller: Long?,
    val prorataBroekNevner: Long?,
    val beskrivelse: String,
    val aarsak: String?,
)

data class OverstyrBeregningGrunnlagDTO(
    val perioder: List<GrunnlagMedPeriode<OverstyrBeregningGrunnlagData>>,
)

data class OverstyrBeregningGrunnlag(
    val perioder: List<GrunnlagMedPeriode<OverstyrBeregningGrunnlagData>>,
    val kilde: Grunnlagsopplysning.Saksbehandler,
)
