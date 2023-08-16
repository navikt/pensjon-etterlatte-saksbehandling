package vilkaarsvurdering.vilkaar

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType.OMS_AKTIVITET_ETTER_6_MND
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType.OMS_AVDOEDES_MEDLEMSKAP
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType.OMS_DOEDSFALL
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType.OMS_ETTERLATTE_LEVER
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType.OMS_GJENLEVENDES_MEDLEMSKAP
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType.OMS_OEVRIGE_VILKAAR
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType.OMS_SIVILSTAND
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType.OMS_YRKESSKADE
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.OmstillingstoenadVilkaar
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class OmstillingstoenadVilkaarTest {

    val grunnlag: Grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

    @Test
    fun `skal gi inngangsvilkaar ved foerstegangsbehandling`() {
        val inngangsvilkaar = OmstillingstoenadVilkaar.inngangsvilkaar(grunnlag)

        val forventetInngangsvilkaar = listOf(
            OMS_ETTERLATTE_LEVER,
            OMS_AVDOEDES_MEDLEMSKAP,
            OMS_GJENLEVENDES_MEDLEMSKAP,
            OMS_DOEDSFALL,
            OMS_YRKESSKADE,
            OMS_OEVRIGE_VILKAAR,
            OMS_AKTIVITET_ETTER_6_MND
        )

        inngangsvilkaar.map { it.hovedvilkaar.type } shouldContainAll forventetInngangsvilkaar
    }

    @ParameterizedTest
    @EnumSource(RevurderingAarsak::class, names = ["REGULERING", "INNTEKTSENDRING"])
    fun `skal ikke returnere vilkaar ved revurderinger som ikke krever dette`(revurderingAarsak: RevurderingAarsak) {
        val vilkaar = OmstillingstoenadVilkaar.loependeVilkaarForRevurdering(grunnlag, revurderingAarsak)
        vilkaar.size shouldBe 0
    }

    @Test
    fun `skal returnere vilkaar OMS_ETTERLATTE_LEVER ved revurdering av type DOEDSFALL`() {
        val vilkaar = OmstillingstoenadVilkaar.loependeVilkaarForRevurdering(grunnlag, RevurderingAarsak.DOEDSFALL)
        vilkaar.size shouldBe 1
        vilkaar.first().hovedvilkaar.type shouldBe OMS_ETTERLATTE_LEVER
    }

    @Test
    fun `skal returnere vilkaar OMS_ETTERLATTE_SIVILSTAND ved revurdering av type SIVILSTAND`() {
        val vilkaar = OmstillingstoenadVilkaar.loependeVilkaarForRevurdering(grunnlag, RevurderingAarsak.SIVILSTAND)
        vilkaar.size shouldBe 1
        vilkaar.first().hovedvilkaar.type shouldBe OMS_SIVILSTAND
    }

    @Test
    fun `skal returnere vilkaar OMS_SIVILSTAND ved revurdering av type BARN`() {
        val vilkaar = OmstillingstoenadVilkaar.loependeVilkaarForRevurdering(grunnlag, RevurderingAarsak.SIVILSTAND)
        vilkaar.size shouldBe 1
        vilkaar.first().hovedvilkaar.type shouldBe OMS_SIVILSTAND
    }
}