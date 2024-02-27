package common.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.common.klienter.SakSammendragResponse
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PesysKlientImplTest {
    @Test
    fun `Skal klare aa mappe ISO-8601 dato fra PEN`() {
        val sammendragJson =
            """
            {
                "sakType": "UFOREP",
                "sakStatus": "LOPENDE",
                "fomDato": "2010-04-01T00:00:00+0200",
                "tomDate": "2010-04-01T00:00:00+0200"
            }
            """.trimIndent()

        val response = objectMapper.readValue<SakSammendragResponse>(sammendragJson)

        response shouldBe
            SakSammendragResponse(
                sakType = SakSammendragResponse.UFORE_SAKTYPE,
                sakStatus = SakSammendragResponse.Status.LOPENDE,
                fomDato = LocalDate.of(2010, 4, 1),
                tomDate = LocalDate.of(2010, 4, 1),
            )
    }
}
