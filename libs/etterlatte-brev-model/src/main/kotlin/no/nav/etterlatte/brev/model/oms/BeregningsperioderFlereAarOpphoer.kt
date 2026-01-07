package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregningsperiode
import java.time.LocalDate

data class BeregningsperioderFlereAarOpphoer(
    val sisteBeregningsperiode: OmstillingsstoenadBeregningsperiode,
    val sisteBeregningsperiodeNesteAar: OmstillingsstoenadBeregningsperiode?,
    val forventetOpphoerDato: LocalDate?,
)
