package no.nav.etterlatte.migrering

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.rapidsandrivers.migrering.AvdoedForelder
import no.nav.etterlatte.rapidsandrivers.migrering.Beregning
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetidsgrunnlag
import java.time.YearMonth
import java.util.*

data class Pesyssak(
    val id: UUID,
    val pesysId: PesysId,
    val enhet: Enhet,
    val soeker: Folkeregisteridentifikator,
    val gjenlevendeForelder: Folkeregisteridentifikator?,
    val avdoedForelder: List<AvdoedForelder>,
    val virkningstidspunkt: YearMonth,
    val beregning: Beregning,
    val trygdetidsPerioder: List<Trygdetidsgrunnlag>
)