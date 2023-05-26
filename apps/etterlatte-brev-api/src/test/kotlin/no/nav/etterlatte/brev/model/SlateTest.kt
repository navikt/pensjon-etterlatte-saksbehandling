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
            value.size shouldBe 5
            value[0].type shouldBe Slate.ElementType.HEADING_ONE
            value[0].children.size shouldBe 1
            value[2].children.size shouldBe 3
        }
    }
}