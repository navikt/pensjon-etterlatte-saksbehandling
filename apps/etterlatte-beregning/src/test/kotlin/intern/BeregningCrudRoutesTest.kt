package no.nav.etterlatte.intern

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.avkorting.IngenInntektInnvilgetPeriode
import no.nav.etterlatte.avkorting.Inntektsavkorting
import no.nav.etterlatte.avkorting.Restanse
import no.nav.etterlatte.avkorting.etteroppgjoer.BeregnetEtteroppgjoerResultat
import no.nav.etterlatte.avkorting.etteroppgjoer.EtteroppgjoerRepository
import no.nav.etterlatte.avkorting.etteroppgjoer.ReferanseEtteroppgjoer
import no.nav.etterlatte.avkorting.regler.EtteroppgjoerGrense
import no.nav.etterlatte.avkorting.regler.EtteroppgjoerRettsgebyr
import no.nav.etterlatte.beregning.AnvendtTrygdetidRegelresultat
import no.nav.etterlatte.beregning.AnvendtTrygdetidRepository
import no.nav.etterlatte.beregning.BeregningRepository
import no.nav.etterlatte.beregning.OverstyrBeregning
import no.nav.etterlatte.beregning.OverstyrBeregningStatus
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagRepository
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlagDao
import no.nav.etterlatte.beregning.grunnlag.Vedtaksperiode
import no.nav.etterlatte.beregning.regler.AnvendtTrygdetid
import no.nav.etterlatte.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.beregning
import no.nav.etterlatte.beregning.regler.etteroppgjoer
import no.nav.etterlatte.beregning.regler.lagreSanksjon
import no.nav.etterlatte.beregning.regler.sanksjon
import no.nav.etterlatte.beregning.regler.toGrunnlag
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.ktor.token.issueSystembrukerToken
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoRequest
import no.nav.etterlatte.libs.common.beregning.OverstyrtBeregningKategori
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.regler.Beregningstall
import no.nav.etterlatte.sanksjon.SanksjonRepository
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BeregningCrudRoutesTest {
    private val oauth = MockOAuth2Server()
    private val beregningRepository = mockk<BeregningRepository>()
    private val grunnlagRepository = mockk<BeregningsGrunnlagRepository>()
    private val trygdetidRepository = mockk<AnvendtTrygdetidRepository>()
    private val avkortingRepository = mockk<AvkortingRepository>()
    private val etteroppgjoerRepository = mockk<EtteroppgjoerRepository>()
    private val sanksjonRepository = mockk<SanksjonRepository>()
    private val behandlingApp = "dev-gcp:etterlatte:etterlatte-behandling"
    private lateinit var behandlingToken: String
    private val behandlingId = UUID.randomUUID()
    private val sakId = SakId(123)
    private val kilde = Grunnlagsopplysning.Saksbehandler.create("Z123456")

    @BeforeAll
    fun start() {
        oauth.startRandomPort()
        behandlingToken = oauth.issueSystembrukerToken(behandlingApp)
    }

    @AfterAll
    fun stopp() {
        oauth.shutdown()
    }

    @BeforeEach
    fun nullstillMocks() {
        clearMocks(
            beregningRepository,
            grunnlagRepository,
            trygdetidRepository,
            avkortingRepository,
            etteroppgjoerRepository,
            sanksjonRepository,
        )
    }

    @Test
    fun `henter og lagrer beregning uten aa endre data`() {
        val beregning = beregning().copy(behandlingId = behandlingId)
        every { beregningRepository.hent(behandlingId) } returns beregning
        every { beregningRepository.lagreEllerOppdaterBeregning(beregning) } returns beregning
        every { beregningRepository.slettOverstyrtBeregningsgrunnlag(behandlingId) } just runs

        medServer { client ->
            client.requestCrud(HttpMethod.Get, "/beregning/$behandlingId").json() shouldBe beregning.toJson()
            client.requestCrud(HttpMethod.Put, "/beregning", beregning).json() shouldBe beregning.toJson()
            client.requestCrud(HttpMethod.Delete, "/beregning/$behandlingId/overstyrt-grunnlag").status shouldBe HttpStatusCode.NoContent
        }

        verify(exactly = 1) {
            beregningRepository.hent(behandlingId)
            beregningRepository.lagreEllerOppdaterBeregning(beregning)
            beregningRepository.slettOverstyrtBeregningsgrunnlag(behandlingId)
        }
    }

    @Test
    fun `bevarer null for valgfrie regelresultater`() {
        val beregning = beregning().let { it.copy(beregningsperioder = it.beregningsperioder.map { it.copy(regelResultat = null) }) }
        val overstyrt = listOf(overstyrtGrunnlag().copy(reguleringRegelresultat = null))
        objectMapper.readValue<List<OverstyrBeregningGrunnlagDao>>(overstyrt.toJson()).single().reguleringRegelresultat shouldBe null
        every { beregningRepository.lagreEllerOppdaterBeregning(beregning) } returns beregning
        every { grunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(behandlingId, overstyrt) } just runs

        medServer { client ->
            client.requestCrud(HttpMethod.Put, "/beregning", beregning).json() shouldBe beregning.toJson()
            client
                .requestCrud(HttpMethod.Put, "/beregningsgrunnlag/$behandlingId/overstyrt", overstyrt)
                .status shouldBe HttpStatusCode.NoContent
        }

        verify(exactly = 1) {
            beregningRepository.lagreEllerOppdaterBeregning(beregning)
            grunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(behandlingId, overstyrt)
        }
    }

    @Test
    fun `henter lagrer og deaktiverer overstyring`() {
        val overstyring =
            OverstyrBeregning(
                sakId = sakId,
                beskrivelse = "Manuelt beregnet",
                tidspunkt = Tidspunkt.now(),
                kategori = OverstyrtBeregningKategori.UKJENT_KATEGORI,
            )
        val inaktiv = overstyring.copy(status = OverstyrBeregningStatus.IKKE_AKTIV)
        every { beregningRepository.hentOverstyrBeregning(sakId) } returns overstyring
        every { beregningRepository.opprettOverstyrBeregning(overstyring) } returns overstyring
        every { beregningRepository.opprettOverstyrBeregning(inaktiv) } returns null
        every { beregningRepository.deaktiverOverstyrtBeregning(sakId) } just runs

        medServer { client ->
            client.requestCrud(HttpMethod.Get, "/beregning/overstyring/$sakId").json() shouldBe overstyring.toJson()
            client.requestCrud(HttpMethod.Put, "/beregning/overstyring", overstyring).json() shouldBe overstyring.toJson()
            client.requestCrud(HttpMethod.Put, "/beregning/overstyring", inaktiv).status shouldBe HttpStatusCode.NoContent
            client.requestCrud(HttpMethod.Delete, "/beregning/overstyring/$sakId").status shouldBe HttpStatusCode.NoContent
        }

        verify(exactly = 1) {
            beregningRepository.hentOverstyrBeregning(sakId)
            beregningRepository.opprettOverstyrBeregning(overstyring)
            beregningRepository.opprettOverstyrBeregning(inaktiv)
            beregningRepository.deaktiverOverstyrtBeregning(sakId)
        }
    }

    @Test
    fun `henter og lagrer beregningsgrunnlag med vedtaksperioder`() {
        val grunnlag =
            BeregningsGrunnlag(
                behandlingId = behandlingId,
                kilde = kilde,
                beregningsMetode = BeregningsMetode.NASJONAL.toGrunnlag(),
                vedtaksperioder = listOf(Vedtaksperiode(YearMonth.of(2024, 1))),
            )
        val overstyrt = listOf(overstyrtGrunnlag())
        every { grunnlagRepository.finnBeregningsGrunnlag(behandlingId) } returns grunnlag
        every { grunnlagRepository.lagreBeregningsGrunnlag(grunnlag) } returns true
        every { grunnlagRepository.finnOverstyrBeregningGrunnlagForBehandling(behandlingId) } returns overstyrt
        every { grunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(behandlingId, overstyrt) } just runs

        medServer { client ->
            client.requestCrud(HttpMethod.Get, "/beregningsgrunnlag/$behandlingId").json() shouldBe grunnlag.toJson()
            client.requestCrud(HttpMethod.Put, "/beregningsgrunnlag", grunnlag).bodyAsText() shouldBe "true"
            client.requestCrud(HttpMethod.Get, "/beregningsgrunnlag/$behandlingId/overstyrt").json() shouldBe overstyrt.toJson()
            client
                .requestCrud(HttpMethod.Put, "/beregningsgrunnlag/$behandlingId/overstyrt", overstyrt)
                .status shouldBe HttpStatusCode.NoContent
        }

        verify(exactly = 1) {
            grunnlagRepository.finnBeregningsGrunnlag(behandlingId)
            grunnlagRepository.lagreBeregningsGrunnlag(grunnlag)
            grunnlagRepository.finnOverstyrBeregningGrunnlagForBehandling(behandlingId)
            grunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(behandlingId, overstyrt)
        }
    }

    @Test
    fun `lagrer typet anvendt trygdetid med uendret regeltre`() {
        val fom = LocalDate.of(2024, 1, 1)
        val request =
            LagreAnvendtTrygdetidRequest(
                anvendt =
                    listOf(
                        GrunnlagMedPeriode(
                            data = listOf(AnvendtTrygdetid(BeregningsMetode.NASJONAL, Beregningstall(40), "avdoed")),
                            fom = fom,
                        ),
                    ),
                utrekning =
                    listOf(
                        AnvendtTrygdetidRegelresultat(
                            periode = RegelPeriode(fom),
                            resultat = mapOf("regel" to mapOf("beskrivelse" to "Regeltre")).toJsonNode(),
                            reglerVersjon = "1",
                        ),
                    ),
            )
        every { trygdetidRepository.lagreAnvendtTrygdetid(behandlingId, request.utrekning, request.anvendt) } returns 1

        medServer { client ->
            val response = client.requestCrud(HttpMethod.Post, "/anvendt-trygdetid/$behandlingId", request)
            response.status shouldBe HttpStatusCode.Created
            response.bodyAsText() shouldBe "1"
        }

        verify(exactly = 1) { trygdetidRepository.lagreAnvendtTrygdetid(behandlingId, request.utrekning, request.anvendt) }
    }

    @Test
    fun `henter og lagrer full avkorting med etteroppgjoer og eldre inntektsgrunnlag`() {
        val inntekt = avkortinggrunnlag(periode = Periode(YearMonth.of(2025, 1), null))
        val etteroppgjoer =
            etteroppgjoer(
                avkortetYtelse =
                    listOf(
                        avkortetYtelse().copy(
                            restanse = Restanse(UUID.randomUUID(), 100, 10, Tidspunkt.now(), null, kilde),
                        ),
                    ),
            )
        val avkorting =
            Avkorting(
                listOf(
                    etteroppgjoer,
                    aarsoppgjoer(
                        aar = 2025,
                        inntektsavkorting =
                            listOf(
                                Inntektsavkorting(inntekt.copy(periode = Periode(inntekt.periode.fom, YearMonth.of(2025, 5)))),
                                Inntektsavkorting(
                                    inntekt.copy(
                                        periode = Periode(YearMonth.of(2025, 6), null),
                                        inntektInnvilgetPeriode = IngenInntektInnvilgetPeriode,
                                    ),
                                ),
                            ),
                    ),
                ),
            )
        val request = LagreAvkortingRequest(sakId, avkorting)
        every { avkortingRepository.hentAvkorting(behandlingId) } returns avkorting
        every { avkortingRepository.lagreAvkorting(behandlingId, sakId, avkorting) } just runs
        every { avkortingRepository.hentFaktiskInntekt(etteroppgjoer.id) } returns etteroppgjoer.inntekt
        every { avkortingRepository.slettForBehandling(behandlingId) } just runs

        medServer { client ->
            val response = client.requestCrud(HttpMethod.Get, "/avkorting/$behandlingId")
            response.status shouldBe HttpStatusCode.OK
            objectMapper.readValue<Avkorting>(response.bodyAsText()) shouldBe avkorting
            client.requestCrud(HttpMethod.Put, "/avkorting/$behandlingId", request).status shouldBe HttpStatusCode.NoContent
            client
                .requestCrud(HttpMethod.Get, "/avkorting/faktisk-inntekt/${etteroppgjoer.id}")
                .json() shouldBe etteroppgjoer.inntekt.toJson()
            client.requestCrud(HttpMethod.Delete, "/avkorting/$behandlingId").status shouldBe HttpStatusCode.NoContent
        }

        verify(exactly = 1) {
            avkortingRepository.hentAvkorting(behandlingId)
            avkortingRepository.lagreAvkorting(behandlingId, sakId, avkorting)
            avkortingRepository.hentFaktiskInntekt(etteroppgjoer.id)
            avkortingRepository.slettForBehandling(behandlingId)
        }
    }

    @Test
    fun `henter aarsoppgjoer behandlinger og inntektsstatus`() {
        val behandlinger = listOf(behandlingId, UUID.randomUUID())
        val aarsoppgjoer = listOf(aarsoppgjoer())
        val inntektRequest = InntektsjusteringAvkortingInfoRequest(sakId, 2024, behandlingId)
        every { avkortingRepository.hentAlleAarsoppgjoer(sakId) } returns aarsoppgjoer
        every { avkortingRepository.hentAlleAarsoppgjoer(behandlinger) } returns aarsoppgjoer
        every { avkortingRepository.hentAlleBehandlingerMedAarsoppgjoer(sakId) } returns behandlinger
        every { avkortingRepository.harSakInntektForAar(inntektRequest) } returns false

        medServer { client ->
            client.requestCrud(HttpMethod.Get, "/avkorting/sak/$sakId/aarsoppgjoer").json() shouldBe aarsoppgjoer.toJson()
            client.requestCrud(HttpMethod.Post, "/avkorting/aarsoppgjoer", behandlinger).json() shouldBe aarsoppgjoer.toJson()
            client.requestCrud(HttpMethod.Get, "/avkorting/sak/$sakId/behandlinger").json() shouldBe behandlinger.toJson()
            client.requestCrud(HttpMethod.Post, "/avkorting/inntekt-for-aar", inntektRequest).bodyAsText() shouldBe "false"
        }

        verify(exactly = 1) {
            avkortingRepository.hentAlleAarsoppgjoer(sakId)
            avkortingRepository.hentAlleAarsoppgjoer(behandlinger)
            avkortingRepository.hentAlleBehandlingerMedAarsoppgjoer(sakId)
            avkortingRepository.harSakInntektForAar(inntektRequest)
        }
    }

    @Test
    fun `henter og lagrer etteroppgjoersresultat med regelresultat og referanser`() {
        val resultat = etteroppgjoerResultat()
        every {
            etteroppgjoerRepository.hentEtteroppgjoerResultat(resultat.aar, resultat.forbehandlingId, resultat.sisteIverksatteBehandlingId)
        } returns resultat
        every { etteroppgjoerRepository.lagreEtteroppgjoerResultat(resultat) } just runs

        medServer { client ->
            client
                .requestCrud(
                    HttpMethod.Get,
                    "/etteroppgjoer/resultat/${resultat.forbehandlingId}/${resultat.aar}/${resultat.sisteIverksatteBehandlingId}",
                ).json() shouldBe resultat.toJson()
            client.requestCrud(HttpMethod.Put, "/etteroppgjoer/resultat", resultat).status shouldBe HttpStatusCode.NoContent
        }

        verify(exactly = 1) {
            etteroppgjoerRepository.hentEtteroppgjoerResultat(resultat.aar, resultat.forbehandlingId, resultat.sisteIverksatteBehandlingId)
            etteroppgjoerRepository.lagreEtteroppgjoerResultat(resultat)
        }
    }

    @Test
    fun `henter oppretter kopierer oppdaterer og sletter sanksjoner`() {
        val sanksjon = sanksjon(behandlingId = behandlingId, sakId = sakId)
        val ny = lagreSanksjon(sakId = sakId)
        val oppdatert = ny.copy(id = sanksjon.id, beskrivelse = "Oppdatert")
        val ident = "Z123456"
        every { sanksjonRepository.hentSanksjon(behandlingId) } returns listOf(sanksjon)
        every { sanksjonRepository.hentSanksjonMedId(sanksjon.id!!) } returns sanksjon
        every { sanksjonRepository.opprettSanksjon(behandlingId, sakId, ident, ny) } just runs
        every { sanksjonRepository.opprettSanksjonFraKopi(behandlingId, sakId, sanksjon) } just runs
        every { sanksjonRepository.oppdaterSanksjon(oppdatert, ident) } just runs
        every { sanksjonRepository.slettSanksjon(sanksjon.id!!) } returns 1

        medServer { client ->
            client.requestCrud(HttpMethod.Get, "/sanksjon/behandling/$behandlingId").json() shouldBe listOf(sanksjon).toJson()
            client.requestCrud(HttpMethod.Get, "/sanksjon/${sanksjon.id}").json() shouldBe sanksjon.toJson()
            client
                .requestCrud(HttpMethod.Post, "/sanksjon/behandling/$behandlingId", OpprettSanksjonRequest(sakId, ident, ny))
                .status shouldBe HttpStatusCode.Created
            client
                .requestCrud(HttpMethod.Post, "/sanksjon/behandling/$behandlingId/kopi", OpprettSanksjonFraKopiRequest(sakId, sanksjon))
                .status shouldBe HttpStatusCode.Created
            client
                .requestCrud(HttpMethod.Put, "/sanksjon", OppdaterSanksjonRequest(ident, oppdatert))
                .status shouldBe HttpStatusCode.NoContent
            client.requestCrud(HttpMethod.Delete, "/sanksjon/${sanksjon.id}").bodyAsText() shouldBe "1"
        }

        verify(exactly = 1) {
            sanksjonRepository.hentSanksjon(behandlingId)
            sanksjonRepository.hentSanksjonMedId(sanksjon.id!!)
            sanksjonRepository.opprettSanksjon(behandlingId, sakId, ident, ny)
            sanksjonRepository.opprettSanksjonFraKopi(behandlingId, sakId, sanksjon)
            sanksjonRepository.oppdaterSanksjon(oppdatert, ident)
            sanksjonRepository.slettSanksjon(sanksjon.id!!)
        }
    }

    @ParameterizedTest
    @MethodSource("endepunkter")
    fun `avviser kall uten token fra behandling`(
        method: HttpMethod,
        path: String,
    ) {
        medServer { client ->
            client.requestCrud(method, path, token = null).status shouldBe HttpStatusCode.Unauthorized
            client.requestCrud(method, path, token = "ugyldig").status shouldBe HttpStatusCode.Unauthorized
            listOf(
                oauth.issueSaksbehandlerToken(),
                oauth.issueSystembrukerToken("dev-gcp:etterlatte:etterlatte-beregning-kafka"),
                oauth.issueSystembrukerToken("prod-gcp:etterlatte:etterlatte-behandling"),
            ).forEach { token ->
                client.requestCrud(method, path, token = token).status shouldBe HttpStatusCode.Forbidden
            }
        }
        verifiserIngenRepositoryKall()
    }

    @ParameterizedTest
    @MethodSource("manglendeRessurser")
    fun `returnerer 404 for manglende enkeltressurser`(path: String) {
        every { beregningRepository.hent(any()) } returns null
        every { beregningRepository.hentOverstyrBeregning(any()) } returns null
        every { grunnlagRepository.finnBeregningsGrunnlag(any()) } returns null
        every { avkortingRepository.hentAvkorting(any()) } returns null
        every { avkortingRepository.hentFaktiskInntekt(any()) } returns null
        every { etteroppgjoerRepository.hentEtteroppgjoerResultat(any(), any(), any()) } returns null
        every { sanksjonRepository.hentSanksjonMedId(any()) } returns null

        medServer { client ->
            client.requestCrud(HttpMethod.Get, path).status shouldBe HttpStatusCode.NotFound
        }
    }

    @ParameterizedTest
    @MethodSource("ugyldigeParametre")
    fun `returnerer 400 for ugyldige parametre`(path: String) {
        medServer { client ->
            client.requestCrud(HttpMethod.Get, path).status shouldBe HttpStatusCode.BadRequest
        }
        verifiserIngenRepositoryKall()
    }

    @ParameterizedTest
    @MethodSource("endepunkterMedBody")
    fun `returnerer 400 for ugyldig request body`(
        method: HttpMethod,
        path: String,
    ) {
        medServer { client ->
            client.requestCrud(method, path, mapOf("ugyldig" to "body")).status shouldBe HttpStatusCode.BadRequest
        }
        verifiserIngenRepositoryKall()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """{"anvendt":{},"utrekning":[]}""",
            """{"anvendt":[],"utrekning":{}}""",
            """{"anvendt":[{"data":[{"trygdetid":40}],"fom":"2024-01-01"}],"utrekning":[]}""",
            """{"anvendt":[],"utrekning":[{"resultat":{}}]}""",
            """{"anvendt":[],"utrekning":[{"periode":{"fraDato":"ugyldig"},"resultat":{},"reglerVersjon":"1"}]}""",
        ],
    )
    fun `avviser anvendt trygdetid med ugyldig struktur`(body: String) {
        medServer { client ->
            client
                .requestCrud(
                    HttpMethod.Post,
                    "/anvendt-trygdetid/$behandlingId",
                    objectMapper.readTree(body),
                ).status shouldBe HttpStatusCode.BadRequest
        }
        verifiserIngenRepositoryKall()
    }

    @Test
    fun `returnerer tomme lister og antall slettede rader uendret`() {
        every { grunnlagRepository.finnOverstyrBeregningGrunnlagForBehandling(behandlingId) } returns emptyList()
        every { avkortingRepository.hentAlleAarsoppgjoer(sakId) } returns emptyList()
        every { avkortingRepository.hentAlleAarsoppgjoer(emptyList()) } returns emptyList()
        every { avkortingRepository.hentAlleBehandlingerMedAarsoppgjoer(sakId) } returns emptyList()
        every { sanksjonRepository.hentSanksjon(behandlingId) } returns emptyList()
        every { sanksjonRepository.slettSanksjon(behandlingId) } returns 0

        medServer { client ->
            listOf(
                "/beregningsgrunnlag/$behandlingId/overstyrt",
                "/avkorting/sak/$sakId/aarsoppgjoer",
                "/avkorting/sak/$sakId/behandlinger",
                "/sanksjon/behandling/$behandlingId",
            ).forEach { path ->
                client.requestCrud(HttpMethod.Get, path).bodyAsText() shouldBe "[]"
            }
            client.requestCrud(HttpMethod.Post, "/avkorting/aarsoppgjoer", emptyList<UUID>()).bodyAsText() shouldBe "[]"
            client.requestCrud(HttpMethod.Delete, "/sanksjon/$behandlingId").bodyAsText() shouldBe "0"
        }
    }

    @Test
    fun `avviser overstyrt grunnlag med annen behandlingId`() {
        medServer { client ->
            client
                .requestCrud(
                    HttpMethod.Put,
                    "/beregningsgrunnlag/$behandlingId/overstyrt",
                    listOf(overstyrtGrunnlag().copy(behandlingId = UUID.randomUUID())),
                ).status shouldBe HttpStatusCode.BadRequest
        }
        verifiserIngenRepositoryKall()
    }

    @Test
    fun `avviser oppdatering av sanksjon uten id`() {
        medServer { client ->
            client
                .requestCrud(HttpMethod.Put, "/sanksjon", OppdaterSanksjonRequest("Z123456", lagreSanksjon()))
                .status shouldBe HttpStatusCode.BadRequest
        }
        verifiserIngenRepositoryKall()
    }

    @Test
    fun `returnerer feil hvis repository feiler`() {
        every { beregningRepository.hent(behandlingId) } throws IllegalStateException("Databasefeil")
        medServer { client ->
            client.requestCrud(HttpMethod.Get, "/beregning/$behandlingId").status shouldBe HttpStatusCode.InternalServerError
        }
    }

    private fun medServer(test: suspend (HttpClient) -> Unit) =
        testApplication {
            val httpClient =
                runServer(oauth) {
                    beregningCrudRoutes(
                        beregningRepository,
                        grunnlagRepository,
                        trygdetidRepository,
                        avkortingRepository,
                        etteroppgjoerRepository,
                        sanksjonRepository,
                        behandlingApp,
                    )
                }
            test(httpClient)
        }

    private suspend fun HttpClient.requestCrud(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        token: String? = behandlingToken,
    ): HttpResponse =
        request("/api/beregning/intern$path") {
            this.method = method
            token?.let { bearerAuth(it) }
            body?.let {
                contentType(ContentType.Application.Json)
                setBody(it.toJson())
            }
        }

    private suspend fun HttpResponse.json() =
        bodyAsText().let {
            status shouldBe HttpStatusCode.OK
            it
        }

    private fun verifiserIngenRepositoryKall() {
        verify {
            beregningRepository wasNot Called
            grunnlagRepository wasNot Called
            trygdetidRepository wasNot Called
            avkortingRepository wasNot Called
            etteroppgjoerRepository wasNot Called
            sanksjonRepository wasNot Called
        }
    }

    private fun overstyrtGrunnlag() =
        OverstyrBeregningGrunnlagDao(
            id = UUID.randomUUID(),
            behandlingId = behandlingId,
            datoFOM = LocalDate.of(2024, 1, 1),
            datoTOM = null,
            utbetaltBeloep = 5000,
            foreldreloessats = false,
            trygdetid = 40,
            trygdetidForIdent = null,
            prorataBroekTeller = null,
            prorataBroekNevner = null,
            sakId = sakId,
            beskrivelse = "Manuelt beregnet",
            aarsak = "ANNET",
            kilde = kilde,
            reguleringRegelresultat = mapOf("regel" to "resultat").toJsonNode(),
        )

    private fun etteroppgjoerResultat() =
        BeregnetEtteroppgjoerResultat(
            id = UUID.randomUUID(),
            aar = 2024,
            forbehandlingId = behandlingId,
            sisteIverksatteBehandlingId = UUID.randomUUID(),
            utbetaltStoenad = 10000,
            nyBruttoStoenad = 9000,
            differanse = -1000,
            grense =
                EtteroppgjoerGrense(
                    tilbakekreving = Beregningstall(100),
                    etterbetaling = Beregningstall(200),
                    rettsgebyr = EtteroppgjoerRettsgebyr(LocalDate.of(2024, 1, 1), Beregningstall(1000)),
                ),
            resultatType = EtteroppgjoerResultatType.TILBAKEKREVING,
            harIngenInntekt = false,
            tidspunkt = Tidspunkt.now(),
            regelResultat = mapOf("regel" to "resultat").toJsonNode(),
            kilde = kilde,
            referanseAvkorting = ReferanseEtteroppgjoer(UUID.randomUUID(), UUID.randomUUID(), listOf(123L)),
        )

    private fun manglendeRessurser() =
        listOf(
            "/beregning/$behandlingId",
            "/beregning/overstyring/$sakId",
            "/beregningsgrunnlag/$behandlingId",
            "/avkorting/$behandlingId",
            "/avkorting/faktisk-inntekt/$behandlingId",
            "/etteroppgjoer/resultat/$behandlingId/2024/$behandlingId",
            "/sanksjon/$behandlingId",
        )

    private fun ugyldigeParametre() =
        listOf(
            "/beregning/ugyldig",
            "/beregning/overstyring/ugyldig",
            "/beregningsgrunnlag/ugyldig",
            "/avkorting/ugyldig",
            "/avkorting/faktisk-inntekt/ugyldig",
            "/avkorting/sak/ugyldig/aarsoppgjoer",
            "/etteroppgjoer/resultat/ugyldig/2024/$behandlingId",
            "/etteroppgjoer/resultat/$behandlingId/ugyldig/$behandlingId",
            "/etteroppgjoer/resultat/$behandlingId/2024/ugyldig",
            "/sanksjon/ugyldig",
            "/sanksjon/behandling/ugyldig",
        )

    private fun endepunkter() =
        listOf(
            Arguments.of(HttpMethod.Get, "/beregning/$behandlingId"),
            Arguments.of(HttpMethod.Put, "/beregning"),
            Arguments.of(HttpMethod.Delete, "/beregning/$behandlingId/overstyrt-grunnlag"),
            Arguments.of(HttpMethod.Get, "/beregning/overstyring/$sakId"),
            Arguments.of(HttpMethod.Put, "/beregning/overstyring"),
            Arguments.of(HttpMethod.Delete, "/beregning/overstyring/$sakId"),
            Arguments.of(HttpMethod.Get, "/beregningsgrunnlag/$behandlingId"),
            Arguments.of(HttpMethod.Put, "/beregningsgrunnlag"),
            Arguments.of(HttpMethod.Get, "/beregningsgrunnlag/$behandlingId/overstyrt"),
            Arguments.of(HttpMethod.Put, "/beregningsgrunnlag/$behandlingId/overstyrt"),
            Arguments.of(HttpMethod.Post, "/anvendt-trygdetid/$behandlingId"),
            Arguments.of(HttpMethod.Get, "/avkorting/$behandlingId"),
            Arguments.of(HttpMethod.Put, "/avkorting/$behandlingId"),
            Arguments.of(HttpMethod.Delete, "/avkorting/$behandlingId"),
            Arguments.of(HttpMethod.Get, "/avkorting/faktisk-inntekt/$behandlingId"),
            Arguments.of(HttpMethod.Get, "/avkorting/sak/$sakId/aarsoppgjoer"),
            Arguments.of(HttpMethod.Get, "/avkorting/sak/$sakId/behandlinger"),
            Arguments.of(HttpMethod.Post, "/avkorting/aarsoppgjoer"),
            Arguments.of(HttpMethod.Post, "/avkorting/inntekt-for-aar"),
            Arguments.of(HttpMethod.Get, "/etteroppgjoer/resultat/$behandlingId/2024/$behandlingId"),
            Arguments.of(HttpMethod.Put, "/etteroppgjoer/resultat"),
            Arguments.of(HttpMethod.Get, "/sanksjon/behandling/$behandlingId"),
            Arguments.of(HttpMethod.Post, "/sanksjon/behandling/$behandlingId"),
            Arguments.of(HttpMethod.Post, "/sanksjon/behandling/$behandlingId/kopi"),
            Arguments.of(HttpMethod.Put, "/sanksjon"),
            Arguments.of(HttpMethod.Get, "/sanksjon/$behandlingId"),
            Arguments.of(HttpMethod.Delete, "/sanksjon/$behandlingId"),
        )

    private fun endepunkterMedBody() = endepunkter().filter { it.get()[0] == HttpMethod.Post || it.get()[0] == HttpMethod.Put }
}
