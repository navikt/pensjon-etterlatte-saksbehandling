package model.finnSoeskenperiodeStrategy

import no.nav.etterlatte.libs.common.beregning.SoeskenPeriode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.model.BeregningService
import java.time.YearMonth

data class FinnSoeskenPeriodeStrategyManuell(
    private val grunnlag: Grunnlag,
    private val datoFOM: YearMonth,
) : FinnSoeskenPeriodeStrategy() {
    private val vilkaarOpplysning = BeregningService.finnOpplysning<List<SoeskenMedIBeregning>>(
        grunnlag.grunnlag,
        Opplysningstyper.SAKSBEHANDLER_SOESKEN_I_BEREGNINGEN
    )
    private val bruker =
        BeregningService.finnOpplysning<Person>(grunnlag.grunnlag, Opplysningstyper.SOEKER_PDL_V1)?.opplysning
    private val avdoedesBarn: Map<Foedselsnummer, Person> = hentAvdoedesBarn()

    override val soeskenperioder: List<SoeskenPeriode> =
        vilkaarOpplysning!!.opplysning
            .filter { it.skalBrukes }
            .mapNotNull { soesken -> avdoedesBarn[soesken.foedselsnummer] }
            .let { FinnSoeskenPeriodeStrategyAutomatisk(it, bruker, datoFOM) }
            .soeskenperioder

    private fun hentAvdoedesBarn(): Map<Foedselsnummer, Person> {
        val avdoedPdl =
            BeregningService.finnOpplysning<Person>(grunnlag.grunnlag, Opplysningstyper.AVDOED_PDL_V1)?.opplysning
        return avdoedPdl!!.avdoedesBarn!!.associateBy { it.foedselsnummer }
    }
}
