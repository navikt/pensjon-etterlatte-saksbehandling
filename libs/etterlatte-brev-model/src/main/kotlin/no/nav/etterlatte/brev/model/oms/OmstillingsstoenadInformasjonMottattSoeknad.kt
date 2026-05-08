package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevDataRedigerbar
import java.time.LocalDate

data class OmstillingsstoenadInformasjonMottattSoeknadData(
    val mottattDato: LocalDate,
    val borINorgeEllerIkkeAvtaleland: Boolean,
)

data class OmstillingsstoenadInformasjonMottattSoeknad(
    override val data: OmstillingsstoenadInformasjonMottattSoeknadData,
) : BrevDataRedigerbar
