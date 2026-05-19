package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.BrevDataRedigerbar

data class BarnepensjonInformasjonDoedsfallData(
    val avdoedNavn: String,
    val borIutland: Boolean,
    val erOver18aar: Boolean,
)

data class BarnepensjonInformasjonDoedsfall(
    override val data: BarnepensjonInformasjonDoedsfallData,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            borIutland: Boolean,
            erOver18aar: Boolean,
            avdoedNavn: String,
        ): BarnepensjonInformasjonDoedsfall =
            BarnepensjonInformasjonDoedsfall(
                data = BarnepensjonInformasjonDoedsfallData(
                    avdoedNavn = avdoedNavn,
                    borIutland = borIutland,
                    erOver18aar = erOver18aar,
                ),
            )
    }
}
