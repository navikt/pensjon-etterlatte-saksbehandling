package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.Slate

data class OmstillingsstoenadAktivitetspliktVarsel(
    override val innhold: List<Slate.Element>,
    val bosattUtland: Boolean,
) : BrevDataFerdigstilling

data class OmstillingsstoenadAktivitetspliktVarselUtfallData(
    val er12MndVarsel: Boolean,
)

class OmstillingsstoenadAktivitetspliktVarselUtfall(
    override val data: OmstillingsstoenadAktivitetspliktVarselUtfallData,
) : BrevDataRedigerbar
