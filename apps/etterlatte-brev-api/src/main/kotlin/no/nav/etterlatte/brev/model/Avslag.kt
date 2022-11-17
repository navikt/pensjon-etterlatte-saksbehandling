package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.model.mapper.finnAvdoed
import no.nav.etterlatte.brev.model.mapper.finnBarn
import no.nav.etterlatte.brev.model.mapper.finnMottaker
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak

data class AvslagBrevRequest(
    val saksnummer: String,
    val barn: Barn,
    val avdoed: Avdoed,
    val aktuelleParagrafer: List<String>,
    override val spraak: Spraak,
    override val avsender: Avsender,
    override val mottaker: Mottaker
) : BrevRequest() {
    override fun templateName(): String = "avslag"

    companion object {
        fun fraVedtak(vedtak: Vedtak, avsender: Avsender): AvslagBrevRequest = AvslagBrevRequest(
            saksnummer = vedtak.sak.id.toString(),
            barn = vedtak.finnBarn(),
            avdoed = vedtak.finnAvdoed(),
            aktuelleParagrafer = emptyList(), // todo: Gå igjennom oppfylte vilkår?
            spraak = Spraak.NB, // todo, må hentes.
            avsender = avsender,
            mottaker = vedtak.finnMottaker()
        )
    }
}