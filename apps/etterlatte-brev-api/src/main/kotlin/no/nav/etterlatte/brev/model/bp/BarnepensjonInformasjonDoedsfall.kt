package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.BrevdataMedInnhold
import no.nav.etterlatte.brev.model.Slate

data class BarnepensjonInformasjonDoedsfall(
    override val innhold: List<Slate.Element>,
    val avdoedNavn: String,
    val borIutland: Boolean,
    val erOver18aar: Boolean,
) : BrevDataRedigerbar,
    BrevdataMedInnhold {
    companion object {
        fun fra(
            borIutland: Boolean,
            erOver18aar: Boolean,
            avdoede: List<Avdoed>,
        ): BarnepensjonInformasjonDoedsfall =
            BarnepensjonInformasjonDoedsfall(
                innhold = emptyList(),
                avdoedNavn =
                    avdoede
                        .first()
                        .navn,
                borIutland = borIutland,
                erOver18aar = erOver18aar,
            )
    }
}
