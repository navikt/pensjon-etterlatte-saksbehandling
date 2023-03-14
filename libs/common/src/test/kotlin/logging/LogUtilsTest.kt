package no.nav.etterlatte.libs.common.logging

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC

internal class LogUtilsTest {
    private val testCorrelationID = "TEST_CORRELATION_ID"

    @Test
    fun `Sjekk MDC correlation ID - vanlig flyt`() {
        assert(MDC.get(CORRELATION_ID) == null)

        withLogContext(correlationId = testCorrelationID) {
            assert(MDC.get(CORRELATION_ID) == testCorrelationID)
        }

        assert(MDC.get(CORRELATION_ID) == null)
    }

    @Test
    fun `Sjekk MDC correlation ID - exception flyt`() {
        assert(MDC.get(CORRELATION_ID) == null)

        assertThrows<RuntimeException> {
            withLogContext(correlationId = testCorrelationID) {
                assert(MDC.get(CORRELATION_ID) == testCorrelationID)

                throw RuntimeException("Oops")
            }

            assert(MDC.get(CORRELATION_ID) == testCorrelationID)
        }
    }
}