package no.nav.etterlatte.brev.model

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.deserialize
import org.junit.jupiter.api.Test

internal class SlateTest {
    @Test
    fun serde() {
        val originalJson = this.javaClass.getResource("/brev/oms/slate_serde.json")!!.readText()

        val deserialized = deserialize<Slate>(originalJson)

        // Bare noen stikkprøver
        with(deserialized) {
            elements.size shouldBe 6
            elements[0].type shouldBe Slate.ElementType.HEADING_TWO
            elements[0].children.size shouldBe 1
            elements[2].children.size shouldBe 3

            elements.last().type shouldBe Slate.ElementType.BULLETED_LIST
            elements.last().children.count { it.type == Slate.ElementType.LIST_ITEM } shouldBe 3
        }
    }
}
