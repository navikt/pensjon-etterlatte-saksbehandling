package beregning.grunnlag

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.grunnlag.BarnepensjonBeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagRepository
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.grunnlag.Reduksjon
import no.nav.etterlatte.klienter.BehandlingKlientImpl
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.RuntimeException
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.*
import java.util.UUID.randomUUID

internal class BeregningsGrunnlagServiceTest {

    private val behandlingKlient = mockk<BehandlingKlientImpl>()
    private val beregningsGrunnlagRepository = mockk<BeregningsGrunnlagRepository>()
    private val beregningsGrunnlagService: BeregningsGrunnlagService = BeregningsGrunnlagService(
        beregningsGrunnlagRepository,
        behandlingKlient
    )

    private val personidenter = listOf(
        "05108208963",
        "18061264406"
    )

    @Test
    fun `skal lagre soeksken med i beregning hvis ikke det finnes`() {
        val soeskenMedIBeregning: List<GrunnlagMedPeriode<List<SoeskenMedIBeregning>>> = listOf(
            GrunnlagMedPeriode(
                fom = LocalDate.of(2022, 8, 1),
                tom = null,
                data = listOf(
                    SoeskenMedIBeregning(STOR_SNERK, true)
                )
            )
        )
        val institusjonsoppholdBeregningsgrunnlag = listOf(
            GrunnlagMedPeriode(
                fom = LocalDate.of(2022, 8, 1),
                tom = null,
                data = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.NEI_KORT_OPPHOLD)
            )
        )

        val behandling = mockBehandling(SakType.BARNEPENSJON, randomUUID())

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.beregn(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnGrunnlagForBehandling(any()) } returns null
        every { beregningsGrunnlagRepository.lagre(any()) } returns true

