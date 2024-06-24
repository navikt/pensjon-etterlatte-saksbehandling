package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.mq.DummyJmsConnectionFactory
import no.nav.etterlatte.mq.EtterlatteJmsConnectionFactory
import no.nav.etterlatte.tilbakekreving.TilbakekrevingHendelseRepository
import no.nav.etterlatte.tilbakekreving.readFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KravgrunnlagConsumerTest {
    private val connectionFactory: EtterlatteJmsConnectionFactory = DummyJmsConnectionFactory()
    private lateinit var kravgrunnlagConsumer: KravgrunnlagConsumer
    private lateinit var kravgrunnlagService: KravgrunnlagService
    private lateinit var tilbakekrevingHendelseRepository: TilbakekrevingHendelseRepository

    @BeforeEach
    fun beforeEach() {
        kravgrunnlagService = mockk(relaxed = true)
        tilbakekrevingHendelseRepository = mockk(relaxed = true)
        kravgrunnlagConsumer =
            KravgrunnlagConsumer(
                connectionFactory = connectionFactory,
                queue = QUEUE,
                kravgrunnlagService = kravgrunnlagService,
                hendelseRepository = tilbakekrevingHendelseRepository,
            ).also { it.start() }
    }

    @Test
    fun `skal motta kravgrunnlag og opprette tilbakekreving`() {
        simulerKravgrunnlagsmeldingFraTilbakekrevingskomponenten()

        verify(exactly = 1) {
            kravgrunnlagService.haandterKravgrunnlag(any())
        }
    }

    @Test
    fun `skal motta kravOgVedtakStatus`() {
        simulerKravOgVedtakStatusMeldingFraTilbakekrevingskomponenten()

        verify(exactly = 1) {
            kravgrunnlagService.haandterKravOgVedtakStatus(any())
        }
    }

    @Test
    fun `skal motta kravgrunnlag paa nytt dersom noe feiler`() {
        every { kravgrunnlagService.haandterKravgrunnlag(any()) } throws Exception("Noe feilet") andThen Unit
        simulerKravgrunnlagsmeldingFraTilbakekrevingskomponenten()

        verify(exactly = 2) {
            kravgrunnlagService.haandterKravgrunnlag(any())
        }
    }

    private fun simulerKravgrunnlagsmeldingFraTilbakekrevingskomponenten() {
        connectionFactory.send(queue = QUEUE, xml = readFile("/kravgrunnlag.xml"))
    }

    private fun simulerKravOgVedtakStatusMeldingFraTilbakekrevingskomponenten() {
        connectionFactory.send(queue = QUEUE, xml = readFile("/krav_og_vedtak_status.xml"))
    }

    companion object {
        const val QUEUE = "DEV.QUEUE.2"
    }
}
