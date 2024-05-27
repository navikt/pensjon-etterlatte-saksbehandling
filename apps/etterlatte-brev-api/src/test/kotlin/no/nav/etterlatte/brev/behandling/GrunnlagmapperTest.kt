package no.nav.etterlatte.brev.behandling

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDSELSNUMMER
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SOEKER_PDL_V1
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.person.VergeEllerFullmektig
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.pdl.personTestData
import org.junit.jupiter.api.Test
import java.util.UUID

class GrunnlagmapperTest {
    private val pdlVergeOekonomiskFnr = "17418340118"
    private val pdlVergePersonligFnr = "27458328671"

    private val adresseService = mockk<AdresseService>()

    @Test
    fun `mapVerge henter vergemaal hvis definert i grunnlag for sak og søker`() {
        val soeker =
            personTestData(mapOf(FOEDSELSNUMMER to opprettOpplysning(SOEKER_FOEDSELSNUMMER.toJsonNode())))
                .copy(
                    vergemaalEllerFremtidsfullmakt =
                        listOf(
                            vergemaal(pdlVergeOekonomiskFnr, "", "personligeOgOekonomiskeInteresser"),
                            vergemaal(pdlVergePersonligFnr, "", "personligeInteresser"),
                        ),
                )
        val opplysningsgrunnlag =
            GrunnlagTestData(
                opplysningsmapSoekerOverrides =
                    mapOf(
                        SOEKER_PDL_V1 to opprettOpplysning(soeker.toJsonNode()),
                    ),
                opplysningsmapSakOverrides = emptyMap(),
            ).hentOpplysningsgrunnlag()

        coEvery {
            adresseService.hentMottakerAdresse(any(), pdlVergeOekonomiskFnr)
        } returns lagretVergeAdresse("Vera Verge", pdlVergeOekonomiskFnr)
        val verge = opplysningsgrunnlag.mapVerge(SakType.BARNEPENSJON, null, adresseService)!! as Vergemaal

        verge.navn() shouldBe "Vera Verge"
        verge.foedselsnummer shouldBe Folkeregisteridentifikator.of(pdlVergeOekonomiskFnr)
    }

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

    private fun lagretVergeAdresse(
        navn: String,
        fnr: String,
    ) = Mottaker(
        navn,
        MottakerFoedselsnummer(fnr),
        "orgnr",
        mockk(),
    )

    private fun opprettOpplysning(jsonNode: JsonNode) =
        Opplysning.Konstant(
            UUID.randomUUID(),
            Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, null),
            jsonNode,
        )
}
