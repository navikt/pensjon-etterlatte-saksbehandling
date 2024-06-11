package no.nav.etterlatte

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.FastsettVirkningstidspunktResponse
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klage.InnkommendeKlageDto
import no.nav.etterlatte.behandling.klage.VurdereFormkravDto
import no.nav.etterlatte.behandling.klage.VurdertUtfallDto
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.funksjonsbrytere.FellesFeatureToggle
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingsBehov
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Formkrav
import no.nav.etterlatte.libs.common.behandling.GrunnForOmgjoering
import no.nav.etterlatte.libs.common.behandling.InitieltUtfallMedBegrunnelseDto
import no.nav.etterlatte.libs.common.behandling.InnstillingTilKabalUtenBrev
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.KabalHjemmel
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageOmgjoering
import no.nav.etterlatte.libs.common.behandling.KlageUtfall
import no.nav.etterlatte.libs.common.behandling.KlageUtfallUtenBrev
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.behandling.VedtaketKlagenGjelder
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.klage.KlageHendelseType
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.SakIdOgReferanse
import no.nav.etterlatte.libs.common.oppgave.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.oppgave.VedtakEndringDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.DoedshendelsePdl
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.sak.UtlandstilknytningRequest
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.lang.Thread.sleep
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VerdikjedeTest : BehandlingIntegrationTest() {
    @BeforeAll
    fun start() {
        startServer(
            // En enkel mock som skrur på alle toggles for verdikjedetesten
            // overstyr egne ønskede toggles her om behovet oppstår
            featureToggleService =
                mockk<FeatureToggleService> {
                    every { isEnabled(any(), any()) } returns true
                },
        )
    }

    @AfterAll
    fun shutdown() = afterAll()

    @Test
    fun verdikjedetest() {
        val fnr = SOEKER_FOEDSELSNUMMER.value
        var behandlingOpprettet: UUID? = null

        testApplication {
            val client =
                runServerWithModule(server) {
                    module(applicationContext)
                }

            assertTrue(applicationContext.featureToggleService.isEnabled(FellesFeatureToggle.NoOperationToggle, false))

            client
                .get("/saker/1") {
                    addAuthToken(tokenSaksbehandler)
                }.apply {
                    assertEquals(HttpStatusCode.NotFound, status)
                }
            val sak: Sak =
                client
                    .post("/personer/saker/${SakType.BARNEPENSJON}") {
                        addAuthToken(tokenSaksbehandler)
                        contentType(ContentType.Application.Json)
                        setBody(FoedselsnummerDTO(fnr))
                    }.apply {
                        assertEquals(HttpStatusCode.OK, status)
                    }.body()

            client
                .get("/saker/${sak.id}") {
                    addAuthToken(tokenSaksbehandler)
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                    val lestSak: Sak = it.body()
                    assertEquals(fnr, lestSak.ident)
                    assertEquals(SakType.BARNEPENSJON, lestSak.sakType)
                }

            val behandlingId =
                client
                    .post("/behandlinger/opprettbehandling") {
                        addAuthToken(this@VerdikjedeTest.systemBruker)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(
                            BehandlingsBehov(
                                1,
                                Persongalleri("søker", "innsender", emptyList(), emptyList(), emptyList()),
                                Tidspunkt.now().toLocalDatetimeUTC().toString(),
                            ),
                        )
                    }.let {
                        assertEquals(HttpStatusCode.OK, it.status)
                        UUID.fromString(it.body())
                    }
            behandlingOpprettet = behandlingId

            client
                .get("/behandlinger/$behandlingId") {
                    addAuthToken(tokenSaksbehandler)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                    it.body<DetaljertBehandling>()
                }

            val oppgaver: List<OppgaveIntern> =
                client
                    .get("/api/oppgaver") {
                        addAuthToken(tokenSaksbehandler)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }.also {
                        assertEquals(HttpStatusCode.OK, it.status)
                    }.body()

            val oppgaverforbehandling = oppgaver.filter { it.referanse == behandlingId.toString() }
            client
                .post("/api/oppgaver/${oppgaverforbehandling[0].id}/tildel-saksbehandler/") {
                    addAuthToken(tokenSaksbehandler)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(SaksbehandlerEndringDto("Saksbehandler01"))
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .post("/behandlinger/$behandlingId/gyldigfremsatt") {
                    addAuthToken(tokenSaksbehandler)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        GyldighetsResultat(
                            VurderingsResultat.OPPFYLT,
                            listOf(
                                VurdertGyldighet(
                                    GyldighetsTyper.INNSENDER_ER_FORELDER,
                                    VurderingsResultat.OPPFYLT,
                                    "innsenderFnr",
                                ),
                            ),
                            Tidspunkt.now().toLocalDatetimeUTC(),
                        ),
                    )
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .get("/behandlinger/$behandlingId") {
                    addAuthToken(tokenSaksbehandler)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                    val behandling: DetaljertBehandling = it.body()
                    assertNotNull(behandling.id)
                    assertEquals("soeker", behandling.soeker)
                }

            client
                .post("/api/behandling/$behandlingId/utlandstilknytning") {
                    addAuthToken(tokenSaksbehandler)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(UtlandstilknytningRequest(UtlandstilknytningType.NASJONAL, "begrunnelse"))
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .post("/api/behandling/$behandlingId/virkningstidspunkt") {
                    addAuthToken(tokenSaksbehandler)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        mapOf("dato" to "2022-02-01T01:00:00.000Z", "begrunnelse" to "En begrunnelse"),
                    )
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                    val expected =
                        FastsettVirkningstidspunktResponse(
                            YearMonth.of(2022, 2),
                            Grunnlagsopplysning.Saksbehandler.create("Saksbehandler01"),
                            "En begrunnelse",
                            null,
                        )
                    assertEquals(expected.dato, it.body<FastsettVirkningstidspunktResponse>().dato)
                    assertEquals(expected.kilde.ident, it.body<FastsettVirkningstidspunktResponse>().kilde.ident)
                    assertEquals(expected.begrunnelse, it.body<FastsettVirkningstidspunktResponse>().begrunnelse)
                }

            client
                .post("/api/behandling/$behandlingId/kommerbarnettilgode") {
                    addAuthToken(tokenSaksbehandler)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(JaNeiMedBegrunnelse(JaNei.JA, "begrunnelse"))
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }

            val dataSource = applicationContext.dataSource

            client
                .get("/behandlinger/$behandlingId/vilkaarsvurder") {
                    addAuthToken(tokenSaksbehandler)
                }.also {
                    val actual =
                        BehandlingDao(
                            KommerBarnetTilGodeDao(ConnectionAutoclosingTest(dataSource)),
                            RevurderingDao(ConnectionAutoclosingTest(dataSource)),
                            ConnectionAutoclosingTest(dataSource),
                        ).hentBehandling(behandlingId)!!
                    assertEquals(BehandlingStatus.OPPRETTET, actual.status)

                    assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .post("/behandlinger/$behandlingId/vilkaarsvurder") {
                    addAuthToken(tokenSaksbehandler)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody("{}")
                }.also {
                    val actual =
                        BehandlingDao(
                            KommerBarnetTilGodeDao(ConnectionAutoclosingTest(dataSource)),
                            RevurderingDao(ConnectionAutoclosingTest(dataSource)),
                            ConnectionAutoclosingTest(dataSource),
                        ).hentBehandling(behandlingId)!!
                    assertEquals(BehandlingStatus.VILKAARSVURDERT, actual.status)

                    assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .post("/behandlinger/$behandlingId/oppdaterTrygdetid") {
                    addAuthToken(tokenSaksbehandler)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody("{}")
                }.also {
                    val actual =
                        BehandlingDao(
                            KommerBarnetTilGodeDao(ConnectionAutoclosingTest(dataSource)),
                            RevurderingDao(ConnectionAutoclosingTest(dataSource)),
                            ConnectionAutoclosingTest(dataSource),
                        ).hentBehandling(behandlingId)!!
                    assertEquals(BehandlingStatus.TRYGDETID_OPPDATERT, actual.status)

                    assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .post("/behandlinger/$behandlingId/beregn") {
                    addAuthToken(tokenSaksbehandler)
                }.also {
                    val actual =
                        BehandlingDao(
                            KommerBarnetTilGodeDao(ConnectionAutoclosingTest(dataSource)),
                            RevurderingDao(ConnectionAutoclosingTest(dataSource)),
                            ConnectionAutoclosingTest(dataSource),
                        ).hentBehandling(behandlingId)!!
                    assertEquals(BehandlingStatus.BEREGNET, actual.status)

                    assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .post("/fattvedtak") {
                    addAuthToken(tokenSaksbehandler)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        VedtakEndringDTO(
                            sakIdOgReferanse =
                                SakIdOgReferanse(
                                    sakId = sak.id,
                                    referanse = behandlingId.toString(),
                                ),
                            vedtakHendelse = VedtakHendelse(123L, Tidspunkt.now(), "Saksbehandler01", null),
                            vedtakType = VedtakType.INNVILGELSE,
                        ),
                    )
                }.also {
                    val actual =
                        BehandlingDao(
                            KommerBarnetTilGodeDao(ConnectionAutoclosingTest(dataSource)),
                            RevurderingDao(ConnectionAutoclosingTest(dataSource)),
                            ConnectionAutoclosingTest(dataSource),
                        ).hentBehandling(behandlingId)!!
                    assertEquals(BehandlingStatus.FATTET_VEDTAK, actual.status)

                    assertEquals(HttpStatusCode.OK, it.status)
                }

            val oppgaveroppgaveliste: List<OppgaveIntern> =
                client
                    .get("/api/oppgaver") {
                        addAuthToken(tokenAttestant)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }.also {
                        assertEquals(HttpStatusCode.OK, it.status)
                    }.body()
            val oppgaverforattestant = oppgaveroppgaveliste.filter { it.referanse == behandlingId.toString() }
            val saksbehandler02 = "Saksbehandler02"
            client
                .post("/api/oppgaver/${oppgaverforattestant[0].id}/tildel-saksbehandler") {
                    addAuthToken(tokenAttestant)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(SaksbehandlerEndringDto(saksbehandler02))
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .post("/attestervedtak") {
                    addAuthToken(tokenAttestant)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        VedtakEndringDTO(
                            sakIdOgReferanse =
                                SakIdOgReferanse(
                                    sakId = sak.id,
                                    referanse = behandlingId.toString(),
                                ),
                            vedtakHendelse = VedtakHendelse(123L, Tidspunkt.now(), saksbehandler02, null, null),
                            vedtakType = VedtakType.INNVILGELSE,
                        ),
                    )
                }.also {
                    val actual =
                        BehandlingDao(
                            KommerBarnetTilGodeDao(ConnectionAutoclosingTest(dataSource)),
                            RevurderingDao(ConnectionAutoclosingTest(dataSource)),
                            ConnectionAutoclosingTest(dataSource),
                        ).hentBehandling(behandlingId)!!
                    assertEquals(BehandlingStatus.ATTESTERT, actual.status)

                    assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .post("/behandlinger/$behandlingId/iverksett") {
                    addAuthToken(tokenSaksbehandler)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        VedtakHendelse(
                            12L,
                            Tidspunkt.now(),
                            null,
                            null,
                            null,
                        ),
                    )
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .get("/behandlinger/$behandlingId") {
                    addAuthToken(tokenSaksbehandler)
                }.also {
                    val behandling = it.body<DetaljertBehandling>()

                    assertEquals(HttpStatusCode.OK, it.status)
                    assertEquals(BehandlingStatus.IVERKSATT, behandling.status)
                }

            // KLAGE
            // Sniker seg inn her siden vi trenger en sak med en iverksatt behandling
            val klage: Klage =
                client
                    .post("/api/klage/opprett/${sak.id}") {
                        addAuthToken(tokenSaksbehandler)
                        contentType(ContentType.Application.Json)
                        setBody(
                            InnkommendeKlageDto(
                                mottattDato = OffsetDateTime.now().toString(),
                                journalpostId = "123546",
                                innsender = "en innsender",
                            ),
                        )
                    }.body()
            val medOppdatertFormkrav: Klage =
                client
                    .put("/api/klage/${klage.id}/formkrav") {
                        addAuthToken(tokenSaksbehandler)
                        contentType(ContentType.Application.Json)
                        setBody(
                            VurdereFormkravDto(
                                Formkrav(
                                    vedtaketKlagenGjelder =
                                        VedtaketKlagenGjelder(
                                            123L.toString(),
                                            UUID.randomUUID().toString(),
                                            datoAttestert = null,
                                            vedtakType = VedtakType.INNVILGELSE,
                                        ),
                                    erKlagerPartISaken = JaNei.JA,
                                    erKlagenSignert = JaNei.JA,
                                    gjelderKlagenNoeKonkretIVedtaket = JaNei.JA,
                                    erKlagenFramsattInnenFrist = JaNei.JA,
                                    erFormkraveneOppfylt = JaNei.JA,
                                ),
                            ),
                        )
                    }.body()
            client.put("/api/klage/${medOppdatertFormkrav.id}/initieltutfall") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
                setBody(
                    InitieltUtfallMedBegrunnelseDto(KlageUtfall.OMGJOERING, "En begrunnelse"),
                )
            }
            val medUtfall =
                client
                    .put("/api/klage/${medOppdatertFormkrav.id}/utfall") {
                        addAuthToken(tokenSaksbehandler)
                        contentType(ContentType.Application.Json)
                        setBody(
                            VurdertUtfallDto(
                                KlageUtfallUtenBrev.DelvisOmgjoering(
                                    omgjoering =
                                        KlageOmgjoering(
                                            grunnForOmgjoering = GrunnForOmgjoering.FEIL_LOVANVENDELSE,
                                            begrunnelse = "Vi skjønte ikke",
                                        ),
                                    innstilling =
                                        InnstillingTilKabalUtenBrev(
                                            lovhjemmel = KabalHjemmel.FTRL_3_5_TRYGDETID.name,
                                            internKommentar = "En tekst",
                                            innstillingTekst = "En ekstra tekst",
                                        ),
                                ),
                            ),
                        )
                    }.body<Klage>()

            val oppgaverKlage: List<OppgaveIntern> =
                client
                    .get("/api/oppgaver") {
                        addAuthToken(tokenSaksbehandler)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }.also {
                        assertEquals(HttpStatusCode.OK, it.status)
                    }.body()
            val oppgaverforbehandlingKlage = oppgaverKlage.filter { it.referanse == klage.id.toString() }
            client
                .post("/api/oppgaver/${oppgaverforbehandlingKlage[0].id}/tildel-saksbehandler/") {
                    addAuthToken(tokenSaksbehandler)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(SaksbehandlerEndringDto("Saksbehandler01"))
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }

            val ferdigstiltKlage =
                client
                    .post("/api/klage/${medUtfall.id}/ferdigstill") {
                        addAuthToken(tokenSaksbehandler)
                    }.body<Klage>()

            assertNotNull(ferdigstiltKlage.resultat?.opprettetOppgaveOmgjoeringId)
            assertNotNull(ferdigstiltKlage.resultat?.sendtInnstillingsbrev)

            client
                .post("/grunnlagsendringshendelse/doedshendelse") {
                    addAuthToken(this@VerdikjedeTest.systemBruker)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(DoedshendelsePdl("1", Endringstype.OPPRETTET, fnr, LocalDate.now()))
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .post("/grunnlagsendringshendelse/doedshendelse") {
                    addAuthToken(this@VerdikjedeTest.systemBruker)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(DoedshendelsePdl("1", Endringstype.OPPRETTET, fnr, LocalDate.now()))
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .post("/grunnlagsendringshendelse/utflyttingshendelse") {
                    addAuthToken(this@VerdikjedeTest.systemBruker)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        UtflyttingsHendelse(
                            hendelseId = "1",
                            endringstype = Endringstype.OPPRETTET,
                            fnr = fnr,
                            tilflyttingsLand = null,
                            tilflyttingsstedIUtlandet = null,
                            utflyttingsdato = null,
                        ),
                    )
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .post("/grunnlagsendringshendelse/forelderbarnrelasjonhendelse") {
                    addAuthToken(this@VerdikjedeTest.systemBruker)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        ForelderBarnRelasjonHendelse(
                            hendelseId = "1",
                            endringstype = Endringstype.OPPRETTET,
                            fnr = fnr,
                            relatertPersonsIdent = null,
                            relatertPersonsRolle = "",
                            minRolleForPerson = "",
                            relatertPersonUtenFolkeregisteridentifikator = null,
                        ),
                    )
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }

            val behandlingIdNyFoerstegangsbehandling =
                client
                    .post("/behandlinger/opprettbehandling") {
                        addAuthToken(this@VerdikjedeTest.systemBruker)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(
                            BehandlingsBehov(
                                1,
                                Persongalleri(fnr, "innsender", emptyList(), emptyList(), emptyList()),
                                Tidspunkt.now().toLocalDatetimeUTC().toString(),
                            ),
                        )
                    }.let {
                        assertEquals(HttpStatusCode.OK, it.status)
                        UUID.fromString(it.body())
                    }

            client
                .post("/api/behandling/$behandlingIdNyFoerstegangsbehandling/avbryt") {
                    addAuthToken(systemBruker)
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .post("/grunnlagsendringshendelse/adressebeskyttelse") {
                    addAuthToken(this@VerdikjedeTest.systemBruker)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        Adressebeskyttelse(
                            hendelseId = "1",
                            endringstype = Endringstype.OPPRETTET,
                            fnr = fnr,
                            adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                        ),
                    )
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .get("/behandlinger/$behandlingId") {
                    addAuthToken(tokenSaksbehandler)
                }.also {
                    assertEquals(HttpStatusCode.NotFound, it.status) // 404 pga adressebeskyttelse
                }
        }

        kotlin.runCatching { sleep(3000) }
        assertNotNull(behandlingOpprettet)
        val rapid = applicationContext.rapid as TestProdusent
        assertEquals(5, rapid.publiserteMeldinger.size)
        assertEquals(
            "BEHANDLING:OPPRETTET",
            objectMapper.readTree(rapid.publiserteMeldinger.first().verdi)[EVENT_NAME_KEY].textValue(),
        )
        assertEquals(
            "KLAGE:${KlageHendelseType.OPPRETTET}",
            objectMapper.readTree(rapid.publiserteMeldinger[1].verdi)[EVENT_NAME_KEY].textValue(),
        )
        assertEquals(
            "KLAGE:${KlageHendelseType.FERDIGSTILT}",
            objectMapper.readTree(rapid.publiserteMeldinger[2].verdi)[EVENT_NAME_KEY].textValue(),
        )
        assertEquals(
            "BEHANDLING:OPPRETTET",
            objectMapper.readTree(rapid.publiserteMeldinger[3].verdi)[EVENT_NAME_KEY].textValue(),
        )
        assertEquals(
            "BEHANDLING:AVBRUTT",
            objectMapper.readTree(rapid.publiserteMeldinger[4].verdi)[EVENT_NAME_KEY].textValue(),
        )
        HendelseDao(
            ConnectionAutoclosingTest(applicationContext.dataSource),
        ).finnHendelserIBehandling(behandlingOpprettet!!).also { println(it) }
    }
}
