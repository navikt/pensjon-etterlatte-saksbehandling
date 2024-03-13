package no.nav.etterlatte

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.omregning.OpprettOmregningResponse
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OmregningIntegrationTest : BehandlingIntegrationTest() {
    private lateinit var user: SaksbehandlerMedEnheterOgRoller

    @BeforeAll
    fun start() {
        startServer()
        user = mockk<SaksbehandlerMedEnheterOgRoller>()
        val saksbehandlerMedRoller =
            mockk<SaksbehandlerMedRoller> {
                every { harRolleStrengtFortrolig() } returns false
                every { harRolleEgenAnsatt() } returns false
            }
        every { user.saksbehandlerMedRoller } returns saksbehandlerMedRoller
        every { user.name() } returns "User"
        every { user.enheter() } returns listOf(Enheter.defaultEnhet.enhetNr)
        nyKontekstMedBrukerOgDatabase(user, applicationContext.dataSource)
    }

    @AfterAll
    fun shutdown() = afterAll()

    private var sakId: Long = 0L

    fun opprettSakMedFoerstegangsbehandling(fnr: String): Pair<Sak, Foerstegangsbehandling?> {
        return inTransaction {
            val sak =
                applicationContext.sakDao.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)

            val behandling =
                applicationContext.behandlingFactory
                    .opprettBehandling(
                        sak.id,
                        persongalleri(),
                        LocalDateTime.now().toString(),
                        Vedtaksloesning.GJENNY,
                    )?.behandling
            Pair(sak, behandling as Foerstegangsbehandling)
        }
    }

    private fun iverksettFoerstegangsbehandling(
        sak: Sak,
        behandling: Foerstegangsbehandling?,
    ) {
        val kommerBarnetTilgode =
            KommerBarnetTilgode(
                JaNei.JA,
                "",
                Grunnlagsopplysning.Saksbehandler.create("A0"),
                behandling!!.id,
            )
        inTransaction {
            applicationContext.kommerBarnetTilGodeService.lagreKommerBarnetTilgode(
                kommerBarnetTilgode,
            )
        }

        sakId = sak.id

        val virkningstidspunkt = virkningstidspunktVurdering()

        val iverksattBehandling =
            behandling.copy(kommerBarnetTilgode = kommerBarnetTilgode)
                .oppdaterGyldighetsproeving(gyldighetsresultatVurdering())
                .oppdaterVirkningstidspunkt(virkningstidspunkt)
                .tilVilkaarsvurdert()
                .tilTrygdetidOppdatert()
                .tilBeregnet()
                .tilFattetVedtak()
                .tilAttestert()
                .tilIverksatt()

        inTransaction { applicationContext.behandlingDao.lagreStatus(iverksattBehandling) }
    }

    @Test
    fun `kan opprette omregning paa sak som har iverksatt foerstegangsbehandling`() {
        testApplication {
            val client =
                runServerWithModule(server) {
                    module(applicationContext)
                }

            for (i in 1..100) {
                val (sak, behandling) = opprettSakMedFoerstegangsbehandling(i.toString())
                iverksettFoerstegangsbehandling(sak, behandling)
                val (omregning) =
                    client.post("/omregning") {
                        addAuthToken(systemBruker)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(
                            Omregningshendelse(
                                sakId,
                                LocalDate.now(),
                                null,
                                Prosesstype.AUTOMATISK,
                            ),
                        )
                    }.let {
                        Assertions.assertEquals(HttpStatusCode.OK, it.status)
                        it.body<OpprettOmregningResponse>()
                    }

                client.get("/behandlinger/$omregning") {
                    addAuthToken(tokenSaksbehandler)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }.let {
                    Assertions.assertEquals(HttpStatusCode.OK, it.status)
                    it.body<DetaljertBehandling>().also { behandling ->
                        Assertions.assertEquals(omregning, behandling.id)
                        Assertions.assertEquals(sakId, behandling.sak)
                    }
                }
            }
        }
    }

    @Test
    fun `omregning feiler hvis det ikke finnes noen iverksatt behandling fra foer`() {
        val (sak, _) = opprettSakMedFoerstegangsbehandling("234")
        testApplication {
            val client =
                runServerWithModule(server) {
                    module(applicationContext)
                }

            client.post("/omregning") {
                addAuthToken(systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Omregningshendelse(sak.id, LocalDate.now(), null, Prosesstype.AUTOMATISK))
            }.also {
                Assertions.assertEquals(HttpStatusCode.InternalServerError, it.status)
            }
        }
    }
}
