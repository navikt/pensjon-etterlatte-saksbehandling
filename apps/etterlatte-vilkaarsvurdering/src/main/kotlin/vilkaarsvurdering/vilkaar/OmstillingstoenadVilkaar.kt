package no.nav.etterlatte.vilkaarsvurdering.vilkaar

import no.nav.etterlatte.libs.common.vilkaarsvurdering.Delvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Lovreferanse
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType

object OmstillingstoenadVilkaar {

    fun inngangsvilkaar() = listOf(
        etVilkaar()
    )

    private fun etVilkaar() = Vilkaar(
        Delvilkaar(
            type = VilkaarType.OMS_TESTVILKAAR,
            tittel = "Testvilkår",
            beskrivelse = "Testvilkår",
            lovreferanse = Lovreferanse(
                paragraf = "§ 17-1",
                lenke = "https://lovdata.no/dokument/NL/lov/1997-02-28-19/KAPITTEL_6-4#%C2%A717-2"
            )
        )
    )
}