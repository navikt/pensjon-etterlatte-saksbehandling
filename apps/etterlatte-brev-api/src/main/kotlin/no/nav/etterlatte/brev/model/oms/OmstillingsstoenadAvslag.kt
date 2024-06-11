package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.model.BrevDataFerdigstilling
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType

data class OmstillingsstoenadAvslag(
    override val innhold: List<Slate.Element>,
    val bosattUtland: Boolean,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            innhold: List<Slate.Element>,
        ): OmstillingsstoenadAvslag =
            OmstillingsstoenadAvslag(
                bosattUtland = generellBrevData.utlandstilknytning?.type == UtlandstilknytningType.BOSATT_UTLAND,
                innhold = innhold,
            )
    }
}

data class OmstillingsstoenadAvslagRedigerbartUtfall(
    val avdoedNavn: String,
) : BrevDataRedigerbar {
    companion object {
        fun fra(generellBrevData: GenerellBrevData) =
            OmstillingsstoenadAvslagRedigerbartUtfall(
                avdoedNavn =
                    generellBrevData.personerISak.avdoede
                        .firstOrNull()
                        ?.navn
                        ?: "<Klarte ikke å finne navn automatisk, du må sette inn her>",
            )
    }
}
