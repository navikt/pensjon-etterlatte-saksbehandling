package no.nav.etterlatte.libs.testdata.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.AVDOEDESBARN
import no.nav.etterlatte.libs.common.person.AvdoedesBarn
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.pdl.personTestData
import java.time.LocalDateTime
import java.util.UUID.randomUUID

data class GrunnlagTestData(
    val opplysningsmapSoekerOverrides: Map<Opplysningstype, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapSoeskenOverrides: Map<Opplysningstype, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapAvdoedOverrides: Map<Opplysningstype, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapGjenlevendeOverrides: Map<Opplysningstype, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapHalvsoeskenOverrides: Map<Opplysningstype, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapSakOverrides: Map<Opplysningstype, Opplysning<JsonNode>> = emptyMap(),
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
        get() =
            mapOf(
                AVDOEDESBARN to
                    Opplysning.Konstant(
                        randomUUID(),
                        kilde,
                        AvdoedesBarn(listOf(soeker, soesken, halvsoesken)).toJsonNode(),
                    ),
            )

    private val avdoedesBarnMedEnDoed
        get() =
            mapOf(
                AVDOEDESBARN to
                    Opplysning.Konstant(
                        randomUUID(),
                        kilde,
                        AvdoedesBarn(
                            listOf
                                (
                                    personTestData
                                        (
                                            soeskenTestopplysningerMap +
                                                mapOf(
                                                    Opplysningstype.DOEDSDATO
                                                        to
                                                        Opplysning.Konstant
                                                            (
                                                                randomUUID(), kilde,
                                                                LocalDateTime.parse("2022-08-17T00:00:00").toJsonNode(),
                                                            ),
                                                ) +
                                                opplysningsmapSoeskenOverrides,
                                        ),
                                ),
                        ).toJsonNode(),
                    ),
            )

    val avdoed
        get() = personTestData(avdoedTestopplysningerMap + avdoedesBarnOverrides + opplysningsmapAvdoedOverrides)

    fun hentOpplysningsgrunnlag(): Grunnlag =
        Grunnlag(
            soeker = soekerTestopplysningerMap + opplysningsmapSoekerOverrides,
            familie =
                listOf(
                    soeskenTestopplysningerMap + opplysningsmapSoeskenOverrides,
                    avdoedTestopplysningerMap + avdoedesBarnOverrides + opplysningsmapAvdoedOverrides,
                    gjenlevendeTestopplysningerMap + opplysningsmapGjenlevendeOverrides,
                    halvsoeskenTestopplysningerMap + opplysningsmapHalvsoeskenOverrides,
                ),
            sak = opplysningsmapSakOverrides,
            metadata = Metadata(1, 15),
        )

    fun hentGrunnlagMedEgneAvdoedesBarn(): Grunnlag =
        Grunnlag(
            soeker = soekerTestopplysningerMap + opplysningsmapSoekerOverrides,
            familie =
                listOf(
                    soeskenTestopplysningerMap + opplysningsmapSoeskenOverrides,
                    avdoedTestopplysningerMap + avdoedesBarnMedEnDoed + opplysningsmapAvdoedOverrides,
                    gjenlevendeTestopplysningerMap + opplysningsmapGjenlevendeOverrides,
                    halvsoeskenTestopplysningerMap + opplysningsmapHalvsoeskenOverrides,
                ),
            sak = opplysningsmapSakOverrides,
            metadata = Metadata(1, 15),
        )

    fun hentPersonGalleri(): Persongalleri =
        Persongalleri(
            soeker = soeker.foedselsnummer.value,
            innsender = gjenlevende.foedselsnummer.value,
            soesken = listOf(soesken.foedselsnummer.value, halvsoesken.foedselsnummer.value),
            avdoed = listOf(avdoed.foedselsnummer.value),
            gjenlevende = listOf(gjenlevende.foedselsnummer.value),
        )

    fun hentGrunnlagMedUkjentAvdoed(): Grunnlag =
        Grunnlag(
            soeker = soekerTestopplysningerMap + opplysningsmapSoekerOverrides,
            familie =
                listOf(
                    gjenlevendeTestopplysningerMap + opplysningsmapGjenlevendeOverrides,
                ),
            sak = opplysningsmapSakOverrides,
            metadata = Metadata(1, 15),
        )
}
