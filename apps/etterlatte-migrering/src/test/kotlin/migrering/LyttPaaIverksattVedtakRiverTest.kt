package no.nav.etterlatte.migrering

import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingResponseDto
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingStatusDto
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.utbetaling.common.UTBETALING_RESPONSE
import no.nav.etterlatte.utbetaling.common.UtbetalinghendelseType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
class LyttPaaIverksattVedtakRiverTest(
    private val datasource: DataSource,
) {
    @Test
    fun `setter status ok ved godkjent utbetaling`() {
        testApplication {
            val behandlingId = UUID.randomUUID()
            val pesysid = PesysId(1L)
            val repository =
                spyk(PesysRepository(datasource)).also {
                    every { it.hentPesysId(behandlingId) } returns
                        Pesyskopling(
                            behandlingId = behandlingId,
                            pesysId = pesysid,
                            sakId = 321L,
                        )
                }
            TestRapid()
                .apply {
                    LyttPaaIverksattVedtakRiver(
                        rapidsConnection = this,
                        pesysRepository = repository,
                    )
                }.sendTestMessage(
                    JsonMessage
                        .newMessage(
                            mapOf(
                                UtbetalinghendelseType.OPPDATERT.lagParMedEventNameKey(),
                                UTBETALING_RESPONSE to
                                    UtbetalingResponseDto(
                                        status = UtbetalingStatusDto.GODKJENT,
                                        behandlingId = behandlingId,
                                    ),
                            ),
                        ).toJson(),
                )
            verify { repository.oppdaterStatus(pesysid, Migreringsstatus.UTBETALING_OK) }
        }
    }

    @Test
    fun `setter status utbetaling feila ved avvist utbetaling`() {
        testApplication {
            val behandlingId = UUID.randomUUID()
            val pesysid = PesysId(1L)
            val repository =
                spyk(PesysRepository(datasource)).also {
                    every { it.hentPesysId(behandlingId) } returns
                        Pesyskopling(
                            behandlingId = behandlingId,
                            pesysId = pesysid,
                            sakId = 321L,
                        )
                }
            TestRapid()
                .apply {
                    LyttPaaIverksattVedtakRiver(
                        rapidsConnection = this,
                        pesysRepository = repository,
                    )
                }.sendTestMessage(
                    JsonMessage
                        .newMessage(
                            mapOf(
                                UtbetalinghendelseType.OPPDATERT.lagParMedEventNameKey(),
                                UTBETALING_RESPONSE to
                                    UtbetalingResponseDto(
                                        status = UtbetalingStatusDto.AVVIST,
                                        behandlingId = behandlingId,
                                    ),
                            ),
                        ).toJson(),
                )

            verify { repository.oppdaterStatus(pesysid, Migreringsstatus.UTBETALING_FEILA) }
        }
    }
}
