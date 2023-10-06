package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.model.AvslagBrevData
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import java.time.LocalDate

data class AvslagYrkesskadeBrevData(
    val dinForelder: String,
    val doedsdato: LocalDate,
    val yrkesskadeEllerYrkessykdom: String,
) : BrevData() {
    companion object {
        fun fra(behandling: Behandling): AvslagYrkesskadeBrevData =
            AvslagBrevData.valider<RevurderingInfo.Yrkesskade>(
                behandling,
                RevurderingAarsak.YRKESSKADE,
            ).let {
                AvslagYrkesskadeBrevData(
                    it.dinForelder,
                    behandling.personerISak.avdoed.doedsdato,
                    it.yrkesskadeEllerYrkessykdom,
                )
            }
    }
}
