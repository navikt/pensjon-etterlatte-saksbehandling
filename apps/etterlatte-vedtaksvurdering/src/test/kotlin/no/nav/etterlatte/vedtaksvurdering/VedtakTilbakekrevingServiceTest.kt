package no.nav.etterlatte.vedtaksvurdering

import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingFattEllerAttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class VedtakTilbakekrevingServiceTest {
    private val repo = mockk<VedtaksvurderingRepository>()
    private val service = VedtakTilbakekrevingService(repo)

    @Test
    fun `opprettEllerOppdaterVedtak oppretter hvis ikke finnes fra foer`() {
        val dto =
            TilbakekrevingVedtakDto(
                UUID.randomUUID(),
                123L,
                SakType.OMSTILLINGSSTOENAD,
                Folkeregisteridentifikator.of("04417103428"),
                objectMapper.createObjectNode(),
            )

        every { repo.hentVedtak(dto.tilbakekrevingId) } returns null
        every { repo.opprettVedtak(any()) } returns vedtak()

        service.opprettEllerOppdaterVedtak(dto) shouldBe 1L

        verify { repo.hentVedtak(dto.tilbakekrevingId) }
        verify {
            repo.opprettVedtak(
                withArg {
                    it.soeker shouldBe dto.soeker
                    it.behandlingId shouldBe dto.tilbakekrevingId
                    it.sakId shouldBe dto.sakId
                    it.sakType shouldBe dto.sakType
                    it.type shouldBe VedtakType.TILBAKEKREVING
                    (it.innhold as VedtakTilbakekrevingInnhold).tilbakekreving shouldBe dto.tilbakekreving
                },
            )
        }
    }

    @Test
    fun `opprettEllerOppdaterVedtak oppdatere hvis finnes fra foer`() {
        val dto =
            TilbakekrevingVedtakDto(
                UUID.randomUUID(),
                123L,
                SakType.OMSTILLINGSSTOENAD,
                Folkeregisteridentifikator.of("04417103428"),
                objectMapper.createObjectNode(),
            )

        val eksisterende = vedtak()

        every { repo.hentVedtak(dto.tilbakekrevingId) } returns eksisterende
        every { repo.oppdaterVedtak(any()) } returns eksisterende

        service.opprettEllerOppdaterVedtak(dto) shouldBe 1L

        verify { repo.hentVedtak(dto.tilbakekrevingId) }
        verify {
            repo.oppdaterVedtak(
                withArg {
                    it.shouldBeEqualToIgnoringFields(eksisterende, Vedtak::innhold)
                    (it.innhold as VedtakTilbakekrevingInnhold).tilbakekreving shouldBe dto.tilbakekreving
                },
            )
        }
    }

    @Test
    fun `opprettEllerOppdaterVedtak skal ikke oppdatere hvis feil status`() {
        val dto =
            TilbakekrevingVedtakDto(
                UUID.randomUUID(),
                123L,
                SakType.OMSTILLINGSSTOENAD,
                Folkeregisteridentifikator.of("04417103428"),
                objectMapper.createObjectNode(),
            )
        every { repo.hentVedtak(dto.tilbakekrevingId) } returns vedtak(status = VedtakStatus.FATTET_VEDTAK)

        assertThrows<VedtakTilstandException> {
            service.opprettEllerOppdaterVedtak(dto)
        }

        verify { repo.hentVedtak(dto.tilbakekrevingId) }
    }

    @Test
    fun `fattVedtak skal fatte vedtak`() {
        val dto =
            TilbakekrevingFattEllerAttesterVedtakDto(
                tilbakekrevingId = UUID.randomUUID(),
                saksbehandler = "saksbehandler",
                enhet = "enhet",
            )
        every { repo.hentVedtak(dto.tilbakekrevingId) } returns vedtak()
        every { repo.fattVedtak(any(), any()) } returns vedtak()

        service.fattVedtak(dto) shouldBe 1L

        verify { repo.hentVedtak(dto.tilbakekrevingId) }
        verify {
            repo.fattVedtak(
                dto.tilbakekrevingId,
                withArg {
                    it.ansvarligSaksbehandler shouldBe dto.saksbehandler
                    it.ansvarligEnhet shouldBe dto.enhet
                },
            )
        }
    }

    @Test
    fun `fattVedtak skal ikke fatte hvis feil status`() {
        val dto =
            TilbakekrevingFattEllerAttesterVedtakDto(
                tilbakekrevingId = UUID.randomUUID(),
                saksbehandler = "saksbehandler",
                enhet = "enhet",
            )
        every { repo.hentVedtak(dto.tilbakekrevingId) } returns vedtak(status = VedtakStatus.FATTET_VEDTAK)

        assertThrows<VedtakTilstandException> {
            service.fattVedtak(dto)
        }
        verify { repo.hentVedtak(dto.tilbakekrevingId) }
    }

    @Test
    fun `attesterVedtak skal attestere vedtak og returnere vedtaket`() {
        val dto =
            TilbakekrevingFattEllerAttesterVedtakDto(
                tilbakekrevingId = UUID.randomUUID(),
                saksbehandler = "saksbehandler",
                enhet = "enhet",
            )
        every { repo.hentVedtak(dto.tilbakekrevingId) } returns vedtakTilbakekreving(status = VedtakStatus.FATTET_VEDTAK)
        val attestertVedtak =
            vedtakTilbakekreving(
                vedtakFattet =
                    VedtakFattet(
                        ansvarligSaksbehandler = "saksbehandler",
                        ansvarligEnhet = "enhet",
                        tidspunkt = Tidspunkt.now(),
                    ),
            )
        every { repo.attesterVedtak(any(), any()) } returns attestertVedtak

        val vedtakDto = service.attesterVedtak(dto)

        with(vedtakDto) {
            id shouldBe 1L
            fattetAv shouldBe "saksbehandler"
            enhet shouldBe "enhet"
            dato shouldBe attestertVedtak.vedtakFattet!!.tidspunkt.toLocalDate()
        }
        verify { repo.hentVedtak(dto.tilbakekrevingId) }
        verify {
            repo.attesterVedtak(
                dto.tilbakekrevingId,
                withArg {
                    it.attestant shouldBe dto.saksbehandler
                    it.attesterendeEnhet shouldBe dto.enhet
                },
            )
        }
    }

    @Test
    fun `attesterVedtak skal ikke attestere hvis feil status`() {
        val dto =
            TilbakekrevingFattEllerAttesterVedtakDto(
                tilbakekrevingId = UUID.randomUUID(),
                saksbehandler = "saksbehandler",
                enhet = "enhet",
            )
        every { repo.hentVedtak(dto.tilbakekrevingId) } returns vedtak(status = VedtakStatus.ATTESTERT)

        assertThrows<VedtakTilstandException> {
            service.attesterVedtak(dto)
        }
        verify { repo.hentVedtak(dto.tilbakekrevingId) }
    }

    @Test
    fun `underkjennVedtak skal underkjenne vedtak`() {
        val tilbakekrevingId = UUID.randomUUID()
        every { repo.hentVedtak(tilbakekrevingId) } returns vedtak(status = VedtakStatus.FATTET_VEDTAK)
        every { repo.underkjennVedtak(tilbakekrevingId) } returns vedtak()

        service.underkjennVedtak(tilbakekrevingId) shouldBe 1L

        verify { repo.hentVedtak(tilbakekrevingId) }
        verify { repo.underkjennVedtak(tilbakekrevingId) }
    }

    @Test
    fun `underkjennVedtak skal ikke underkjenne hvis feil status`() {
        val tilbakekrevingId = UUID.randomUUID()
        every { repo.hentVedtak(tilbakekrevingId) } returns vedtak(status = VedtakStatus.ATTESTERT)

        assertThrows<VedtakTilstandException> {
            service.underkjennVedtak(tilbakekrevingId)
        }
        verify { repo.hentVedtak(tilbakekrevingId) }
    }
}
