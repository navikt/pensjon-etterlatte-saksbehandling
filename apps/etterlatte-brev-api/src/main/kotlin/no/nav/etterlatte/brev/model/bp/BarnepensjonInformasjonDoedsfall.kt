package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.BrevdataMedInnhold
import no.nav.etterlatte.brev.model.Slate

data class BarnepensjonInformasjonDoedsfall(
    override val innhold: List<Slate.Element>,
    val avdoedNavn: String,
) : BrevDataRedigerbar, BrevdataMedInnhold {
    companion object {
        fun fra(generellBrevData: GenerellBrevData): BarnepensjonInformasjonDoedsfall =
            BarnepensjonInformasjonDoedsfall(
                innhold = emptyList(),
                avdoedNavn = generellBrevData.personerISak.avdoede.first().navn,
            )
    }
}
