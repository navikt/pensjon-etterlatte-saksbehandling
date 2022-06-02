package model.brev

import model.brev.mapper.finnAvdoed
import model.brev.mapper.finnBarn
import model.brev.mapper.finnMottaker
import no.nav.etterlatte.domene.vedtak.Vedtak
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.model.brev.BrevRequest
import no.nav.etterlatte.model.brev.Mottaker

data class AvslagBrevRequest(
    val saksnummer: String,
    val barn: Barn,
    val avdoed: Avdoed,
    val aktuelleParagrafer: List<String>,
    override val spraak: Spraak,
    override val mottaker: Mottaker
) : BrevRequest() {
    override fun templateName(): String = "avslag"

    companion object {
        fun fraVedtak(vedtak: Vedtak): AvslagBrevRequest = AvslagBrevRequest(
            saksnummer = vedtak.sak.id.toString(),
            barn = vedtak.finnBarn(),
            avdoed = vedtak.finnAvdoed(),
            aktuelleParagrafer = emptyList(), // todo: Gå igjennom oppfylte vilkår?
            spraak = Spraak.NB, // todo, må hentes.
            mottaker = vedtak.finnMottaker()
        )
    }
}
