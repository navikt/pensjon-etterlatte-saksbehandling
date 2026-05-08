package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.libs.common.behandling.TidligereFamiliepleier
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType

data class OmstillingsstoenadAvslagData(val bosattUtland: Boolean)

data class OmstillingsstoenadAvslag(
    override val innhold: List<Slate.Element>,
    override val data: OmstillingsstoenadAvslagData,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            innhold: List<Slate.Element>,
            utlandstilknytningType: UtlandstilknytningType?,
        ): OmstillingsstoenadAvslag =
            OmstillingsstoenadAvslag(
                innhold = innhold,
                data = OmstillingsstoenadAvslagData(
                    bosattUtland = utlandstilknytningType == UtlandstilknytningType.BOSATT_UTLAND,
                ),
            )
    }
}

data class OmstillingsstoenadAvslagRedigerbartUtfallData(
    val avdoedNavn: String,
    val erSluttbehandling: Boolean,
    val tidligereFamiliepleier: Boolean,
)

data class OmstillingsstoenadAvslagRedigerbartUtfall(
    override val data: OmstillingsstoenadAvslagRedigerbartUtfallData,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            avdoede: List<Avdoed>,
            erSluttbehandling: Boolean,
            tidligereFamiliepleier: TidligereFamiliepleier?,
        ) = OmstillingsstoenadAvslagRedigerbartUtfall(
            data = OmstillingsstoenadAvslagRedigerbartUtfallData(
                avdoedNavn =
                    avdoede.firstOrNull()?.navn
                        ?: "<Klarte ikke å finne navn automatisk, du må sette inn her>",
                erSluttbehandling = erSluttbehandling,
                tidligereFamiliepleier = tidligereFamiliepleier?.svar ?: false,
            ),
        )
    }
}
