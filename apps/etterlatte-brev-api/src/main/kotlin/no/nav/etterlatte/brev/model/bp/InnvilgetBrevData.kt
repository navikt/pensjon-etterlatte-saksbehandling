package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BrevData

// Dette er for den eksisterende, fullt automatiske brevmalen som snart skal fases ut
data class InnvilgetBrevData(
    val utbetalingsinfo: Utbetalingsinfo,
    val avkortingsinfo: Avkortingsinfo? = null,
    val avdoed: Avdoed,
) : BrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            utbetalingsinfo: Utbetalingsinfo,
            avkortingsinfo: Avkortingsinfo? = null,
        ): InnvilgetBrevData =
            InnvilgetBrevData(
                utbetalingsinfo = utbetalingsinfo,
                avdoed = generellBrevData.personerISak.avdoede.minBy { it.doedsdato },
                avkortingsinfo = avkortingsinfo,
            )
    }
}
