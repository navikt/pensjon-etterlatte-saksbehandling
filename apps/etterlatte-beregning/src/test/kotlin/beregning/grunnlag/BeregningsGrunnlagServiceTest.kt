package beregning.grunnlag

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.grunnlag.BarnepensjonBeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagRepository
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.Institusjonsopphold
import no.nav.etterlatte.klienter.BehandlingKlientImpl
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.RuntimeException
import java.time.LocalDate
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
        val institusjonsopphold = Institusjonsopphold(false)

        val behandling = mockBehandling(SakType.BARNEPENSJON, randomUUID())

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.beregn(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnGrunnlagForBehandling(any()) } returns null
        every { beregningsGrunnlagRepository.lagre(any()) } returns true

        runBlocking {
            beregningsGrunnlagService.lagreBarnepensjonBeregningsGrunnlag(
                randomUUID(),
                BarnepensjonBeregningsGrunnlag(soeskenMedIBeregning, institusjonsopphold),
                mockk {
                    every { ident() } returns "Z123456"
                }
            )

            verify(exactly = 1) { beregningsGrunnlagRepository.lagre(any()) }
        }
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
            Institusjonsopphold(false)
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
            Institusjonsopphold(false)
        )

        runBlocking {
            assertThrows<RuntimeException> {
                beregningsGrunnlagService.dupliserBeregningsGrunnlag(omregningsId, behandlingsId)

                verify(exactly = 0) { beregningsGrunnlagRepository.lagre(any()) }
            }
        }
    }

    private fun mockBehandling(type: SakType, uuid: UUID): DetaljertBehandling =
        mockk<DetaljertBehandling>().apply {
            every { id } returns uuid
            every { sak } returns 1
            every { sakType } returns type
            every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2023, 1))
        }

    private companion object {
        val STOR_SNERK = Folkeregisteridentifikator.of("11057523044")
    }
}