package no.nav.etterlatte.migrering

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotliquery.TransactionalSession
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.libs.common.behandling.UtenlandstilknytningType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingResponseDto
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingStatusDto
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.hentListe
import no.nav.etterlatte.migrering.grunnlag.Utenlandstilknytningsjekker
import no.nav.etterlatte.migrering.pen.BarnepensjonGrunnlagResponse
import no.nav.etterlatte.migrering.pen.PenKlient
import no.nav.etterlatte.migrering.person.krr.DigitalKontaktinformasjon
import no.nav.etterlatte.migrering.person.krr.KrrKlient
import no.nav.etterlatte.migrering.start.MigrerSpesifikkSakRiver
import no.nav.etterlatte.migrering.start.MigreringFeatureToggle
import no.nav.etterlatte.migrering.start.MigreringRiver
import no.nav.etterlatte.migrering.verifisering.GjenlevendeForelderPatcher
import no.nav.etterlatte.migrering.verifisering.PDLKlient
import no.nav.etterlatte.migrering.verifisering.Verifiserer
import no.nav.etterlatte.opprettInMemoryDatabase
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.migrering.LOPENDE_JANUAR_2024_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.MIGRERING_KJORING_VARIANT
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringKjoringVariant
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PESYS_ID_KEY
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
import rapidsandrivers.SAK_ID_FLERE_KEY
import rapidsandrivers.SAK_ID_KEY
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

