package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import java.time.LocalDate

data class BarnepensjonInformasjonMottattSoeknad(
    val mottattDato: LocalDate,
    val borINorgeEllerIkkeAvtaleland: Boolean,
    val erOver18aar: Boolean,
    val bosattUtland: Boolean,
) : BrevDataRedigerbar
