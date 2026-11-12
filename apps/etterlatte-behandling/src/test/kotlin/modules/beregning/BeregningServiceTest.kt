package no.nav.etterlatte.modules.beregning

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.OverstyrBeregningDTO
import no.nav.etterlatte.libs.common.beregning.OverstyrtBeregningKategori
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.modules.beregning.BeregningTestKontekst
import no.nav.etterlatte.modules.beregning.regler.bruker
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabaseContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.sql.Connection
import java.time.YearMonth
import java.util.UUID.randomUUID

@ExtendWith(BeregningTestKontekst::class)
internal class BeregningServiceTest {
    private val behandlingStatusService = mockk<BehandlingStatusService>()
    private val behandlingService = mockk<BehandlingService>()
    private val beregningRepository = mockk<BeregningRepository>()
    private val beregnBarnepensjonService = mockk<BeregnBarnepensjonService>()
    private val beregnOmstillingsstoenadService = mockk<BeregnOmstillingsstoenadService>()
    private val beregnOverstyrBeregningService = mockk<BeregnOverstyrBeregningService>()
    private val beregningService: BeregningService =
        BeregningService(
            beregningRepository = beregningRepository,
            behandlingService = behandlingService,
            beregnBarnepensjonService = beregnBarnepensjonService,
            beregnOmstillingsstoenadService = beregnOmstillingsstoenadService,
            beregnOverstyrBeregningService = beregnOverstyrBeregningService,
            behandlingStatusService = behandlingStatusService,
        )

