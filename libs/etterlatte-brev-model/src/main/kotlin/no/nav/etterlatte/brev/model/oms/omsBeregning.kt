package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregning
import no.nav.etterlatte.brev.model.erYrkesskade
import no.nav.etterlatte.brev.model.fromDto
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.kodeverk.LandDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto

fun omsBeregning(
    vedleggInnhold: List<Slate.Element>,
    behandling: DetaljertBehandling,
    trygdetid: TrygdetidDto,
    avkortingsinfo: Avkortingsinfo,
    landKodeverk: List<LandDto>,
): OmstillingsstoenadBeregning {
    val beregningsperioder =
        avkortingsinfo.beregningsperioder.map { it.tilOmstillingsstoenadBeregningsperiode() }
    val beregningsperioderOpphoer = utledBeregningsperioderOpphoer(behandling, beregningsperioder)
    return OmstillingsstoenadBeregning(
        innhold = vedleggInnhold,
        virkningsdato = avkortingsinfo.virkningsdato,
        beregningsperioder = beregningsperioder,
        sisteBeregningsperiode = beregningsperioderOpphoer.sisteBeregningsperiode,
        sisteBeregningsperiodeNesteAar = beregningsperioderOpphoer.sisteBeregningsperiodeNesteAar,
        trygdetid =
            trygdetid.fromDto(
                beregningsMetodeFraGrunnlag = beregningsperioderOpphoer.sisteBeregningsperiode.beregningsMetodeFraGrunnlag,
                beregningsMetodeAnvendt = beregningsperioderOpphoer.sisteBeregningsperiode.beregningsMetodeAnvendt,
                navnAvdoed = null,
                landKodeverk = landKodeverk,
            ),
        oppphoersdato = beregningsperioderOpphoer.forventetOpphoerDato,
        opphoerNesteAar =
            beregningsperioderOpphoer.forventetOpphoerDato?.year == (behandling.virkningstidspunkt().dato.year + 1),
        erYrkesskade = trygdetid.erYrkesskade(),
    )
}
