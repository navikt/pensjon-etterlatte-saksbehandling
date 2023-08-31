package no.nav.etterlatte.migrering

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.migrering.pen.BarnepensjonGrunnlagResponse
import no.nav.etterlatte.migrering.pen.PenKlient
import no.nav.etterlatte.rapidsandrivers.migrering.Beregning
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetid
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import rapidsandrivers.SAK_ID_KEY
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

class MigreringIntegrationTest {

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
    fun `skal sende migreringsmelding for hver enkelt sak`() {
        testApplication {
            val repository = PesysRepository(datasource)
            val syntetiskFnr = "19078504903"
            val sakInn = Pesyssak(
                UUID.randomUUID(),
                PesysId(4),
                Enhet("4808"),
                Folkeregisteridentifikator.of(syntetiskFnr),
                Folkeregisteridentifikator.of(syntetiskFnr),
                emptyList(),
                YearMonth.now(),
                YearMonth.now().minusYears(10),
                Beregning(
                    brutto = BigDecimal(1000),
                    netto = BigDecimal(1000),
                    anvendtTrygdetid = BigDecimal(40),
                    datoVirkFom = Tidspunkt.now(),
                    g = BigDecimal(100000)
                ),
                Trygdetid(emptyList()),
                false
            )
            repository.lagrePesyssak(sakInn)
            val featureToggleService = DummyFeatureToggleService().also {
                it.settBryter(MigreringFeatureToggle.SendSakTilMigrering, true)
            }
            val inspector = TestRapid()
                .apply {
                    Migrering(
                        this,
                        repository,
                        sakmigrerer = Sakmigrerer(repository, featureToggleService)
                    )
                }

            val melding = JsonMessage.newMessage(
                mapOf(EVENT_NAME_KEY to Migreringshendelser.START_MIGRERING)
            )
            inspector.sendTestMessage(melding.toJson())

            val melding1 = inspector.inspektør.message(0)

            val request = objectMapper.readValue(melding1.get("request").asText(), MigreringRequest::class.java)
            assertEquals(PesysId(4), request.pesysId)
            assertEquals(sakInn.soeker, request.soeker)
        }
    }

    @Test
    fun `kan ta imot og handtere respons fra PEN`() {
        testApplication {
            val repository = PesysRepository(datasource)
            val featureToggleService = DummyFeatureToggleService().also {
                it.settBryter(MigreringFeatureToggle.SendSakTilMigrering, false)
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
                assertEquals(get(0).pesysId.id, 22974139)
                assertEquals(get(0).virkningstidspunkt, YearMonth.of(2023, Month.SEPTEMBER))
            }
        }
    }
}