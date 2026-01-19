package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.HarVedlegg
import no.nav.etterlatte.brev.Slate
import java.time.LocalDate

data class OmstillingsstoenadBeregning(
    override val innhold: List<Slate.Element>,
    val virkningsdato: LocalDate,
    val beregningsperioder: List<OmstillingsstoenadBeregningsperiode>,
    val sisteBeregningsperiode: OmstillingsstoenadBeregningsperiode,
    val sisteBeregningsperiodeNesteAar: OmstillingsstoenadBeregningsperiode?,
    val trygdetid: TrygdetidMedBeregningsmetode,
    val oppphoersdato: LocalDate?,
    val opphoerNesteAar: Boolean,
    val erYrkesskade: Boolean,
) : HarVedlegg
