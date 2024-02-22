package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class UtbetalingsvedtakTest {
    @Test
    fun `Skal objectmappe string til Utbetalingsvedtak`() {
        val utbetalingsvedtakJSON =
            """
            {
              "vedtakId": 286,
              "sak": {
                "ident": "24111164246",
                "id": 78,
                "sakType": "BARNEPENSJON"
              },
              "behandling": {
                "type": "FØRSTEGANGSBEHANDLING",
                "id": "7b555144-0ea8-481a-8075-a0faa7a87876"
              },
              "pensjonTilUtbetaling": [
                {
                  "id": 72,
                  "periode": {
                    "fom": "2022-09",
                    "tom": null
                  },
                  "beloep": 9290.0,
                  "type": "UTBETALING"
                }
              ],
              "vedtakFattet": {
                "ansvarligSaksbehandler": "Z994985",
                "ansvarligEnhet": "123"
              },
              "attestasjon": {
                "attestant": "Z994985",
                "attesterendeEnhet": "123"
              }
            }
            """.trimIndent()

        assertDoesNotThrow { objectMapper.readValue<Utbetalingsvedtak>(utbetalingsvedtakJSON) }
    }

    @Test
    fun `Skal objectmappe OMS vedtak til Utbetalingsvedtak`() {
        val utbetalingsvedtakJSON =
            """
            {
              "vedtakId": 286,
              "sak": {
                "ident": "24111164246",
                "id": 78,
                "sakType": "OMSTILLINGSSTOENAD"
              },
              "behandling": {
                "type": "FØRSTEGANGSBEHANDLING",
                "id": "7b555144-0ea8-481a-8075-a0faa7a87876"
              },
              "pensjonTilUtbetaling": [
                {
                  "id": 72,
                  "periode": {
                    "fom": "2022-09",
                    "tom": null
                  },
                  "beloep": 9290.0,
                  "type": "UTBETALING"
                }
              ],
              "vedtakFattet": {
                "ansvarligSaksbehandler": "Z994985",
                "ansvarligEnhet": "123"
              },
              "attestasjon": {
                "attestant": "Z994985",
                "attesterendeEnhet": "123"
              }
            }
            """.trimIndent()

        assertDoesNotThrow { objectMapper.readValue<Utbetalingsvedtak>(utbetalingsvedtakJSON) }
    }
}
