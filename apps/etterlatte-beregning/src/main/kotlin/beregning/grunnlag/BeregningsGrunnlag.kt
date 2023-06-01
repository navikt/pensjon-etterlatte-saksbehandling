package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import java.util.UUID

data class BeregningsGrunnlag(
    val behandlingId: UUID,
    val kilde: Grunnlagsopplysning.Saksbehandler,
    val soeskenMedIBeregning: List<GrunnlagMedPeriode<List<SoeskenMedIBeregning>>>,
    val institusjonsoppholdBeregnignsGrunnlag: List<GrunnlagMedPeriode<InstitusjonsoppholdBeregnignsGrunnlag>>
)