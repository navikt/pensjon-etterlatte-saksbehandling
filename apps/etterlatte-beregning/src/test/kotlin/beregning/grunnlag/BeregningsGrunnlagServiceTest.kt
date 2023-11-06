package beregning.grunnlag

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.BeregnBarnepensjonServiceFeatureToggle
import no.nav.etterlatte.beregning.grunnlag.BarnepensjonBeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagRepository
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.grunnlag.Reduksjon
import no.nav.etterlatte.beregning.regler.toGrunnlag
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.klienter.BehandlingKlientImpl
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.BeregningsMetodeBeregningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN_FOEDSELSNUMMER
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import java.util.UUID.randomUUID

internal class BeregningsGrunnlagServiceTest {
    private val behandlingKlient = mockk<BehandlingKlientImpl>()
    private val beregningsGrunnlagRepository = mockk<BeregningsGrunnlagRepository>()
    private val featureToggleService = DummyFeatureToggleService()
    private val beregningsGrunnlagService: BeregningsGrunnlagService =
        BeregningsGrunnlagService(
            beregningsGrunnlagRepository,
            behandlingKlient,
            featureToggleService,
        )

    @Test
    fun `skal lagre soeksken med i beregning hvis ikke det finnes`() {
        val soeskenMedIBeregning: List<GrunnlagMedPeriode<List<SoeskenMedIBeregning>>> =
            listOf(
                GrunnlagMedPeriode(
                    fom = LocalDate.of(2022, 8, 1),
                    tom = null,
                    data =
                        listOf(
                            SoeskenMedIBeregning(HELSOESKEN_FOEDSELSNUMMER, true),
                        ),
                ),
            )
        val institusjonsoppholdBeregningsgrunnlag =
            listOf(
                GrunnlagMedPeriode(
                    fom = LocalDate.of(2022, 8, 1),
                    tom = null,
                    data = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.NEI_KORT_OPPHOLD),
                ),
            )