        runBlocking {
            beregningsGrunnlagService.lagreBarnepensjonBeregningsGrunnlag(
                randomUUID(),
                BarnepensjonBeregningsGrunnlag(soeskenMedIBeregning, institusjonsoppholdBeregningsgrunnlag),
                mockk {
                    every { ident() } returns "Z123456"
                }
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
        val revurdering = mockBehandling(
            type = SakType.BARNEPENSJON,
            uuid = randomUUID(),
            behandlingstype = BehandlingType.REVURDERING,
            sakId = sakId
        )
        every { revurdering.virkningstidspunkt } returns virkMock

        val soesken = personidenter.map { Folkeregisteridentifikator.of(it) }

        val periode1 = GrunnlagMedPeriode(
            data = soesken.map { SoeskenMedIBeregning(it, true) },
            fom = LocalDate.of(2022, 8, 1),
            tom = null
        )
        val periode2 = GrunnlagMedPeriode(
            data = soesken.map { SoeskenMedIBeregning(it, false) },
            fom = virk.atDay(1).minusMonths(1)
        )

        val grunnlagIverksatt = beregningsgrunnlag(
            behandlingId = foerstegangsbehandling.id,
            soeskenMedIBeregning = listOf(periode1)
        )
        val grunnlagEndring = beregningsgrunnlag(
            behandlingId = revurdering.id,
            soeskenMedIBeregning = listOf(periode1.copy(tom = periode2.fom.minusDays(1)), periode2)
        )

        coEvery { behandlingKlient.hentBehandling(foerstegangsbehandling.id, any()) } returns foerstegangsbehandling
        coEvery { behandlingKlient.beregn(revurdering.id, any(), any()) } returns true
        coEvery {
            behandlingKlient.hentSisteIverksatteBehandling(
                sakId,
                any()
            )
        } returns foerstegangsbehandling
        coEvery { behandlingKlient.hentBehandling(revurdering.id, any()) } returns revurdering

        every {
            beregningsGrunnlagRepository.finnGrunnlagForBehandling(foerstegangsbehandling.id)
        } returns grunnlagIverksatt
        every { beregningsGrunnlagRepository.finnGrunnlagForBehandling(revurdering.id) } returns grunnlagEndring

        runBlocking {
            val lagret = beregningsGrunnlagService.lagreBarnepensjonBeregningsGrunnlag(
                behandlingId = revurdering.id,
                barnepensjonBeregningsGrunnlag = BarnepensjonBeregningsGrunnlag(
                    soeskenMedIBeregning = grunnlagEndring.soeskenMedIBeregning,
                    institusjonsopphold = grunnlagEndring.institusjonsoppholdBeregningsgrunnlag
                ),
                brukerTokenInfo = mockk(relaxed = true)
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
        val revurdering = mockBehandling(
            type = SakType.BARNEPENSJON,
            uuid = randomUUID(),
            behandlingstype = BehandlingType.REVURDERING,
            sakId = sakId
        )
        every { revurdering.virkningstidspunkt } returns virkMock

        val soesken = personidenter.map { Folkeregisteridentifikator.of(it) }

        val periode1 = GrunnlagMedPeriode(
            data = soesken.map { SoeskenMedIBeregning(it, true) },
            fom = LocalDate.of(2022, 8, 1),
            tom = null
        )
        val periode2 = GrunnlagMedPeriode(
            data = soesken.map { SoeskenMedIBeregning(it, false) },
            fom = virk.atDay(1)
        )

        val grunnlagIverksatt = beregningsgrunnlag(
            behandlingId = foerstegangsbehandling.id,
            soeskenMedIBeregning = listOf(periode1)
        )
        val grunnlagEndring = beregningsgrunnlag(
            behandlingId = revurdering.id,
            soeskenMedIBeregning = listOf(periode1.copy(tom = periode2.fom.minusDays(1)), periode2)
        )

        coEvery { behandlingKlient.hentBehandling(foerstegangsbehandling.id, any()) } returns foerstegangsbehandling
        coEvery { behandlingKlient.beregn(revurdering.id, any(), any()) } returns true
        coEvery {
            behandlingKlient.hentSisteIverksatteBehandling(
                sakId,
                any()
            )
        } returns foerstegangsbehandling
        coEvery { behandlingKlient.hentBehandling(revurdering.id, any()) } returns revurdering

        every {
            beregningsGrunnlagRepository.finnGrunnlagForBehandling(foerstegangsbehandling.id)
        } returns grunnlagIverksatt
        every { beregningsGrunnlagRepository.finnGrunnlagForBehandling(revurdering.id) } returns grunnlagEndring
        every { beregningsGrunnlagRepository.lagre(any()) } returns true

        runBlocking {
            val lagret = beregningsGrunnlagService.lagreBarnepensjonBeregningsGrunnlag(
                behandlingId = revurdering.id,
                barnepensjonBeregningsGrunnlag = BarnepensjonBeregningsGrunnlag(
                    soeskenMedIBeregning = grunnlagEndring.soeskenMedIBeregning,
                    institusjonsopphold = grunnlagEndring.institusjonsoppholdBeregningsgrunnlag
                ),
                brukerTokenInfo = mockk(relaxed = true)
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
        coEvery { behandlingKlient.beregn(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnGrunnlagForBehandling(omregningsId) } returns null
        every { beregningsGrunnlagRepository.finnGrunnlagForBehandling(behandlingsId) } returns BeregningsGrunnlag(
            behandlingsId,
            Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
            emptyList(),
            emptyList()
        )

        every { beregningsGrunnlagRepository.lagre(any()) } returns true

        runBlocking {
            beregningsGrunnlagService.dupliserBeregningsGrunnlag(omregningsId, behandlingsId)

            verify(exactly = 1) { beregningsGrunnlagRepository.lagre(any()) }
        }
    }

    @Test
    fun `kan ikke lage et kopi av grunnlaget hvis forrige mangler`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON, randomUUID())

        val behandlingsId = randomUUID()

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.beregn(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnGrunnlagForBehandling(behandlingsId) } returns null

        runBlocking {
            assertThrows<RuntimeException> {
                beregningsGrunnlagService.dupliserBeregningsGrunnlag(randomUUID(), behandlingsId)

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
        coEvery { behandlingKlient.beregn(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnGrunnlagForBehandling(any()) } returns BeregningsGrunnlag(
            behandlingsId,
            Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
            emptyList(),
            emptyList()
        )

        runBlocking {
            assertThrows<RuntimeException> {
                beregningsGrunnlagService.dupliserBeregningsGrunnlag(omregningsId, behandlingsId)

                verify(exactly = 0) { beregningsGrunnlagRepository.lagre(any()) }
            }
        }
    }

    private fun mockBehandling(
        type: SakType,
        uuid: UUID,
        behandlingstype: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        sakId: Long = 1L
    ): DetaljertBehandling =
        mockk<DetaljertBehandling>().apply {
            every { id } returns uuid
            every { sak } returns sakId
            every { sakType } returns type
            every { behandlingType } returns behandlingstype
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2023, 1))
        }

    private companion object {
        val STOR_SNERK = Folkeregisteridentifikator.of("11057523044")
    }

    private fun beregningsgrunnlag(
        behandlingId: UUID = randomUUID(),
        soeskenMedIBeregning: List<GrunnlagMedPeriode<List<SoeskenMedIBeregning>>> = emptyList(),
        institusjonsoppholdBeregningsgrunnlag: List<GrunnlagMedPeriode<InstitusjonsoppholdBeregningsgrunnlag>> =
            emptyList(),
        kilde: Grunnlagsopplysning.Saksbehandler = Grunnlagsopplysning.Saksbehandler("test", Tidspunkt.now())
    ): BeregningsGrunnlag {
        return BeregningsGrunnlag(
            behandlingId = behandlingId,
            kilde = kilde,
            soeskenMedIBeregning = soeskenMedIBeregning,
            institusjonsoppholdBeregningsgrunnlag = institusjonsoppholdBeregningsgrunnlag
        )
    }
}