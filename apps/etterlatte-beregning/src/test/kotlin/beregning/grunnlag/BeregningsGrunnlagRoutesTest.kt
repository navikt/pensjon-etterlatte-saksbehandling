package no.nav.etterlatte.beregning.grunnlag

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import no.nav.etterlatte.beregning.regler.toGrunnlag
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.klienter.GrunnlagKlient
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.util.UUID.randomUUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BeregningsGrunnlagRoutesTest {
    private val server = MockOAuth2Server()
    private lateinit var applicationConfig: HoconApplicationConfig
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val repository = mockk<BeregningsGrunnlagRepository>()
    private val grunnlagKlient = mockk<GrunnlagKlient>()
    private val featureToggleService = DummyFeatureToggleService()
    private val service = BeregningsGrunnlagService(repository, behandlingKlient, featureToggleService, grunnlagKlient)

    @BeforeAll
    fun before() {
        server.start()

        applicationConfig =
            buildTestApplicationConfigurationForOauth(server.config.httpServer.port(), AZURE_ISSUER, CLIENT_ID)
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    @Test
    fun `skal returnere 204 naar beregnings ikke finnes`() {
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns
            DetaljertBehandling(
                id = randomUUID(),
                sak = 123,
                sakType = SakType.BARNEPENSJON,
                behandlingOpprettet = LocalDateTime.now(),
                soeknadMottattDato = null,
                innsender = null,
                soeker = "diam",
                gjenlevende = listOf(),
                avdoed = listOf(),
                soesken = listOf(),
                status = BehandlingStatus.TRYGDETID_OPPDATERT,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                virkningstidspunkt = null,
                revurderingsaarsak = null,
                prosesstype = Prosesstype.MANUELL,
                boddEllerArbeidetUtlandet = null,
                revurderingInfo = null,
                enhet = "1111",
                kilde = Vedtaksloesning.GJENNY,
            )

        every { repository.finnBarnepensjonGrunnlagForBehandling(any()) } returns null

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            val response =
                client.get("/api/beregning/beregningsgrunnlag/${randomUUID()}/barnepensjon") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.NoContent
        }
    }

    @Test
    fun `skal hente beregningsgrunnlag for sist iverksatte hvis ikke noe finnes og det er revurdering`() {
        val idRevurdering = randomUUID()
        val idForrigeIverksatt = randomUUID()
        val sakId = 123L
        val virkRevurdering =
            Virkningstidspunkt(
                dato = YearMonth.of(2023, Month.JANUARY),
                kilde =
                    Grunnlagsopplysning.Saksbehandler(
                        ident = "",
                        tidspunkt = Tidspunkt.now(),
                    ),
                begrunnelse = "",
            )
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(idRevurdering, any()) } returns
            DetaljertBehandling(
                id = randomUUID(),
                sak = sakId,
                sakType = SakType.BARNEPENSJON,
                behandlingOpprettet = LocalDateTime.now(),
                soeknadMottattDato = null,
                innsender = null,
                soeker = "",
                gjenlevende = listOf(),
                avdoed = listOf(),
                soesken = listOf(),
                status = BehandlingStatus.TRYGDETID_OPPDATERT,
                behandlingType = BehandlingType.REVURDERING,
                virkningstidspunkt = virkRevurdering,
                revurderingsaarsak = null,
                prosesstype = Prosesstype.MANUELL,
                boddEllerArbeidetUtlandet = null,
                revurderingInfo = null,
                enhet = "1111",
                kilde = Vedtaksloesning.GJENNY,
            )
        coEvery {
            behandlingKlient.hentSisteIverksatteBehandling(sakId, any())
        } returns SisteIverksatteBehandling(idForrigeIverksatt)
        every { repository.finnBarnepensjonGrunnlagForBehandling(idRevurdering) } returns null
        every { repository.finnBarnepensjonGrunnlagForBehandling(idForrigeIverksatt) } returns
            BeregningsGrunnlag(
                behandlingId = idForrigeIverksatt,
                kilde =
                    Grunnlagsopplysning.Saksbehandler(
                        ident = "",
                        tidspunkt = Tidspunkt.now(),
                    ),
                soeskenMedIBeregning = listOf(),
                institusjonsoppholdBeregningsgrunnlag = emptyList(),
                beregningsMetode = BeregningsMetode.NASJONAL.toGrunnlag(),
            )

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            val response =
                client.get("/api/beregning/beregningsgrunnlag/$idRevurdering/barnepensjon") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.OK
        }

        coVerify(exactly = 1) {
            repository.finnBarnepensjonGrunnlagForBehandling(idRevurdering)
            repository.finnBarnepensjonGrunnlagForBehandling(idForrigeIverksatt)
            behandlingKlient.hentSisteIverksatteBehandling(sakId, any())
        }
    }

    @Test
    fun `skal hente beregning`() {
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true

        val id = randomUUID()

        every { repository.finnBarnepensjonGrunnlagForBehandling(any()) } returns
            BeregningsGrunnlag(
                behandlingId = id,
                kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
                soeskenMedIBeregning = emptyList(),
                institusjonsoppholdBeregningsgrunnlag = emptyList(),
                beregningsMetode = BeregningsMetode.BEST.toGrunnlag(),
            )

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            val response =
                client.get("/api/beregning/beregningsgrunnlag/$id/barnepensjon") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val hentet = objectMapper.readValue(response.bodyAsText(), BeregningsGrunnlag::class.java)

            hentet.soeskenMedIBeregning shouldBe emptyList()
        }
    }

    @Test
    fun `skal returnere not found naar saksbehandler ikke har tilgang til behandling`() {
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns false

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            client.get("/api/beregning/beregningsgrunnlag/${randomUUID()}/barnepensjon") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }.let {
                it.status shouldBe HttpStatusCode.NotFound
            }
        }
    }

    @Test
    fun `skal returnere not found naar saksbehandler ikke har tilgang til behandling ved opprettelse`() {
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns false

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            val client =
                createClient {
                    install(ContentNegotiation) {
                        jackson { registerModule(JavaTimeModule()) }
                    }
                }

            client.post("/api/beregning/beregningsgrunnlag/${randomUUID()}/barnepensjon") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(
                    BarnepensjonBeregningsGrunnlag(
                        emptyList(),
                        emptyList(),
                    ),
                )
            }.let {
                it.status shouldBe HttpStatusCode.NotFound
            }
        }
    }

    @Test
    fun `skal opprettere`() {
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { repository.finnBarnepensjonGrunnlagForBehandling(any()) } returns null
        every { repository.lagre(any()) } returns true
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns hentOpplysningsgrunnlag
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns
            DetaljertBehandling(
                id = randomUUID(),
                sak = 123,
                sakType = SakType.BARNEPENSJON,
                behandlingOpprettet = LocalDateTime.now(),
                soeknadMottattDato = null,
                innsender = null,
                soeker = "diam",
                gjenlevende = listOf(),
                avdoed = listOf(),
                soesken = listOf(),
                status = BehandlingStatus.TRYGDETID_OPPDATERT,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                virkningstidspunkt =
                    Virkningstidspunkt(
                        YearMonth.of(2023, 1),
                        kilde =
                            Grunnlagsopplysning.Saksbehandler(
                                ident = "",
                                tidspunkt = Tidspunkt.now(),
                            ),
                        "",
                    ),
                revurderingsaarsak = null,
                prosesstype = Prosesstype.MANUELL,
                boddEllerArbeidetUtlandet = null,
                revurderingInfo = null,
                enhet = "1111",
                kilde = Vedtaksloesning.GJENNY,
            )

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            val client =
                createClient {
                    install(ContentNegotiation) {
                        jackson { registerModule(JavaTimeModule()) }
                    }
                }

            client.post("/api/beregning/beregningsgrunnlag/${randomUUID()}/barnepensjon") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(
                    BarnepensjonBeregningsGrunnlag(
                        emptyList(),
                        emptyList(),
                    ),
                )
            }.let {
                it.status shouldBe HttpStatusCode.NoContent
            }
        }
    }

    @Test
    fun `skal returnere conflict fra opprettelse `() {
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { repository.finnBarnepensjonGrunnlagForBehandling(any()) } returns null
        every { repository.lagre(any()) } returns false
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns hentOpplysningsgrunnlag
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns
            DetaljertBehandling(
                id = randomUUID(),
                sak = 123,
                sakType = SakType.BARNEPENSJON,
                behandlingOpprettet = LocalDateTime.now(),
                soeknadMottattDato = null,
                innsender = null,
                soeker = "diam",
                gjenlevende = listOf(),
                avdoed = listOf(),
                soesken = listOf(),
                status = BehandlingStatus.TRYGDETID_OPPDATERT,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                virkningstidspunkt =
                    Virkningstidspunkt(
                        YearMonth.of(2023, 1),
                        kilde =
                            Grunnlagsopplysning.Saksbehandler(
                                ident = "",
                                tidspunkt = Tidspunkt.now(),
                            ),
                        "",
                    ),
                revurderingsaarsak = null,
                prosesstype = Prosesstype.MANUELL,
                boddEllerArbeidetUtlandet = null,
                revurderingInfo = null,
                enhet = "1111",
                kilde = Vedtaksloesning.GJENNY,
            )

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            val client =
                createClient {
                    install(ContentNegotiation) {
                        jackson { registerModule(JavaTimeModule()) }
                    }
                }

            client.post("/api/beregning/beregningsgrunnlag/${randomUUID()}/barnepensjon") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(
                    BarnepensjonBeregningsGrunnlag(
                        emptyList(),
                        emptyList(),
                    ),
                )
            }.let {
                it.status shouldBe HttpStatusCode.Conflict
            }
        }
    }

    @Test
    fun `skal duplisere hvis en saksbehandler kaller duplisering`() {
        val forrige = randomUUID()
        val nye = randomUUID()

        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { repository.finnBarnepensjonGrunnlagForBehandling(forrige) } returns
            BeregningsGrunnlag(
                behandlingId = forrige,
                kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
                soeskenMedIBeregning = emptyList(),
                institusjonsoppholdBeregningsgrunnlag = emptyList(),
                beregningsMetode = BeregningsMetode.BEST.toGrunnlag(),
            )
        every { repository.finnBarnepensjonGrunnlagForBehandling(nye) } returns null
        every { repository.lagre(any()) } returns true

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            client.post("/api/beregning/beregningsgrunnlag/$nye/fra/$forrige") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }.let {
                it.status shouldBe HttpStatusCode.NoContent
            }
        }
    }

    @Test
    fun `skal duplisere hvis en system bruker kaller duplisering`() {
        val forrige = randomUUID()
        val nye = randomUUID()

        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { repository.finnBarnepensjonGrunnlagForBehandling(forrige) } returns
            BeregningsGrunnlag(
                behandlingId = forrige,
                kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
                soeskenMedIBeregning = emptyList(),
                institusjonsoppholdBeregningsgrunnlag = emptyList(),
                beregningsMetode = BeregningsMetode.BEST.toGrunnlag(),
            )
        every { repository.finnBarnepensjonGrunnlagForBehandling(nye) } returns null
        every { repository.lagre(any()) } returns true

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            client.post("/api/beregning/beregningsgrunnlag/$nye/fra/$forrige") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $systemToken")
            }.let {
                it.status shouldBe HttpStatusCode.NoContent
            }
        }
    }

    @Test
    fun `skal feile hvis forrige beregningsgrunnlag ikke finnes`() {
        val forrige = randomUUID()
        val nye = randomUUID()

        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { repository.finnBarnepensjonGrunnlagForBehandling(forrige) } returns null
        every { repository.lagre(any()) } returns true

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            client.post("/api/beregning/beregningsgrunnlag/$nye/fra/$forrige") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $systemToken")
            }.let {
                it.status shouldBe HttpStatusCode.InternalServerError
            }
        }
    }

    @Test
    fun `skal feile hvis nye beregningsgrunnlag allerede finnes`() {
        val forrige = randomUUID()
        val nye = randomUUID()

        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { repository.finnBarnepensjonGrunnlagForBehandling(forrige) } returns
            BeregningsGrunnlag(
                behandlingId = forrige,
                kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
                soeskenMedIBeregning = emptyList(),
                institusjonsoppholdBeregningsgrunnlag = emptyList(),
                beregningsMetode = BeregningsMetode.BEST.toGrunnlag(),
            )
        every { repository.finnBarnepensjonGrunnlagForBehandling(nye) } returns
            BeregningsGrunnlag(
                behandlingId = nye,
                kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
                soeskenMedIBeregning = emptyList(),
                institusjonsoppholdBeregningsgrunnlag = emptyList(),
                beregningsMetode = BeregningsMetode.BEST.toGrunnlag(),
            )
        every { repository.lagre(any()) } returns true

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            client.post("/api/beregning/beregningsgrunnlag/$nye/fra/$forrige") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $systemToken")
            }.let {
                it.status shouldBe HttpStatusCode.InternalServerError
            }
        }
    }

    @Test
    fun `skal hente overstyr beregning grunnlag`() {
        val behandlingId = randomUUID()

        coEvery { behandlingKlient.harTilgangTilBehandling(behandlingId, any()) } returns true

        every { repository.finnOverstyrBeregningGrunnlagForBehandling(behandlingId) } returns
            listOf(
                OverstyrBeregningGrunnlagDao(
                    id = randomUUID(),
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.now().minusYears(12L),
                    datoTOM = LocalDate.now().minusYears(6L),
                    utbetaltBeloep = 123L,
                    trygdetid = 10L,
                    sakId = 1L,
                    beskrivelse = "test periode 1",
                ),
                OverstyrBeregningGrunnlagDao(
                    id = randomUUID(),
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.now().minusYears(6L),
                    datoTOM = null,
                    utbetaltBeloep = 456L,
                    trygdetid = 20L,
                    sakId = 1L,
                    beskrivelse = "test periode 2",
                ),
            )

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            val client =
                createClient {
                    install(ContentNegotiation) {
                        jackson { registerModule(JavaTimeModule()) }
                    }
                }

            client.get("/api/beregning/beregningsgrunnlag/$behandlingId/overstyr") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }.let { response ->
                response.status shouldBe HttpStatusCode.OK

                val grunnlag = objectMapper.readValue(response.bodyAsText(), OverstyrBeregningGrunnlagDTO::class.java)

                grunnlag.perioder.let { perioder ->
                    perioder.size shouldBe 2
                    perioder.minBy { it.fom }.let { periode ->
                        periode.fom shouldBe LocalDate.now().minusYears(12L)
                        periode.tom shouldBe LocalDate.now().minusYears(6L)
                        periode.data.utbetaltBeloep shouldBe 123L
                        periode.data.trygdetid shouldBe 10L
                    }
                    perioder.maxBy { it.fom }.let { periode ->
                        periode.fom shouldBe LocalDate.now().minusYears(6L)
                        periode.tom shouldBe null
                        periode.data.utbetaltBeloep shouldBe 456L
                        periode.data.trygdetid shouldBe 20L
                    }
                }
            }
        }
    }

    @Test
    fun `skal lagre overstyr beregning grunnlag`() {
        val behandlingId = randomUUID()
        val slot = slot<List<OverstyrBeregningGrunnlagDao>>()

        coEvery { behandlingKlient.harTilgangTilBehandling(behandlingId, any()) } returns true

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns
            DetaljertBehandling(
                id = randomUUID(),
                sak = 222,
                sakType = SakType.BARNEPENSJON,
                behandlingOpprettet = LocalDateTime.now(),
                soeknadMottattDato = null,
                innsender = null,
                soeker = "diam",
                gjenlevende = listOf(),
                avdoed = listOf(),
                soesken = listOf(),
                status = BehandlingStatus.TRYGDETID_OPPDATERT,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                virkningstidspunkt = null,
                revurderingsaarsak = null,
                prosesstype = Prosesstype.MANUELL,
                boddEllerArbeidetUtlandet = null,
                revurderingInfo = null,
                enhet = "1111",
                kilde = Vedtaksloesning.GJENNY,
            )

        every { repository.lagreOverstyrBeregningGrunnlagForBehandling(behandlingId, capture(slot)) } just runs

        every { repository.finnOverstyrBeregningGrunnlagForBehandling(behandlingId) } returns
            listOf(
                OverstyrBeregningGrunnlagDao(
                    id = randomUUID(),
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.now().minusYears(12L),
                    datoTOM = LocalDate.now().minusYears(6L),
                    utbetaltBeloep = 123L,
                    trygdetid = 10L,
                    sakId = 222L,
                    beskrivelse = "test periode 1",
                ),
                OverstyrBeregningGrunnlagDao(
                    id = randomUUID(),
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.now().minusYears(6L),
                    datoTOM = null,
                    utbetaltBeloep = 456L,
                    trygdetid = 20L,
                    sakId = 222L,
                    beskrivelse = "test periode 2",
                ),
            )

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { beregningsGrunnlag(service, behandlingKlient) } }

            val client =
                createClient {
                    install(ContentNegotiation) {
                        jackson { registerModule(JavaTimeModule()) }
                    }
                }

            client.post("/api/beregning/beregningsgrunnlag/$behandlingId/overstyr") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(
                    OverstyrBeregningGrunnlagDTO(
                        perioder =
                            listOf(
                                GrunnlagMedPeriode(
                                    data =
                                        OverstyrBeregningGrunnlag(
                                            utbetaltBeloep = 123L,
                                            trygdetid = 10L,
                                            beskrivelse = "test periode 1",
                                        ),
                                    fom = LocalDate.now().minusYears(12L),
                                    tom = LocalDate.now().minusYears(6L),
                                ),
                                GrunnlagMedPeriode(
                                    data =
                                        OverstyrBeregningGrunnlag(
                                            utbetaltBeloep = 456L,
                                            trygdetid = 20L,
                                            beskrivelse = "test periode 2",
                                        ),
                                    fom = LocalDate.now().minusYears(6L),
                                    tom = null,
                                ),
                            ),
                    ),
                )
            }.let { response ->
                response.status shouldBe HttpStatusCode.OK

                val grunnlag = objectMapper.readValue(response.bodyAsText(), OverstyrBeregningGrunnlagDTO::class.java)

                grunnlag.perioder.let { perioder ->
                    perioder.size shouldBe 2
                    perioder.minBy { it.fom }.let { periode ->
                        periode.fom shouldBe LocalDate.now().minusYears(12L)
                        periode.tom shouldBe LocalDate.now().minusYears(6L)
                        periode.data.utbetaltBeloep shouldBe 123L
                        periode.data.trygdetid shouldBe 10L
                    }
                    perioder.maxBy { it.fom }.let { periode ->
                        periode.fom shouldBe LocalDate.now().minusYears(6L)
                        periode.tom shouldBe null
                        periode.data.utbetaltBeloep shouldBe 456L
                        periode.data.trygdetid shouldBe 20L
                    }
                }

                slot.captured.let { daoList ->
                    daoList.size shouldBe 2

                    daoList.minBy { it.datoFOM }.let { dao ->
                        dao.datoFOM shouldBe LocalDate.now().minusYears(12L)
                        dao.datoTOM shouldBe LocalDate.now().minusYears(6L)
                        dao.utbetaltBeloep shouldBe 123L
                        dao.trygdetid shouldBe 10L
                        dao.sakId shouldBe 222L
                    }
                    daoList.maxBy { it.datoFOM }.let { dao ->
                        dao.datoFOM shouldBe LocalDate.now().minusYears(6L)
                        dao.datoTOM shouldBe null
                        dao.utbetaltBeloep shouldBe 456L
                        dao.trygdetid shouldBe 20L
                        dao.sakId shouldBe 222L
                    }
                }
            }
        }
    }

    private val token: String by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims = mapOf("navn" to "John Doe", "NAVident" to "Saksbehandler01"),
        ).serialize()
    }

    private val systemToken: String by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims = mapOf("oid" to "woot", "sub" to "woot"),
        ).serialize()
    }

    private companion object {
        const val CLIENT_ID = "azure-id for saksbehandler"
    }
}
