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
import no.nav.etterlatte.brev.BrevHendelseType
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.insert
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.VergeEllerFullmektig
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingResponseDto
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingStatusDto
import no.nav.etterlatte.libs.database.hentListe
import no.nav.etterlatte.migrering.grunnlag.Utenlandstilknytningsjekker
import no.nav.etterlatte.migrering.pen.BarnepensjonGrunnlagResponse
import no.nav.etterlatte.migrering.pen.PenKlient
import no.nav.etterlatte.migrering.pen.SakSammendragResponse
import no.nav.etterlatte.migrering.person.krr.DigitalKontaktinformasjon
import no.nav.etterlatte.migrering.person.krr.KrrKlient
import no.nav.etterlatte.migrering.start.MigrerSpesifikkSakRiver
import no.nav.etterlatte.migrering.start.MigreringFeatureToggle
import no.nav.etterlatte.migrering.start.StartMigrering
import no.nav.etterlatte.migrering.start.StartMigreringRepository
import no.nav.etterlatte.migrering.verifisering.GjenlevendeForelderPatcher
import no.nav.etterlatte.migrering.verifisering.PdlTjenesterKlient
import no.nav.etterlatte.migrering.verifisering.PersonHenter
import no.nav.etterlatte.migrering.verifisering.Verifiserer
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.LOPENDE_JANUAR_2024_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.MIGRERING_KJORING_VARIANT
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringKjoringVariant
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PESYS_ID_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.utbetaling.common.UTBETALING_RESPONSE
import no.nav.etterlatte.utbetaling.common.UtbetalinghendelseType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StartMigreringRiverIntegrationTest(private val datasource: DataSource) {
    companion object {
        @RegisterExtension
        private val dbExtension = DatabaseExtension()
    }

    @AfterEach
    fun reset() = dbExtension.resetDb()

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
                        val pdlTjenesterKlient =
                            mockk<PdlTjenesterKlient>().also {
                                every {
                                    it.hentPerson(
                                        any(),
                                        any(),
                                    )
                                } returns
                                    mockk<PersonDTO>().also {
                                        every { it.vergemaalEllerFremtidsfullmakt } returns emptyList()
                                        every { it.foedselsdato } returns
                                            OpplysningDTO(
                                                LocalDate.of(
                                                    2010,
                                                    Month.JANUARY,
                                                    1,
                                                ),
                                                "",
                                            )
                                        every { it.doedsdato } returns null
                                        every { it.adressebeskyttelse } returns null
                                    }
                            }
                        val personHenter = PersonHenter(pdlTjenesterKlient)
                        MigrerSpesifikkSakRiver(
                            rapidsConnection = this,
                            penKlient =
                                mockk<PenKlient>()
                                    .also {
                                        every { runBlocking { it.hentSak(any(), any()) } } returns responsFraPEN
                                    },
                            pesysRepository = repository,
                            featureToggleService = featureToggleService,
                            verifiserer =
                                Verifiserer(
                                    repository,
                                    GjenlevendeForelderPatcher(pdlTjenesterKlient, personHenter),
                                    mockk<Utenlandstilknytningsjekker>().also {
                                        every { it.finnUtenlandstilknytning(any()) } returns UtlandstilknytningType.NASJONAL
                                    },
                                    personHenter,
                                    grunnlagKlient = mockk(),
                                    penKlient =
                                        mockk<PenKlient>().also {
                                            every {
                                                runBlocking {
                                                    it.sakMedUfoere(
                                                        any(),
                                                    )
                                                }
                                            } returns emptyList()
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
                        Migreringshendelser.MIGRER_SPESIFIKK_SAK.lagParMedEventNameKey(),
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
                    .also {
                        every { runBlocking { it.hentSak(any(), any()) } } returns responsFraPEN
                        every { runBlocking { it.opphoerSak(any()) } } just runs
                        every { runBlocking { it.sakMedUfoere(any()) } } returns emptyList()
                    }

            val inspector =
                TestRapid()
                    .apply {
                        val pdlTjenesterKlient =
                            mockk<PdlTjenesterKlient>().also {
                                every {
                                    it.hentPerson(
                                        any(),
                                        any(),
                                    )
                                } returns
                                    mockk<PersonDTO>().also {
                                        every { it.vergemaalEllerFremtidsfullmakt } returns emptyList()
                                        every { it.foedselsdato } returns
                                            OpplysningDTO(
                                                LocalDate.of(
                                                    2010,
                                                    Month.JANUARY,
                                                    1,
                                                ),
                                                "",
                                            )
                                        every { it.adressebeskyttelse } returns null
                                        every { it.doedsdato } returns null
                                    }
                            }
                        val personHenter = PersonHenter(pdlTjenesterKlient)
                        MigrerSpesifikkSakRiver(
                            rapidsConnection = this,
                            penKlient = penKlient,
                            pesysRepository = repository,
                            featureToggleService = featureToggleService,
                            verifiserer =
                                Verifiserer(
                                    repository,
                                    GjenlevendeForelderPatcher(pdlTjenesterKlient, personHenter),
                                    mockk<Utenlandstilknytningsjekker>().also {
                                        every { it.finnUtenlandstilknytning(any()) } returns UtlandstilknytningType.NASJONAL
                                    },
                                    personHenter,
                                    grunnlagKlient = mockk(),
                                    penKlient =
                                        mockk<PenKlient>().also {
                                            every {
                                                runBlocking {
                                                    it.sakMedUfoere(
                                                        any(),
                                                    )
                                                }
                                            } returns emptyList()
                                        },
                                ),
                            krrKlient = mockk<KrrKlient>().also { coEvery { it.hentDigitalKontaktinformasjon(any()) } returns null },
                        )
                        LagreKoblingRiver(this, repository)
                        LyttPaaIverksattVedtakRiver(this, repository, penKlient, featureToggleService)
                        LyttPaaDistribuerBrevRiver(this, repository)
                    }
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        Migreringshendelser.MIGRER_SPESIFIKK_SAK.lagParMedEventNameKey(),
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
            val gjennySakId = 321L
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        Migreringshendelser.LAGRE_KOPLING.lagParMedEventNameKey(),
                        BEHANDLING_ID_KEY to behandlingId,
                        SAK_ID_KEY to gjennySakId,
                        PESYS_ID_KEY to pesysId,
                    ),
                ).toJson(),
            )
            assertEquals(repository.hentPesysId(behandlingId)?.pesysId, pesysId)

            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        UtbetalinghendelseType.OPPDATERT.lagParMedEventNameKey(),
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
            assertEquals(Migreringsstatus.UTBETALING_OK, repository.hentStatus(pesysId.id))

            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        BrevHendelseType.DISTRIBUERT.lagParMedEventNameKey(),
                        "bestillingsId" to UUID.randomUUID().toString(),
                        "vedtak" to VedtakMock(behandlingId = behandlingId),
                    ),
                ).toJson(),
            )
            assertEquals(Migreringsstatus.FERDIG, repository.hentStatus(pesysId.id))
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
                    .also {
                        every {
                            runBlocking {
                                it.hentSak(any(), any())
                            }
                        } returns responsFraPEN
                        every { runBlocking { it.opphoerSak(any()) } } just runs
                        every { runBlocking { it.sakMedUfoere(any()) } } returns emptyList()
                    }

            val inspector =
                TestRapid()
                    .apply {
                        val pdlTjenesterKlient =
                            mockk<PdlTjenesterKlient>().also {
                                every {
                                    it.hentPerson(
                                        any(),
                                        any(),
                                    )
                                } returns
                                    mockk<PersonDTO>().also {
                                        every { it.vergemaalEllerFremtidsfullmakt } returns emptyList()
                                        every { it.foedselsdato } returns
                                            OpplysningDTO(
                                                LocalDate.of(
                                                    2010,
                                                    Month.JANUARY,
                                                    1,
                                                ),
                                                "",
                                            )
                                        every { it.doedsdato } returns null
                                        every { it.adressebeskyttelse } returns null
                                    }
                            }
                        val personHenter = PersonHenter(pdlTjenesterKlient)
                        MigrerSpesifikkSakRiver(
                            rapidsConnection = this,
                            penKlient = penKlient,
                            pesysRepository = repository,
                            featureToggleService = featureToggleService,
                            verifiserer =
                                Verifiserer(
                                    repository,
                                    GjenlevendeForelderPatcher(pdlTjenesterKlient, personHenter),
                                    mockk<Utenlandstilknytningsjekker>().also {
                                        every { it.finnUtenlandstilknytning(any()) } returns UtlandstilknytningType.NASJONAL
                                    },
                                    personHenter,
                                    grunnlagKlient = mockk(),
                                    penKlient =
                                        mockk<PenKlient>().also {
                                            every {
                                                runBlocking {
                                                    it.sakMedUfoere(
                                                        any(),
                                                    )
                                                }
                                            } returns emptyList()
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
                        Migreringshendelser.MIGRER_SPESIFIKK_SAK.lagParMedEventNameKey(),
                        SAK_ID_KEY to pesysId.id,
                        LOPENDE_JANUAR_2024_KEY to true,
                        MIGRERING_KJORING_VARIANT to MigreringKjoringVariant.MED_PAUSE,
                    ),
                ).toJson(),
            )

            assertEquals(1, inspector.inspektør.size)
            val oppstartMigreringMelding = inspector.inspektør.message(0)
            assertEquals(
                Migreringshendelser.MIGRER_SAK.lagEventnameForType(),
                oppstartMigreringMelding.get(EVENT_NAME_KEY).asText(),
            )
            assertEquals(
                MigreringKjoringVariant.MED_PAUSE.name,
                oppstartMigreringMelding.get(MIGRERING_KJORING_VARIANT).asText(),
            )
            val behandlingId = UUID.randomUUID()
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        Migreringshendelser.LAGRE_KOPLING.lagParMedEventNameKey(),
                        BEHANDLING_ID_KEY to behandlingId,
                        PESYS_ID_KEY to pesysId,
                        SAK_ID_KEY to 321L,
                    ),
                ).toJson(),
            )

            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        Migreringshendelser.PAUSE.lagParMedEventNameKey(),
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
                        Migreringshendelser.MIGRER_SPESIFIKK_SAK.lagParMedEventNameKey(),
                        SAK_ID_KEY to pesysId.id,
                        LOPENDE_JANUAR_2024_KEY to true,
                        MIGRERING_KJORING_VARIANT to MigreringKjoringVariant.FORTSETT_ETTER_PAUSE,
                    ),
                ).toJson(),
            )
            assertEquals(3, inspector.inspektør.size)
            val forttsettMigreringMelding = inspector.inspektør.message(2)
            assertEquals(
                Migreringshendelser.BEREGNET_FERDIG.lagEventnameForType(),
                forttsettMigreringMelding.get(EVENT_NAME_KEY).asText(),
            )
            assertEquals(behandlingId.toString(), forttsettMigreringMelding.get(BEHANDLING_ID_KEY).asText())
            assertEquals(pesysId.id, forttsettMigreringMelding.get(SAK_ID_KEY).asLong())
            assertEquals(
                Migreringshendelser.BEREGNET_FERDIG.lagEventnameForType(),
                forttsettMigreringMelding.get(EVENT_NAME_KEY).asText(),
            )
            assertEquals(
                MigreringKjoringVariant.FORTSETT_ETTER_PAUSE.name,
                forttsettMigreringMelding.get(MIGRERING_KJORING_VARIANT).asText(),
            )
        }
    }

    @Test
    fun `hvis en person ikke fins i PDL skal skal gjenopprettes manuelt`() {
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
                        val pdlTjenesterKlient =
                            mockk<PdlTjenesterKlient>().also {
                                every {
                                    it.hentPerson(
                                        any(),
                                        any(),
                                    )
                                } throws IllegalStateException("")
                            }
                        val personHenter = PersonHenter(pdlTjenesterKlient)
                        MigrerSpesifikkSakRiver(
                            rapidsConnection = this,
                            penKlient =
                                mockk<PenKlient>()
                                    .also { every { runBlocking { it.hentSak(any(), any()) } } returns responsFraPEN },
                            pesysRepository = repository,
                            featureToggleService = featureToggleService,
                            verifiserer =
                                Verifiserer(
                                    repository,
                                    GjenlevendeForelderPatcher(pdlTjenesterKlient, personHenter),
                                    mockk<Utenlandstilknytningsjekker>().also { every { it.finnUtenlandstilknytning(any()) } returns null },
                                    personHenter,
                                    grunnlagKlient = mockk(),
                                    penKlient =
                                        mockk<PenKlient>().also {
                                            every {
                                                runBlocking {
                                                    it.sakMedUfoere(
                                                        any(),
                                                    )
                                                }
                                            } returns emptyList()
                                        },
                                ),
                            krrKlient = mockk<KrrKlient>().also { coEvery { it.hentDigitalKontaktinformasjon(any()) } returns null },
                        )
                    }
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        Migreringshendelser.MIGRER_SPESIFIKK_SAK.lagParMedEventNameKey(),
                        SAK_ID_KEY to pesysid,
                        LOPENDE_JANUAR_2024_KEY to true,
                        MIGRERING_KJORING_VARIANT to MigreringKjoringVariant.FULL_KJORING,
                    ),
                ).toJson(),
            )
            with(inspector.inspektør.message(0)) {
                assertEquals(EventNames.FEILA.lagEventnameForType(), get(EVENT_NAME_KEY).textValue())
            }
            assertEquals(Migreringsstatus.VERIFISERING_FEILA, repository.hentStatus(pesysid))
        }
    }

    @Test
    fun `Setter kanGjenopprettesAutomatisk false hvis finnes en form for ufore`() {
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
                        val pdlTjenesterKlient =
                            mockk<PdlTjenesterKlient>().also {
                                every {
                                    it.hentPerson(
                                        any(),
                                        any(),
                                    )
                                } returns
                                    mockk<PersonDTO>().also {
                                        every { it.vergemaalEllerFremtidsfullmakt } returns emptyList()
                                        every { it.foedselsdato } returns
                                            OpplysningDTO(
                                                LocalDate.of(
                                                    2010,
                                                    Month.JANUARY,
                                                    1,
                                                ),
                                                "",
                                            )
                                        every { it.adressebeskyttelse } returns null
                                        every { it.doedsdato } returns null
                                    }
                            }
                        val personHenter = PersonHenter(pdlTjenesterKlient)
                        MigrerSpesifikkSakRiver(
                            rapidsConnection = this,
                            penKlient =
                                mockk<PenKlient>()
                                    .also {
                                        every { runBlocking { it.hentSak(any(), any()) } } returns responsFraPEN
                                        every { runBlocking { it.opphoerSak(any()) } } just runs
                                    },
                            pesysRepository = repository,
                            featureToggleService = featureToggleService,
                            verifiserer =
                                Verifiserer(
                                    repository,
                                    GjenlevendeForelderPatcher(pdlTjenesterKlient, personHenter),
                                    mockk<Utenlandstilknytningsjekker>().also { every { it.finnUtenlandstilknytning(any()) } returns null },
                                    personHenter,
                                    grunnlagKlient = mockk(),
                                    penKlient =
                                        mockk<PenKlient>().also {
                                            every { runBlocking { it.sakMedUfoere(any()) } } returns
                                                listOf(
                                                    SakSammendragResponse(
                                                        sakType = SakSammendragResponse.UFORE_SAKTYPE,
                                                        sakStatus = SakSammendragResponse.Status.LOPENDE,
                                                    ),
                                                )
                                        },
                                ),
                            krrKlient = mockk<KrrKlient>().also { coEvery { it.hentDigitalKontaktinformasjon(any()) } returns null },
                        )
                    }
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        Migreringshendelser.MIGRER_SPESIFIKK_SAK.lagParMedEventNameKey(),
                        SAK_ID_KEY to pesysid,
                        LOPENDE_JANUAR_2024_KEY to true,
                        MIGRERING_KJORING_VARIANT to MigreringKjoringVariant.FULL_KJORING,
                    ),
                ).toJson(),
            )
            with(inspector.inspektør.message(0)) {
                val kanAutomatiskGjenopprettes =
                    objectMapper.treeToValue(
                        this[HENDELSE_DATA_KEY],
                        MigreringRequest::class.java,
                    ).kanAutomatiskGjenopprettes
                assertFalse(kanAutomatiskGjenopprettes)
            }
        }
    }

    @Test
    fun `Barn som har komplisert vergemaal i PDL skal gjenopprettes manuelt`() {
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
                        val pdlTjenesterKlient =
                            mockk<PdlTjenesterKlient>().also {
                                every {
                                    it.hentPerson(
                                        any(),
                                        any(),
                                    )
                                } returns
                                    mockk<PersonDTO>().also {
                                        val listOf = komplisertVergemaal()
                                        every { it.vergemaalEllerFremtidsfullmakt } returns listOf
                                        every { it.foedselsdato } returns
                                            OpplysningDTO(
                                                LocalDate.of(
                                                    2010,
                                                    Month.JANUARY,
                                                    1,
                                                ),
                                                "",
                                            )
                                        every { it.doedsdato } returns null
                                        every { it.adressebeskyttelse } returns null
                                    }
                            }
                        val personHenter = PersonHenter(pdlTjenesterKlient)
                        MigrerSpesifikkSakRiver(
                            rapidsConnection = this,
                            penKlient =
                                mockk<PenKlient>()
                                    .also { every { runBlocking { it.hentSak(any(), any()) } } returns responsFraPEN },
                            pesysRepository = repository,
                            featureToggleService = featureToggleService,
                            verifiserer =
                                Verifiserer(
                                    repository,
                                    GjenlevendeForelderPatcher(pdlTjenesterKlient, personHenter),
                                    mockk<Utenlandstilknytningsjekker>().also { every { it.finnUtenlandstilknytning(any()) } returns null },
                                    personHenter,
                                    grunnlagKlient = mockk(),
                                    penKlient =
                                        mockk<PenKlient>().also {
                                            every {
                                                runBlocking {
                                                    it.sakMedUfoere(
                                                        any(),
                                                    )
                                                }
                                            } returns emptyList()
                                        },
                                ),
                            krrKlient = mockk<KrrKlient>().also { coEvery { it.hentDigitalKontaktinformasjon(any()) } returns null },
                        )
                    }
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        Migreringshendelser.MIGRER_SPESIFIKK_SAK.lagParMedEventNameKey(),
                        SAK_ID_KEY to pesysid,
                        LOPENDE_JANUAR_2024_KEY to true,
                        MIGRERING_KJORING_VARIANT to MigreringKjoringVariant.FULL_KJORING,
                    ),
                ).toJson(),
            )
            with(inspector.inspektør.message(0)) {
                val kanAutomatiskGjenopprettes =
                    objectMapper.treeToValue(
                        this[HENDELSE_DATA_KEY],
                        MigreringRequest::class.java,
                    ).kanAutomatiskGjenopprettes
                assertFalse(kanAutomatiskGjenopprettes)
            }
        }
    }

    @Test
    fun `feiler hvis soeker er doed i PDL`() {
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
                        val pdlTjenesterKlient =
                            mockk<PdlTjenesterKlient>().also {
                                every {
                                    it.hentPerson(
                                        PersonRolle.BARN,
                                        any(),
                                    )
                                } returns
                                    mockk {
                                        every { doedsdato } returns OpplysningDTO(LocalDate.of(2020, 1, 1), "")
                                        every { adressebeskyttelse } returns null
                                        every { vergemaalEllerFremtidsfullmakt } returns null
                                    }
                            }
                        val personHenter = PersonHenter(pdlTjenesterKlient)
                        MigrerSpesifikkSakRiver(
                            rapidsConnection = this,
                            penKlient =
                                mockk<PenKlient>()
                                    .also { every { runBlocking { it.hentSak(any(), any()) } } returns responsFraPEN },
                            pesysRepository = repository,
                            featureToggleService = featureToggleService,
                            verifiserer =
                                Verifiserer(
                                    repository,
                                    GjenlevendeForelderPatcher(pdlTjenesterKlient, personHenter),
                                    mockk<Utenlandstilknytningsjekker>().also { every { it.finnUtenlandstilknytning(any()) } returns null },
                                    personHenter,
                                    grunnlagKlient = mockk(),
                                    penKlient =
                                        mockk<PenKlient>().also {
                                            every {
                                                runBlocking {
                                                    it.sakMedUfoere(
                                                        any(),
                                                    )
                                                }
                                            } returns emptyList()
                                        },
                                ),
                            krrKlient = mockk<KrrKlient>().also { coEvery { it.hentDigitalKontaktinformasjon(any()) } returns null },
                        )
                    }
            inspector.sendTestMessage(
                JsonMessage.newMessage(
                    mapOf(
                        Migreringshendelser.MIGRER_SPESIFIKK_SAK.lagParMedEventNameKey(),
                        SAK_ID_KEY to pesysid,
                        LOPENDE_JANUAR_2024_KEY to true,
                        MIGRERING_KJORING_VARIANT to MigreringKjoringVariant.FULL_KJORING,
                    ),
                ).toJson(),
            )
            with(inspector.inspektør.message(0)) {
                assertEquals(EventNames.FEILA.lagEventnameForType(), get(EVENT_NAME_KEY).textValue())
            }
            assertEquals(Migreringsstatus.VERIFISERING_FEILA, repository.hentStatus(pesysid))
        }
    }

    @Test
    fun `migrere flere saker samtidig`() {
        testApplication {
            val inspector = TestRapid()
            val repo = StartMigreringRepository(datasource)
            settOppTestsaker()
            val root =
                StartMigrering(
                    repository = repo,
                    rapidsConnection = inspector,
                    featureToggleService =
                        DummyFeatureToggleService().apply {
                            settBryter(
                                MigreringFeatureToggle.SendSakTilMigrering,
                                true,
                            )
                        },
                    iTraad = { it() },
                    sleep = { _ -> run {} },
                )

            with(inspector.inspektør.message(0)) {
                assertEquals(111L, get(SAK_ID_KEY).asLong())
                assertEquals(
                    Migreringshendelser.MIGRER_SPESIFIKK_SAK.lagEventnameForType(),
                    get(EVENT_NAME_KEY).asText(),
                )
                assertEquals(false, get(LOPENDE_JANUAR_2024_KEY).asBoolean())
            }
            with(inspector.inspektør.message(1)) {
                assertEquals(222L, get(SAK_ID_KEY).asLong())
                assertEquals(
                    Migreringshendelser.MIGRER_SPESIFIKK_SAK.lagEventnameForType(),
                    get(EVENT_NAME_KEY).asText(),
                )
                assertEquals(false, get(LOPENDE_JANUAR_2024_KEY).asBoolean())
            }
            with(inspector.inspektør.message(2)) {
                assertEquals(333L, get(SAK_ID_KEY).asLong())
                assertEquals(
                    Migreringshendelser.MIGRER_SPESIFIKK_SAK.lagEventnameForType(),
                    get(EVENT_NAME_KEY).asText(),
                )
                assertEquals(false, get(LOPENDE_JANUAR_2024_KEY).asBoolean())
            }
        }
    }

    private fun settOppTestsaker() {
        with(StartMigreringRepository.Databasetabell) {
            datasource.insert(
                TABELLNAVN,
                mapOf(
                    SAKID to 111,
                    LOPENDE_JANUAR_2024_KEY to false,
                    KJOERING to MigreringKjoringVariant.FULL_KJORING.name,
                ),
            )
            datasource.insert(
                TABELLNAVN,
                mapOf(
                    SAKID to 222,
                    LOPENDE_JANUAR_2024_KEY to false,
                    KJOERING to MigreringKjoringVariant.FULL_KJORING.name,
                ),
            )
            datasource.insert(
                TABELLNAVN,
                mapOf(
                    SAKID to 333,
                    LOPENDE_JANUAR_2024_KEY to false,
                    KJOERING to MigreringKjoringVariant.FULL_KJORING.name,
                ),
            )
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

private fun komplisertVergemaal(): List<OpplysningDTO<VergemaalEllerFremtidsfullmakt>> {
    val vergemaalEllerFremtidsfullmakt1 =
        mockk<VergemaalEllerFremtidsfullmakt> {
            every {
                vergeEllerFullmektig
            } returns
                mockk<VergeEllerFullmektig> {
                    every { motpartsPersonident } returns null
                    every { navn } returns null
                    every { embete } returns ""
                    every { type } returns ""
                    every { omfangetErInnenPersonligOmraade } returns false
                    every { tjenesteomraade } returns "personligeOgOekonomiskeInteresser"
                }
        }
    val vergemaalEllerFremtidsfullmakt2 =
        mockk<VergemaalEllerFremtidsfullmakt> {
            every {
                vergeEllerFullmektig
            } returns
                mockk<VergeEllerFullmektig> {
                    every { motpartsPersonident } returns null
                    every { embete } returns ""
                    every { type } returns ""
                    every { navn } returns null
                    every { omfangetErInnenPersonligOmraade } returns false
                    every { tjenesteomraade } returns "oekonomiskeInteresser"
                }
        }

    return listOf(
        opplysningDTO(vergemaalEllerFremtidsfullmakt1),
        opplysningDTO(vergemaalEllerFremtidsfullmakt2),
    )
}

private fun opplysningDTO(vergemaalEllerFremtidsfullmakt1: VergemaalEllerFremtidsfullmakt) =
    mockk<OpplysningDTO<VergemaalEllerFremtidsfullmakt>> { every { verdi } returns vergemaalEllerFremtidsfullmakt1 }

data class VedtakMock(val behandlingId: UUID)
