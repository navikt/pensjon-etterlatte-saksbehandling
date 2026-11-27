import no.nav.etterlatte.brev.BrevBatchRequest
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.serialize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BrevBatchKontraktTest {
    @Test
    fun `sak-ID-er er tall og Slate er en JSON-array`() {
        val json =
            """
            {
              "sakIder": [100001, 100002, 100001],
              "tittel": " Informasjon ",
              "innhold": [{"type":"paragraph","children":[{"text":" Tekst med linjeskift.\n " }]}],
              "spraak": "nn"
            }
            """.trimIndent()
        val request = deserialize<BrevBatchRequest>(json)
        assertEquals(listOf(SakId(100001), SakId(100002), SakId(100001)), request.sakIder)
        assertEquals(" Informasjon ", request.tittel)
        assertEquals(Spraak.NN, request.spraak)
        assertEquals(
            Slate(listOf(Slate.Element(Slate.ElementType.PARAGRAPH, listOf(Slate.InnerElement(text = " Tekst med linjeskift.\n "))))),
            request.innhold,
        )
        val serialized = serialize(request)
        assertTrue(serialized.contains("\"sakIder\":[100001,100002,100001]"))
        assertTrue(serialized.contains("\"innhold\":["))
        assertEquals(request, deserialize<BrevBatchRequest>(serialized))
    }
}
