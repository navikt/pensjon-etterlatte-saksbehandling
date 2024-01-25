package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.Slate

data class AvslagBrevdataOMS(
    val avdoedNavn: String,
    val innhold: List<Slate.Element>,
) : BrevData() {
    companion object {
        fun fra(
            avdoedNavn: String,
            innhold: List<Slate.Element>,
        ): AvslagBrevdataOMS =
            AvslagBrevdataOMS(
                avdoedNavn = avdoedNavn,
                innhold = innhold,
            )
    }
}
