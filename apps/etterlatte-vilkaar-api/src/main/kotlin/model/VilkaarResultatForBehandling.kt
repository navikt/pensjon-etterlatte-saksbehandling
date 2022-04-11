package model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat

data class VilkaarResultatForBehandling(
    val behandling: String,
    val avdoedSoeknad: JsonNode?,
    val soekerSoeknad: JsonNode?,
    val soekerPdl: JsonNode?,
    val avdoedPdl: JsonNode?,
    val gjenlevendePdl: JsonNode?,
    val versjon: Long,
    val vilkaarResultat: VilkaarResultat,
)