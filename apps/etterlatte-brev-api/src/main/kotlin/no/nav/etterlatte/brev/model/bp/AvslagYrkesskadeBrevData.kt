package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.GenerellBrevData
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
        fun fra(generellBrevData: GenerellBrevData): AvslagYrkesskadeBrevData =
            AvslagBrevData.valider<RevurderingInfo.Yrkesskade>(
                generellBrevData.revurderingsaarsak,
                generellBrevData.forenkletVedtak.revurderingInfo,
                RevurderingAarsak.YRKESSKADE,
            ).let {
                AvslagYrkesskadeBrevData(
                    it.dinForelder,
                    generellBrevData.personerISak.avdoed.doedsdato,
                    it.yrkesskadeEllerYrkessykdom,
                )
            }
    }
}
