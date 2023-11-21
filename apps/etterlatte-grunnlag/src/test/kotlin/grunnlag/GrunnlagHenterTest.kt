package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.equals.shouldBeEqual
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
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import org.junit.jupiter.api.Test

class GrunnlagHenterTest {
    private val pdltjenesterKlient = mockk<PdlTjenesterKlientImpl>()
    private val persondataKlient = mockk<PersondataKlient>()
    private val grunnlagHenter = GrunnlagHenter(pdltjenesterKlient, persondataKlient)

    private val objectMapper: ObjectMapper
        get() {
            return jacksonObjectMapper().registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

    @Test
    fun fetchGrunnlag_shouldGetRelevantInfo() {
        val sakType = SakType.OMSTILLINGSSTOENAD

        val grunnlagTestData = GrunnlagTestData()

        val listOf: List<Person> =
            listOf(
                grunnlagTestData.soeker,
                grunnlagTestData.gjenlevende,
                grunnlagTestData.avdoed,
                grunnlagTestData.gjenlevende,
            )
        listOf.forEach { person ->
            every { pdltjenesterKlient.hentOpplysningsperson(person.foedselsnummer.value, any(), sakType) } returns
                mockPerson().copy(foedselsnummer = OpplysningDTO(person.foedselsnummer, null))
            every { pdltjenesterKlient.hentPerson(person.foedselsnummer.value, any(), sakType) } returns
                person
        }

        val persondataAdresseVerge = mockk<PersondataAdresse>()
        every { persondataAdresseVerge.toVergeAdresse() } returns sampleVergeAdresse()
        every {
            persondataKlient.hentAdresseForVerge(grunnlagTestData.soeker.foedselsnummer.value)
        } returns persondataAdresseVerge

        val fetched =
            runBlocking {
                grunnlagHenter.hentGrunnlagsdata(
                    Opplysningsbehov(
                        1L,
                        sakType,
                        Persongalleri(
                            grunnlagTestData.soeker.foedselsnummer.value,
                            grunnlagTestData.gjenlevende.foedselsnummer.value,
                            emptyList(),
                            listOf(grunnlagTestData.avdoed.foedselsnummer.value),
                            listOf(grunnlagTestData.gjenlevende.foedselsnummer.value),
                        ),
                    ),
                )
            }
        fetched.personopplysninger.any { it.first == grunnlagTestData.soeker.foedselsnummer } shouldBe true

        val vergeAdresseOpplysning =
            fetched.saksopplysninger
                .first { it.opplysningType == Opplysningstype.VERGES_ADRESSE }
        val actualVergesAdresse: VergeAdresse =
            objectMapper.readValue(vergeAdresseOpplysning.opplysning.toString())

        actualVergesAdresse shouldBeEqual sampleVergeAdresse()
    }

    private fun sampleVergeAdresse() =
        VergeAdresse(
            "NORSKPOSTADRESSE",
            "Vergestien 2",
            land = "Norge",
            landkode = "NO",
        )
}
