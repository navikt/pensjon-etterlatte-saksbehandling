import com.fasterxml.jackson.databind.JsonNode
import grunnlag.avdødTestopplysningerMap
import grunnlag.gjenlevendeTestopplysningerMap
import grunnlag.kilde
import grunnlag.søkerTestopplysningerMap
import grunnlag.søskenTestopplysningerMap
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.AVDOEDESBARN
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.toJsonNode

data class GrunnlagTestData(
    val opplysningsmapSøkerOverrides: Map<Opplysningstyper, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapSøskenOverrides: Map<Opplysningstyper, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapAvdødOverrides: Map<Opplysningstyper, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapGjenlevendeOverrides: Map<Opplysningstyper, Opplysning<JsonNode>> = emptyMap()
) {
    companion object {
        fun GrunnlagTestData.søker(): Person = personTestData(søkerTestopplysningerMap + opplysningsmapSøkerOverrides)
        fun GrunnlagTestData.søsken(): Person = personTestData(søkerTestopplysningerMap + opplysningsmapSøskenOverrides)

        private fun GrunnlagTestData.avdoedesBarnOverrides(): Map<Opplysningstyper, Opplysning<JsonNode>> = mapOf(
            AVDOEDESBARN to Opplysning.Konstant(kilde, listOf(søker(), søsken()).toJsonNode())
        )

        fun GrunnlagTestData.avdød(): Person =
            personTestData(avdødTestopplysningerMap + avdoedesBarnOverrides() + opplysningsmapAvdødOverrides)

        fun GrunnlagTestData.gjenlevende(): Person =
            personTestData(gjenlevendeTestopplysningerMap + opplysningsmapGjenlevendeOverrides)

        fun GrunnlagTestData.opplysningsgrunnlag(): Opplysningsgrunnlag = Opplysningsgrunnlag(
            søker = søkerTestopplysningerMap + opplysningsmapSøkerOverrides,
            familie = listOf(
                søskenTestopplysningerMap + opplysningsmapSøskenOverrides,
                avdødTestopplysningerMap + avdoedesBarnOverrides() + opplysningsmapAvdødOverrides,
                gjenlevendeTestopplysningerMap + opplysningsmapGjenlevendeOverrides
            ),
            sak = mapOf(),
            metadata = Metadata(1, 15)
        )
    }
}