package no.nav.etterlatte.brev

import org.junit.jupiter.api.Test

class BrevkoderTest {
    @Test
    fun name() {
        Brevkoder.entries
            .map {
                it.ferdigstilling
            }.distinct()
            .forEach(::println)
    }
}
