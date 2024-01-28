package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType

data class AvslagBrevDataOMS(
    val innhold: List<Slate.Element>,
    val avdoedNavn: String,
    val bosattUtland: Boolean,
) : BrevData() {
    companion object {
        fun fra(
            avdoedNavn: String,
            utlandstilknytning: Utlandstilknytning?,
            innhold: List<Slate.Element>,
        ): AvslagBrevDataOMS =
            AvslagBrevDataOMS(
                avdoedNavn = avdoedNavn,
                bosattUtland = utlandstilknytning?.type == UtlandstilknytningType.BOSATT_UTLAND,
                innhold = innhold,
            )
    }
}
