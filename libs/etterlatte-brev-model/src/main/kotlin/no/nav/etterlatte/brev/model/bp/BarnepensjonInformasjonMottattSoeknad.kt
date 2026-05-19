package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.BrevDataRedigerbar
import java.time.LocalDate

data class BarnepensjonInformasjonMottattSoeknadData(
    val mottattDato: LocalDate,
    val borINorgeEllerIkkeAvtaleland: Boolean,
    val erOver18aar: Boolean,
    val bosattUtland: Boolean,
)

data class BarnepensjonInformasjonMottattSoeknad(
    override val data: BarnepensjonInformasjonMottattSoeknadData,
) : BrevDataRedigerbar
