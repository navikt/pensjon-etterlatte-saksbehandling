import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning

data class MigreringGrunnlagRequest(
    val soeker: Pair<String, List<Grunnlagsopplysning<JsonNode>>>,
    val gjenlevende: List<Pair<String, List<Grunnlagsopplysning<JsonNode>>>>,
    val avdoede: List<Pair<String, List<Grunnlagsopplysning<JsonNode>>>>
)