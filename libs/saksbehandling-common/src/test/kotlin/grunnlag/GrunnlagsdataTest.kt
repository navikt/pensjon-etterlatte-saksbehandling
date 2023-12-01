package grunnlag

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.hentVergeadresse
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.BrevMottaker
import no.nav.etterlatte.libs.common.toJsonNode
import org.junit.jupiter.api.Test
import java.util.UUID

class GrunnlagsdataTest {
    @Test
    fun `skal hente verges adresse`() {
        val lagretAdresse =
            mapOf(
                "navn" to "ADVOKAT DANIEL TIGER",
                "foedselsnummer" to null,
                "adresse" to
                    mapOf(
                        "adresseType" to "NORSKPOSTADRESSE",
                        "adresselinje1" to "c/o ADVOKAT DANIEL TIGER",
                        "adresselinje2" to "POSTBOKS 1064",
                        "adresselinje3" to "1510 MOSS",
                        "postnummer" to "1510",
                        "poststed" to "MOSS",
                        "landkode" to "NO",
                        "land" to "NORGE",
                    ),
            ).toJsonNode()

        val grunnlagsdata: Grunnlagsdata<JsonNode> =
            mapOf(
                Opplysningstype.VERGES_ADRESSE to
                    Opplysning.Konstant(UUID.randomUUID(), mockk<Grunnlagsopplysning.Kilde>(), lagretAdresse),
            )

        val adresse: BrevMottaker = grunnlagsdata.hentVergeadresse()!!.verdi

        adresse.navn shouldBe "ADVOKAT DANIEL TIGER"
    }
}