        val behandling = mockBehandling(SakType.BARNEPENSJON, randomUUID())

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnBarnepensjonGrunnlagForBehandling(any()) } returns null
        every { beregningsGrunnlagRepository.lagre(any()) } returns true

        runBlocking {
            beregningsGrunnlagService.lagreBarnepensjonBeregningsGrunnlag(
                randomUUID(),
                BarnepensjonBeregningsGrunnlag(soeskenMedIBeregning, institusjonsoppholdBeregningsgrunnlag),
                mockk {
                    every { ident() } returns "Z123456"
                },
            )

            verify(exactly = 1) { beregningsGrunnlagRepository.lagre(any()) }
        }
    }

    @Test
    fun `skal ikke tillate endringer i beregningsgrunnlaget før virk på revurdering`() {
        val sakId = 1337L
        val foerstegangsbehandling = mockBehandling(type = SakType.BARNEPENSJON, uuid = randomUUID(), sakId = sakId)

        val virk = YearMonth.of(2023, Month.JANUARY)
        val virkMock = mockk<Virkningstidspunkt>()
        every { virkMock.dato } returns virk
        val revurdering =
            mockBehandling(
                type = SakType.BARNEPENSJON,
                uuid = randomUUID(),
                behandlingstype = BehandlingType.REVURDERING,
                sakId = sakId,
            )
        every { revurdering.virkningstidspunkt } returns virkMock

        val soesken = listOf(HELSOESKEN_FOEDSELSNUMMER, HELSOESKEN2_FOEDSELSNUMMER)

        val periode1 =
            GrunnlagMedPeriode(
                data = soesken.map { SoeskenMedIBeregning(it, true) },
                fom = LocalDate.of(2022, 8, 1),
                tom = null,
            )
        val periode2 =
            GrunnlagMedPeriode(
                data = soesken.map { SoeskenMedIBeregning(it, false) },
                fom = virk.atDay(1).minusMonths(1),
            )

        val grunnlagIverksatt =
            beregningsgrunnlag(
                behandlingId = foerstegangsbehandling.id,
                soeskenMedIBeregning = listOf(periode1),
            )
        val grunnlagEndring =
            beregningsgrunnlag(
                behandlingId = revurdering.id,
                soeskenMedIBeregning = listOf(periode1.copy(tom = periode2.fom.minusDays(1)), periode2),
            )

        coEvery { behandlingKlient.hentBehandling(foerstegangsbehandling.id, any()) } returns foerstegangsbehandling
        coEvery { behandlingKlient.kanBeregnes(revurdering.id, any(), any()) } returns true
        coEvery {
            behandlingKlient.hentSisteIverksatteBehandling(
                sakId,
                any(),
            )
        } returns SisteIverksatteBehandling(foerstegangsbehandling.id)
        coEvery { behandlingKlient.hentBehandling(revurdering.id, any()) } returns revurdering

        every {
            beregningsGrunnlagRepository.finnBarnepensjonGrunnlagForBehandling(foerstegangsbehandling.id)
        } returns grunnlagIverksatt
        every {
            beregningsGrunnlagRepository.finnBarnepensjonGrunnlagForBehandling(revurdering.id)
        } returns grunnlagEndring

        runBlocking {
            val lagret =
                beregningsGrunnlagService.lagreBarnepensjonBeregningsGrunnlag(
                    behandlingId = revurdering.id,
                    barnepensjonBeregningsGrunnlag =
                        BarnepensjonBeregningsGrunnlag(
                            soeskenMedIBeregning = grunnlagEndring.soeskenMedIBeregning,
                            institusjonsopphold = grunnlagEndring.institusjonsoppholdBeregningsgrunnlag,
                        ),
                    brukerTokenInfo = mockk(relaxed = true),
                )
            assertFalse(lagret)
        }

        coVerify(exactly = 0) { beregningsGrunnlagRepository.lagre(any()) }
    }

    @Test
    fun `skal tillate endringer i beregningsgrunnlaget etter virk på revurdering`() {
        val sakId = 1337L
        val foerstegangsbehandling = mockBehandling(type = SakType.BARNEPENSJON, uuid = randomUUID(), sakId = sakId)

        val virk = YearMonth.of(2023, Month.JANUARY)
        val virkMock = mockk<Virkningstidspunkt>()
        every { virkMock.dato } returns virk
        val revurdering =
            mockBehandling(
                type = SakType.BARNEPENSJON,
                uuid = randomUUID(),
                behandlingstype = BehandlingType.REVURDERING,
                sakId = sakId,
            )
        every { revurdering.virkningstidspunkt } returns virkMock

        val soesken = listOf(HELSOESKEN_FOEDSELSNUMMER, HELSOESKEN2_FOEDSELSNUMMER)

        val periode1 =
            GrunnlagMedPeriode(
                data = soesken.map { SoeskenMedIBeregning(it, true) },
                fom = LocalDate.of(2022, 8, 1),
                tom = null,
            )
        val periode2 =
            GrunnlagMedPeriode(
                data = soesken.map { SoeskenMedIBeregning(it, false) },
                fom = virk.atDay(1),
            )

        val grunnlagIverksatt =
            beregningsgrunnlag(
                behandlingId = foerstegangsbehandling.id,
                soeskenMedIBeregning = listOf(periode1),
            )
        val grunnlagEndring =
            beregningsgrunnlag(
                behandlingId = revurdering.id,
                soeskenMedIBeregning = listOf(periode1.copy(tom = periode2.fom.minusDays(1)), periode2),
            )

        coEvery { behandlingKlient.hentBehandling(foerstegangsbehandling.id, any()) } returns foerstegangsbehandling
        coEvery { behandlingKlient.kanBeregnes(revurdering.id, any(), any()) } returns true
        coEvery {
            behandlingKlient.hentSisteIverksatteBehandling(
                sakId,
                any(),
            )
        } returns SisteIverksatteBehandling(foerstegangsbehandling.id)
        coEvery { behandlingKlient.hentBehandling(revurdering.id, any()) } returns revurdering

        every {
            beregningsGrunnlagRepository.finnBarnepensjonGrunnlagForBehandling(foerstegangsbehandling.id)
        } returns grunnlagIverksatt
        every {
            beregningsGrunnlagRepository.finnBarnepensjonGrunnlagForBehandling(revurdering.id)
        } returns grunnlagEndring
        every { beregningsGrunnlagRepository.lagre(any()) } returns true

        runBlocking {
            val lagret =
                beregningsGrunnlagService.lagreBarnepensjonBeregningsGrunnlag(
                    behandlingId = revurdering.id,
                    barnepensjonBeregningsGrunnlag =
                        BarnepensjonBeregningsGrunnlag(
                            soeskenMedIBeregning = grunnlagEndring.soeskenMedIBeregning,
                            institusjonsopphold = grunnlagEndring.institusjonsoppholdBeregningsgrunnlag,
                        ),
                    brukerTokenInfo = mockk(relaxed = true),
                )
            assertTrue(lagret)
        }

        coVerify(exactly = 1) { beregningsGrunnlagRepository.lagre(any()) }
    }

    @Test
    fun `skal lage et kopi av grunnlaget`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON, randomUUID())

        val omregningsId = randomUUID()
        val behandlingsId = randomUUID()

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnBarnepensjonGrunnlagForBehandling(omregningsId) } returns null
        every {
            beregningsGrunnlagRepository.finnBarnepensjonGrunnlagForBehandling(behandlingsId)
        } returns
            BeregningsGrunnlag(
                behandlingsId,
                Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
                emptyList(),
                emptyList(),
                BeregningsMetode.BEST.toGrunnlag(),
            )

        every { beregningsGrunnlagRepository.lagre(any()) } returns true

        runBlocking {
            beregningsGrunnlagService.dupliserBeregningsGrunnlagBP(omregningsId, behandlingsId)

            verify(exactly = 1) { beregningsGrunnlagRepository.lagre(any()) }
        }
    }

    @Test
    fun `kan ikke lage et kopi av grunnlaget hvis forrige mangler`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON, randomUUID())

        val behandlingsId = randomUUID()

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnBarnepensjonGrunnlagForBehandling(behandlingsId) } returns null

        runBlocking {
            assertThrows<RuntimeException> {
                beregningsGrunnlagService.dupliserBeregningsGrunnlagBP(randomUUID(), behandlingsId)

                verify(exactly = 0) { beregningsGrunnlagRepository.lagre(any()) }
            }
        }
    }

    @Test
    fun `kan ikke lage et kopi av grunnlaget hvis den allerede finnes`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON, randomUUID())

        val behandlingsId = randomUUID()
        val omregningsId = randomUUID()

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnBarnepensjonGrunnlagForBehandling(any()) } returns
            BeregningsGrunnlag(
                behandlingId = behandlingsId,
                kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
                soeskenMedIBeregning = emptyList(),
                institusjonsoppholdBeregningsgrunnlag = emptyList(),
                beregningsMetode = BeregningsMetode.BEST.toGrunnlag(),
            )

        runBlocking {
            assertThrows<RuntimeException> {
                beregningsGrunnlagService.dupliserBeregningsGrunnlagBP(omregningsId, behandlingsId)

                verify(exactly = 0) { beregningsGrunnlagRepository.lagre(any()) }
            }
        }
    }

    @Test
    fun `skal lagre default soeksken med i beregning hvis det er tom`() {
        val soeskenMedIBeregning: List<GrunnlagMedPeriode<List<SoeskenMedIBeregning>>> = emptyList()

        val institusjonsoppholdBeregningsgrunnlag =
            listOf(
                GrunnlagMedPeriode(
                    fom = LocalDate.of(2022, 8, 1),
                    tom = null,
                    data = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.NEI_KORT_OPPHOLD),
                ),
            )

        val behandling = mockBehandling(SakType.BARNEPENSJON, randomUUID())

        val slot = slot<BeregningsGrunnlag>()

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnBarnepensjonGrunnlagForBehandling(any()) } returns null
        every { beregningsGrunnlagRepository.lagre(capture(slot)) } returns true

        runBlocking {
            beregningsGrunnlagService.lagreBarnepensjonBeregningsGrunnlag(
                randomUUID(),
                BarnepensjonBeregningsGrunnlag(soeskenMedIBeregning, institusjonsoppholdBeregningsgrunnlag),
                mockk {
                    every { ident() } returns "Z123456"
                },
            )

            assertEquals(
                listOf<GrunnlagMedPeriode<List<SoeskenMedIBeregning>>>(
                    GrunnlagMedPeriode(
                        fom = behandling.virkningstidspunkt!!.dato.atDay(1),
                        tom = null,
                        data = emptyList(),
                    ),
                ),
                slot.captured.soeskenMedIBeregning,
            )

            verify(exactly = 1) { beregningsGrunnlagRepository.lagre(any()) }
        }
    }

    @Test
    fun `skal lagre beregningsmetode`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON, randomUUID())
        val slot = slot<BeregningsGrunnlag>()

        featureToggleService.settBryter(BeregnBarnepensjonServiceFeatureToggle.BrukFaktiskTrygdetid, true)

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnBarnepensjonGrunnlagForBehandling(any()) } returns null
        every { beregningsGrunnlagRepository.lagre(capture(slot)) } returns true

        runBlocking {
            beregningsGrunnlagService.lagreBarnepensjonBeregningsGrunnlag(
                randomUUID(),
                BarnepensjonBeregningsGrunnlag(emptyList(), emptyList(), BeregningsMetodeBeregningsgrunnlag(BeregningsMetode.BEST)),
                mockk {
                    every { ident() } returns "Z123456"
                },
            )

            assertEquals(BeregningsMetode.BEST, slot.captured.beregningsMetode.beregningsMetode)

            verify(exactly = 1) { beregningsGrunnlagRepository.lagre(any()) }
        }
    }

    @Test
    fun `skal lagre beregningsmetode NASJONAL hvis feature toggle er false`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON, randomUUID())
        val slot = slot<BeregningsGrunnlag>()

        featureToggleService.settBryter(BeregnBarnepensjonServiceFeatureToggle.BrukFaktiskTrygdetid, false)

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnBarnepensjonGrunnlagForBehandling(any()) } returns null
        every { beregningsGrunnlagRepository.lagre(capture(slot)) } returns true

        runBlocking {
            beregningsGrunnlagService.lagreBarnepensjonBeregningsGrunnlag(
                randomUUID(),
                BarnepensjonBeregningsGrunnlag(emptyList(), emptyList(), BeregningsMetodeBeregningsgrunnlag(BeregningsMetode.BEST)),
                mockk {
                    every { ident() } returns "Z123456"
                },
            )

            assertEquals(BeregningsMetode.NASJONAL, slot.captured.beregningsMetode.beregningsMetode)

            verify(exactly = 1) { beregningsGrunnlagRepository.lagre(any()) }
        }
    }

    private fun mockBehandling(
        type: SakType,
        uuid: UUID,
        behandlingstype: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        sakId: Long = 1L,
    ): DetaljertBehandling =
        mockk<DetaljertBehandling>().apply {
            every { id } returns uuid
            every { sak } returns sakId
            every { sakType } returns type
            every { behandlingType } returns behandlingstype
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2023, 1))
        }

    private fun beregningsgrunnlag(
        behandlingId: UUID = randomUUID(),
        soeskenMedIBeregning: List<GrunnlagMedPeriode<List<SoeskenMedIBeregning>>> = emptyList(),
        institusjonsoppholdBeregningsgrunnlag: List<GrunnlagMedPeriode<InstitusjonsoppholdBeregningsgrunnlag>> =
            emptyList(),
        kilde: Grunnlagsopplysning.Saksbehandler = Grunnlagsopplysning.Saksbehandler("test", Tidspunkt.now()),
    ): BeregningsGrunnlag {
        return BeregningsGrunnlag(
            behandlingId = behandlingId,
            kilde = kilde,
            soeskenMedIBeregning = soeskenMedIBeregning,
            institusjonsoppholdBeregningsgrunnlag = institusjonsoppholdBeregningsgrunnlag,
            beregningsMetode = BeregningsMetode.NASJONAL.toGrunnlag(),
        )
    }
}
