package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.Navkontor
import no.nav.etterlatte.behandling.domain.SaksbehandlerEnhet
import no.nav.etterlatte.behandling.domain.SaksbehandlerTema
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.BrevStatus
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.behandling.klienter.OpprettetBrevDto
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.common.klienter.SakSammendragResponse
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.ktor.issueSaksbehandlerToken
import no.nav.etterlatte.ktor.issueSystembrukerToken
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.Mottaker
import no.nav.etterlatte.libs.common.behandling.Mottakerident
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SakidOgRolle
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.brev.BestillingsIdDto
import no.nav.etterlatte.libs.common.brev.JournalpostIdDto
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakLagretDto
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.oppgaveGosys.GosysApiOppgave
import no.nav.etterlatte.oppgaveGosys.GosysOppgaveKlient
import no.nav.etterlatte.oppgaveGosys.GosysOppgaver
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import java.util.UUID

abstract class BehandlingIntegrationTest {
    companion object {
        @RegisterExtension
        private val dbExtension = DatabaseExtension()
    }

    private val postgreSQLContainer = GenerellDatabaseExtension.postgreSQLContainer
    protected val server: MockOAuth2Server = MockOAuth2Server()
    internal lateinit var applicationContext: ApplicationContext

    protected fun startServer(
        norg2Klient: Norg2Klient? = null,
        featureToggleService: FeatureToggleService = DummyFeatureToggleService(),
    ) {
        server.start()

        applicationContext =
            ApplicationContext(
                env =
                    System.getenv().toMutableMap().apply {
                        put("KAFKA_RAPID_TOPIC", "test")
                        put("DB_HOST", postgreSQLContainer.host)
                        put("DB_USERNAME", postgreSQLContainer.username)
                        put("DB_PASSWORD", postgreSQLContainer.password)
                        put("DB_PORT", postgreSQLContainer.firstMappedPort.toString())
                        put("DB_DATABASE", postgreSQLContainer.databaseName)
                        put("AZUREAD_ATTESTANT_GROUPID", azureAdAttestantClaim)
                        put("AZUREAD_SAKSBEHANDLER_GROUPID", azureAdSaksbehandlerClaim)
                        put("AZUREAD_STRENGT_FORTROLIG_GROUPID", azureAdStrengtFortroligClaim)
                        put("AZUREAD_EGEN_ANSATT_GROUPID", azureAdEgenAnsattClaim)
                        put("AZUREAD_FORTROLIG_GROUPID", "ea930b6b-9397-44d9-b9e6-f4cf527a632a")
                        put("AZUREAD_NASJONAL_TILGANG_UTEN_LOGG_GROUPID", "753805ea-65a7-4855-bdc3-e6130348df9f")
                        put("AZUREAD_NASJONAL_TILGANG_MED_LOGG_GROUPID", "ea7411eb-8b48-41a0-bc56-7b521fbf0c25")
                        put("NORG2_URL", "http://localhost")
                        put("NAVANSATT_URL", "http://localhost")
                        put("SKJERMING_URL", "http://localhost")
                        put("OPPGAVE_URL", "http://localhost")
                        put("PEN_URL", "http://localhost")
                        put("PEN_CLIENT_ID", "ddd52335-cfe8-4ee9-9e68-416a5ab26efa")
                        put("ETTERLATTE_KLAGE_API_URL", "http://localhost")
                        put("ETTERLATTE_TILBAKEKREVING_URL", "http://localhost")
                        put("ETTERLATTE_MIGRERING_URL", "http://localhost")
                        put("OPPGAVE_SCOPE", "scope")
                    }.let { Miljoevariabler(it) },
                config =
                    ConfigFactory.parseMap(
                        mapOf(
                            "pdltjenester.url" to "http://localhost",
                            "grunnlag.resource.url" to "http://localhost",
                            "vedtak.resource.url" to "http://localhost",
                        ),
                    ),
                rapid = TestProdusent(),
                featureToggleService = featureToggleService,
                pdlHttpClient = pdlHttpClient(),
                skjermingHttpKlient = skjermingHttpClient(),
                grunnlagHttpClient = grunnlagHttpClient(),
                leaderElectionHttpClient = leaderElection(),
                navAnsattKlient = NavAnsattKlientTest(),
                norg2Klient = norg2Klient ?: Norg2KlientTest(),
                grunnlagKlientObo = GrunnlagKlientTest(),
                vedtakKlient = spyk(VedtakKlientTest()),
                gosysOppgaveKlient = GosysOppgaveKlientTest(),
                brevApiHttpClient = BrevApiKlientTest(),
                klageHttpClient = klageHttpClientTest(),
                tilbakekrevingHttpClient = tilbakekrevingHttpClientTest(),
                migreringHttpClient = migreringHttpClientTest(),
                pesysKlient = PesysKlientTest(),
            )
    }

