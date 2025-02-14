package no.nav.etterlatte.brev.dokument

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ModelTest {
    @Test
    fun `idType i AvsenderMottaker skal returnere verdien av type`() {
        val avsenderMottaker = AvsenderMottaker("123", "type123", "navn123", "land123", true)
        avsenderMottaker.idType shouldBe avsenderMottaker.type
    }
}
