package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.mq.EtterlatteJmsConnectionFactory
import no.nav.etterlatte.testsupport.readFile
import no.nav.etterlatte.tilbakekreving.DummyJmsConnectionFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigInteger

class KravgrunnlagConsumerTest {

    private val connectionFactory: EtterlatteJmsConnectionFactory = DummyJmsConnectionFactory()
    private lateinit var kravgrunnlagConsumer: KravgrunnlagConsumer
    private lateinit var kravgrunnlagService: KravgrunnlagService

    @BeforeEach
    fun beforeEach() {
        kravgrunnlagService = mockk(relaxed = true)
        kravgrunnlagConsumer = KravgrunnlagConsumer(connectionFactory, QUEUE, kravgrunnlagService).also { it.start() }
    }

    @Test
    fun `skal motta kravgrunnlag og opprette tilbakekreving`() {
        simulerKravgrunnlagsmeldingFraTilbakekrevingskomponenten()

        verify(exactly = 1) {
            kravgrunnlagService.opprettTilbakekreving(match { it.kravgrunnlagId == BigInteger.valueOf(302004) })
        }
    }

    @Test
    fun `skal motta kravgrunnlag paa nytt dersom noe feiler`() {
        every { kravgrunnlagService.opprettTilbakekreving(any()) } throws Exception("Noe feilet") andThen Unit
        simulerKravgrunnlagsmeldingFraTilbakekrevingskomponenten()

        verify(exactly = 2) {
            kravgrunnlagService.opprettTilbakekreving(match { it.kravgrunnlagId == BigInteger.valueOf(302004) })
        }
    }

    private fun simulerKravgrunnlagsmeldingFraTilbakekrevingskomponenten() {
        connectionFactory.send(queue = QUEUE, xml = readFile("/kravgrunnlag.xml"))
    }

    companion object {
        const val QUEUE = "DEV.QUEUE.2"
    }
}