    fun skjermingHttpClient(): HttpClient =
        HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    if (request.url.fullPath.contains("skjermet")) {
                        val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(false.toJson(), headers = headers)
                    } else {
                        error(request.url.fullPath)
                    }
                }
            }
            install(ContentNegotiation) {
                register(
                    ContentType.Application.Json,
                    JacksonConverter(objectMapper),
                )
            }
        }

    fun pdlHttpClient(): HttpClient =
        HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    if (request.url.fullPath.contains("geografisktilknytning")) {
                        val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        val json = GeografiskTilknytning(kommune = "0301").toJson()
                        respond(json, headers = headers)
                    } else if (request.url.fullPath.contains("folkeregisteridenter")) {
                        val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        val json = emptyMap<String, String>().toJson()
                        respond(json, headers = headers)
                    } else if (request.url.fullPath.contains("person/v2")) {
                        val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        val json = mockPerson().toJson()
                        respond(json, headers = headers)
                    } else if (request.url.fullPath.startsWith("/")) {
                        val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        val json = javaClass.getResource("")!!.readText() // TODO: endre name
                        respond(json, headers = headers)
                    } else {
                        error(request.url.fullPath)
                    }
                }
            }
            install(ContentNegotiation) {
                register(
                    ContentType.Application.Json,
                    JacksonConverter(objectMapper),
                )
            }
        }

    fun grunnlagHttpClient(): HttpClient =
        HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    if (request.url.fullPath.matches(Regex("api/grunnlag/behandling/*"))) {
                        val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(Grunnlag.empty().toJson(), headers = headers)
                    } else if (request.url.fullPath.endsWith("/PERSONGALLERI_V1")) {
                        val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(
                            content =
                                Grunnlagsopplysning(
                                    id = UUID.randomUUID(),
                                    kilde = Grunnlagsopplysning.Privatperson("fnr", Tidspunkt.now()),
                                    meta = emptyMap<String, String>().toObjectNode(),
                                    opplysningType = Opplysningstype.PERSONGALLERI_V1,
                                    opplysning =
                                        Persongalleri(
                                            "soeker",
                                            "innsender",
                                            listOf("soesken"),
                                            listOf("avdoed"),
                                            listOf("gjenlevende"),
                                        ),
                                ).toJson(),
                            headers = headers,
                        )
                    } else if (request.url.fullPath.endsWith("/roller")) {
                        val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(
                            PersonMedSakerOgRoller("08071272487", listOf(SakidOgRolle(1, Saksrolle.SOEKER))).toJson(),
                            headers = headers,
                        )
                    } else if (request.url.fullPath.endsWith("/saker")) {
                        val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(
                            setOf(1).toJson(),
                            headers = headers,
                        )
                    } else if (request.url.fullPath.contains("/grunnlag/sak")) {
                        val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(
                            Grunnlag.empty().toJson(),
                            headers = headers,
                        )
                    } else if (request.url.fullPath.endsWith("/oppdater-grunnlag")) {
                        respondOk()
                    } else if (request.url.fullPath.endsWith("/opprett-grunnlag")) {
                        respondOk()
                    } else {
                        error(request.url.fullPath)
                    }
                }
            }
            install(ContentNegotiation) {
                register(
                    ContentType.Application.Json,
                    JacksonConverter(objectMapper),
                )
            }
        }

    fun klageHttpClientTest() =
        HttpClient(MockEngine) {
            engine {
                addHandler {
                    respondOk()
                }
            }
            install(ContentNegotiation) {
                register(
                    ContentType.Application.Json,
                    JacksonConverter(objectMapper),
                )
            }
        }

    fun tilbakekrevingHttpClientTest() =
        HttpClient(MockEngine) {
            engine {
                addHandler {
                    respondOk()
                }
            }
            install(ContentNegotiation) {
                register(
                    ContentType.Application.Json,
                    JacksonConverter(objectMapper),
                )
            }
        }

    fun migreringHttpClientTest() =
        HttpClient(MockEngine) {
            engine {
                addHandler {
                    respondOk()
                }
            }
            install(ContentNegotiation) {
                register(
                    ContentType.Application.Json,
                    JacksonConverter(objectMapper),
                )
            }
        }

    fun leaderElection() =
        HttpClient(MockEngine) {
            engine {
                addHandler { req ->
                    if (req.url.fullPath == "electorPath") {
                        respond("me")
                    } else {
                        error(req.url.fullPath)
                    }
                }
            }
        }

    fun resetDatabase() {
        dbExtension.resetDb()
    }

    protected fun afterAll() {
        applicationContext.close()
        server.shutdown()
    }

    private val azureAdStrengtFortroligClaim: String by lazy {
        "5ef775f2-61f8-4283-bf3d-8d03f428aa14"
    }

    private val azureAdAttestantClaim: String by lazy {
        "0af3955f-df85-4eb0-b5b2-45bf2c8aeb9e"
    }

    private val azureAdSaksbehandlerClaim: String by lazy {
        "63f46f74-84a8-4d1c-87a8-78532ab3ae60"
    }

    private val azureAdEgenAnsattClaim: String by lazy {
        "dbe4ad45-320b-4e9a-aaa1-73cca4ee124d"
    }

    protected fun HttpRequestBuilder.addAuthToken(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    protected val tokenSaksbehandler: String by lazy {
        server.issueSaksbehandlerToken(navn = "John Doe", navIdent = "Saksbehandler01", groups = listOf(azureAdSaksbehandlerClaim))
    }

    protected val tokenSaksbehandler2: String by lazy {
        server.issueSaksbehandlerToken(navn = "Jane Doe", navIdent = "Saksbehandler02", groups = listOf(azureAdSaksbehandlerClaim))
    }

    protected val fagsystemTokenEY: String by lazy { server.issueSystembrukerToken() }

    protected val tokenAttestant: String by lazy {
        server.issueSaksbehandlerToken(
            navn = "John Doe",
            navIdent = "Saksbehandler02",
            groups = listOf(azureAdAttestantClaim, azureAdSaksbehandlerClaim),
        )
    }

    protected val tokenSaksbehandlerMedStrengtFortrolig: String by lazy {
        server.issueSaksbehandlerToken(
            navn = "John Doe",
            navIdent = "saksebehandlerstrengtfortrolig",
            groups =
                listOf(
                    azureAdAttestantClaim,
                    azureAdSaksbehandlerClaim,
                    azureAdStrengtFortroligClaim,
                ),
        )
    }

    protected val tokenSaksbehandlerMedEgenAnsattTilgang: String by lazy {
        server.issueSaksbehandlerToken(
            navn = "John Doe",
            navIdent = "saksbehandlerskjermet",
            groups =
                listOf(
                    azureAdSaksbehandlerClaim,
                    azureAdEgenAnsattClaim,
                    azureAdAttestantClaim,
                ),
        )
    }

    protected val systemBruker: String by lazy { server.issueSystembrukerToken() }
}

