package no.nav.etterlatte.migrering

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotliquery.TransactionalSession
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingResponseDto
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingStatusDto
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.hentListe
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.migrering.pen.BarnepensjonGrunnlagResponse
import no.nav.etterlatte.migrering.pen.PenKlient
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PESYS_ID
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.utbetaling.common.EVENT_NAME_OPPDATERT
import no.nav.etterlatte.utbetaling.common.UTBETALING_RESPONSE
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.SAK_ID_KEY
import java.time.Month
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

internal class MigreringIntegrationTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

    private lateinit var datasource: DataSource

    @BeforeEach
    fun start() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)
        datasource = DataSourceBuilder.createDataSource(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password
        ).also { it.migrate() }
    }

    @AfterEach
    fun stop() = postgreSQLContainer.stop()

    @Test
    fun `kan ta imot og handtere respons fra PEN`() {
        testApplication {
            val repository = PesysRepository(datasource)
            val featureToggleService = DummyFeatureToggleService().also {
                it.settBryter(MigreringFeatureToggle.SendSakTilMigrering, true)
            }
            val responsFraPEN = objectMapper.readValue<BarnepensjonGrunnlagResponse>(
                this::class.java.getResource("/penrespons.json")!!.readText()
            )

            val inspector = TestRapid()
                .apply {
                    MigrerSpesifikkSak(
                        rapidsConnection = this,
                        penKlient = mockk<PenKlient>()
                            .also { every { runBlocking { it.hentSak(any()) } } returns responsFraPEN },
                        pesysRepository = repository,
                        sakmigrerer = Sakmigrerer(repository, featureToggleService)
                    )
                }
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        EVENT_NAME_KEY to Migreringshendelser.MIGRER_SPESIFIKK_SAK,
                        SAK_ID_KEY to "22974139"
                    )
                ).toJson()
            )
            with(repository.hentSaker()) {
                assertEquals(1, size)
                assertEquals(get(0).id, 22974139)
                assertEquals(get(0).virkningstidspunkt, YearMonth.of(2023, Month.SEPTEMBER))
            }

            val melding1 = inspector.inspektør.message(0)

            val request = objectMapper.readValue(melding1.get(HENDELSE_DATA_KEY).toJson(), MigreringRequest::class.java)
            assertEquals(PesysId(22974139), request.pesysId)
            assertEquals(Folkeregisteridentifikator.of("06421594773"), request.soeker)
        }
    }

    @Test
    fun `Migrer hele veien`() {
        val pesysId: Long = 22974139
        testApplication {
            val repository = PesysRepository(datasource)
            val featureToggleService = DummyFeatureToggleService().also {
                it.settBryter(MigreringFeatureToggle.SendSakTilMigrering, true)
            }
            val responsFraPEN = objectMapper.readValue<BarnepensjonGrunnlagResponse>(
                this::class.java.getResource("/penrespons.json")!!.readText()
            )
            val penKlient = mockk<PenKlient>()
                .also { every { runBlocking { it.hentSak(any()) } } returns responsFraPEN }
                .also { every { runBlocking { it.opphoerSak(any()) } } just runs }

            val inspector = TestRapid()
                .apply {
                    MigrerSpesifikkSak(
                        rapidsConnection = this,
                        penKlient = penKlient,
                        pesysRepository = repository,
                        sakmigrerer = Sakmigrerer(repository, featureToggleService)
                    )
                    LagreKopling(this, repository)
                    LyttPaaIverksattVedtak(this, repository, penKlient)
                }
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        EVENT_NAME_KEY to Migreringshendelser.MIGRER_SPESIFIKK_SAK,
                        SAK_ID_KEY to pesysId
                    )
                ).toJson()
            )
            with(repository.hentSaker()) {
                assertEquals(1, size)
                assertEquals(get(0).id, pesysId)
                assertEquals(get(0).virkningstidspunkt, YearMonth.of(2023, Month.SEPTEMBER))
                assertEquals(repository.hentStatus(pesysId), Migreringsstatus.UNDER_MIGRERING)
            }
            val behandlingId = UUID.randomUUID()
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        EVENT_NAME_KEY to Migreringshendelser.LAGRE_KOPLING,
                        BEHANDLING_ID_KEY to behandlingId,
                        PESYS_ID to pesysId
                    )
                ).toJson()
            )
            assertEquals(repository.hentPesysId(behandlingId)?.pesysId, PesysId(pesysId))

            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        EVENT_NAME_KEY to EVENT_NAME_OPPDATERT,
                        UTBETALING_RESPONSE to UtbetalingResponseDto(
                            status = UtbetalingStatusDto.GODKJENT,
                            vedtakId = 1L,
                            behandlingId = behandlingId,
                            feilmelding = null
                        )
                    )
                ).toJson()
            )
            verify { penKlient.opphoerSak(PesysId(pesysId)) }
            assertEquals(repository.hentStatus(pesysId), Migreringsstatus.FERDIG)
        }
    }
}

internal fun PesysRepository.hentSaker(tx: TransactionalSession? = null): List<Pesyssak> = tx.session {
    hentListe(
        "SELECT sak from pesyssak WHERE status = '${Migreringsstatus.UNDER_MIGRERING.name}'"
    ) {
        tilPesyssak(it.string("sak"))
    }
}

private fun tilPesyssak(sak: String) = objectMapper.readValue(sak, Pesyssak::class.java)