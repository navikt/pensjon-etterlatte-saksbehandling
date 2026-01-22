package no.nav.etterlatte.brev.behandling

import java.time.LocalDate

data class Avkortingsinfo(
    val virkningsdato: LocalDate,
    val beregningsperioder: List<AvkortetBeregningsperiode>,
    val endringIUtbetalingVedVirk: Boolean,
    val erInnvilgelsesaar: Boolean,
)
