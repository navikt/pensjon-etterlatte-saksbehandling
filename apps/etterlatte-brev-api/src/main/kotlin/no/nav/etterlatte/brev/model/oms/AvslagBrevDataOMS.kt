package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.Slate

data class AvslagBrevDataOMS(
    val avdoedNavn: String,
    val innhold: List<Slate.Element>,
) : BrevData() {
    companion object {
        fun fra(
            avdoedNavn: String,
            innhold: List<Slate.Element>,
        ): AvslagBrevDataOMS =
            AvslagBrevDataOMS(
                avdoedNavn = avdoedNavn,
                innhold = innhold,
            )
    }
}
