package no.nav.etterlatte.migrering

import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.rapidsandrivers.migrering.AvdoedForelder
import no.nav.etterlatte.rapidsandrivers.migrering.Beregning
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetid
import java.time.YearMonth

data class Pesyssak(
    val id: Long,
    val enhet: Enhet,
    val soeker: Folkeregisteridentifikator,
    val gjenlevendeForelder: Folkeregisteridentifikator?,
    val avdoedForelder: List<AvdoedForelder>,
    val dodAvYrkesskade: Boolean,
    val virkningstidspunkt: YearMonth,
    val beregning: Beregning,
    val trygdetid: Trygdetid,
    val flyktningStatus: Boolean,
    val spraak: Spraak,
) {
    fun tilMigreringsrequest() =
        MigreringRequest(
            pesysId = PesysId(id),
            enhet = enhet,
            soeker = soeker,
            gjenlevendeForelder = gjenlevendeForelder,
            avdoedForelder = avdoedForelder,
            dodAvYrkesskade = dodAvYrkesskade,
            flyktningStatus = flyktningStatus,
            virkningstidspunkt = virkningstidspunkt,
            beregning = beregning,
            trygdetid = trygdetid,
            spraak = spraak,
        )
}