    @Test
    fun `skal beregne barnepensjon og lagre resultatet hvis statussjekk i behandling passerer`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON)
        val beregning = mockk<Beregning>()

        every { beregningRepository.lagreEllerOppdaterBeregning(any()) } returns beregning
        every { beregningRepository.hentOverstyrBeregning(any()) } returns null
        coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns behandling
        coEvery { behandlingStatusService.settBeregnet(any(), any(), any()) } returns Unit
        coEvery { beregnBarnepensjonService.beregn(any(), any()) } returns mockk()
        every { beregning.behandlingId } returns behandling.id
        every { beregning.copy(any(), any(), any(), any(), any(), any(), any()) } returns beregning

        runBlocking {
            beregningService.opprettBeregning(behandling.id, bruker)

            coVerify(exactly = 1) { behandlingStatusService.settBeregnet(any(), any(), true) }
            coVerify(exactly = 1) { behandlingStatusService.settBeregnet(any(), any(), false) }
            coVerify(exactly = 1) { beregnBarnepensjonService.beregn(any(), any()) }
            verify(exactly = 1) { beregningRepository.lagreEllerOppdaterBeregning(any()) }
            coVerifyOrder {
                behandlingStatusService.settBeregnet(any(), bruker, true)
                beregnBarnepensjonService.beregn(any(), any())
                beregningRepository.lagreEllerOppdaterBeregning(any())
                behandlingStatusService.settBeregnet(any(), bruker, false)
            }
        }
    }

    @Test
    fun `skal beregne barnepensjon og lagre resultatet hvis statussjekk i behandling passerer med manuelt beregning`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON)
        val beregning = mockk<Beregning>()
        val overstyrBeregning =
            OverstyrBeregning(
                behandling.sak,
                "Test",
                Tidspunkt.now(),
                kategori = OverstyrtBeregningKategori.UKJENT_KATEGORI,
            )

        every { beregningRepository.lagreEllerOppdaterBeregning(any()) } returns beregning
        every { beregningRepository.hentOverstyrBeregning(any()) } returns overstyrBeregning
        coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns behandling
        coEvery { behandlingStatusService.settBeregnet(any(), any(), any()) } returns Unit
        coEvery { beregnOverstyrBeregningService.beregn(any(), any(), any()) } returns mockk()
        every { beregning.behandlingId } returns behandling.id
        every { beregning.copy(any(), any(), any(), any(), any(), any(), overstyrBeregning) } returns beregning

        runBlocking {
            beregningService.opprettBeregning(behandling.id, bruker)

            coVerify(exactly = 1) { behandlingStatusService.settBeregnet(any(), any(), true) }
            coVerify(exactly = 1) { behandlingStatusService.settBeregnet(any(), any(), false) }
            coVerify(exactly = 1) { beregnOverstyrBeregningService.beregn(any(), any(), any()) }
            verify(exactly = 1) { beregningRepository.lagreEllerOppdaterBeregning(any()) }
        }
    }

    @Test
    fun `skal ikke beregne barnepensjon eller lagre noe hvis statussjekk i behandling ikke passerer`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON)

        every { beregningRepository.lagreEllerOppdaterBeregning(any()) } returns mockk()
        coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns behandling
        coEvery { behandlingStatusService.settBeregnet(any(), any(), any()) } throws TilstandException.UgyldigTilstand("Ugyldig status")

        runBlocking {
            assertThrows<TilstandException.UgyldigTilstand> {
                beregningService.opprettBeregning(behandling.id, bruker)
            }

            coVerify(exactly = 1) { behandlingStatusService.settBeregnet(any(), any(), true) }
            coVerify(exactly = 0) { beregnBarnepensjonService.beregn(any(), any()) }
            verify(exactly = 0) { beregningRepository.lagreEllerOppdaterBeregning(any()) }
        }
    }

    @Test
    fun `skal beregne omstillingsstoenad og lagre resultatet hvis statussjekk i behandling passerer`() {
        val behandling = mockBehandling(SakType.OMSTILLINGSSTOENAD)
        val beregning = mockk<Beregning>()

        every { beregningRepository.lagreEllerOppdaterBeregning(any()) } returns beregning
        every { beregningRepository.hentOverstyrBeregning(any()) } returns null
        coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns behandling
        coEvery { behandlingStatusService.settBeregnet(any(), any(), any()) } returns Unit
        coEvery { beregnOmstillingsstoenadService.beregn(any(), any()) } returns mockk()
        every { beregning.behandlingId } returns behandling.id
        every { beregning.copy(any(), any(), any(), any(), any(), any(), any()) } returns beregning

        runBlocking {
            beregningService.opprettBeregning(behandling.id, bruker)

            coVerify(exactly = 1) { behandlingStatusService.settBeregnet(any(), any(), true) }
            coVerify(exactly = 1) { behandlingStatusService.settBeregnet(any(), any(), false) }
            coVerify(exactly = 1) { beregnOmstillingsstoenadService.beregn(any(), any()) }
            verify(exactly = 1) { beregningRepository.lagreEllerOppdaterBeregning(any()) }
        }
    }

    @Test
    fun `skal beregne omstillingsstoenad og lagre resultatet hvis statussjekk i behandling passerermed manuelt beregning`() {
        val behandling = mockBehandling(SakType.OMSTILLINGSSTOENAD)
        val beregning = mockk<Beregning>()
        val overstyrBeregning =
            OverstyrBeregning(
                behandling.sak,
                "Test",
                Tidspunkt.now(),
                kategori = OverstyrtBeregningKategori.UKJENT_KATEGORI,
            )

        every { beregningRepository.lagreEllerOppdaterBeregning(any()) } returns beregning
        every { beregningRepository.hentOverstyrBeregning(any()) } returns overstyrBeregning
        coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns behandling
        coEvery { behandlingStatusService.settBeregnet(any(), any(), any()) } returns Unit
        coEvery { beregnOverstyrBeregningService.beregn(any(), any(), any()) } returns mockk()
        every { beregning.behandlingId } returns behandling.id
        every { beregning.copy(any(), any(), any(), any(), any(), any(), overstyrBeregning) } returns beregning

        runBlocking {
            beregningService.opprettBeregning(behandling.id, bruker)

            coVerify(exactly = 1) { behandlingStatusService.settBeregnet(any(), any(), true) }
            coVerify(exactly = 1) { behandlingStatusService.settBeregnet(any(), any(), false) }
            coVerify(exactly = 1) { beregnOverstyrBeregningService.beregn(any(), any(), any()) }
            verify(exactly = 1) { beregningRepository.lagreEllerOppdaterBeregning(any()) }
        }
    }

    @Test
    fun `skal ikke beregne omstillingsstoenad eller lagre noe hvis statussjekk i behandling ikke passerer`() {
        val behandling = mockBehandling(SakType.OMSTILLINGSSTOENAD)

        coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns behandling
        coEvery { behandlingStatusService.settBeregnet(any(), any(), any()) } throws TilstandException.UgyldigTilstand("Ugyldig status")

        runBlocking {
            assertThrows<TilstandException.UgyldigTilstand> {
                beregningService.opprettBeregning(behandling.id, bruker)
            }

            coVerify(exactly = 1) { behandlingStatusService.settBeregnet(any(), any(), true) }
            coVerify(exactly = 0) { beregnOmstillingsstoenadService.beregn(any(), any()) }
            verify(exactly = 0) { beregningRepository.lagreEllerOppdaterBeregning(any()) }
        }
    }

    @Test
    fun `skal ikke hente overstyr beregning der den ikke finnes`() {
        val behandling = mockBehandling(SakType.OMSTILLINGSSTOENAD)

        coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns behandling
        every { beregningRepository.hentOverstyrBeregning(any()) } returns null

        runBlocking {
            val overstyrBeregning = beregningService.hentOverstyrBeregningPaaBehandlingId(behandling.id, bruker)

            overstyrBeregning shouldBe null

            verify(exactly = 1) { beregningRepository.hentOverstyrBeregning(any()) }
        }
    }

    @Test
    fun `skal hente overstyr beregning der den finnes`() {
        val behandling = mockBehandling(SakType.OMSTILLINGSSTOENAD)

        coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns behandling
        every { beregningRepository.hentOverstyrBeregning(any()) } returns
            OverstyrBeregning(
                behandling.sak,
                "Test",
                Tidspunkt.now(),
                kategori = OverstyrtBeregningKategori.UKJENT_KATEGORI,
            )

        runBlocking {
            val overstyrBeregning = beregningService.hentOverstyrBeregningPaaBehandlingId(behandling.id, bruker)

            overstyrBeregning shouldNotBe null

            overstyrBeregning?.sakId shouldBe behandling.sak
            overstyrBeregning?.beskrivelse shouldBe "Test"
            overstyrBeregning?.kategori shouldBe OverstyrtBeregningKategori.UKJENT_KATEGORI

            verify(exactly = 1) { beregningRepository.hentOverstyrBeregning(any()) }
        }
    }

    @Test
    fun `skal opprette overstyrt beregning der den ikke finnes`() {
        val behandling = mockBehandling(SakType.OMSTILLINGSSTOENAD)

        coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns behandling
        coEvery { behandlingStatusService.settTrygdetidOppdatert(any(), any(), true) } returns Unit
        coEvery { behandlingStatusService.settTrygdetidOppdatert(any(), any(), any()) } returns Unit
        every { beregningRepository.hentOverstyrBeregning(any()) } returns null
        every { beregningRepository.opprettOverstyrBeregning(any()) } returnsArgument 0

        runBlocking {
            val overstyrBeregning =
                beregningService.opprettOverstyrBeregning(
                    behandling.id,
                    OverstyrBeregningDTO("Test", OverstyrtBeregningKategori.UKJENT_KATEGORI),
                    bruker,
                )

            overstyrBeregning shouldNotBe null

            overstyrBeregning?.sakId shouldBe behandling.sak
            overstyrBeregning?.beskrivelse shouldBe "Test"
            overstyrBeregning?.kategori shouldBe OverstyrtBeregningKategori.UKJENT_KATEGORI

            verify(exactly = 1) {
                beregningRepository.opprettOverstyrBeregning(any())
                beregningRepository.hentOverstyrBeregning(any())
            }
        }
    }

    @Test
    fun `skal kunne slette overstyrt beregning der den finnes`() {
        val behandling = mockBehandling(SakType.OMSTILLINGSSTOENAD)

        coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns behandling
        coEvery { behandlingStatusService.settTrygdetidOppdatert(any(), any(), true) } returns Unit
        coEvery { behandlingStatusService.settTrygdetidOppdatert(any(), any(), any()) } returns Unit
        every { beregningRepository.hentOverstyrBeregning(any()) } returns null
        every { beregningRepository.opprettOverstyrBeregning(any()) } returnsArgument 0
        every { beregningRepository.deaktiverOverstyrtBeregning(any()) } just runs
        every { beregningRepository.slettOverstyrtBeregningsgrunnlag(any()) } just runs

        runBlocking {
            val overstyrBeregning =
                beregningService.opprettOverstyrBeregning(
                    behandling.id,
                    OverstyrBeregningDTO("Test", OverstyrtBeregningKategori.UKJENT_KATEGORI),
                    bruker,
                )

            overstyrBeregning shouldNotBe null

            overstyrBeregning?.sakId shouldBe behandling.sak
            overstyrBeregning?.beskrivelse shouldBe "Test"

            verify(exactly = 1) {
                beregningRepository.opprettOverstyrBeregning(any())
                beregningRepository.hentOverstyrBeregning(any())
            }

            beregningService.deaktiverOverstyrtberegning(behandling.id, bruker)
            val tomOverstyrt = beregningService.hentOverstyrBeregning(behandling)
            tomOverstyrt shouldBe null
        }
    }

    @Test
    fun `skal ikke overskrive overstyr beregning der den finnes`() {
        val behandling = mockBehandling(SakType.OMSTILLINGSSTOENAD)

        coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns behandling
        coEvery { behandlingStatusService.settTrygdetidOppdatert(any(), any(), true) } returns Unit
        coEvery { behandlingStatusService.settTrygdetidOppdatert(any(), any(), any()) } returns Unit
        every { beregningRepository.hentOverstyrBeregning(any()) } returns
            OverstyrBeregning(
                behandling.sak,
                "Test",
                Tidspunkt.now(),
                kategori = OverstyrtBeregningKategori.UKJENT_KATEGORI,
            )

        runBlocking {
            val overstyrBeregning =
                beregningService.opprettOverstyrBeregning(
                    behandling.id,
                    OverstyrBeregningDTO("Test 2", OverstyrtBeregningKategori.UKJENT_AVDOED),
                    bruker,
                )

            overstyrBeregning shouldNotBe null

            overstyrBeregning?.sakId shouldBe behandling.sak
            overstyrBeregning?.beskrivelse shouldBe "Test"
            overstyrBeregning?.kategori shouldBe OverstyrtBeregningKategori.UKJENT_KATEGORI

            verify(exactly = 1) { beregningRepository.hentOverstyrBeregning(any()) }
        }
    }

    @Test
    fun `skal viderefoere tekniske feil fra statussjekken uten aa lagre`() =
        runBlocking<Unit> {
            val behandlingId = randomUUID()
            val feil = IllegalStateException("Databasen er utilgjengelig")
            every { behandlingStatusService.settBeregnet(behandlingId, bruker, true) } throws feil

            assertThrows<IllegalStateException> {
                beregningService.opprettBeregning(behandlingId, bruker)
            } shouldBeSameInstanceAs feil

            verify(exactly = 0) { behandlingService.hentDetaljertBehandling(any(), any()) }
            verify(exactly = 0) { beregningRepository.lagreEllerOppdaterBeregning(any()) }
        }

    @Test
    fun `skal stoppe beregningen naar behandlingen mangler`() =
        runBlocking<Unit> {
            val behandlingId = randomUUID()
            every { behandlingStatusService.settBeregnet(behandlingId, bruker, true) } returns Unit
            every { behandlingService.hentDetaljertBehandling(behandlingId, bruker) } returns null

            assertThrows<GenerellIkkeFunnetException> {
                beregningService.opprettBeregning(behandlingId, bruker)
            }

            coVerify(exactly = 0) { beregnBarnepensjonService.beregn(any(), any()) }
            verify(exactly = 0) { beregningRepository.lagreEllerOppdaterBeregning(any()) }
            verify(exactly = 0) { behandlingStatusService.settBeregnet(any(), any(), false) }
        }

    @ParameterizedTest
    @ValueSource(ints = [1, 2])
    fun `skal bare deaktivere overstyring ved revurdering fra foerste virkningstidspunkt`(maaned: Int) =
        runBlocking<Unit> {
            val behandling = mockBehandling(SakType.OMSTILLINGSSTOENAD)
            val behandlingId = behandling.id
            val sakId = behandling.sak
            every { behandling.behandlingType } returns BehandlingType.REVURDERING
            every { behandlingService.hentDetaljertBehandling(behandlingId, bruker) } returns behandling
            every { behandlingService.hentFoerstegangsbehandling(sakId) } returns
                mockk<Foerstegangsbehandling> {
                    every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2023, maaned))
                }
            every { behandlingStatusService.settTrygdetidOppdatert(any(), any(), any()) } returns Unit
            every { beregningRepository.deaktiverOverstyrtBeregning(any()) } just runs
            every { beregningRepository.slettOverstyrtBeregningsgrunnlag(any()) } just runs

            if (maaned == 1) {
                beregningService.deaktiverOverstyrtberegning(behandlingId, bruker)
            } else {
                assertThrows<KanIkkeAktivereOverstyrtBeregningGrunnetVirkningsdato> {
                    beregningService.deaktiverOverstyrtberegning(behandlingId, bruker)
                }
            }

            val forventetAntall = if (maaned == 1) 1 else 0
            verify(exactly = forventetAntall) { beregningRepository.deaktiverOverstyrtBeregning(sakId) }
            verify(exactly = forventetAntall) { beregningRepository.slettOverstyrtBeregningsgrunnlag(behandlingId) }
            verify(exactly = forventetAntall) { behandlingStatusService.settTrygdetidOppdatert(behandlingId, bruker, false) }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `interne behandlingskall skal bruke eksisterende transaksjon eller opprette en ny`(harTransaksjon: Boolean) =
        runBlocking<Unit> {
            var nyeTransaksjoner = 0
            val database =
                object : DatabaseKontekst {
                    override fun activeTx(): Connection = error("Testen bruker ingen databaseforbindelse")

                    override fun harIntransaction(): Boolean = harTransaksjon

                    override fun <T> inTransaction(block: () -> T): T {
                        nyeTransaksjoner++
                        return block()
                    }
                }
            nyKontekstMedBrukerOgDatabaseContext(Self("beregning-test"), database)
            val behandling = mockBehandling(SakType.OMSTILLINGSSTOENAD)
            val behandlingId = behandling.id
            val sakId = behandling.sak
            every { behandlingService.hentDetaljertBehandling(behandlingId, bruker) } returns behandling
            every { beregningRepository.hentOverstyrBeregning(sakId) } returns null

            beregningService.hentOverstyrBeregningPaaBehandlingId(behandlingId, bruker) shouldBe null

            nyeTransaksjoner shouldBe if (harTransaksjon) 0 else 1
        }

    private fun mockBehandling(type: SakType): DetaljertBehandling =
        mockk<DetaljertBehandling>().apply {
            every { id } returns randomUUID()
            every { sak } returns sakId1
            every { sakType } returns type
            every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2023, 1))
            every { opphoerFraOgMed } returns null
        }
}
