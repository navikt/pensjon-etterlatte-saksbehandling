package common.klienter

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.common.klienter.SakSammendragResponse
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate

class PesysKlientImplTest {
    @Test
    fun `Skal klare aa mappe ISO-8601 dato fra PEN`() {
        val sammendragJson =
            """
            {
                "sakType": "UFOREP",
                "sakStatus": "LOPENDE",
                "fomDato": "2010-04-01",
                "tomDate": "2010-04-30"
            }
            """.trimIndent()

        val response = objectMapper.readValue<SakSammendragResponse>(sammendragJson)

        response shouldBe
            SakSammendragResponse(
                sakType = SakSammendragResponse.UFORE_SAKTYPE,
                sakStatus = SakSammendragResponse.Status.LOPENDE,
                fomDato = LocalDate.of(2010, 4, 1),
                tomDate = LocalDate.of(2010, 4, 30),
            )
    }
}
