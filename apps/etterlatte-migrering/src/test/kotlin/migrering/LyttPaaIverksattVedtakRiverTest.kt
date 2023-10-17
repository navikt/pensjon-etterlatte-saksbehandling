package no.nav.etterlatte.migrering

import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingResponseDto
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingStatusDto
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.migrering.pen.PenKlient
import no.nav.etterlatte.opprettInMemoryDatabase
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.utbetaling.common.EVENT_NAME_OPPDATERT
import no.nav.etterlatte.utbetaling.common.UTBETALING_RESPONSE
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.UUID
import javax.sql.DataSource

class LyttPaaIverksattVedtakRiverTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

    private lateinit var datasource: DataSource

    @BeforeEach
    fun start() {
        datasource = opprettInMemoryDatabase(postgreSQLContainer).dataSource
    }

    @AfterEach
    fun stop() = postgreSQLContainer.stop()

    @Test
    fun `sender opphoersmelding til PEN ved godkjent utbetaling`() {
        testApplication {
            val behandlingId = UUID.randomUUID()
            val pesysid = PesysId(1L)
            val repository =
                spyk(PesysRepository(datasource)).also {
                    every { it.hentPesysId(behandlingId) } returns
                        Pesyskopling(
                            behandlingId = behandlingId,
                            pesysId = pesysid,
                        )
                }

            val penKlient = mockk<PenKlient>().also { every { runBlocking { it.opphoerSak(pesysid) } } just runs }
            val featureToggleService = DummyFeatureToggleService().also { it.settBryter(MigreringFeatureToggle.OpphoerSakIPesys, true) }
            TestRapid()
                .apply {
                    LyttPaaIverksattVedtakRiver(
                        rapidsConnection = this,
                        pesysRepository = repository,
                        penKlient = penKlient,
                        featureToggleService = featureToggleService,
                    )
                }.sendTestMessage(
                    JsonMessage.newMessage(
                        mapOf(
                            EVENT_NAME_KEY to EVENT_NAME_OPPDATERT,
                            UTBETALING_RESPONSE to
                                UtbetalingResponseDto(
                                    status = UtbetalingStatusDto.GODKJENT,
                                    behandlingId = behandlingId,
                                ),
                        ),
                    ).toJson(),
                )

            verify { runBlocking { penKlient.opphoerSak(pesysid) } }
        }
    }

    @Test
    fun `sender ikke opphoersmelding til PEN ved avvist utbetaling`() {
        testApplication {
            val behandlingId = UUID.randomUUID()
            val pesysid = PesysId(1L)
            val repository =
                spyk(PesysRepository(datasource)).also {
                    every { it.hentPesysId(behandlingId) } returns
                        Pesyskopling(
                            behandlingId = behandlingId,
                            pesysId = pesysid,
                        )
                }

            val penKlient = mockk<PenKlient>().also { every { runBlocking { it.opphoerSak(pesysid) } } just runs }
            val featureToggleService = DummyFeatureToggleService().also { it.settBryter(MigreringFeatureToggle.OpphoerSakIPesys, true) }
            TestRapid()
                .apply {
                    LyttPaaIverksattVedtakRiver(
                        rapidsConnection = this,
                        pesysRepository = repository,
                        penKlient = penKlient,
                        featureToggleService,
                    )
                }.sendTestMessage(
                    JsonMessage.newMessage(
                        mapOf(
                            EVENT_NAME_KEY to EVENT_NAME_OPPDATERT,
                            UTBETALING_RESPONSE to
                                UtbetalingResponseDto(
                                    status = UtbetalingStatusDto.AVVIST,
                                    behandlingId = behandlingId,
                                ),
                        ),
                    ).toJson(),
                )

            verify(exactly = 0) { runBlocking { penKlient.opphoerSak(any()) } }
        }
    }
}
