package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo

data class InnvilgetBrevData(
    val utbetalingsinfo: Utbetalingsinfo,
    val avkortingsinfo: Avkortingsinfo? = null,
    val avdoed: Avdoed
) : BrevData() {

    companion object {
        fun fra(behandling: Behandling): InnvilgetBrevData =
            InnvilgetBrevData(
                utbetalingsinfo = behandling.utbetalingsinfo!!,
                avdoed = behandling.persongalleri.avdoed,
                avkortingsinfo = behandling.avkortingsinfo
            )
    }
}

data class InnvilgetBrevDataNy(
    val utbetalingsinfo: Utbetalingsinfo,
    val avkortingsinfo: Avkortingsinfo? = null,
    val avdoed: Avdoed,
    val etterbetalingDTO: EtterbetalingDTO? = null,
    val innhold: List<Slate.Element>
) : BrevData() {

    companion object {
        fun fra(behandling: Behandling): InnvilgetBrevDataNy =
            InnvilgetBrevDataNy(
                utbetalingsinfo = behandling.utbetalingsinfo!!,
                avdoed = behandling.persongalleri.avdoed,
                avkortingsinfo = behandling.avkortingsinfo,
                etterbetalingDTO = null,
                innhold = listOf()
            )
    }
}