package no.nav.etterlatte.behandling.revurdering

import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService

class RevurderingKopierGrunnlag(
    private val featureToggleService: FeatureToggleService,
    // private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    // private val trygdetidKlient: TrygdetidKlient,
    private val beregningKlient: BeregningKlient,
) {
    fun kopier() {
        if (featureToggleService.isEnabled(RevurderingFeatureToggle.KopierGrunnlag, false)) {
            // TODO klientkall vilkår
            // TODO klientkall trygdetid
            // TODO klientkall beregningsgrunnlag
            // TODO klientkall beregning
            // TODO klientkall avkorting
            // TODO mer?
        }
    }
}
