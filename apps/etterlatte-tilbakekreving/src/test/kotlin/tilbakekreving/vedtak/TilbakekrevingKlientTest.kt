package tilbakekreving.vedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.tilbakekreving.VedtakId
import no.nav.etterlatte.tilbakekreving.vedtak.TilbakekrevingKlient
import no.nav.etterlatte.tilbakekreving.vedtak.Tilbakekrevingsvedtak
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingPortType
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import no.nav.tilbakekreving.typer.v1.MmelDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class TilbakekrevingKlientTest {
    private val tilbakekrevingPortType = mockk<TilbakekrevingPortType>()
    private val tilbakekrevingKlient = TilbakekrevingKlient(tilbakekrevingPortType)

    @Test
    fun `skal kunne sende tilbakekrevingvedtak og haandtere ok fra tilbakekrevingskomponenten`() {
        every { tilbakekrevingPortType.tilbakekrevingsvedtak(any()) } returns
            TilbakekrevingsvedtakResponse().apply {
                mmel = MmelDto().apply { alvorlighetsgrad = "00" }
            }

        val tilbakekrevingsvedtak = Tilbakekrevingsvedtak(vedtakId = VedtakId(1))
        tilbakekrevingKlient.sendTilbakekrevingsvedtak(tilbakekrevingsvedtak)
    }

    @Test
    fun `skal kunne sende tilbakekrevingvedtak og haandtere feilmelding fra tilbakekrevingskomponenten`() {
        every { tilbakekrevingPortType.tilbakekrevingsvedtak(any()) } returns
            TilbakekrevingsvedtakResponse().apply {
                mmel = MmelDto().apply { alvorlighetsgrad = "08" }
            }

        val tilbakekrevingsvedtak = Tilbakekrevingsvedtak(vedtakId = VedtakId(1))

        assertThrows<Exception>("Tilbakekrevingsvedtak feilet med alvorlighetsgrad 08") {
            tilbakekrevingKlient.sendTilbakekrevingsvedtak(tilbakekrevingsvedtak)
        }
    }
}
