package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import mockPerson
import no.nav.etterlatte.grunnlag.adresse.PersondataAdresse
import no.nav.etterlatte.grunnlag.adresse.VergeAdresse
import no.nav.etterlatte.grunnlag.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.grunnlag.klienter.PersondataKlient
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.VergeEllerFullmektig
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import org.junit.jupiter.api.Test
import java.util.UUID

class GrunnlagFetcherTest {
    private val pdltjenesterKlient = mockk<PdlTjenesterKlientImpl>()
    private val persondataKlient = mockk<PersondataKlient>()
    private val grunnlagHenter = GrunnlagHenter(pdltjenesterKlient, persondataKlient)

    @Test
    fun fetchGrunnlag_shouldGetRelevantInfo() {
        val sakType = SakType.OMSTILLINGSSTOENAD
        val vergesIdent = Folkeregisteridentifikator.of("09438336165")

        val grunnlagTestData =
            GrunnlagTestData(
                opplysningsmapSoekerOverrides =
                    mapOf(
                        Pair(Opplysningstype.VERGEMAALELLERFREMTIDSFULLMAKT, testdataOverrideVerge(vergesIdent)),
                    ),
            )

        mapOf(
            "soeker" to grunnlagTestData.soeker,
            "innsender" to grunnlagTestData.gjenlevende,
            "avdoed" to grunnlagTestData.avdoed,
            "gjenlevende" to grunnlagTestData.gjenlevende,
        ).forEach { (key, person) ->
            every { pdltjenesterKlient.hentOpplysningsperson(key, any(), sakType) } returns
                mockPerson().copy(foedselsnummer = OpplysningDTO(person.foedselsnummer, null))
            every { pdltjenesterKlient.hentPerson(key, any(), sakType) } returns
                person
        }

        val persondataAdresseVerge = mockk<PersondataAdresse>()
        every { persondataAdresseVerge.toVergeAdresse() } returns sampleVergeAdresse()
        every { persondataKlient.hentAdresseForVerge(vergesIdent) } returns persondataAdresseVerge

        val fetched =
            runBlocking {
                grunnlagHenter.hentGrunnlagsdata(
                    Opplysningsbehov(
                        1L,
                        sakType,
                        Persongalleri(
                            "soeker",
                            "innsender",
                            listOf("soesken"),
                            listOf("avdoed"),
                            listOf("gjenlevende"),
                        ),
                    ),
                )
            }
        fetched.personopplysninger.any { it.first == grunnlagTestData.soeker.foedselsnummer } shouldBe true
        fetched.saksopplysninger.any { isExpectedVergeAdresse(it, vergesIdent) } shouldBe true
    }

    private fun testdataOverrideVerge(vergesIdent: Folkeregisteridentifikator) =
        Opplysning.Konstant(
            UUID.randomUUID(),
            kilde,
            listOf(
                VergemaalEllerFremtidsfullmakt(
                    "",
                    "",
                    VergeEllerFullmektig(vergesIdent, null, null, null),
                ),
            ).toJsonNode(),
        )

    private fun isExpectedVergeAdresse(
        it: Grunnlagsopplysning<JsonNode>,
        vergesIdent: Folkeregisteridentifikator,
    ): Boolean {
        if (it.opplysningType == Opplysningstype.VERGES_ADRESSER) {
            val vergeAdresseMap: Map<Folkeregisteridentifikator, VergeAdresse> =
                objectMapper.readValue(
                    it.opplysning.toJson(),
                    jacksonTypeRef<Map<Folkeregisteridentifikator, VergeAdresse>>(),
                )
            return sampleVergeAdresse() == vergeAdresseMap[vergesIdent]
        }
        return false
    }

    private fun sampleVergeAdresse() =
        VergeAdresse(
            "NORSKPOSTADRESSE",
            "Vergestien 2",
            land = "Norge",
            landkode = "NO",
        )
}