internal class MigreringRiverIntegrationTest {
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
                        val pdlKlient =
                            mockk<PDLKlient>().also {
                                every {
                                    it.hentPerson(
                                        any(),
                                        any(),
                                    )
                                } returns
                                    mockk<PersonDTO>().also {
                                        every { it.vergemaalEllerFremtidsfullmakt } returns emptyList()
                                    }
                            }
                        MigrerSpesifikkSakRiver(
                            rapidsConnection = this,
                            penKlient =
                                mockk<PenKlient>()
                                    .also { every { runBlocking { it.hentSak(any(), any()) } } returns responsFraPEN },
                            pesysRepository = repository,
                            featureToggleService = featureToggleService,
                            verifiserer =
                                Verifiserer(
                                    pdlKlient,
                                    repository,
                                    featureToggleService,
                                    GjenlevendeForelderPatcher(pdlKlient),
                                    mockk<Utenlandstilknytningsjekker>().also {
                                        every { it.finnUtenlandstilknytning(any()) } returns UtenlandstilknytningType.NASJONAL
                                    },
                                ),
                            krrKlient =
                                mockk<KrrKlient>().also {
                                    coEvery { it.hentDigitalKontaktinformasjon(any()) } returns
                                        DigitalKontaktinformasjon(
                                            personident = "",
                                            aktiv = true,
                                            kanVarsles = true,
                                            reservert = false,
                                            spraak = "se",
                                            epostadresse = null,
                                            mobiltelefonnummer = null,
                                            sikkerDigitalPostkasse = null,
                                        )
                                },
                        )
                    }
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        EVENT_NAME_KEY to Migreringshendelser.MIGRER_SPESIFIKK_SAK,
                        SAK_ID_KEY to "22974139",
                        LOPENDE_JANUAR_2024_KEY to true,
                        MIGRERING_KJORING_VARIANT to MigreringKjoringVariant.FULL_KJORING,
                    ),
                ).toJson(),
            )
            with(repository.hentSaker()) {
                assertEquals(1, size)
                assertEquals(get(0).id, 22974139)
                assertEquals(get(0).foersteVirkningstidspunkt, YearMonth.of(2023, Month.SEPTEMBER))
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
                    it.settBryter(MigreringFeatureToggle.OpphoerSakIPesys, true)
                }
            val responsFraPEN =
                objectMapper.readValue<BarnepensjonGrunnlagResponse>(
                    this::class.java.getResource("/penrespons.json")!!.readText(),
                )
            val penKlient =
                mockk<PenKlient>()
                    .also { every { runBlocking { it.hentSak(any(), any()) } } returns responsFraPEN }
                    .also { every { runBlocking { it.opphoerSak(any()) } } just runs }

            val inspector =
                TestRapid()
                    .apply {
                        val pdlKlient =
                            mockk<PDLKlient>().also {
                                every {
                                    it.hentPerson(
                                        any(),
                                        any(),
                                    )
                                } returns
                                    mockk<PersonDTO>().also {
                                        every { it.vergemaalEllerFremtidsfullmakt } returns emptyList()
                                    }
                            }
                        MigrerSpesifikkSakRiver(
                            rapidsConnection = this,
                            penKlient = penKlient,
                            pesysRepository = repository,
                            featureToggleService = featureToggleService,
                            verifiserer =
                                Verifiserer(
                                    pdlKlient,
                                    repository,
                                    featureToggleService,
                                    GjenlevendeForelderPatcher(pdlKlient),
                                    mockk<Utenlandstilknytningsjekker>().also {
                                        every { it.finnUtenlandstilknytning(any()) } returns UtenlandstilknytningType.NASJONAL
                                    },
                                ),
                            krrKlient = mockk<KrrKlient>().also { coEvery { it.hentDigitalKontaktinformasjon(any()) } returns null },
                        )
                        LagreKoblingRiver(this, repository)
                        LyttPaaIverksattVedtakRiver(this, repository, penKlient, featureToggleService)
                    }
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        EVENT_NAME_KEY to Migreringshendelser.MIGRER_SPESIFIKK_SAK,
                        SAK_ID_KEY to pesysId.id,
                        LOPENDE_JANUAR_2024_KEY to true,
                        MIGRERING_KJORING_VARIANT to MigreringKjoringVariant.FULL_KJORING,
                    ),
                ).toJson(),
            )
            with(repository.hentSaker()) {
                assertEquals(1, size)
                assertEquals(get(0).id, pesysId.id)
                assertEquals(get(0).foersteVirkningstidspunkt, YearMonth.of(2023, Month.SEPTEMBER))
                assertEquals(repository.hentStatus(pesysId.id), Migreringsstatus.UNDER_MIGRERING)
            }
            val behandlingId = UUID.randomUUID()
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        EVENT_NAME_KEY to Migreringshendelser.LAGRE_KOPLING,
                        BEHANDLING_ID_KEY to behandlingId,
                        PESYS_ID_KEY to pesysId,
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
    fun `Start migrering med pause`() {
        val pesysId = PesysId(22974139)
        testApplication {
            val repository = PesysRepository(datasource)
            val featureToggleService =
                DummyFeatureToggleService().also {
                    it.settBryter(MigreringFeatureToggle.SendSakTilMigrering, true)
                    it.settBryter(MigreringFeatureToggle.OpphoerSakIPesys, true)
                }
            val responsFraPEN =
                objectMapper.readValue<BarnepensjonGrunnlagResponse>(
                    this::class.java.getResource("/penrespons.json")!!.readText(),
                )
            val penKlient =
                mockk<PenKlient>()
                    .also { every { runBlocking { it.hentSak(any(), any()) } } returns responsFraPEN }
                    .also { every { runBlocking { it.opphoerSak(any()) } } just runs }

            val inspector =
                TestRapid()
                    .apply {
                        val pdlKlient =
                            mockk<PDLKlient>().also {
                                every {
                                    it.hentPerson(
                                        any(),
                                        any(),
                                    )
                                } returns
                                    mockk<PersonDTO>().also {
                                        every { it.vergemaalEllerFremtidsfullmakt } returns emptyList()
                                    }
                            }
                        MigrerSpesifikkSakRiver(
                            rapidsConnection = this,
                            penKlient = penKlient,
                            pesysRepository = repository,
                            featureToggleService = featureToggleService,
                            verifiserer =
                                Verifiserer(
                                    pdlKlient,
                                    repository,
                                    featureToggleService,
                                    GjenlevendeForelderPatcher(pdlKlient),
                                    mockk<Utenlandstilknytningsjekker>().also {
                                        every { it.finnUtenlandstilknytning(any()) } returns UtenlandstilknytningType.NASJONAL
                                    },
                                ),
                            krrKlient = mockk<KrrKlient>().also { coEvery { it.hentDigitalKontaktinformasjon(any()) } returns null },
                        )
                        LagreKoblingRiver(this, repository)
                        PauseMigreringRiver(this, repository)
                    }

            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        EVENT_NAME_KEY to Migreringshendelser.MIGRER_SPESIFIKK_SAK,
                        SAK_ID_KEY to pesysId.id,
                        LOPENDE_JANUAR_2024_KEY to true,
                        MIGRERING_KJORING_VARIANT to MigreringKjoringVariant.MED_PAUSE,
                    ),
                ).toJson(),
            )

            assertEquals(1, inspector.inspektør.size)
            val oppstartMigreringMelding = inspector.inspektør.message(0)
            assertEquals(Migreringshendelser.MIGRER_SAK, oppstartMigreringMelding.get(EVENT_NAME_KEY).asText())
            assertEquals(
                MigreringKjoringVariant.MED_PAUSE.name,
                oppstartMigreringMelding.get(MIGRERING_KJORING_VARIANT).asText(),
            )
            val behandlingId = UUID.randomUUID()
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        EVENT_NAME_KEY to Migreringshendelser.LAGRE_KOPLING,
                        BEHANDLING_ID_KEY to behandlingId,
                        PESYS_ID_KEY to pesysId,
                    ),
                ).toJson(),
            )

            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        EVENT_NAME_KEY to Migreringshendelser.PAUSE,
                        PESYS_ID_KEY to pesysId,
                    ),
                ).toJson(),
            )

            with(repository.hentSaker()) {
                assertEquals(1, size)
                assertEquals(get(0).id, pesysId.id)
                assertEquals(get(0).foersteVirkningstidspunkt, YearMonth.of(2023, Month.SEPTEMBER))
                assertEquals(repository.hentStatus(pesysId.id), Migreringsstatus.PAUSE)
            }

            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        EVENT_NAME_KEY to Migreringshendelser.MIGRER_SPESIFIKK_SAK,
                        SAK_ID_KEY to pesysId.id,
                        LOPENDE_JANUAR_2024_KEY to true,
                        MIGRERING_KJORING_VARIANT to MigreringKjoringVariant.FORTSETT_ETTER_PAUSE,
                    ),
                ).toJson(),
            )
            assertEquals(3, inspector.inspektør.size)
            val forttsettMigreringMelding = inspector.inspektør.message(2)
            assertEquals(Migreringshendelser.VEDTAK, forttsettMigreringMelding.get(EVENT_NAME_KEY).asText())
            assertEquals(behandlingId.toString(), forttsettMigreringMelding.get(BEHANDLING_ID_KEY).asText())
            assertEquals(pesysId.id, forttsettMigreringMelding.get(SAK_ID_KEY).asLong())
            assertEquals(Migreringshendelser.VEDTAK, forttsettMigreringMelding.get(EVENT_NAME_KEY).asText())
            assertEquals(
                MigreringKjoringVariant.FORTSETT_ETTER_PAUSE.name,
                forttsettMigreringMelding.get(MIGRERING_KJORING_VARIANT).asText(),
            )
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
                        val pdlKlient =
                            mockk<PDLKlient>().also {
                                every {
                                    it.hentPerson(
                                        any(),
                                        any(),
                                    )
                                } throws IllegalStateException("")
                            }
                        MigrerSpesifikkSakRiver(
                            rapidsConnection = this,
                            penKlient =
                                mockk<PenKlient>()
                                    .also { every { runBlocking { it.hentSak(any(), any()) } } returns responsFraPEN },
                            pesysRepository = repository,
                            featureToggleService = featureToggleService,
                            verifiserer =
                                Verifiserer(
                                    pdlKlient,
                                    repository,
                                    featureToggleService,
                                    GjenlevendeForelderPatcher(pdlKlient),
                                    mockk<Utenlandstilknytningsjekker>().also { every { it.finnUtenlandstilknytning(any()) } returns null },
                                ),
                            krrKlient = mockk<KrrKlient>().also { coEvery { it.hentDigitalKontaktinformasjon(any()) } returns null },
                        )
                    }
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        EVENT_NAME_KEY to Migreringshendelser.MIGRER_SPESIFIKK_SAK,
                        SAK_ID_KEY to pesysid,
                        LOPENDE_JANUAR_2024_KEY to true,
                        MIGRERING_KJORING_VARIANT to MigreringKjoringVariant.FULL_KJORING,
                    ),
                ).toJson(),
            )
            with(inspector.inspektør.message(0)) {
                assertEquals(EventNames.FEILA, get(EVENT_NAME_KEY).textValue())
            }
            assertEquals(Migreringsstatus.VERIFISERING_FEILA, repository.hentStatus(pesysid))
        }
    }

    @Test
    fun `Feiler med barn som har vergemaal i PDL`() {
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
                        val pdlKlient =
                            mockk<PDLKlient>().also {
                                every {
                                    it.hentPerson(
                                        any(),
                                        any(),
                                    )
                                } returns
                                    mockk<PersonDTO>().also {
                                        every { it.vergemaalEllerFremtidsfullmakt } returns listOf(mockk())
                                    }
                            }
                        MigrerSpesifikkSakRiver(
                            rapidsConnection = this,
                            penKlient =
                                mockk<PenKlient>()
                                    .also { every { runBlocking { it.hentSak(any(), any()) } } returns responsFraPEN },
                            pesysRepository = repository,
                            featureToggleService = featureToggleService,
                            verifiserer =
                                Verifiserer(
                                    pdlKlient,
                                    repository,
                                    featureToggleService,
                                    GjenlevendeForelderPatcher(pdlKlient),
                                    mockk<Utenlandstilknytningsjekker>().also { every { it.finnUtenlandstilknytning(any()) } returns null },
                                ),
                            krrKlient = mockk<KrrKlient>().also { coEvery { it.hentDigitalKontaktinformasjon(any()) } returns null },
                        )
                    }
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        EVENT_NAME_KEY to Migreringshendelser.MIGRER_SPESIFIKK_SAK,
                        SAK_ID_KEY to pesysid,
                        LOPENDE_JANUAR_2024_KEY to true,
                        MIGRERING_KJORING_VARIANT to MigreringKjoringVariant.FULL_KJORING,
                    ),
                ).toJson(),
            )
            with(inspector.inspektør.message(0)) {
                assertEquals(EventNames.FEILA, get(EVENT_NAME_KEY).textValue())
            }
            assertEquals(Migreringsstatus.VERIFISERING_FEILA, repository.hentStatus(pesysid))
        }
    }

    @Test
    fun `migrere flere saker samtidig`() {
        testApplication {
            val inspector =
                TestRapid()
                    .apply {
                        MigreringRiver(rapidsConnection = this)
                    }
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        EVENT_NAME_KEY to Migreringshendelser.START_MIGRERING,
                        SAK_ID_FLERE_KEY to listOf("111", "222", "333"),
                        LOPENDE_JANUAR_2024_KEY to false,
                        MIGRERING_KJORING_VARIANT to MigreringKjoringVariant.FULL_KJORING,
                    ),
                ).toJson(),
            )

            with(inspector.inspektør.message(0)) {
                assertEquals(111L, get(SAK_ID_KEY).asLong())
                assertEquals(Migreringshendelser.MIGRER_SPESIFIKK_SAK, get(EVENT_NAME_KEY).asText())
                assertEquals(false, get(LOPENDE_JANUAR_2024_KEY).asBoolean())
            }
            with(inspector.inspektør.message(1)) {
                assertEquals(222L, get(SAK_ID_KEY).asLong())
                assertEquals(Migreringshendelser.MIGRER_SPESIFIKK_SAK, get(EVENT_NAME_KEY).asText())
                assertEquals(false, get(LOPENDE_JANUAR_2024_KEY).asBoolean())
            }
            with(inspector.inspektør.message(2)) {
                assertEquals(333L, get(SAK_ID_KEY).asLong())
                assertEquals(Migreringshendelser.MIGRER_SPESIFIKK_SAK, get(EVENT_NAME_KEY).asText())
                assertEquals(false, get(LOPENDE_JANUAR_2024_KEY).asBoolean())
            }
        }
    }
}

internal fun PesysRepository.hentSaker(tx: TransactionalSession? = null): List<Pesyssak> =
    tx.session {
        hentListe(
            "SELECT sak from pesyssak WHERE status in('${Migreringsstatus.UNDER_MIGRERING.name}','${Migreringsstatus.PAUSE.name}')",
        ) {
            objectMapper.readValue(it.string("sak"), Pesyssak::class.java)
        }
    }
