package no.nav.etterlatte.statistikk.river

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.libs.common.aktivitetsplikt.AKTIVITETSPLIKT_DTO_RIVER_KEY
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetType
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktAktivitetsgradDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktHendelse
import no.nav.etterlatte.libs.common.aktivitetsplikt.UnntakFraAktivitetDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.UnntakFraAktivitetsplikt
import no.nav.etterlatte.libs.common.aktivitetsplikt.VurdertAktivitetsgrad
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.statistikk.service.AktivitetspliktService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

class AktivitetspliktHendelseRiverTest {
    private val aktivitetspliktService: AktivitetspliktService = mockk()

    private val testRapid: TestRapid =
        TestRapid().apply {
            AktivitetspliktHendelseRiver(this, aktivitetspliktService)
        }

    @Test
    fun `melding om aktivitet leses og lagres`() {
        val dto =
            AktivitetspliktDto(
                sakId = 123,
                avdoedDoedsmaaned = YearMonth.of(2023, 12),
                aktivitetsgrad =
                    listOf(
                        AktivitetspliktAktivitetsgradDto(
                            vurdering = VurdertAktivitetsgrad.AKTIVITET_100,
                            fom = null,
                            tom = null,
                        ),
                    ),
                unntak =
                    listOf(
                        UnntakFraAktivitetDto(
                            unntak = UnntakFraAktivitetsplikt.OMSORG_BARN_UNDER_ETT_AAR,
                            fom = null,
                            tom = null,
                        ),
                    ),
                brukersAktivitet =
                    listOf(
                        AktivitetDto(
                            typeAktivitet = AktivitetType.ARBEIDSSOEKER,
                            fom = YearMonth.of(2024, 1).atDay(1),
                            tom = null,
                        ),
                    ),
            )
        every { aktivitetspliktService.oppdaterVurderingAktivitetsplikt(any(), any()) } just runs

        val testMessage =
            JsonMessage
                .newMessage(
                    mapOf(
                        AktivitetspliktHendelse.OPPDATERT.lagParMedEventNameKey(),
                        CORRELATION_ID_KEY to UUID.randomUUID(),
                        AKTIVITETSPLIKT_DTO_RIVER_KEY to dto,
                    ),
                )

        val inspector =
            testRapid
                .apply { sendTestMessage(testMessage.toJson()) }
                .inspektør

        Assertions.assertEquals(0, inspector.size) // Sender ikke ut ny melding
        verify(exactly = 1) {
            aktivitetspliktService.oppdaterVurderingAktivitetsplikt(dto, any())
        }
    }
}
