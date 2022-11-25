package vilkaarsvurdering

import behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultatDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfallDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VirkningstidspunktDto
import java.time.LocalDateTime
import java.util.*

class VilkaarsvurderingTestData {
    companion object {
        val oppfylt = VilkaarsvurderingDto(
            UUID.randomUUID(),
            objectMapper.createObjectNode(),
            VirkningstidspunktTestData.virkningstidsunkt().let {
                VirkningstidspunktDto(it.dato, it.kilde.toJsonNode())
            },
            VilkaarsvurderingResultatDto(
                VilkaarsvurderingUtfallDto.OPPFYLT,
                null,
                LocalDateTime.now(),
                "ABCDEF"
            )
        )

        val ikkeOppfylt = VilkaarsvurderingDto(
            UUID.randomUUID(),
            objectMapper.createObjectNode(),
            VirkningstidspunktTestData.virkningstidsunkt().let {
                VirkningstidspunktDto(it.dato, it.kilde.toJsonNode())
            },
            VilkaarsvurderingResultatDto(
                VilkaarsvurderingUtfallDto.IKKE_OPPFYLT,
                null,
                LocalDateTime.now(),
                "ABCDEF"
            )
        )
    }
}