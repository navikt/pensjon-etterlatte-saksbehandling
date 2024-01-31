package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType

data class OmstillingsstoenadAvslag(
    val innhold: List<Slate.Element>,
    val avdoedNavn: String,
    val bosattUtland: Boolean,
) : BrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            innhold: List<Slate.Element>,
        ): OmstillingsstoenadAvslag =
            OmstillingsstoenadAvslag(
                avdoedNavn = generellBrevData.personerISak.avdoede.first().navn,
                bosattUtland = generellBrevData.utlandstilknytning?.type == UtlandstilknytningType.BOSATT_UTLAND,
                innhold = innhold,
            )
    }
}
