package no.nav.etterlatte.rapidsandrivers

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RetryOgFeilhaandteringKtTest {
    private val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    @Test
    fun `feilhaandtering kaster ikke feilen videre, men publiserer på feilkø`() {
        val packet =
            JsonMessage("{}", MessageProblems(""))
        val context = mockk<MessageContext> { every { publish(any<String>()) } returns Unit }
        var antallForsoek = 0
        withRetryOgFeilhaandtering(
            packet = packet,
            context = context,
            feilendeSteg = "feilende steg",
            kontekst = Kontekst.REGULERING,
        ) {
            antallForsoek++
            throw RuntimeException()
        }
        verify(exactly = 1) { context.publish(any<String>()) }
        assertEquals(Kontekst.REGULERING.retries + 1, antallForsoek)
    }

    @Test
    fun `feilhaandtering gjoer ingenting hvis ingenting feiler`() {
        val packet = JsonMessage("{}", MessageProblems(""))
        val context = mockk<MessageContext>().also { every { it.publish(any<String>()) } returns Unit }
        var antallForsoek = 0
        withRetryOgFeilhaandtering(
            packet = packet,
            context = context,
            feilendeSteg = "feilende steg",
            kontekst = Kontekst.REGULERING,
        ) {
            antallForsoek++
        }
        verify(exactly = 0) { context.publish(any<String>()) }
        assertEquals(1, antallForsoek)
    }

    @Test
    fun `Feilhaandtering skal kjoere ny retry hvis feiler for doedshendelse`() {
        val packet =
            JsonMessage("{}", MessageProblems(""))
        val context = mockk<MessageContext>().also { every { it.publish(any<String>()) } returns Unit }
        var antallForsoek = 0
        withRetryOgFeilhaandtering(
            packet = packet,
            context = context,
            feilendeSteg = "Feilende steg",
            kontekst = Kontekst.DOEDSHENDELSE,
        ) {
            antallForsoek++
            throw RuntimeException()
        }
        verify(exactly = 1) { context.publish(any<String>()) }
        assertEquals(Kontekst.DOEDSHENDELSE.retries + 1, antallForsoek)
    }
}
