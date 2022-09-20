import com.fasterxml.jackson.databind.JsonNode
import grunnlag.avdødTestopplysningerMap
import grunnlag.gjenlevendeTestopplysningerMap
import grunnlag.halvsøskenTestopplysningerMap
import grunnlag.kilde
import grunnlag.søkerTestopplysningerMap
import grunnlag.søskenTestopplysningerMap
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.AVDOEDESBARN
import no.nav.etterlatte.libs.common.person.AvdoedesBarn
import no.nav.etterlatte.libs.common.toJsonNode

data class GrunnlagTestData(
    val opplysningsmapSøkerOverrides: Map<Opplysningstyper, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapSøskenOverrides: Map<Opplysningstyper, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapAvdødOverrides: Map<Opplysningstyper, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapGjenlevendeOverrides: Map<Opplysningstyper, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapHalvsøskenOverrides: Map<Opplysningstyper, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapSakOverrides: Map<Opplysningstyper, Opplysning<JsonNode>> = emptyMap()
) {
    val søker
        get() = personTestData(søkerTestopplysningerMap + opplysningsmapSøkerOverrides)
    val søsken
        get() = personTestData(søskenTestopplysningerMap + opplysningsmapSøskenOverrides)
    val halvsøsken
        get() = personTestData(halvsøskenTestopplysningerMap + opplysningsmapHalvsøskenOverrides)
    val gjenlevende
        get() = personTestData(gjenlevendeTestopplysningerMap + opplysningsmapGjenlevendeOverrides)

    private val avdoedesBarnOverrides
        get() = mapOf(
            AVDOEDESBARN to Opplysning.Konstant(
                kilde,
                AvdoedesBarn(listOf(søker, søsken, halvsøsken)).toJsonNode()
            )
        )
    val avdød
        get() = personTestData(avdødTestopplysningerMap + avdoedesBarnOverrides + opplysningsmapAvdødOverrides)

    fun hentOpplysningsgrunnlag(): Opplysningsgrunnlag = Opplysningsgrunnlag(
        søker = søkerTestopplysningerMap + opplysningsmapSøkerOverrides,
        familie = listOf(
            søskenTestopplysningerMap + opplysningsmapSøskenOverrides,
            avdødTestopplysningerMap + avdoedesBarnOverrides + opplysningsmapAvdødOverrides,
            gjenlevendeTestopplysningerMap + opplysningsmapGjenlevendeOverrides,
            halvsøskenTestopplysningerMap + opplysningsmapHalvsøskenOverrides
        ),
        sak = opplysningsmapSakOverrides,
        metadata = Metadata(1, 15)
    )

    fun hentPersonGalleri(): Persongalleri = Persongalleri(
        soeker = søker.foedselsnummer.value,
        innsender = gjenlevende.foedselsnummer.value,
        soesken = listOf(søsken.foedselsnummer.value, halvsøsken.foedselsnummer.value),
        avdoed = listOf(avdød.foedselsnummer.value),
        gjenlevende = listOf(gjenlevende.foedselsnummer.value)
    )
}