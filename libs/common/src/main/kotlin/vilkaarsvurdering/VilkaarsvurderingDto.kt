package no.nav.etterlatte.libs.common.vilkaarsvurdering

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

data class VilkaarsvurderingDto(
    val behandlingId: UUID,
    val vilkaar: JsonNode,
    val virkningstidspunkt: VirkningstidspunktDto,
    val resultat: VilkaarsvurderingResultatDto?
)

data class VilkaarsvurderingResultatDto(
    val utfall: VilkaarsvurderingUtfallDto,
    val kommentar: String?,
    val tidspunkt: LocalDateTime,
    val saksbehandler: String
)

data class VirkningstidspunktDto(
    val dato: YearMonth,
    val kilde: JsonNode
)

enum class VilkaarsvurderingUtfallDto {
    OPPFYLT,
    IKKE_OPPFYLT
}