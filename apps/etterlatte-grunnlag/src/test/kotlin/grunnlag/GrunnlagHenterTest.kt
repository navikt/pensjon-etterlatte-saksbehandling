package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import mockPerson
import no.nav.etterlatte.grunnlag.adresse.Adresse
import no.nav.etterlatte.grunnlag.adresse.BrevMottaker
import no.nav.etterlatte.grunnlag.adresse.Foedselsnummer
import no.nav.etterlatte.grunnlag.adresse.PersondataAdresse
import no.nav.etterlatte.grunnlag.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.grunnlag.klienter.PersondataKlient
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.VergeEllerFullmektig
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import org.junit.jupiter.api.Test
import java.util.UUID

class GrunnlagHenterTest {
    private val pdltjenesterKlient = mockk<PdlTjenesterKlientImpl>()
    private val persondataKlient = mockk<PersondataKlient>()
    private val grunnlagHenter = GrunnlagHenter(pdltjenesterKlient, persondataKlient)

    private val vergesFnr = "09498230323"

    private val objectMapper: ObjectMapper
        get() {
            return jacksonObjectMapper().registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

    @Test
    fun fetchGrunnlag_shouldGetRelevantInfo() {
        val sakType = SakType.BARNEPENSJON

        val grunnlagTestData = GrunnlagTestData(opplysningsmapSoekerOverrides = mapOf(vergeOpplysning()))

        val listOf: List<Person> =
            listOf(
                grunnlagTestData.soeker,
                grunnlagTestData.gjenlevende,
                grunnlagTestData.avdoed,
            )
        listOf.forEach { person ->
            every { pdltjenesterKlient.hentOpplysningsperson(person.foedselsnummer.value, any(), sakType) } returns
                mockPerson().copy(foedselsnummer = OpplysningDTO(person.foedselsnummer, null))
            every { pdltjenesterKlient.hentPerson(person.foedselsnummer.value, any(), sakType) } returns
                person
        }
        val soekerFnr = grunnlagTestData.soeker.foedselsnummer
        every { pdltjenesterKlient.hentOpplysningsperson(soekerFnr.value, any(), sakType) } returns
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(soekerFnr, null),
                vergemaalEllerFremtidsfullmakt = soekersVerge(),
            )
        coEvery {
            pdltjenesterKlient.hentPersongalleri(
                soekerFnr.value,
                any(),
                any(),
            )
        } returns grunnlagTestData.hentPersonGalleri()

        val persondataAdresseVerge = mockk<PersondataAdresse>()
        every { persondataAdresseVerge.toBrevMottaker() } returns sampleVergeAdresse()
        every {
            persondataKlient.hentAdresseForVerge(soekerFnr.value)
        } returns persondataAdresseVerge

        val fetched =
            runBlocking {
                grunnlagHenter.hentGrunnlagsdata(
                    Opplysningsbehov(
                        1L,
                        sakType,
                        Persongalleri(
                            soekerFnr.value,
                            grunnlagTestData.gjenlevende.foedselsnummer.value,
                            emptyList(),
                            listOf(grunnlagTestData.avdoed.foedselsnummer.value),
                            listOf(grunnlagTestData.gjenlevende.foedselsnummer.value),
                        ),
                    ),
                )
            }
        fetched.personopplysninger.any { it.first == soekerFnr } shouldBe true

        val vergeAdresseOpplysning =
            fetched.saksopplysninger
                .first { it.opplysningType == Opplysningstype.VERGES_ADRESSE }
        val actualVergesAdresse: BrevMottaker =
            objectMapper.readValue(vergeAdresseOpplysning.opplysning.toString())

        actualVergesAdresse shouldBeEqual sampleVergeAdresse()
    }

    private fun vergeOpplysning() =
        Opplysningstype.VERGEMAALELLERFREMTIDSFULLMAKT to
            konstantOpplysning(
                listOf(
                    vergemaal(
                        vergesFnr,
                        "Vera Verge",
                        "personligeOgOekonomiskeInteresser",
                    ),
                ).toJsonNode(),
            )

    private fun soekersVerge(): List<OpplysningDTO<VergemaalEllerFremtidsfullmakt>> {
        return listOf(
            OpplysningDTO(
                VergemaalEllerFremtidsfullmakt(
                    "",
                    "",
                    VergeEllerFullmektig(
                        Folkeregisteridentifikator.of(vergesFnr),
                        "",
                        "oekonomiskeInteresser",
                        false,
                    ),
                ),
                "",
            ),
        )
    }

    private fun sampleVergeAdresse() =
        BrevMottaker(
            navn = "Tore",
            foedselsnummer = Foedselsnummer(vergesFnr),
            adresse =
                Adresse(
                    adresseType = "NORSKPOSTADRESSE",
                    adresselinje1 = "Vergestien 2",
                    land = "Norge",
                    landkode = "NO",
                ),
        )

    private fun konstantOpplysning(jsonNode: JsonNode) =
        Opplysning.Konstant(
            UUID.randomUUID(),
            Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, null),
            jsonNode,
        )

    @Suppress("SameParameterValue")
    private fun vergemaal(
        fnr: String,
        navn: String,
        omfang: String,
    ) = VergemaalEllerFremtidsfullmakt(
        null,
        null,
        VergeEllerFullmektig(
            Folkeregisteridentifikator.of(fnr),
            navn,
            omfang,
            false,
        ),
    )
}
