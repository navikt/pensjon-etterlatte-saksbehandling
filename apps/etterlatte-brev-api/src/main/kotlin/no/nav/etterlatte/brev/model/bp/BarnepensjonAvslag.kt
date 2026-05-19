package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType

data class BarnepensjonAvslagData(val brukerUnder18Aar: Boolean, val bosattUtland: Boolean)

data class BarnepensjonAvslag(
    override val innhold: List<Slate.Element>,
    override val data: BarnepensjonAvslagData,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            innhold: InnholdMedVedlegg,
            brukerUnder18Aar: Boolean,
            utlandstilknytning: UtlandstilknytningType?,
        ): BarnepensjonAvslag =
            BarnepensjonAvslag(
                innhold = innhold.innhold(),
                data = BarnepensjonAvslagData(
                    brukerUnder18Aar = brukerUnder18Aar,
                    bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
                ),
            )
    }
}

data class BarnepensjonAvslagRedigerbartData(
    val erSluttbehandling: Boolean,
    val avdoedNavn: String,
)

data class BarnepensjonAvslagRedigerbar(
    override val data: BarnepensjonAvslagRedigerbartData,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            avdoede: List<Avdoed>,
            erSluttbehandling: Boolean,
        ): BarnepensjonAvslagRedigerbar =
            BarnepensjonAvslagRedigerbar(
                data = BarnepensjonAvslagRedigerbartData(
                    erSluttbehandling = erSluttbehandling,
                    avdoedNavn =
                        avdoede.firstOrNull()?.navn
                            ?: "<Klarte ikke å finne navn automatisk, du må sette inn her>",
                ),
            )
    }
}
