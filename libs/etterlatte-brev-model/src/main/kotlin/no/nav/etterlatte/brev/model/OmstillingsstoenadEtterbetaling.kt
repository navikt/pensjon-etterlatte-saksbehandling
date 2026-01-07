package no.nav.etterlatte.brev.model

import java.time.LocalDate

data class OmstillingsstoenadEtterbetaling(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val etterbetalingsperioder: List<OmstillingsstoenadBeregningsperiode>,
)
