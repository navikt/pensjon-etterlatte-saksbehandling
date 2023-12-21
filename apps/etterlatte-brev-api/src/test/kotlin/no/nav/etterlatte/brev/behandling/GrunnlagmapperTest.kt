package no.nav.etterlatte.brev.behandling

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.MottakerAdresse
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.person.VergeEllerFullmektig
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import org.junit.jupiter.api.Test
import java.util.UUID

class GrunnlagmapperTest {
    private val vergesAdresseFnr = "31488338237"
    private val pdlVergeOekonomiskFnr = "17418340118"
    private val pdlVergePersonligFnr = "27458328671"

    @Test
    fun `mapVerge henter vergemaal hvis definert i grunnlag for sak og søker`() {
        val opplysningsgrunnlag =
            GrunnlagTestData(
                opplysningsmapSoekerOverrides =
                    mapOf(
                        Opplysningstype.VERGEMAALELLERFREMTIDSFULLMAKT to
                            opprettOpplysning(
                                listOf(
                                    vergemaal(
                                        pdlVergeOekonomiskFnr,
                                        "Vera Verge",
                                        "personligeOgOekonomiskeInteresser",
                                    ),
                                    vergemaal(
                                        pdlVergePersonligFnr,
                                        "Petter Personlig",
                                        "personligeInteresser",
                                    ),
                                ).toJsonNode(),
                            ),
                    ),
                opplysningsmapSakOverrides =
                    mapOf(
                        Opplysningstype.VERGES_ADRESSE to
                            Opplysning.Konstant(
                                UUID.randomUUID(),
                                mockk<Grunnlagsopplysning.Kilde>(),
                                lagretVergeAdresse(),
                            ),
                    ),
            ).hentOpplysningsgrunnlag()

        val verge = opplysningsgrunnlag.mapVerge(SakType.BARNEPENSJON, UUID.randomUUID(), null)!! as Vergemaal

        verge.navn() shouldBe "ADVOKAT VERA V VERGE"
        verge.mottaker.foedselsnummer shouldBe MottakerFoedselsnummer(vergesAdresseFnr)
        verge.mottaker.navn shouldBe "ADVOKAT VERA V VERGE"
        verge.mottaker.adresse shouldBe
            MottakerAdresse(
                adresseType = "NORSKPOSTADRESSE",
                adresselinje1 = "c/o ADVOKAT VERA",
                adresselinje2 = "POSTBOKS 1064",
                adresselinje3 = "1510 MOSS",
                postnummer = "1510",
                poststed = "MOSS",
                landkode = "NO",
                land = "NORGE",
            )
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

    private fun lagretVergeAdresse() =
        mapOf(
            "navn" to "ADVOKAT VERA V VERGE",
            "foedselsnummer" to mapOf("value" to vergesAdresseFnr),
            "adresse" to
                mapOf(
                    "adresseType" to "NORSKPOSTADRESSE",
                    "adresselinje1" to "c/o ADVOKAT VERA",
                    "adresselinje2" to "POSTBOKS 1064",
                    "adresselinje3" to "1510 MOSS",
                    "postnummer" to "1510",
                    "poststed" to "MOSS",
                    "landkode" to "NO",
                    "land" to "NORGE",
                ),
        ).toJsonNode()

    private fun opprettOpplysning(jsonNode: JsonNode) =
        Opplysning.Konstant(
            UUID.randomUUID(),
            Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, null),
            jsonNode,
        )
}
