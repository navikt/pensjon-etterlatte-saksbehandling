package no.nav.etterlatte.libs.common.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC

internal class LogUtilsTest {
    private val testCorrelationID = "TEST_CORRELATION_ID"

    @Test
    fun `Sjekk MDC correlation ID - vanlig flyt`() {
        assertNull(MDC.get(CORRELATION_ID))

        withLogContext(correlationId = testCorrelationID) {
            assertEquals(MDC.get(CORRELATION_ID), testCorrelationID)
        }

        assertNull(MDC.get(CORRELATION_ID))
    }

    @Test
    fun `Sjekk MDC correlation ID - exception flyt`() {
        assertNull(MDC.get(CORRELATION_ID))

        assertThrows<RuntimeException> {
            withLogContext(correlationId = testCorrelationID) {
                assertEquals(MDC.get(CORRELATION_ID), testCorrelationID)

                throw RuntimeException("Oops")
            }

            assertEquals(MDC.get(CORRELATION_ID), testCorrelationID)
        }
    }
}
