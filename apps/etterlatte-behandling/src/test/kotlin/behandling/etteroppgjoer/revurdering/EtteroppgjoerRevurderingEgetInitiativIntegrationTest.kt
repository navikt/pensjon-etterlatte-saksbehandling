package no.nav.etterlatte.behandling.etteroppgjoer.revurdering

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.User
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.etteroppgjoer.Etteroppgjoer
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerDao
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerDataService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingDao
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.TrygdetidKlient
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.revurdering
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Month.JANUARY
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

/**
 * Integrasjonstest for omgjøring av etteroppgjør på eget initiativ.
 *
 * Testen kjører mot en ekte Postgres (Testcontainers) via [DatabaseExtension] og bruker en ekte
 * [EtteroppgjoerService] + [EtteroppgjoerDao], slik at statusovergangen til [EtteroppgjoerStatus.OMGJOERING]
 * faktisk persisteres og leses tilbake fra databasen. Den tunge opprettelsen av selve revurderingen
 * ([EtteroppgjoerRevurderingService.opprettEtteroppgjoerRevurdering]) er stubbet, siden den er dekket av egne
 * tester og krever et fullt sett av nedstrøms-klienter (beregning, trygdetid, vedtak).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class EtteroppgjoerRevurderingEgetInitiativIntegrationTest(
    val dataSource: DataSource,
) {
    private val inntektsaar = 2024

    private lateinit var sakSkrivDao: SakSkrivDao
    private lateinit var etteroppgjoerDao: EtteroppgjoerDao
    private lateinit var etteroppgjoerService: EtteroppgjoerService
    private lateinit var behandlingService: BehandlingService
    private lateinit var etteroppgjoerForbehandlingService: EtteroppgjoerForbehandlingService
    private lateinit var service: EtteroppgjoerRevurderingService

    private lateinit var sak: Sak

    @BeforeAll
    fun setup() {
        sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)))
        etteroppgjoerDao = EtteroppgjoerDao(ConnectionAutoclosingTest(dataSource))

        nyKontekstMedBrukerOgDatabase(
            mockk<User>().also { every { it.name() } returns this::class.java.simpleName },
            dataSource,
        )
    }

    @BeforeEach
    fun resetOgWiring() {
        dataSource.connection.use {
            it.prepareStatement("""TRUNCATE TABLE etteroppgjoer CASCADE""").executeUpdate()
            it.prepareStatement("""TRUNCATE TABLE sak CASCADE""").executeUpdate()
        }

        sak =
            sakSkrivDao.opprettSak(
                fnr = "bruker1",
                type = SakType.OMSTILLINGSSTOENAD,
                enhet = Enheter.PORSGRUNN.enhetNr,
            )

        behandlingService = mockk(relaxed = true)
        etteroppgjoerForbehandlingService = mockk(relaxed = true)

        etteroppgjoerService =
            EtteroppgjoerService(
                dao = etteroppgjoerDao,
                vedtakInternalService = mockk(relaxed = true),
                forbehandlingDao = mockk<EtteroppgjoerForbehandlingDao>(relaxed = true),
                behandlingService = behandlingService,
                beregningKlient = mockk(relaxed = true),
                etteroppgjoerOppgaveService = mockk(relaxed = true),
                sigrunKlient = mockk(relaxed = true),
                inntektskomponentService = mockk(relaxed = true),
                hendelseDao = mockk(relaxed = true),
            )

        service =
            spyk(
                EtteroppgjoerRevurderingService(
                    behandlingService = behandlingService,
                    etteroppgjoerService = etteroppgjoerService,
                    etteroppgjoerForbehandlingService = etteroppgjoerForbehandlingService,
                    grunnlagService = mockk<GrunnlagService>(),
                    revurderingService = mockk<RevurderingService>(),
                    vilkaarsvurderingService = mockk<VilkaarsvurderingService>(),
                    trygdetidKlient = mockk<TrygdetidKlient>(),
                    beregningKlient = mockk<BeregningKlient>(),
                    etteroppgjoerDataService = mockk<EtteroppgjoerDataService>(),
                ),
            )
    }

    @Test
    fun `omgjoering paa eget initiativ setter status OMGJOERING i databasen og oppretter revurdering`() {
        val ferdigstiltForbehandlingId = UUID.randomUUID()
        etteroppgjoerDao.lagreEtteroppgjoer(
            Etteroppgjoer(
                sakId = sak.id,
                inntektsaar = inntektsaar,
                status = EtteroppgjoerStatus.FERDIGSTILT,
                sisteFerdigstilteForbehandling = ferdigstiltForbehandlingId,
            ),
        )

        val revurderingForbehandlingId = UUID.randomUUID()
        val iverksattEtteroppgjoerRevurdering =
            revurdering(
                status = BehandlingStatus.IVERKSATT,
                revurderingAarsak = Revurderingaarsak.ETTEROPPGJOER,
                relatertBehandlingId = revurderingForbehandlingId,
                sistEndret = YearMonth.of(inntektsaar + 1, JANUARY).atDay(20).atStartOfDay(),
                opprinnelse = BehandlingOpprinnelse.SAKSBEHANDLER,
                sakId = sak.id,
                sakType = SakType.OMSTILLINGSSTOENAD,
            )
        every { behandlingService.hentBehandlingerForSak(sak.id) } returns listOf(iverksattEtteroppgjoerRevurdering)
        every { etteroppgjoerForbehandlingService.hentForbehandling(revurderingForbehandlingId) } returns
            mockk<EtteroppgjoerForbehandling> {
                every { id } returns revurderingForbehandlingId
                every { aar } returns inntektsaar
            }

        val nyRevurdering = mockk<Revurdering>()
        val opprinnelseSlot = slot<BehandlingOpprinnelse>()
        val omgjoerForbehandlingIdSlot = slot<UUID>()
        every {
            service.opprettEtteroppgjoerRevurdering(
                sakId = sak.id,
                inntektsaar = inntektsaar,
                opprinnelse = capture(opprinnelseSlot),
                omgjoerForbehandlingId = capture(omgjoerForbehandlingIdSlot),
                brukerTokenInfo = any(),
            )
        } returns nyRevurdering

        val resultat =
            service.omgjoerEtteroppgjoerRevurderingEgetInitiativ(sak.id, inntektsaar, simpleSaksbehandler())

        resultat shouldBe nyRevurdering
        opprinnelseSlot.captured shouldBe BehandlingOpprinnelse.SAKSBEHANDLER
        omgjoerForbehandlingIdSlot.captured shouldBe ferdigstiltForbehandlingId

        // Statusovergangen skal være persistert i databasen
        val lagretEtteroppgjoer = etteroppgjoerDao.hentEtteroppgjoerForInntektsaar(sak.id, inntektsaar)
        lagretEtteroppgjoer!!.status shouldBe EtteroppgjoerStatus.OMGJOERING
    }

    @Test
    fun `omgjoering paa eget initiativ feiler naar etteroppgjoeret ikke er ferdigstilt og lar status staa uendret`() {
        etteroppgjoerDao.lagreEtteroppgjoer(
            Etteroppgjoer(
                sakId = sak.id,
                inntektsaar = inntektsaar,
                status = EtteroppgjoerStatus.VENTER_PAA_SVAR,
                sisteFerdigstilteForbehandling = UUID.randomUUID(),
            ),
        )

        assertThrows<IkkeTillattException> {
            service.omgjoerEtteroppgjoerRevurderingEgetInitiativ(sak.id, inntektsaar, simpleSaksbehandler())
        }

        val lagretEtteroppgjoer = etteroppgjoerDao.hentEtteroppgjoerForInntektsaar(sak.id, inntektsaar)
        lagretEtteroppgjoer!!.status shouldBe EtteroppgjoerStatus.VENTER_PAA_SVAR

        verify(exactly = 0) {
            service.opprettEtteroppgjoerRevurdering(
                sakId = any(),
                inntektsaar = any(),
                opprinnelse = any(),
                omgjoerForbehandlingId = any(),
                brukerTokenInfo = any(),
            )
        }
    }
}
