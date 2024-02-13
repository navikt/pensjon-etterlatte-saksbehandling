package no.nav.etterlatte.vedtaksvurdering

import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.KlageVedtakDto
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
        val klageId = UUID.randomUUID()
        every { vedtaksvurderingRepository.hentVedtak(klageId) } returns null
        every { vedtaksvurderingRepository.opprettVedtak(any()) } returns vedtakKlage

        val klageVedtakDto =
            KlageVedtakDto(
                klageId = klageId,
                sakId = vedtakKlage.sakId,
                sakType = vedtakKlage.sakType,
                soeker = Folkeregisteridentifikator.of("04417103428"),
                klage = objectMapper.createObjectNode(),
                enhet = "enheten",
            )

        val vedtakId =
            vedtakKlageService.opprettEllerOppdaterVedtakOmAvvisning(klageVedtakDto)
        vedtakId shouldBeEqual vedtakKlage.id

        verify {
            vedtaksvurderingRepository.hentVedtak(klageId)
            vedtaksvurderingRepository.opprettVedtak(
                withArg {
                    it.soeker shouldBe klageVedtakDto.soeker
                    it.behandlingId shouldBe klageVedtakDto.klageId
                    it.sakId shouldBe klageVedtakDto.sakId
                    it.sakType shouldBe klageVedtakDto.sakType
                    it.type shouldBe VedtakType.AVVIST_KLAGE
                    (it.innhold as VedtakInnhold.Klage).klage shouldBe klageVedtakDto.klage
                },
            )
        }
    }

    @Test
    fun `opprettEllerOppdaterVedtakOmAvvisning skal oppdatere vedtak når det finnes fra før`() {
        val vedtakKlage = vedtakKlage()
        val klageId = UUID.randomUUID()
        every { vedtaksvurderingRepository.hentVedtak(klageId) } returns vedtakKlage
        every { vedtaksvurderingRepository.oppdaterVedtak(any()) } returns vedtakKlage

        val oppdatertKlageInnhold = mapOf("foo" to "bar").toObjectNode()
        val klageVedtakDto =
            KlageVedtakDto(
                klageId = klageId,
                sakId = vedtakKlage.sakId,
                sakType = vedtakKlage.sakType,
                soeker = Folkeregisteridentifikator.of("04417103428"),
                klage = oppdatertKlageInnhold,
                enhet = "enheten",
            )

        val vedtakId =
            vedtakKlageService.opprettEllerOppdaterVedtakOmAvvisning(klageVedtakDto)
        vedtakId shouldBeEqual vedtakKlage.id

        verify {
            vedtaksvurderingRepository.hentVedtak(klageId)
            vedtaksvurderingRepository.oppdaterVedtak(
                withArg {
                    it.shouldBeEqualToIgnoringFields(vedtakKlage, Vedtak::innhold)
                    (it.innhold as VedtakInnhold.Klage).klage shouldBe oppdatertKlageInnhold
                },
            )
        }
    }
}
