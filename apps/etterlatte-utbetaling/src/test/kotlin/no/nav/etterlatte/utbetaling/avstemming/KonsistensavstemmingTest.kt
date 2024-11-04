package no.nav.etterlatte.utbetaling.avstemming

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.oppdragForKonsistensavstemming
import no.nav.etterlatte.utbetaling.oppdragslinjeForKonsistensavstemming
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.reflect.full.memberProperties

class KonsistensavstemmingTest {
    @Nested
    inner class Serializers {
        @Test
        fun `verifiser at serialiser (custom) og deserialiser (med standard Jackson) gir samme resultat`() {
            val testdata = opprettKonsistensavstemmingAlleFelterUtfylt()

            for (prop in Konsistensavstemming::class.memberProperties) {
                prop.get(testdata) shouldNotBe null
            }

            testdata.loependeUtbetalinger.forEach { oppdrag ->
                for (prop in OppdragForKonsistensavstemming::class.memberProperties) {
                    prop.get(oppdrag) shouldNotBe null
                }

                oppdrag.utbetalingslinjer.forEach { osLinje ->
                    for (prop in OppdragslinjeForKonsistensavstemming::class.memberProperties) {
                        prop.get(osLinje) shouldNotBe null
                    }
                }
            }

            // Custom serializer
            val serialized = testdata.toJson()

            // Standard (reflection-based) deserializer
            val deSerialized = objectMapper.readValue(serialized, Konsistensavstemming::class.java)

            // Kun data classes, så equality kan benyttes uten videre
            deSerialized shouldBeEqual testdata
        }
    }

    private fun opprettKonsistensavstemmingAlleFelterUtfylt(opprettetTilOgMed: Tidspunkt = Tidspunkt.now()): Konsistensavstemming {
        val oppdragslinjer =
            listOf(
                oppdragslinjeForKonsistensavstemming(
                    id = 125L,
                    beloep = BigDecimal(11000),
                    fraOgMed = LocalDate.of(2023, 10, 7),
                    tilOgMed = LocalDate.of(2023, 11, 14),
                    forrigeUtbetalingslinjeId = 123L,
                ),
                oppdragslinjeForKonsistensavstemming(
                    id = 126L,
                    beloep = BigDecimal(12000),
                    fraOgMed = LocalDate.of(2023, 12, 1),
                    tilOgMed = LocalDate.of(2023, 12, 31),
                    forrigeUtbetalingslinjeId = 125L,
                ),
            )
        val oppdrag = oppdragForKonsistensavstemming(oppdragslinjeForKonsistensavstemming = oppdragslinjer)

        return Konsistensavstemming(
            id = UUIDBase64(),
            sakType = Saktype.BARNEPENSJON,
            opprettet = Tidspunkt.now(),
            avstemmingsdata = "noe slags data her",
            loependeFraOgMed = Tidspunkt.now(),
            opprettetTilOgMed = opprettetTilOgMed,
            loependeUtbetalinger = listOf(oppdrag),
        )
    }
}
