package grunnlag

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.grunnlag.GrunnlagHenter
import no.nav.etterlatte.grunnlag.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import org.junit.jupiter.api.Test

class GrunnlagHenterTest {
    private val pdltjenesterKlient = mockk<PdlTjenesterKlientImpl>()
    private val grunnlagHenter = GrunnlagHenter(pdltjenesterKlient)

    @Test
    fun fetchGrunnlag_shouldGetRelevantInfo() {
        val sakType = SakType.BARNEPENSJON

        val grunnlagTestData = GrunnlagTestData()

        val listOf: List<Person> =
            listOf(
                grunnlagTestData.soeker,
                grunnlagTestData.gjenlevende,
            ) + grunnlagTestData.avdoede
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
            )
        coEvery {
            pdltjenesterKlient.hentPersongalleri(
                soekerFnr.value,
                any(),
                any(),
            )
        } returns grunnlagTestData.hentPersonGalleri()

        val fetched =
            runBlocking {
                grunnlagHenter.hentGrunnlagsdata(
                    Opplysningsbehov(
                        sakId1,
                        sakType,
                        Persongalleri(
                            soekerFnr.value,
                            grunnlagTestData.gjenlevende.foedselsnummer.value,
                            emptyList(),
                            grunnlagTestData.avdoede.map { it.foedselsnummer.value },
                            listOf(grunnlagTestData.gjenlevende.foedselsnummer.value),
                        ),
                        kilde = Grunnlagsopplysning.Saksbehandler.create("ident"),
                    ),
                )
            }
        fetched.personopplysninger.any { it.first == soekerFnr } shouldBe true
    }
}
