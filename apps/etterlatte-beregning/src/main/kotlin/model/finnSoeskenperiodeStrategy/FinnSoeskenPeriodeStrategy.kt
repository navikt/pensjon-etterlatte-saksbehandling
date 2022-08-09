package model.finnSoeskenperiodeStrategy

import no.nav.etterlatte.libs.common.beregning.SoeskenPeriode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import java.time.YearMonth

sealed class FinnSoeskenPeriodeStrategy {
    abstract val soeskenperioder: List<SoeskenPeriode>

    companion object {
        fun create(grunnlag: Grunnlag, virkFOM: YearMonth, virkTOM: YearMonth): FinnSoeskenPeriodeStrategy {
            val saksbehandlerHarValgtSoeskenPeriode = grunnlag.grunnlag.find { it.opplysningType === Opplysningstyper.SAKSBEHANDLER_SOESKEN_I_BEREGNINGEN } != null

            return when {
                saksbehandlerHarValgtSoeskenPeriode  -> FinnSoeskenPeriodeStrategyManuell(grunnlag, virkFOM, virkTOM)
                else -> FinnSoeskenPeriodeStrategyAutomatisk(grunnlag, virkFOM)
            }
        }
    }
}



