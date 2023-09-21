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
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.hentListe
import no.nav.etterlatte.migrering.pen.BarnepensjonGrunnlagResponse
import no.nav.etterlatte.migrering.pen.PenKlient
import no.nav.etterlatte.migrering.verifisering.PDLKlient
import no.nav.etterlatte.migrering.verifisering.Verifiserer
import no.nav.etterlatte.opprettInMemoryDatabase
import no.nav.etterlatte.rapidsandrivers.EventNames
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
import java.util.UUID
import javax.sql.DataSource

internal class MigreringIntegrationTest {
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
    fun `kan ta imot og handtere respons fra PEN`() {
        testApplication {
            val repository = PesysRepository(datasource)
            val featureToggleService =
                DummyFeatureToggleService().also {
                    it.settBryter(MigreringFeatureToggle.SendSakTilMigrering, true)
                }
            val responsFraPEN =
                objectMapper.readValue<BarnepensjonGrunnlagResponse>(
                    this::class.java.getResource("/penrespons.json")!!.readText(),
                )

            val inspector =
                TestRapid()
                    .apply {
                        MigrerSpesifikkSak(
                            rapidsConnection = this,
                            penKlient =
                                mockk<PenKlient>()
                                    .also { every { runBlocking { it.hentSak(any()) } } returns responsFraPEN },
                            pesysRepository = repository,
                            featureToggleService = featureToggleService,
                            verifiserer =
                                Verifiserer(
                                    mockk<PDLKlient>().also {
                                        every {
                                            it.hentPerson(
                                                any(),
                                                any(),
                                            )
                                        } returns mockk()
                                    },
                                    repository,
                                ),
                        )
                    }
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        EVENT_NAME_KEY to Migreringshendelser.MIGRER_SPESIFIKK_SAK,
                        SAK_ID_KEY to "22974139",
                    ),
                ).toJson(),
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
        val pesysId = PesysId(22974139)
        testApplication {
            val repository = PesysRepository(datasource)
            val featureToggleService =
                DummyFeatureToggleService().also {
                    it.settBryter(MigreringFeatureToggle.SendSakTilMigrering, true)
                }
            val responsFraPEN =
                objectMapper.readValue<BarnepensjonGrunnlagResponse>(
                    this::class.java.getResource("/penrespons.json")!!.readText(),
                )
            val penKlient =
                mockk<PenKlient>()
                    .also { every { runBlocking { it.hentSak(any()) } } returns responsFraPEN }
                    .also { every { runBlocking { it.opphoerSak(any()) } } just runs }

            val inspector =
                TestRapid()
                    .apply {
                        MigrerSpesifikkSak(
                            rapidsConnection = this,
                            penKlient = penKlient,
                            pesysRepository = repository,
                            featureToggleService = featureToggleService,
                            verifiserer =
                                Verifiserer(
                                    mockk<PDLKlient>().also {
                                        every {
                                            it.hentPerson(
                                                any(),
                                                any(),
                                            )
                                        } returns mockk()
                                    },
                                    repository,
                                ),
                        )
                        LagreKopling(this, repository)
                        LyttPaaIverksattVedtak(this, repository, penKlient)
                    }
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        EVENT_NAME_KEY to Migreringshendelser.MIGRER_SPESIFIKK_SAK,
                        SAK_ID_KEY to pesysId.id,
                    ),
                ).toJson(),
            )
            with(repository.hentSaker()) {
                assertEquals(1, size)
                assertEquals(get(0).id, pesysId.id)
                assertEquals(get(0).virkningstidspunkt, YearMonth.of(2023, Month.SEPTEMBER))
                assertEquals(repository.hentStatus(pesysId.id), Migreringsstatus.UNDER_MIGRERING)
            }
            val behandlingId = UUID.randomUUID()
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        EVENT_NAME_KEY to Migreringshendelser.LAGRE_KOPLING,
                        BEHANDLING_ID_KEY to behandlingId,
                        PESYS_ID to pesysId,
                    ),
                ).toJson(),
            )
            assertEquals(repository.hentPesysId(behandlingId)?.pesysId, pesysId)

            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        EVENT_NAME_KEY to EVENT_NAME_OPPDATERT,
                        UTBETALING_RESPONSE to
                            UtbetalingResponseDto(
                                status = UtbetalingStatusDto.GODKJENT,
                                vedtakId = 1L,
                                behandlingId = behandlingId,
                                feilmelding = null,
                            ),
                    ),
                ).toJson(),
            )
            verify { runBlocking { penKlient.opphoerSak(pesysId) } }
            assertEquals(repository.hentStatus(pesysId.id), Migreringsstatus.FERDIG)
        }
    }

    @Test
    fun `feiler hvis en person ikke fins i PDL`() {
        testApplication {
            val pesysid = 22974139L
            val repository = PesysRepository(datasource)
            val featureToggleService =
                DummyFeatureToggleService().also {
                    it.settBryter(MigreringFeatureToggle.SendSakTilMigrering, true)
                }
            val responsFraPEN =
                objectMapper.readValue<BarnepensjonGrunnlagResponse>(
                    this::class.java.getResource("/penrespons.json")!!.readText(),
                )

            val inspector =
                TestRapid()
                    .apply {
                        MigrerSpesifikkSak(
                            rapidsConnection = this,
                            penKlient =
                                mockk<PenKlient>()
                                    .also { every { runBlocking { it.hentSak(any()) } } returns responsFraPEN },
                            pesysRepository = repository,
                            featureToggleService = featureToggleService,
                            verifiserer =
                                Verifiserer(
                                    mockk<PDLKlient>().also {
                                        every {
                                            it.hentPerson(
                                                any(),
                                                any(),
                                            )
                                        } throws IllegalStateException("")
                                    },
                                    repository,
                                ),
                        )
                    }
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        EVENT_NAME_KEY to Migreringshendelser.MIGRER_SPESIFIKK_SAK,
                        SAK_ID_KEY to pesysid,
                    ),
                ).toJson(),
            )
            with(inspector.inspektør.message(0)) {
                assertEquals(EventNames.FEILA, get(EVENT_NAME_KEY).textValue())
            }
            assertEquals(Migreringsstatus.VERIFISERING_FEILA, repository.hentStatus(pesysid))
        }
    }
}

internal fun PesysRepository.hentSaker(tx: TransactionalSession? = null): List<Pesyssak> =
    tx.session {
        hentListe(
            "SELECT sak from pesyssak WHERE status = '${Migreringsstatus.UNDER_MIGRERING.name}'",
        ) {
            objectMapper.readValue(it.string("sak"), Pesyssak::class.java)
        }
    }
