package model.brev

import no.nav.etterlatte.domene.vedtak.Vedtak
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.model.brev.BrevRequest
import no.nav.etterlatte.model.brev.Mottaker
import java.time.LocalDate

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
            barn = Barn(
                navn = "Ola nordmann", // todo: Hentes fra pdl/grunnlag
                fnr = vedtak.sak.ident,
            ),
            avdoed = Avdoed("Gammel, mann", LocalDate.now()), // todo: Hentes fra behandling/grunnlag,
            aktuelleParagrafer = emptyList(), // todo: Gå igjennom oppfylte vilkår?
            spraak = Spraak.NB, // todo, må hentes.
            // todo: Adresse hentes fra grunnlag/pdl
            mottaker = Mottaker(navn = "Barn barnsen", adresse = "Testadresse", postnummer = "0000")
        )
    }
}
