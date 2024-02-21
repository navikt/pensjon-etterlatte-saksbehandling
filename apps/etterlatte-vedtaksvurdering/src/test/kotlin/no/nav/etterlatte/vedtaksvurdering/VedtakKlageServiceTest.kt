package no.nav.etterlatte.vedtaksvurdering

import com.fasterxml.jackson.databind.node.ObjectNode
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.KlageFattVedtakDto
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
            klageVedtakDto(vedtakKlage, objectMapper.createObjectNode())

        val vedtakId =
            vedtakKlageService.opprettEllerOppdaterVedtakOmAvvisning(klageId, klageVedtakDto)
        vedtakId shouldBeEqual vedtakKlage.id

        verify {
            vedtaksvurderingRepository.hentVedtak(klageId)
            vedtaksvurderingRepository.opprettVedtak(
                withArg {
                    it.soeker shouldBe klageVedtakDto.soeker
                    it.behandlingId shouldBe klageId
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

        val klageVedtakDto =
            klageVedtakDto(vedtakKlage, mapOf("foo" to "bar").toObjectNode())

        val vedtakId =
            vedtakKlageService.opprettEllerOppdaterVedtakOmAvvisning(klageId, klageVedtakDto)
        vedtakId shouldBeEqual vedtakKlage.id

        verify {
            vedtaksvurderingRepository.hentVedtak(klageId)
            vedtaksvurderingRepository.oppdaterVedtak(
                withArg {
                    it.shouldBeEqualToIgnoringFields(vedtakKlage, Vedtak::innhold)
                    (it.innhold as VedtakInnhold.Klage).klage shouldBe klageVedtakDto.klage
                },
            )
        }
    }

    @Test
    fun `fattVedtak skal fatte vedtak når det finnes fra før`() {
        val vedtakKlage = vedtakKlage()
        val klageId = UUID.randomUUID()
        every { vedtaksvurderingRepository.hentVedtak(klageId) } returns vedtakKlage
        every { vedtaksvurderingRepository.fattVedtak(any(), any(), any()) } returns vedtakKlage

        val oppdatertKlageInnhold = mapOf("foo" to "bar").toObjectNode()
        val klageVedtakDto =
            klageVedtakDto(vedtakKlage, oppdatertKlageInnhold)

        val vedtakId =
            vedtakKlageService.fattVedtak(klageId, KlageFattVedtakDto("enhetenmin"), saksbehandler)
        vedtakId shouldBeEqual vedtakKlage.id

        verify {
            vedtaksvurderingRepository.hentVedtak(klageId)
            vedtaksvurderingRepository.fattVedtak(
                klageId,
                withArg {
                    it.ansvarligEnhet shouldBe "enhetenmin"
                    it.tidspunkt shouldNotBe null
                    it.ansvarligSaksbehandler shouldBe saksbehandler.ident
                },
            )
        }
    }

    private fun klageVedtakDto(
        vedtakKlage: Vedtak,
        oppdatertKlageInnhold: ObjectNode,
    ) = KlageVedtakDto(
        sakId = vedtakKlage.sakId,
        sakType = vedtakKlage.sakType,
        soeker = vedtakKlage.soeker,
        klage = oppdatertKlageInnhold,
        enhet = "enheten",
    )
}
