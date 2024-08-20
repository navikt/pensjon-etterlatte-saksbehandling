package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import java.time.LocalDate

data class OmstillingsstoenadInformasjonMottattSoeknad(
    val mottattDato: LocalDate,
    val borINorgeEllerIkkeAvtaleland: Boolean,
) : BrevDataRedigerbar
