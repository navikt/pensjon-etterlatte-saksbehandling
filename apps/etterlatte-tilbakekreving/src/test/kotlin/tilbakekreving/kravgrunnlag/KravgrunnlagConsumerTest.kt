package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.mq.DummyJmsConnectionFactory
import no.nav.etterlatte.mq.EtterlatteJmsConnectionFactory
import no.nav.etterlatte.tilbakekreving.TilbakekrevingHendelse
import no.nav.etterlatte.tilbakekreving.TilbakekrevingHendelseRepository
import no.nav.etterlatte.tilbakekreving.TilbakekrevingHendelseStatus
import no.nav.etterlatte.tilbakekreving.TilbakekrevingHendelseType
import no.nav.etterlatte.tilbakekreving.readFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

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
            tilbakekrevingHendelseRepository.hentSisteTilbakekrevingHendelse(any(), any())
            tilbakekrevingHendelseRepository.lagreTilbakekrevingHendelse(any(), any(), any(), any())
            kravgrunnlagService.haandterKravgrunnlag(any())
            tilbakekrevingHendelseRepository.ferdigstillTilbakekrevingHendelse(any(), any())
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
    fun `skal feile ved mottak av kravgrunnlag dersom forrige hendelse for sak ikke er ferdigstilt`() {
        every { tilbakekrevingHendelseRepository.hentSisteTilbakekrevingHendelse(any(), any()) } returns
            tilbakekrevingHendelse(
                type = TilbakekrevingHendelseType.KRAVGRUNNLAG_MOTTATT,
                status = TilbakekrevingHendelseStatus.MOTTATT,
            )

        simulerKravgrunnlagsmeldingFraTilbakekrevingskomponenten()

        verify(exactly = 4) {
            // 3 retries
            tilbakekrevingHendelseRepository.hentSisteTilbakekrevingHendelse(any(), any())
        }
        verify(exactly = 0) {
            tilbakekrevingHendelseRepository.lagreTilbakekrevingHendelse(any(), any(), any(), any())
            kravgrunnlagService.haandterKravgrunnlag(any())
            tilbakekrevingHendelseRepository.ferdigstillTilbakekrevingHendelse(any(), any())
        }
    }

    @Test
    fun `skal feile ved mottak av kravOgVedtakStatus dersom forrige hendelse for sak ikke er ferdigstilt`() {
        every { tilbakekrevingHendelseRepository.hentSisteTilbakekrevingHendelse(any(), any()) } returns
            tilbakekrevingHendelse(
                type = TilbakekrevingHendelseType.KRAV_VEDTAK_STATUS_MOTTATT,
                status = TilbakekrevingHendelseStatus.MOTTATT,
            )

        simulerKravgrunnlagsmeldingFraTilbakekrevingskomponenten()

        verify(exactly = 4) {
            // 3 retries
            tilbakekrevingHendelseRepository.hentSisteTilbakekrevingHendelse(any(), any())
        }
        verify(exactly = 0) {
            tilbakekrevingHendelseRepository.lagreTilbakekrevingHendelse(any(), any(), any(), any())
            kravgrunnlagService.haandterKravgrunnlag(any())
            tilbakekrevingHendelseRepository.ferdigstillTilbakekrevingHendelse(any(), any())
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

    private fun tilbakekrevingHendelse(
        type: TilbakekrevingHendelseType,
        status: TilbakekrevingHendelseStatus,
    ) = TilbakekrevingHendelse(
        id = UUID.randomUUID(),
        opprettet = Tidspunkt.now(),
        sakId = 1,
        payload = "payload",
        status = status,
        type = type,
        jmsTimestamp = Tidspunkt.now(),
    )

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