class GrunnlagKlientTest : GrunnlagKlient {
    override suspend fun finnPersonOpplysning(
        behandlingId: UUID,
        opplysningsType: Opplysningstype,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlagsopplysning<Person> {
        val personopplysning = personOpplysning(doedsdato = LocalDate.parse("2022-01-01"))
        return grunnlagsOpplysningMedPersonopplysning(personopplysning)
    }

    override suspend fun hentPersongalleri(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlagsopplysning<Persongalleri> {
        return Grunnlagsopplysning(
            id = UUID.randomUUID(),
            kilde = Grunnlagsopplysning.Privatperson("fnr", Tidspunkt.now()),
            meta = emptyMap<String, String>().toObjectNode(),
            opplysningType = Opplysningstype.PERSONGALLERI_V1,
            opplysning =
                Persongalleri(
                    "soeker",
                    "innsender",
                    listOf("soesken"),
                    listOf("avdoed"),
                    listOf("gjenlevende"),
                ),
        )
    }
}

class VedtakKlientTest : VedtakKlient {
    override suspend fun lagreVedtakTilbakekreving(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: String,
    ): Long {
        return 123L
    }

    override suspend fun fattVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: String,
    ): Long {
        return 123L
    }

    override suspend fun attesterVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: String,
    ): TilbakekrevingVedtakLagretDto {
        return TilbakekrevingVedtakLagretDto(
            id = 123L,
            fattetAv = "saksbehandler",
            enhet = "enhet",
            dato = LocalDate.now(),
        )
    }

