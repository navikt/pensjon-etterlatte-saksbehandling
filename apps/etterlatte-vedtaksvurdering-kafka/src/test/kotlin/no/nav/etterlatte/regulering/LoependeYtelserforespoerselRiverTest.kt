package no.nav.etterlatte.regulering

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILMELDING_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.TILBAKESTILTE_BEHANDLINGER_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class LoependeYtelserforespoerselRiverTest {
    private val foersteMai2023 = LocalDate.of(2023, 5, 1)
    private val sakId = 1L

    private fun genererReguleringMelding(dato: LocalDate) =
        JsonMessage.newMessage(
            mapOf(
                ReguleringHendelseType.SAK_FUNNET.lagParMedEventNameKey(),
                SAK_ID_KEY to 1,
                DATO_KEY to dato,
                TILBAKESTILTE_BEHANDLINGER_KEY to "",
            ),
        )

    @Test
    fun `kan ta imot reguleringsmelding og kalle paa vedtakservice med riktige verdier`() {
        val melding = genererReguleringMelding(foersteMai2023)
        val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
        val inspector = TestRapid().apply { LoependeYtelserforespoerselRiver(this, vedtakServiceMock) }

        inspector.sendTestMessage(melding.toJson())
        verify(exactly = 1) {
            vedtakServiceMock.harLoependeYtelserFra(1L, LocalDate.of(2023, 5, 1))
        }
    }

    @Test
    fun `skal lage ny melding med dato basert paa hva ytelsen har som foerste mulige dato`() {
        val fraDato = LocalDate.of(2023, 8, 1)
        val melding = genererReguleringMelding(foersteMai2023)
        val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
        every { vedtakServiceMock.tilbakestillVedtak(any()) } just runs
        every { vedtakServiceMock.harLoependeYtelserFra(sakId, foersteMai2023) } returns
            LoependeYtelseDTO(
                erLoepende = true,
                underSamordning = false,
                dato = fraDato,
            )
        val inspector = TestRapid().apply { LoependeYtelserforespoerselRiver(this, vedtakServiceMock) }

        inspector.sendTestMessage(melding.toJson())
        val sendtMelding = inspector.inspektør.message(0)
        Assertions.assertEquals(
            ReguleringHendelseType.LOEPENDE_YTELSE_FUNNET.lagEventnameForType(),
            sendtMelding.get(EVENT_NAME_KEY).asText(),
        )
        Assertions.assertEquals(
            Omregningshendelse(
                sakId = sakId,
                fradato = fraDato,
                prosesstype = Prosesstype.AUTOMATISK,
            ),
            objectMapper.readValue(sendtMelding.get(HENDELSE_DATA_KEY).toString(), Omregningshendelse::class.java),
        )
    }

    @Test
    fun `sender avbryt-melding dersom det ikke er en loepende ytelse`() {
        val melding = genererReguleringMelding(foersteMai2023)
        val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
        every { vedtakServiceMock.harLoependeYtelserFra(sakId, foersteMai2023) } returns
            LoependeYtelseDTO(
                erLoepende = false,
                underSamordning = false,
                dato = foersteMai2023,
            )
        val inspector = TestRapid().apply { LoependeYtelserforespoerselRiver(this, vedtakServiceMock) }

        inspector.sendTestMessage(melding.toJson())
        Assertions.assertEquals(1, inspector.inspektør.size)
        Assertions.assertEquals(
            inspector.inspektør
                .message(0)
                .get(EVENT_NAME_KEY)
                .asText(),
            ReguleringHendelseType.YTELSE_IKKE_LOEPENDE.lagEventnameForType(),
        )
    }

    @Test
    fun `tilbakestiller alle vedtak for behandlinger som blitt tilbakestillt allerede`() {
        val behandlinger = listOf(UUID.randomUUID(), UUID.randomUUID())
        val melding =
            mapOf(
                ReguleringHendelseType.SAK_FUNNET.lagParMedEventNameKey(),
                SAK_ID_KEY to 1,
                DATO_KEY to foersteMai2023,
                TILBAKESTILTE_BEHANDLINGER_KEY to "${behandlinger[0]};${behandlinger[1]}",
            ).let { JsonMessage.newMessage(it) }

        val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
        every { vedtakServiceMock.harLoependeYtelserFra(sakId, foersteMai2023) } returns
            LoependeYtelseDTO(
                erLoepende = true,
                underSamordning = false,
                dato = foersteMai2023,
            )
        val inspector = TestRapid().apply { LoependeYtelserforespoerselRiver(this, vedtakServiceMock) }

        inspector.sendTestMessage(melding.toJson())
        verify(exactly = 1) { vedtakServiceMock.tilbakestillVedtak(behandlinger[0]) }
        verify(exactly = 1) { vedtakServiceMock.tilbakestillVedtak(behandlinger[1]) }
    }

    @Test
    fun `avbryter hvis sak er under samordning`() {
        val melding = genererReguleringMelding(foersteMai2023)
        val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
        every { vedtakServiceMock.harLoependeYtelserFra(sakId, foersteMai2023) } returns
            LoependeYtelseDTO(
                erLoepende = true,
                underSamordning = true,
                dato = foersteMai2023,
            )
        val inspector = TestRapid().apply { LoependeYtelserforespoerselRiver(this, vedtakServiceMock) }

        inspector.sendTestMessage(melding.toJson())
        Assertions.assertEquals(1, inspector.inspektør.size)
        Assertions.assertTrue(
            "SakErUnderSamordning" in
                inspector.inspektør
                    .message(0)
                    .get(FEILMELDING_KEY)
                    .textValue(),
        )
    }
}
