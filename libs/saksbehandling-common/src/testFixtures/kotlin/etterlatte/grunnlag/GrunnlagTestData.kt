package no.nav.etterlatte.libs.testdata.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.grunnlag.GenerellKilde
import no.nav.etterlatte.grunnlag.Personopplysning
import no.nav.etterlatte.grunnlag.PersonopplysningerResponse
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.lagOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.AVDOEDESBARN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.AVDOED_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.INNSENDER_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.PERSONGALLERI_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SOEKER_PDL_V1
import no.nav.etterlatte.libs.common.person.AvdoedesBarn
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.pdl.personTestData
import java.time.LocalDateTime
import java.util.UUID
import java.util.UUID.randomUUID

data class GrunnlagTestData(
    val opplysningsmapSoekerOverrides: Map<Opplysningstype, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapSoeskenOverrides: Map<Opplysningstype, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapAvdoedOverrides: Map<Opplysningstype, Opplysning<JsonNode>> = emptyMap(),
    val opplysningsmapAvdoedeOverrides: List<Map<Opplysningstype, Opplysning<JsonNode>>> = emptyList(),
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

    val sak: Map<Opplysningstype, Opplysning<JsonNode>> =
        opplysningsmapSakOverrides +
            mapOf(
                PERSONGALLERI_V1 to
                    Opplysning.Konstant.create(
                        lagOpplysning(
                            opplysningsType = PERSONGALLERI_V1,
                            kilde = Grunnlagsopplysning.automatiskSaksbehandler,
                            opplysning = hentPersonGalleri().toJsonNode(),
                            periode = null,
                        ),
                    ),
            )

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
                            listOf(
                                personTestData(
                                    soeskenTestopplysningerMap +
                                        mapOf(
                                            Opplysningstype.DOEDSDATO
                                                to
                                                Opplysning.Konstant(
                                                    randomUUID(),
                                                    kilde,
                                                    LocalDateTime.parse("2022-08-17T00:00:00").toJsonNode(),
                                                ),
                                        ) +
                                        opplysningsmapSoeskenOverrides,
                                ),
                            ),
                        ).toJsonNode(),
                    ),
            )

    val avdoede
        get() =
            when (opplysningsmapAvdoedeOverrides.isEmpty()) {
                true -> listOf(personTestData(avdoedTestopplysningerMap + avdoedesBarnOverrides + opplysningsmapAvdoedOverrides))
                false -> opplysningsmapAvdoedeOverrides.map { personTestData(it) }
            }

    fun hentOpplysningsgrunnlag(): Grunnlag =
        Grunnlag(
            soeker = soekerTestopplysningerMap + opplysningsmapSoekerOverrides,
            familie =
                listOfNotNull(
                    soeskenTestopplysningerMap + opplysningsmapSoeskenOverrides,
                    (avdoedTestopplysningerMap + avdoedesBarnOverrides + opplysningsmapAvdoedOverrides)
                        .takeIf { opplysningsmapAvdoedeOverrides.isEmpty() },
                    gjenlevendeTestopplysningerMap + opplysningsmapGjenlevendeOverrides,
                    halvsoeskenTestopplysningerMap + opplysningsmapHalvsoeskenOverrides,
                ) + opplysningsmapAvdoedeOverrides,
            sak = sak,
            metadata = Metadata(sakId1, 15),
        )

    fun hentGrunnlagMedEgneAvdoedesBarn(): Grunnlag =
        Grunnlag(
            soeker = soekerTestopplysningerMap + opplysningsmapSoekerOverrides,
            familie =
                listOfNotNull(
                    soeskenTestopplysningerMap + opplysningsmapSoeskenOverrides,
                    (avdoedTestopplysningerMap + avdoedesBarnMedEnDoed + opplysningsmapAvdoedOverrides)
                        .takeIf { opplysningsmapAvdoedeOverrides.isEmpty() },
                    gjenlevendeTestopplysningerMap + opplysningsmapGjenlevendeOverrides,
                    halvsoeskenTestopplysningerMap + opplysningsmapHalvsoeskenOverrides,
                ) + opplysningsmapAvdoedeOverrides,
            sak = sak,
            metadata = Metadata(sakId1, 15),
        )

    fun hentPersonGalleri(): Persongalleri =
        Persongalleri(
            soeker = soeker.foedselsnummer.value,
            innsender = gjenlevende.foedselsnummer.value,
            soesken = listOf(soesken.foedselsnummer.value, halvsoesken.foedselsnummer.value),
            avdoed = avdoede.map { it.foedselsnummer.value },
            gjenlevende = listOf(gjenlevende.foedselsnummer.value),
        )

    fun hentGrunnlagMedUkjentAvdoed(): Grunnlag =
        Grunnlag(
            soeker = soekerTestopplysningerMap + opplysningsmapSoekerOverrides,
            familie =
                listOf(
                    gjenlevendeTestopplysningerMap + opplysningsmapGjenlevendeOverrides,
                ),
            sak = sak,
            metadata = Metadata(sakId1, 1),
        )

    fun hentPersonopplysninger(): PersonopplysningerResponse =
        PersonopplysningerResponse(
            innsender = Personopplysning(INNSENDER_PDL_V1, UUID.randomUUID(), kilde(), gjenlevende),
            soeker = Personopplysning(SOEKER_PDL_V1, randomUUID(), kilde(), soeker),
            avdoede = avdoede.map { Personopplysning(AVDOED_PDL_V1, randomUUID(), kilde(), it) },
            gjenlevende = listOf(Personopplysning(GJENLEVENDE_FORELDER_PDL_V1, randomUUID(), kilde(), gjenlevende)),
            annenForelder = null,
        )

    private fun kilde() = GenerellKilde("", Tidspunkt.now(), "")
}