    override suspend fun underkjennVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Long {
        return 123L
    }

    override suspend fun lagreVedtakKlage(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto {
        return mockk<VedtakDto> {
            every { id } returns 123L
        }
    }

    override suspend fun fattVedtakKlage(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto {
        return mockk<VedtakDto> {
            every { id } returns 123L
        }
    }

    override suspend fun attesterVedtakKlage(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto {
        return mockk<VedtakDto> {
            every { id } returns 123L
        }
    }

    override suspend fun underkjennVedtakKlage(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto {
        return mockk<VedtakDto> {
            every { id } returns 123L
        }
    }
}

class BrevApiKlientTest : BrevApiKlient {
    private var brevId = 1L

    override suspend fun opprettKlageInnstillingsbrevISak(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto {
        return opprettetBrevDto(brevId++)
    }

    override suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto {
        return opprettetBrevDto(brevId++)
    }

    override suspend fun ferdigstillBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
    }

    override suspend fun journalfoerBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): JournalpostIdDto {
        return JournalpostIdDto(UUID.randomUUID().toString())
    }

    override suspend fun distribuerBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): BestillingsIdDto {
        return BestillingsIdDto(UUID.randomUUID().toString())
    }

    override suspend fun hentBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto {
        return opprettetBrevDto(brevId)
    }

    override suspend fun slettVedtaksbrev(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
    }

    private fun opprettetBrevDto(brevId: Long) =
        OpprettetBrevDto(
            id = brevId,
            status = BrevStatus.OPPRETTET,
            mottaker =
                Mottaker(
                    navn = "Mottaker mottakersen",
                    foedselsnummer = Mottakerident("19448310410"),
                    orgnummer = null,
                ),
            journalpostId = null,
            bestillingsID = null,
        )
}

class GosysOppgaveKlientTest : GosysOppgaveKlient {
    override suspend fun hentOppgaver(
        enhetsnr: String?,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysOppgaver {
        return GosysOppgaver(0, emptyList())
    }

    override suspend fun hentOppgave(
        id: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave {
        return gosysApiOppgave()
    }

    private fun gosysApiOppgave(): GosysApiOppgave {
        return GosysApiOppgave(
            1,
            2,
            "EYB",
            "-",
            "",
            Tidspunkt.now(),
            "4808",
            null,
            "aktoerId",
            "beskrivelse",
            "NY",
            LocalDate.now(),
        )
    }

    override suspend fun tildelOppgaveTilSaksbehandler(
        oppgaveId: String,
        oppgaveVersjon: Long,
        tildeles: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave {
        return gosysApiOppgave()
    }

    override suspend fun endreFrist(
        oppgaveId: String,
        oppgaveVersjon: Long,
        nyFrist: LocalDate,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave {
        return gosysApiOppgave()
    }
}

class Norg2KlientTest : Norg2Klient {
    override fun hentEnheterForOmraade(
        tema: String,
        omraade: String,
    ): List<ArbeidsFordelingEnhet> {
        return listOf(ArbeidsFordelingEnhet(Enheter.STEINKJER.navn, Enheter.STEINKJER.enhetNr))
    }

    override suspend fun hentNavkontorForOmraade(omraade: String): Navkontor {
        return Navkontor("1202 NAV BERGEN SØR", "4808")
    }
}

class NavAnsattKlientTest : NavAnsattKlient {
    override suspend fun hentEnheterForSaksbehandler(ident: String): List<SaksbehandlerEnhet> {
        return listOf(
            SaksbehandlerEnhet(Enheter.defaultEnhet.enhetNr, Enheter.defaultEnhet.navn),
            SaksbehandlerEnhet(Enheter.STEINKJER.enhetNr, Enheter.STEINKJER.navn),
        )
    }

    override suspend fun hentTemaForSaksbehandler(ident: String): List<SaksbehandlerTema> {
        return listOf(SaksbehandlerTema(SakType.BARNEPENSJON.name), SaksbehandlerTema(SakType.OMSTILLINGSSTOENAD.name))
    }

    override suspend fun hentSaksbehanderNavn(ident: String): SaksbehandlerInfo? {
        return SaksbehandlerInfo("ident", "Max Manus")
    }
}

class PesysKlientTest : PesysKlient {
    override suspend fun hentSaker(fnr: String): List<SakSammendragResponse> {
        return emptyList()
    }
}
