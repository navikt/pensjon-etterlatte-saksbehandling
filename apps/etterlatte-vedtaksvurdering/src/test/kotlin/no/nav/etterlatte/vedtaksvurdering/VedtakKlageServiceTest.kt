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
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.VedtakKlageService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
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
