package vilkaarsvurdering

import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

class VilkaarsvurderingTestData {
    companion object {
        val oppfylt = VilkaarsvurderingDto(
            UUID.randomUUID(),
            emptyList(),
            YearMonth.of(2022, 1),
            VilkaarsvurderingResultat(VilkaarsvurderingUtfall.OPPFYLT, null, LocalDateTime.now(), "ABCDEF")
        )

        val ikkeOppfylt = VilkaarsvurderingDto(
            UUID.randomUUID(),
            emptyList(),
            YearMonth.of(2022, 1),
            VilkaarsvurderingResultat(VilkaarsvurderingUtfall.IKKE_OPPFYLT, null, LocalDateTime.now(), "ABCDEF")
        )
    }
}