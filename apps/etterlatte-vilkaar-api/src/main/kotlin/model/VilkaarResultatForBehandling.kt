package no.nav.etterlatte.model


import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import java.util.*

data class VilkaarResultatForBehandling(
    val behandling: UUID,
    val avdoedSoeknad: JsonNode?,
    val soekerSoeknad: JsonNode?,
    val soekerPdl: JsonNode?,
    val avdoedPdl: JsonNode?,
    val gjenlevendePdl: JsonNode?,
    val versjon: Long,
    val vilkaarResultat: VilkaarResultat,
)