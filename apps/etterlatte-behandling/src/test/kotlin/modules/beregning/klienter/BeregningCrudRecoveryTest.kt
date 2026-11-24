package no.nav.etterlatte.modules.beregning.klienter

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson3.JacksonConverter
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingStatusServiceImpl
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.common.ConnectionAutoclosingImpl
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.modules.beregning.BeregnBarnepensjonService
import no.nav.etterlatte.modules.beregning.Beregning
import no.nav.etterlatte.modules.beregning.BeregningRepository
import no.nav.etterlatte.modules.beregning.BeregningService
import no.nav.etterlatte.modules.beregning.regler.beregning
import no.nav.etterlatte.modules.beregning.regler.bruker
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import tools.jackson.module.kotlin.readValue
import java.net.SocketTimeoutException
import java.util.UUID.randomUUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
internal class BeregningCrudRecoveryTest(
    private val dataSource: DataSource,
) {
    @AfterEach
    fun afterEach() {
        Kontekst.remove()
    }

    @Test
    fun `nytt forsoek gjenoppretter samsvar naar CRUD committer men svaret gaar tapt`() =
        runBlocking<Unit> {
            nyKontekstMedBrukerOgDatabase(Self("beregning-recovery-test"), dataSource)
            val connection = ConnectionAutoclosingImpl(dataSource)
            val behandlingDao =
                BehandlingDao(KommerBarnetTilGodeDao(connection), RevurderingDao(connection), connection)
            val opprettetSak =
                inTransaction {
                    SakSkrivDao(SakendringerDao(connection)).opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)
                }
            val opprettBehandling =
                opprettBehandling(
                    BehandlingType.FØRSTEGANGSBEHANDLING,
                    opprettetSak.id,
                    status = BehandlingStatus.TRYGDETID_OPPDATERT,
                )
            val behandlingId = opprettBehandling.id
            inTransaction { behandlingDao.opprettBehandling(opprettBehandling) }

            val behandling =
                mockk<DetaljertBehandling> {
                    every { id } returns behandlingId
                    every { sak } returns opprettetSak.id
                    every { sakType } returns SakType.BARNEPENSJON
                    every { opphoerFraOgMed } returns null
                }
            val behandlingService =
                mockk<BehandlingService> {
                    every { hentDetaljertBehandling(behandlingId, bruker) } returns behandling
                    every { hentBehandling(behandlingId) } answers { behandlingDao.hentBehandling(behandlingId) }
                    every { registrerBehandlingHendelse(any(), any()) } just runs
                }
            val behandlingStatusService =
                BehandlingStatusServiceImpl(
                    behandlingDao = behandlingDao,
                    behandlingService = behandlingService,
                    behandlingInfoDao = mockk(),
                    oppgaveService = mockk(),
                    grunnlagsendringshendelseService = mockk(),
                    generellBehandlingService = mockk(),
                    aktivitetspliktService = mockk(),
                    saksbehandlerService = mockk(),
                    etteroppgjoerService = mockk(),
                    forbehandlingService = mockk(),
                    grunnlagService = mockk(),
                    vilkaarsvurderingDao = mockk(),
                    featureToggleService = mockk(),
                )
            val foersteBeregning =
                beregning().let {
                    it.copy(
                        behandlingId = behandlingId,
                        type = Beregningstype.BP,
                        beregningsperioder =
                            it.beregningsperioder.map { periode ->
                                periode.copy(beregningsMetode = BeregningsMetode.NASJONAL)
                            },
                        grunnlagMetadata = it.grunnlagMetadata.copy(sakId = opprettetSak.id),
                    )
                }
            val nyBeregning = foersteBeregning.copy(beregningId = randomUUID())
            val beregnBarnepensjonService =
                mockk<BeregnBarnepensjonService> {
                    coEvery { beregn(behandling, bruker, any()) } returnsMany listOf(foersteBeregning, nyBeregning)
                }
            val lagring = BeregningRepository(dataSource)
            var antallSkrivinger = 0
            val engine =
                MockEngine { request ->
                    if (request.method == HttpMethod.Get) {
                        request.url.encodedPath shouldBe "/api/beregning/intern/beregning/overstyring/${opprettetSak.id.sakId}"
                        respond("", HttpStatusCode.NotFound)
                    } else {
                        request.method shouldBe HttpMethod.Put
                        request.url.encodedPath shouldBe "/api/beregning/intern/beregning"
                        val lagret = lagring.lagreEllerOppdaterBeregning(objectMapper.readValue<Beregning>(request.body.toByteArray()))
                        antallSkrivinger++
                        if (antallSkrivinger == 1) throw SocketTimeoutException("Svaret gikk tapt etter commit")
                        respond(lagret.toJson(), HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
                    }
                }
            HttpClient(engine) {
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(objectMapper))
                }
            }.use { client ->
                val service =
                    BeregningService(
                        beregningRepository = BeregningCrudKlient(client, "http://beregning"),
                        behandlingService = behandlingService,
                        beregnBarnepensjonService = beregnBarnepensjonService,
                        beregnOmstillingsstoenadService = mockk(),
                        beregnOverstyrBeregningService = mockk(),
                        behandlingStatusService = behandlingStatusService,
                    )

                assertThrows<SocketTimeoutException> { service.opprettBeregning(behandlingId, bruker) }

                lagring.hent(behandlingId)?.beregningId shouldBe foersteBeregning.beregningId
                inTransaction { behandlingDao.hentBehandling(behandlingId)?.status } shouldBe BehandlingStatus.TRYGDETID_OPPDATERT

                val resultat = service.opprettBeregning(behandlingId, bruker)

                inTransaction { behandlingDao.hentBehandling(behandlingId)?.status } shouldBe BehandlingStatus.BEREGNET
                val lagret = requireNotNull(lagring.hent(behandlingId))
                lagret.beregningId shouldBe nyBeregning.beregningId
                lagret.beregningsperioder.map { it.copy(id = null) } shouldBe nyBeregning.beregningsperioder
                resultat shouldBe lagret
                antallSkrivinger shouldBe 2
            }
        }
}
