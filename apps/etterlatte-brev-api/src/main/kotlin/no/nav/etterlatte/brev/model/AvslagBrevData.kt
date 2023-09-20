package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.model.AvslagBrevData.valider
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import java.time.LocalDate

object AvslagBrevData : BrevData() {
    fun fra(behandling: Behandling): AvslagBrevData = AvslagBrevData
}

data class AvslagYrkesskadeBrevData(
    val dinForelder: String,
    val doedsdato: LocalDate,
    val yrkesskadeEllerYrkessykdom: String,
) : BrevData() {
    companion object {
        fun fra(behandling: Behandling): AvslagYrkesskadeBrevData =
            valider<RevurderingInfo.Yrkesskade>(
                behandling,
                RevurderingAarsak.YRKESSKADE,
            ).let {
                AvslagYrkesskadeBrevData(
                    it.dinForelder,
                    behandling.persongalleri.avdoed.doedsdato,
                    it.yrkesskadeEllerYrkessykdom,
                )
            }
    }
}
