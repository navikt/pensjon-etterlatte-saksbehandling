package vilkaarsvurdering

import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class VilkaarsvurderingTestData {
    companion object {
        val oppfylt = Vilkaarsvurdering(
            UUID.randomUUID(),
            emptyList(),
            LocalDate.now(),
            VilkaarsvurderingResultat(VilkaarsvurderingUtfall.OPPFYLT, null, LocalDateTime.now(), "ABCDEF")
        )

        val ikkeOppfylt = Vilkaarsvurdering(
            UUID.randomUUID(),
            emptyList(),
            LocalDate.now(),
            VilkaarsvurderingResultat(VilkaarsvurderingUtfall.IKKE_OPPFYLT, null, LocalDateTime.now(), "ABCDEF")
        )
    }
}