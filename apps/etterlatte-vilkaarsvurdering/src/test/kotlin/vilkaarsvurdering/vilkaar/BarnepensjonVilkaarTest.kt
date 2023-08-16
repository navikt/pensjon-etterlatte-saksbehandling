package vilkaarsvurdering.vilkaar

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType.BP_ALDER_BARN
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType.BP_DOEDSFALL_FORELDER
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType.BP_FORMAAL
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType.BP_FORTSATT_MEDLEMSKAP
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType.BP_YRKESSKADE_AVDOED
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.BarnepensjonVilkaar
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.OmstillingstoenadVilkaar
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BarnepensjonVilkaarTest {

    val grunnlag: Grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

    @Test
    fun `skal gi inngangsvilkaar ved foerstegangsbehandling`() {
        val inngangsvilkaar = BarnepensjonVilkaar.inngangsvilkaar(grunnlag)

        val forventetInngangsvilkaar = listOf(
            BP_FORMAAL,
            BP_DOEDSFALL_FORELDER,
            BP_ALDER_BARN,
            BP_FORTSATT_MEDLEMSKAP,
            BP_FORUTGAAENDE_MEDLEMSKAP,
            BP_YRKESSKADE_AVDOED
        )

        inngangsvilkaar.map { it.hovedvilkaar.type } shouldContainAll forventetInngangsvilkaar
    }

    @ParameterizedTest
    @EnumSource(RevurderingAarsak::class, names = ["REGULERING"])
    fun `skal ikke returnere vilkaar ved revurderinger som ikke krever dette`(revurderingAarsak: RevurderingAarsak) {
        val vilkaar = OmstillingstoenadVilkaar.loependeVilkaarForRevurdering(grunnlag, revurderingAarsak)
        vilkaar.size shouldBe 0
    }

    // TODO skrive tester for øvrige vilkår
}