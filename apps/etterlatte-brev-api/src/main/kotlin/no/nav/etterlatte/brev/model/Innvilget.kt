package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak

data class InnvilgetBrevRequest(
    val saksnummer: String,
    val utbetalingsinfo: Utbetalingsinfo,
    val grunnbeloep: Int,
    val barn: Soeker,
    val avdoed: Avdoed,
    val aktuelleParagrafer: List<String>,
    override val spraak: Spraak,
    override val avsender: Avsender,
    override val mottaker: Mottaker
) : BrevRequest() {
    override fun templateName(): String = "innvilget"

    companion object {
        fun fraVedtak(
            behandling: Behandling,
            avsender: Avsender,
            mottaker: Mottaker
        ): InnvilgetBrevRequest =
            InnvilgetBrevRequest(
                saksnummer = behandling.sakId.toString(),
                utbetalingsinfo = behandling.utbetalingsinfo!!,
                grunnbeloep = behandling.utbetalingsinfo.grunnbeloep.grunnbeloep,
                barn = behandling.persongalleri.soeker,
                avdoed = behandling.persongalleri.avdoed,
                aktuelleParagrafer = emptyList(), // todo: Gå igjennom oppfylte vilkår? Nødvendig?
                spraak = behandling.spraak,
                mottaker = mottaker,
                avsender = avsender
            )
    }
}
