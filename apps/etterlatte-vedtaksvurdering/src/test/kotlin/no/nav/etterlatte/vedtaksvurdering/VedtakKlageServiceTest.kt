package no.nav.etterlatte.vedtaksvurdering

import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.VedtakKlageService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class VedtakKlageServiceTest {
    private val vedtaksvurderingRepository = mockk<VedtaksvurderingRepository>()
    private val vedtakKlageService: VedtakKlageService = VedtakKlageService(vedtaksvurderingRepository)

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `opprettEllerOppdaterVedtakOmAvvisning skal opprette vedtak når det ikke finnes fra før`() {
        val vedtakKlage = vedtakKlage()
        val klage = klage()
        every { vedtaksvurderingRepository.hentVedtak(klage.id) } returns null
        every { vedtaksvurderingRepository.opprettVedtak(any()) } returns vedtakKlage

        val vedtakId =
            vedtakKlageService.opprettEllerOppdaterVedtakOmAvvisning(klage)
        vedtakId shouldBeEqual vedtakKlage.id

        verify {
            vedtaksvurderingRepository.hentVedtak(klage.id)
            vedtaksvurderingRepository.opprettVedtak(
                withArg {
                    it.soeker.value shouldBe klage.sak.ident
                    it.behandlingId shouldBe klage.id
                    it.sakId shouldBe klage.sak.id
                    it.sakType shouldBe klage.sak.sakType
                    it.type shouldBe VedtakType.AVVIST_KLAGE
                    (it.innhold as VedtakInnhold.Klage).klage shouldBe klage.toObjectNode()
                },
            )
        }
    }

    @Test
    fun `opprettEllerOppdaterVedtakOmAvvisning skal oppdatere vedtak når det finnes fra før`() {
        val klage = klage()
        val vedtakKlage =
            vedtakKlage(
                klage = klage.toObjectNode(),
                behandlingId = klage.id,
            )
        every { vedtaksvurderingRepository.hentVedtak(klage.id) } returns vedtakKlage
        every { vedtaksvurderingRepository.oppdaterVedtak(any()) } returns vedtakKlage

        val vedtakId =
            vedtakKlageService.opprettEllerOppdaterVedtakOmAvvisning(klage)
        vedtakId shouldBeEqual vedtakKlage.id

        verify {
            vedtaksvurderingRepository.hentVedtak(klage.id)
            vedtaksvurderingRepository.oppdaterVedtak(
                withArg {
                    it.shouldBeEqualToIgnoringFields(vedtakKlage, Vedtak::innhold)
                    (it.innhold as VedtakInnhold.Klage).klage shouldBe klage.toObjectNode()
                },
            )
        }
    }

    @Test
    fun `fattVedtak skal oppdatere og fatte vedtak når det finnes fra før`() {
        val vedtakKlage = vedtakKlage()
        val klage = klage()
        every { vedtaksvurderingRepository.hentVedtak(klage.id, any()) } returns vedtakKlage
        every { vedtaksvurderingRepository.oppdaterVedtak(any(), any()) } returns vedtakKlage
        every { vedtaksvurderingRepository.fattVedtak(any(), any(), any()) } returns vedtakKlage

        val vedtakId =
            vedtakKlageService.fattVedtak(klage, saksbehandler)
        vedtakId shouldBeEqual vedtakKlage.id

        verify {
            vedtaksvurderingRepository.hentVedtak(klage.id)
            vedtaksvurderingRepository.oppdaterVedtak(
                withArg { it.innhold shouldBe VedtakInnhold.Klage(klage.toObjectNode()) },
            )
            vedtaksvurderingRepository.fattVedtak(
                klage.id,
                withArg {
                    it.ansvarligEnhet shouldBe klage.sak.enhet
                    it.tidspunkt shouldNotBe null
                    it.ansvarligSaksbehandler shouldBe saksbehandler.ident
                },
            )
        }
    }

    @Test
    fun `attesterVedtak skal attestere vedtak og returnere vedtaket`() {
        val klage = klage()
        val vedtakKlage =
            vedtakKlage(
                status = VedtakStatus.FATTET_VEDTAK,
                vedtakFattet =
                    VedtakFattet(
                        ansvarligSaksbehandler = "fatter",
                        ansvarligEnhet = "enhet",
                        tidspunkt = Tidspunkt.now(),
                    ),
            )
        every { vedtaksvurderingRepository.hentVedtak(klage.id) } returns vedtakKlage

        val attestertVedtak =
            vedtakKlage.copy(
                attestasjon =
                    Attestasjon(attestant = "saksbehandler2", attesterendeEnhet = "enhet", tidspunkt = Tidspunkt.now()),
            )
        every { vedtaksvurderingRepository.attesterVedtak(any(), any()) } returns attestertVedtak

        val vedtakDto = vedtakKlageService.attesterVedtak(klage, saksbehandler)

        vedtakDto shouldBe attestertVedtak.id
        verify { vedtaksvurderingRepository.hentVedtak(klage.id) }
        verify {
            vedtaksvurderingRepository.attesterVedtak(
                klage.id,
                withArg {
                    it.attestant shouldBe saksbehandler.ident
                    it.attesterendeEnhet shouldBe klage.sak.enhet
                },
            )
        }
    }

    @Test
    fun `attesterVedtak skal ikke attestere hvis feil status`() {
        val klage = klage()
        val vedtakKlage =
            vedtakKlage(
                status = VedtakStatus.ATTESTERT,
                vedtakFattet =
                    VedtakFattet(
                        ansvarligSaksbehandler = "saksbehandler",
                        ansvarligEnhet = "enhet",
                        tidspunkt = Tidspunkt.now(),
                    ),
            )
        every { vedtaksvurderingRepository.hentVedtak(klage.id) } returns vedtakKlage

        assertThrows<VedtakTilstandException> {
            vedtakKlageService.attesterVedtak(klage, saksbehandler)
        }
    }

    @Test
    fun `attesterVedtak skal ikke attestere hvis attestant ogsaa er saksbehandler`() {
        val klage = klage()
        val vedtakKlage =
            vedtakKlage(
                status = VedtakStatus.FATTET_VEDTAK,
                vedtakFattet =
                    VedtakFattet(
                        ansvarligSaksbehandler = saksbehandler.ident(),
                        ansvarligEnhet = "enhet",
                        tidspunkt = Tidspunkt.now(),
                    ),
            )
        every { vedtaksvurderingRepository.hentVedtak(klage.id) } returns vedtakKlage

        assertThrows<UgyldigAttestantException> {
            vedtakKlageService.attesterVedtak(klage, saksbehandler)
        }
    }

    @Test
    fun `underkjennVedtak skal underkjenne vedtak`() {
        val tilbakekrevingId = UUID.randomUUID()
        every { vedtaksvurderingRepository.hentVedtak(tilbakekrevingId) } returns vedtak(status = VedtakStatus.FATTET_VEDTAK)
        every { vedtaksvurderingRepository.underkjennVedtak(tilbakekrevingId) } returns vedtak()

        vedtakKlageService.underkjennVedtak(tilbakekrevingId) shouldBe 1L

        verify { vedtaksvurderingRepository.hentVedtak(tilbakekrevingId) }
        verify { vedtaksvurderingRepository.underkjennVedtak(tilbakekrevingId) }
    }

    @Test
    fun `underkjennVedtak skal ikke underkjenne hvis feil status`() {
        val tilbakekrevingId = UUID.randomUUID()
        every { vedtaksvurderingRepository.hentVedtak(tilbakekrevingId) } returns vedtak(status = VedtakStatus.ATTESTERT)

        assertThrows<VedtakTilstandException> {
            vedtakKlageService.underkjennVedtak(tilbakekrevingId)
        }
        verify { vedtaksvurderingRepository.hentVedtak(tilbakekrevingId) }
    }

    private fun klage(): Klage {
        return Klage(
            UUID.randomUUID(),
            Sak(FNR_1, SakType.BARNEPENSJON, 1L, "einheit"),
            Tidspunkt.now(),
            KlageStatus.OPPRETTET,
            kabalResultat = null,
            kabalStatus = null,
            formkrav = null,
            innkommendeDokument = null,
            resultat = null,
            utfall = null,
            aarsakTilAvbrytelse = null,
            initieltUtfall = null,
        )
    }
}
