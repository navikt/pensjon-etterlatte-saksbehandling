package no.nav.etterlatte.vedtaksvurdering

import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.common.Enhet
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.SKAL_SENDE_BREV
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingFattEllerAttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.temporal.ChronoUnit
import java.util.UUID

class VedtakTilbakekrevingServiceTest {
    private val repo = mockk<VedtaksvurderingRepository>()
    private val rapid = mockk<VedtaksvurderingRapidService>()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val service = VedtakTilbakekrevingService(repo, rapid, behandlingKlient)

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
                    (it.innhold as VedtakInnhold.Tilbakekreving).tilbakekreving shouldBe dto.tilbakekreving
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
                    (it.innhold as VedtakInnhold.Tilbakekreving).tilbakekreving shouldBe dto.tilbakekreving
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
                enhet = Enhet.defaultEnhet,
            )
        every { repo.hentVedtak(dto.tilbakekrevingId) } returns vedtak()
        every { repo.fattVedtak(any(), any()) } returns vedtak()

        service.fattVedtak(dto, saksbehandler) shouldBe 1L

        verify { repo.hentVedtak(dto.tilbakekrevingId) }
        verify {
            repo.fattVedtak(
                dto.tilbakekrevingId,
                withArg {
                    it.ansvarligSaksbehandler shouldBe saksbehandler.ident
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
                enhet = Enhet.defaultEnhet,
            )
        every { repo.hentVedtak(dto.tilbakekrevingId) } returns vedtak(status = VedtakStatus.FATTET_VEDTAK)

        assertThrows<VedtakTilstandException> {
            service.fattVedtak(dto, saksbehandler)
        }
        verify { repo.hentVedtak(dto.tilbakekrevingId) }
    }

    @Test
    fun `attesterVedtak skal attestere vedtak og returnere vedtaket`() {
        val attesterDto =
            TilbakekrevingFattEllerAttesterVedtakDto(
                tilbakekrevingId = UUID.randomUUID(),
                enhet = Enhet.defaultEnhet,
            )

        every { repo.hentVedtak(attesterDto.tilbakekrevingId) } returns
            vedtakTilbakekreving(
                status = VedtakStatus.FATTET_VEDTAK,
                vedtakFattet =
                    VedtakFattet(
                        ansvarligSaksbehandler = "saksbehandler",
                        ansvarligEnhet = Enhet.defaultEnhet,
                        tidspunkt = Tidspunkt.now(),
                    ),
            )

        coEvery { behandlingKlient.hentTilbakekrevingBehandling(attesterDto.tilbakekrevingId, any()) } returns
            mockk {
                every { sendeBrev } returns true
            }

        val attestertVedtak =
            vedtakTilbakekreving(
                behandlingId = attesterDto.tilbakekrevingId,
                vedtakFattet =
                    VedtakFattet(
                        ansvarligSaksbehandler = "saksbehandler",
                        ansvarligEnhet = Enhet.defaultEnhet,
                        tidspunkt = Tidspunkt.now(),
                    ),
                attestasjon =
                    Attestasjon(
                        attestant = "annen saksbehandler",
                        attesterendeEnhet = Enhet.defaultEnhet,
                        tidspunkt = Tidspunkt.now(),
                    ),
            )
        every { repo.attesterVedtak(any(), any()) } returns attestertVedtak
        every { rapid.sendToRapid(any()) } returns Unit

        val vedtakDto = service.attesterVedtak(attesterDto, saksbehandler)

        with(vedtakDto) {
            id shouldBe 1L
            fattetAv shouldBe "saksbehandler"
            enhet shouldBe Enhet.PORSGRUNN
            dato shouldBe attestertVedtak.vedtakFattet!!.tidspunkt.toLocalDate()
        }
        verify { repo.hentVedtak(attesterDto.tilbakekrevingId) }
        verify {
            repo.attesterVedtak(
                attesterDto.tilbakekrevingId,
                withArg {
                    it.attestant shouldBe saksbehandler.ident
                    it.attesterendeEnhet shouldBe attesterDto.enhet
                },
            )
        }
        verify {
            val returDto = attestertVedtak.toDto()
            rapid.sendToRapid(
                withArg {
                    it.vedtak shouldBe returDto
                    with(it.rapidInfo1) {
                        vedtakhendelse shouldBe VedtakKafkaHendelseHendelseType.ATTESTERT
                        vedtak shouldBe returDto
                        behandlingId shouldBe attesterDto.tilbakekrevingId
                        extraParams.size shouldBe 1
                        extraParams shouldContain Pair(SKAL_SENDE_BREV, true)
                    }
                },
            )
        }
    }

    @Test
    fun `attesterVedtak skal ikke attestere hvis feil status`() {
        val dto =
            TilbakekrevingFattEllerAttesterVedtakDto(
                tilbakekrevingId = UUID.randomUUID(),
                enhet = Enhet.defaultEnhet,
            )
        every { repo.hentVedtak(dto.tilbakekrevingId) } returns vedtak(status = VedtakStatus.ATTESTERT)

        assertThrows<VedtakTilstandException> {
            service.attesterVedtak(dto, saksbehandler)
        }
        verify { repo.hentVedtak(dto.tilbakekrevingId) }
    }

    @Test
    fun `attesterVedtak skal ikke attestere hvis fattet av samme saksbehandler`() {
        val dto =
            TilbakekrevingFattEllerAttesterVedtakDto(
                tilbakekrevingId = UUID.randomUUID(),
                enhet = Enhet.defaultEnhet,
            )
        every { repo.hentVedtak(dto.tilbakekrevingId) } returns
            vedtak(
                status = VedtakStatus.FATTET_VEDTAK,
                vedtakFattet =
                    VedtakFattet(
                        saksbehandler.ident,
                        Enhet.defaultEnhet,
                        Tidspunkt.now().minus(1, ChronoUnit.DAYS),
                    ),
            )

        assertThrows<UgyldigAttestantException> {
            service.attesterVedtak(dto, saksbehandler)
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
