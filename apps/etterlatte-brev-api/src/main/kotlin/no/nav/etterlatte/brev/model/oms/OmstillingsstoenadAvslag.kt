package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType

data class OmstillingsstoenadAvslag(
    override val innhold: List<Slate.Element>,
    val bosattUtland: Boolean,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            innhold: List<Slate.Element>,
            utlandstilknytningType: UtlandstilknytningType?,
        ): OmstillingsstoenadAvslag =
            OmstillingsstoenadAvslag(
                bosattUtland = utlandstilknytningType == UtlandstilknytningType.BOSATT_UTLAND,
                innhold = innhold,
            )
    }
}

data class OmstillingsstoenadAvslagRedigerbartUtfall(
    val avdoedNavn: String,
) : BrevDataRedigerbar {
    companion object {
        fun fra(avdoede: List<Avdoed>) =
            OmstillingsstoenadAvslagRedigerbartUtfall(
                avdoedNavn =
                    avdoede.firstOrNull()?.navn
                        ?: "<Klarte ikke å finne navn automatisk, du må sette inn her>",
            )
    }
}
