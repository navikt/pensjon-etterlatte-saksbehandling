package model.finnSoeskenperiodeStrategy

import no.nav.etterlatte.libs.common.beregning.SoeskenPeriode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.model.BeregningService
import java.time.YearMonth

sealed class FinnSoeskenPeriodeStrategy {
    abstract val soeskenperioder: List<SoeskenPeriode>

    companion object {
        fun create(grunnlag: Grunnlag, virkFOM: YearMonth, virkTOM: YearMonth): FinnSoeskenPeriodeStrategy {
            val saksbehandlerHarValgtSoeskenPeriode = grunnlag.grunnlag.find { it.opplysningType === Opplysningstyper.SAKSBEHANDLER_SOESKEN_I_BEREGNINGEN } != null

            return when {
                saksbehandlerHarValgtSoeskenPeriode  -> FinnSoeskenPeriodeStrategyManuell(grunnlag, virkFOM)
                else -> {
                    val avdoedPdl = BeregningService.finnOpplysning<Person>(grunnlag.grunnlag, Opplysningstyper.AVDOED_PDL_V1)?.opplysning
                    val bruker = BeregningService.finnOpplysning<Person>(grunnlag.grunnlag, Opplysningstyper.SOEKER_PDL_V1)?.opplysning
                    FinnSoeskenPeriodeStrategyAutomatisk(avdoedPdl?.avdoedesBarn, bruker, virkFOM)
                }
            }
        }
    }
}
