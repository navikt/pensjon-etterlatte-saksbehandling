package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.model.BrevDataFerdigstilling
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.Slate

data class OmstillingsstoenadAktivitetspliktVarsel(
    override val innhold: List<Slate.Element>,
) : BrevDataFerdigstilling

class OmstillingsstoenadAktivitetspliktVarselUtfall : BrevDataRedigerbar
