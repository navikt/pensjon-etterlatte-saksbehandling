import com.fasterxml.jackson.databind.JsonNode
import grunnlag.avdoedTestopplysningerMap
import grunnlag.gjenlevendeTestopplysningerMap
import grunnlag.halvsoeskenTestopplysningerMap
import grunnlag.kilde
import grunnlag.soekerTestopplysningerMap
import grunnlag.soeskenTestopplysningerMap
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.AVDOEDESBARN
import no.nav.etterlatte.libs.common.person.AvdoedesBarn
import no.nav.etterlatte.libs.common.toJsonNode
import java.util.UUID.randomUUID

data class GrunnlagTestData(
    val opplysningsmapSoekerOverrides: Map<Opplysningstyper, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapSoeskenOverrides: Map<Opplysningstyper, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapAvdoedOverrides: Map<Opplysningstyper, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapGjenlevendeOverrides: Map<Opplysningstyper, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapHalvsoeskenOverrides: Map<Opplysningstyper, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapSakOverrides: Map<Opplysningstyper, Opplysning<JsonNode>> = emptyMap()
) {
    val soeker
        get() = personTestData(soekerTestopplysningerMap + opplysningsmapSoekerOverrides)
    val soesken
        get() = personTestData(soeskenTestopplysningerMap + opplysningsmapSoeskenOverrides)
    val halvsoesken
        get() = personTestData(halvsoeskenTestopplysningerMap + opplysningsmapHalvsoeskenOverrides)
    val gjenlevende
        get() = personTestData(gjenlevendeTestopplysningerMap + opplysningsmapGjenlevendeOverrides)

    private val avdoedesBarnOverrides
        get() = mapOf(
            AVDOEDESBARN to Opplysning.Konstant(
                randomUUID(),
                kilde,
                AvdoedesBarn(listOf(soeker, soesken, halvsoesken)).toJsonNode()
            )
        )
    val avdoed
        get() = personTestData(avdoedTestopplysningerMap + avdoedesBarnOverrides + opplysningsmapAvdoedOverrides)

    fun hentOpplysningsgrunnlag(): Opplysningsgrunnlag = Opplysningsgrunnlag(
        soeker = soekerTestopplysningerMap + opplysningsmapSoekerOverrides,
        familie = listOf(
            soeskenTestopplysningerMap + opplysningsmapSoeskenOverrides,
            avdoedTestopplysningerMap + avdoedesBarnOverrides + opplysningsmapAvdoedOverrides,
            gjenlevendeTestopplysningerMap + opplysningsmapGjenlevendeOverrides,
            halvsoeskenTestopplysningerMap + opplysningsmapHalvsoeskenOverrides
        ),
        sak = opplysningsmapSakOverrides,
        metadata = Metadata(1, 15)
    )

    fun hentPersonGalleri(): Persongalleri = Persongalleri(
        soeker = soeker.foedselsnummer.value,
        innsender = gjenlevende.foedselsnummer.value,
        soesken = listOf(soesken.foedselsnummer.value, halvsoesken.foedselsnummer.value),
        avdoed = listOf(avdoed.foedselsnummer.value),
        gjenlevende = listOf(gjenlevende.foedselsnummer.value)
    )
}