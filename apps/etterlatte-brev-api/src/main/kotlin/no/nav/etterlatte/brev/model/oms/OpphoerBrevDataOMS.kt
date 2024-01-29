package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import java.time.LocalDate

data class OpphoerBrevDataOMS(
    val innhold: List<Slate.Element>,
    val bosattUtland: Boolean,
) : BrevData() {
    companion object {
        fun fra(
            utlandstilknytning: Utlandstilknytning?,
            innhold: List<Slate.Element>,
        ): OpphoerBrevDataOMS =
            OpphoerBrevDataOMS(
                bosattUtland = utlandstilknytning?.type == UtlandstilknytningType.BOSATT_UTLAND,
                innhold = innhold,
            )
    }
}

data class OpphoerBrevDataUtfallOMS(
    val innhold: List<Slate.Element>,
    val virkningsdato: LocalDate,
) : BrevData() {
    companion object {
        fun fra(
            virkningsdato: LocalDate,
            innhold: List<Slate.Element>,
        ): OpphoerBrevDataUtfallOMS =
            OpphoerBrevDataUtfallOMS(
                virkningsdato = virkningsdato,
                innhold = innhold,
            )
    }
}
