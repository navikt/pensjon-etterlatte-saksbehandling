package no.nav.etterlatte.vilkaarsvurdering

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.attachMockContextWithDb
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingServiceImpl
import no.nav.etterlatte.behandling.BehandlingStatusServiceImpl
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarTypeOgUtfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.vilkaarsvurdering.VurdertVilkaarsvurderingResultatDto
import no.nav.etterlatte.mockSaksbehandler
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.vilkaarsvurdering.dao.VilkaarsvurderingRepositoryWrapperDatabase
import no.nav.etterlatte.vilkaarsvurdering.ektedao.DelvilkaarRepository
import no.nav.etterlatte.vilkaarsvurdering.ektedao.VilkaarsvurderingRepository
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VilkaarsvurderingIntegrationTest(
    private val ds: DataSource,
) : BehandlingIntegrationTest() {
    private lateinit var behandlingService: BehandlingService
    private lateinit var behandlingStatus: BehandlingStatusServiceImpl
    private val grunnlagKlient = mockk<GrunnlagKlient>()
    private val grunnlagVersjon = 12L
    private val grunnlagKlientMock =
        mockk<GrunnlagKlient> {
            coEvery { hentPersongalleri(any(), any()) } returns mockPersongalleri()
        }
    private val saksbehandler = mockSaksbehandler("Saksbehandler01")
    private val sbBrukertokenInfo = simpleSaksbehandler()

    private fun mockPersongalleri() =
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
        )

    private lateinit var vilkaarsvurderingServiceImpl: VilkaarsvurderingService

    private fun grunnlagMedVersjon(grunnlagVersjon: Long): Grunnlag {
        val grunnlagMock =
            mockk<Grunnlag> {
                every { metadata } returns
                    mockk {
                        every { versjon } returns grunnlagVersjon
                    }
            }
        return grunnlagMock
    }

    @BeforeAll
    fun before() {
        startServer()
        behandlingService =
            BehandlingServiceImpl(
                behandlingDao = applicationContext.behandlingDao,
                behandlingHendelser = mockk<BehandlingHendelserKafkaProducer>(),
                grunnlagsendringshendelseDao = applicationContext.grunnlagsendringshendelseDao,
                hendelseDao = applicationContext.hendelseDao,
                grunnlagKlient = grunnlagKlientMock,
                behandlingRequestLogger = mockk(),
                kommerBarnetTilGodeDao = mockk(),
                oppgaveService = applicationContext.oppgaveService,
                grunnlagService = applicationContext.grunnlagsService,
                beregningKlient = mockk(),
            )
        behandlingStatus =
            BehandlingStatusServiceImpl(
                behandlingDao = applicationContext.behandlingDao,
                behandlingService = behandlingService,
                behandlingInfoDao = applicationContext.behandlingInfoDao,
                oppgaveService = applicationContext.oppgaveService,
                grunnlagsendringshendelseService = applicationContext.grunnlagsendringshendelseService,
                generellBehandlingService = applicationContext.generellBehandlingService,
            )
        vilkaarsvurderingServiceImpl =
            VilkaarsvurderingService(
                VilkaarsvurderingRepositoryWrapperDatabase(
                    VilkaarsvurderingRepository(ConnectionAutoclosingTest(ds), DelvilkaarRepository()),
                ),
                behandlingService,
                grunnlagKlient,
                behandlingStatus,
            )

        val grunnlagMock = grunnlagMedVersjon(grunnlagVersjon)
        coEvery { grunnlagKlient.hentGrunnlagForBehandling(any(), any()) } returns grunnlagMock
    }

    @AfterEach
    fun afterEach() {
        resetDatabase()
    }

    @AfterAll
    fun after() {
        afterAll()
    }

    private fun opprettSakOgBehandling(saksbehandler: SaksbehandlerMedEnheterOgRoller): UUID {
        nyKontekstMedBrukerOgDatabase(saksbehandler, applicationContext.dataSource)
        val sakid =
            inTransaction { applicationContext.sakSkrivDao.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id }
        val opprettBehandlingMedPersongalleri =
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sakid,
            )
        inTransaction { applicationContext.behandlingDao.opprettBehandling(opprettBehandlingMedPersongalleri) }
        val behandlingId = opprettBehandlingMedPersongalleri.id
        inTransaction {
            applicationContext.behandlingDao.lagreNyttVirkningstidspunkt(
                behandlingId,
                Virkningstidspunkt.create(
                    YearMonth.now(),
                    "begrunnelse",
                    saksbehandler = Grunnlagsopplysning.Saksbehandler.create("ident"),
                ),
            )
        }
        val kilde = Grunnlagsopplysning.Saksbehandler.create("saksbehandler01")
        inTransaction {
            applicationContext.kommerBarnetTilGodeDao.lagreKommerBarnetTilGode(
                KommerBarnetTilgode(
                    JaNei.JA,
                    "",
                    kilde,
                    behandlingId,
                ),
            )
        }
        inTransaction {
            applicationContext.gyldighetsproevingService.lagreGyldighetsproeving(
                behandlingId,
                JaNeiMedBegrunnelse(
                    JaNei.JA,
                    "",
                ),
                kilde,
            )
        }
        return behandlingId
    }

    private val token: String by lazy { mockOAuth2Server.issueSaksbehandlerToken() }

    @Test
    fun `Oppretter og henter vilkaarsvurdering`() {
        testApplication {
            runServer(mockOAuth2Server) {
                attachMockContextWithDb(saksbehandler, applicationContext.dataSource)
                vilkaarsvurdering(vilkaarsvurderingServiceImpl)
            }

            val behandlingId = opprettSakOgBehandling(saksbehandler)

            client.post("/api/vilkaarsvurdering/$behandlingId/opprett") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            val response =
                client.get("/api/vilkaarsvurdering/$behandlingId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val vilkaarsvurdering = objectMapper.readValue(response.bodyAsText(), VilkaarsvurderingDto::class.java)
            val vilkaar = vilkaarsvurdering.vilkaar.first { it.hovedvilkaar.type == VilkaarType.BP_DOEDSFALL_FORELDER_2024 }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(behandlingId, vilkaarsvurdering.behandlingId)
            assertEquals(grunnlagVersjon, vilkaarsvurdering.grunnlagVersjon)
            assertEquals(grunnlagVersjon, vilkaarsvurdering.behandlingGrunnlagVersjon)
            assertFalse(vilkaarsvurdering.isGrunnlagUtdatert())

            assertEquals(VilkaarType.BP_DOEDSFALL_FORELDER_2024, vilkaar.hovedvilkaar.type)
            assertEquals("§ 18-1", vilkaar.hovedvilkaar.lovreferanse.paragraf)
            assertEquals("Dødsfall forelder", vilkaar.hovedvilkaar.tittel)
            assertEquals(
                """
                For å ha rett på ytelsen må en eller begge foreldre være registrer død i folkeregisteret eller hos utenlandske myndigheter.
                """.trimIndent(),
                vilkaar.hovedvilkaar.beskrivelse,
            )
            assertEquals("https://lovdata.no/lov/1997-02-28-19/%C2%A718-1", vilkaar.hovedvilkaar.lovreferanse.lenke)
            assertNull(vilkaar.vurdering)
        }
    }

    fun List<Vilkaar>.hentVilkaarMedHovedvilkaarType(vilkaarType: VilkaarType): Vilkaar? = this.find { it.hovedvilkaar.type == vilkaarType }

    @Test
    fun `Oppdaterer delvilkår`() {
        testApplication {
            runServer(mockOAuth2Server) {
                attachMockContextWithDb(saksbehandler, applicationContext.dataSource)
                vilkaarsvurdering(vilkaarsvurderingServiceImpl)
            }
            val behandlingId = opprettSakOgBehandling(saksbehandler)

            val res =
                client.post("/api/vilkaarsvurdering/$behandlingId/opprett") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val vilkaar = objectMapper.readValue(res.bodyAsText(), VilkaarsvurderingDto::class.java).vilkaar

            val vurdertVilkaarDto =
                VurdertVilkaarDto(
                    vilkaarId = vilkaar.hentVilkaarMedHovedvilkaarType(VilkaarType.BP_DOEDSFALL_FORELDER_2024)?.id!!,
                    hovedvilkaar =
                        VilkaarTypeOgUtfall(
                            VilkaarType.BP_DOEDSFALL_FORELDER_2024,
                            Utfall.OPPFYLT,
                        ),
                    unntaksvilkaar = null,
                    kommentar = "Søker oppfyller vilkåret",
                )

            val oppdatertVilkaarsvurderingResponse =
                client.post("/api/vilkaarsvurdering/$behandlingId") {
                    setBody(vurdertVilkaarDto.toJson())
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val oppdatertVilkaarsvurdering =
                objectMapper
                    .readValue(oppdatertVilkaarsvurderingResponse.bodyAsText(), VilkaarsvurderingDto::class.java)
            val oppdatertVilkaar =
                oppdatertVilkaarsvurdering.vilkaar.find {
                    it.hovedvilkaar.type == vurdertVilkaarDto.hovedvilkaar.type
                }

            assertEquals(HttpStatusCode.OK, oppdatertVilkaarsvurderingResponse.status)
            assertEquals(behandlingId, oppdatertVilkaarsvurdering.behandlingId)
            assertEquals(vurdertVilkaarDto.hovedvilkaar.type, oppdatertVilkaar?.hovedvilkaar?.type)
            assertEquals(vurdertVilkaarDto.hovedvilkaar.resultat, oppdatertVilkaar?.hovedvilkaar?.resultat)
            assertEquals(vurdertVilkaarDto.kommentar, oppdatertVilkaar?.vurdering?.kommentar)
            assertEquals("Saksbehandler01", oppdatertVilkaar?.vurdering?.saksbehandler)
            assertNotNull(oppdatertVilkaar?.vurdering?.tidspunkt)
        }
    }

    @Test
    fun `Oppdaterer status på vilkårsvurdering`() {
        testApplication {
            runServer(mockOAuth2Server) {
                attachMockContextWithDb(saksbehandler, applicationContext.dataSource)
                vilkaarsvurdering(vilkaarsvurderingServiceImpl)
            }

            val behandlingId = opprettSakOgBehandling(saksbehandler)
            runBlocking {
                vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(behandlingId, sbBrukertokenInfo)
                vilkaarsvurderingServiceImpl.oppdaterTotalVurdering(behandlingId, sbBrukertokenInfo, vilkaarsvurderingResultat())
            }
            client.post("/api/vilkaarsvurdering/$behandlingId/oppdater-status") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            val response =
                client.get("/api/vilkaarsvurdering/$behandlingId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val vilkaarsvurdering = objectMapper.readValue(response.bodyAsText(), VilkaarsvurderingDto::class.java)
            val vilkaar = vilkaarsvurdering.vilkaar.first { it.hovedvilkaar.type == VilkaarType.BP_DOEDSFALL_FORELDER_2024 }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(behandlingId, vilkaarsvurdering.behandlingId)
            assertEquals(grunnlagVersjon, vilkaarsvurdering.grunnlagVersjon)
            assertEquals(grunnlagVersjon, vilkaarsvurdering.behandlingGrunnlagVersjon)
            assertFalse(vilkaarsvurdering.isGrunnlagUtdatert())

            assertEquals(VilkaarType.BP_DOEDSFALL_FORELDER_2024, vilkaar.hovedvilkaar.type)
            assertEquals("§ 18-1", vilkaar.hovedvilkaar.lovreferanse.paragraf)
            assertEquals("Dødsfall forelder", vilkaar.hovedvilkaar.tittel)
            assertEquals(
                """
                For å ha rett på ytelsen må en eller begge foreldre være registrer død i folkeregisteret eller hos utenlandske myndigheter.
                """.trimIndent(),
                vilkaar.hovedvilkaar.beskrivelse,
            )
            assertEquals("https://lovdata.no/lov/1997-02-28-19/%C2%A718-1", vilkaar.hovedvilkaar.lovreferanse.lenke)
            assertNull(vilkaar.vurdering)
        }
    }

    @Test
    fun `skal nullstille et vurdert hovedvilkaar fra vilkaarsvurdering`() {
        testApplication {
            runServer(mockOAuth2Server) {
                attachMockContextWithDb(saksbehandler, applicationContext.dataSource)
                vilkaarsvurdering(vilkaarsvurderingServiceImpl)
            }
            val behandlingId = opprettSakOgBehandling(saksbehandler)

            val (vilkaarsvurdering) = vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(behandlingId, sbBrukertokenInfo)

            val vurdertVilkaarDto =
                VurdertVilkaarDto(
                    vilkaarId = vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.BP_DOEDSFALL_FORELDER_2024)?.id!!,
                    hovedvilkaar =
                        VilkaarTypeOgUtfall(
                            type = VilkaarType.BP_DOEDSFALL_FORELDER_2024,
                            resultat = Utfall.OPPFYLT,
                        ),
                    kommentar = "Søker oppfyller vilkåret",
                )

            client.post("/api/vilkaarsvurdering/$behandlingId") {
                setBody(vurdertVilkaarDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val vurdertVilkaar =
                vilkaarsvurderingServiceImpl
                    .hentVilkaarsvurdering(behandlingId)!!
                    .vilkaar
                    .first { it.hovedvilkaar.type == vurdertVilkaarDto.hovedvilkaar.type }

            assertNotNull(vurdertVilkaar)
            assertNotNull(vurdertVilkaar.vurdering)
            assertNotNull(vurdertVilkaar.hovedvilkaar.resultat)

            val response =
                client
                    .delete("/api/vilkaarsvurdering/$behandlingId/${vurdertVilkaarDto.vilkaarId}") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }

            val vurdertVilkaarSlettet =
                vilkaarsvurderingServiceImpl
                    .hentVilkaarsvurdering(behandlingId)!!
                    .vilkaar
                    .first { it.hovedvilkaar.type == vurdertVilkaarDto.hovedvilkaar.type }

            assertEquals(HttpStatusCode.OK, response.status)
            assertNull(vurdertVilkaarSlettet.vurdering)
            assertNull(vurdertVilkaarSlettet.hovedvilkaar.resultat)
            vurdertVilkaarSlettet.unntaksvilkaar.forEach {
                assertNull(it.resultat)
            }
        }
    }

    @Test
    fun `skal sette og nullstille totalresultat for en vilkaarsvurdering`() {
        testApplication {
            runServer(mockOAuth2Server) {
                attachMockContextWithDb(saksbehandler, applicationContext.dataSource)
                vilkaarsvurdering(vilkaarsvurderingServiceImpl)
            }

            val behandlingId = opprettSakOgBehandling(saksbehandler)

            val (_) = vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(behandlingId, sbBrukertokenInfo)

            val resultat =
                VurdertVilkaarsvurderingResultatDto(
                    resultat = VilkaarsvurderingUtfall.OPPFYLT,
                    kommentar = "Søker oppfyller vurderingen",
                )

            val oppdatertVilkaarsvurderingResponse =
                client.post("/api/vilkaarsvurdering/resultat/$behandlingId") {
                    setBody(resultat.toJson())
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val oppdatertVilkaarsvurdering =
                objectMapper
                    .readValue(oppdatertVilkaarsvurderingResponse.bodyAsText(), VilkaarsvurderingDto::class.java)
            assertEquals(HttpStatusCode.OK, oppdatertVilkaarsvurderingResponse.status)
            assertEquals(behandlingId, oppdatertVilkaarsvurdering.behandlingId)
            assertEquals(resultat.resultat, oppdatertVilkaarsvurdering?.resultat?.utfall)
            assertEquals(resultat.kommentar, oppdatertVilkaarsvurdering?.resultat?.kommentar)
            assertEquals("Saksbehandler01", oppdatertVilkaarsvurdering?.resultat?.saksbehandler)
            assertNotNull(oppdatertVilkaarsvurdering?.resultat?.tidspunkt)

            val sletteResponse =
                client.delete("/api/vilkaarsvurdering/resultat/$behandlingId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val slettetVilkaarsvurdering =
                objectMapper
                    .readValue(sletteResponse.bodyAsText(), VilkaarsvurderingDto::class.java)
            assertEquals(HttpStatusCode.OK, sletteResponse.status)
            assertEquals(behandlingId, slettetVilkaarsvurdering.behandlingId)
            assertEquals(null, slettetVilkaarsvurdering?.resultat)
        }
    }

    @Test
    fun `Skal slette eksisterende vilkaarsvurdering`() {
        testApplication {
            runServer(mockOAuth2Server) {
                attachMockContextWithDb(saksbehandler, applicationContext.dataSource)
                vilkaarsvurdering(vilkaarsvurderingServiceImpl)
            }

            val behandlingId = opprettSakOgBehandling(saksbehandler)

            val (_) = vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(behandlingId, sbBrukertokenInfo)

            val response =
                client.delete("/api/vilkaarsvurdering/$behandlingId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("", response.bodyAsText())
        }
    }

    private fun vilkaarsvurderingResultat(utfall: VilkaarsvurderingUtfall = VilkaarsvurderingUtfall.OPPFYLT) =
        VilkaarsvurderingResultat(
            utfall = utfall,
            kommentar = "Kommentar",
            tidspunkt = LocalDateTime.now(),
            saksbehandler = "Saksbehandler",
        )
}
