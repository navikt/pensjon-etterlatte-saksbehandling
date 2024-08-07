package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.model.BrevDataRedigerbar

data class BarnepensjonInformasjonMottattSoeknad(
    val borINorgeEllerIkkeAvtaleland: Boolean,
    val erOver18aar: Boolean,
    val bosattUtland: Boolean,
) : BrevDataRedigerbar
