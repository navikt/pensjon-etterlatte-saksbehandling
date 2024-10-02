package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.behandling.Avdoed

data class BarnepensjonInformasjonDoedsfall(
    val avdoedNavn: String,
    val borIutland: Boolean,
    val erOver18aar: Boolean,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            borIutland: Boolean,
            erOver18aar: Boolean,
            avdoede: List<Avdoed>,
        ): BarnepensjonInformasjonDoedsfall =
            BarnepensjonInformasjonDoedsfall(
                avdoedNavn = avdoede.first().navn,
                borIutland = borIutland,
                erOver18aar = erOver18aar,
            )
    }
}
