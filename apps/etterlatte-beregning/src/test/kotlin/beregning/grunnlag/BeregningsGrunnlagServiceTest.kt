package beregning.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.grunnlag.BPBeregningsgrunnlagMerEnnEnAvdoedException
import no.nav.etterlatte.beregning.grunnlag.BPBeregningsgrunnlagSoeskenIkkeAvdoedesBarnException
import no.nav.etterlatte.beregning.grunnlag.BPBeregningsgrunnlagSoeskenMarkertDoedException
import no.nav.etterlatte.beregning.grunnlag.BarnepensjonBeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagRepository
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlagDTO
import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlagDao
import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlagData
import no.nav.etterlatte.beregning.grunnlag.REFORM_TIDSPUNKT_BP
import no.nav.etterlatte.beregning.grunnlag.Reduksjon
import no.nav.etterlatte.beregning.grunnlag.VirkningstidspunktBPErFoerReformMenManglerSoeskenjustering
import no.nav.etterlatte.beregning.regler.toGrunnlag
import no.nav.etterlatte.klienter.BehandlingKlientImpl
import no.nav.etterlatte.klienter.GrunnlagKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.BeregningsMetodeBeregningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.HALVSOESKEN_ANNEN_FORELDER
import no.nav.etterlatte.libs.testdata.grunnlag.HALVSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.token.BrukerTokenInfo
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
    private val grunnlagKlient = mockk<GrunnlagKlient>()
    private val beregningsGrunnlagService: BeregningsGrunnlagService =
        BeregningsGrunnlagService(
            beregningsGrunnlagRepository,
            behandlingKlient,
            grunnlagKlient,
        )

    private fun grunnlagMedEkstraAvdoedForelder(doedsdato: LocalDate): Grunnlag {
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val nyligAvdoedFoedselsnummer = AVDOED2_FOEDSELSNUMMER
        val nyligAvdoed: List<Grunnlagsdata<JsonNode>> =
            listOf(
                mapOf(
                    Opplysningstype.DOEDSDATO to konstantOpplysning(doedsdato),
                    Opplysningstype.PERSONROLLE to konstantOpplysning(PersonRolle.AVDOED),
                    Opplysningstype.FOEDSELSNUMMER to konstantOpplysning(nyligAvdoedFoedselsnummer),
                ),
            )

        return GrunnlagTestData(opplysningsmapAvdoedeOverrides = grunnlag.hentAvdoede() + nyligAvdoed).hentOpplysningsgrunnlag()
    }

    private fun <T : Any> konstantOpplysning(a: T): Opplysning.Konstant<JsonNode> {
        val kilde = Grunnlagsopplysning.Pdl(Tidspunkt.now(), "", "")
        return Opplysning.Konstant(randomUUID(), kilde, a.toJsonNode())
    }

    @Test
    fun `kan kun ha en avdøed`() {
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
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        val behandlingId = randomUUID()
        val brukertokeninfo = BrukerTokenInfo.of("token", "s1", null, null, null)
        coEvery { grunnlagKlient.hentGrunnlag(behandlingId, brukertokeninfo) } returns
            grunnlagMedEkstraAvdoedForelder(
                LocalDate.now(),
            )
        assertThrows<BPBeregningsgrunnlagMerEnnEnAvdoedException> {
            runBlocking {
                beregningsGrunnlagService.lagreBarnepensjonBeregningsGrunnlag(
                    behandlingId,
                    BarnepensjonBeregningsGrunnlag(soeskenMedIBeregning, null),
                    brukertokeninfo,
                )
            }
        }
    }

    @Test
    fun `alle søsken må være avdødes barn hvis ikke kast BPBeregningsgrunnlagBrukerUgydligFnr`() {
        val soeskenMedIBeregning: List<GrunnlagMedPeriode<List<SoeskenMedIBeregning>>> =
            listOf(
                GrunnlagMedPeriode(
                    fom = LocalDate.of(2022, 8, 1),
                    tom = null,
                    data =
                        listOf(
                            SoeskenMedIBeregning(HALVSOESKEN_ANNEN_FORELDER, true),
                        ),
                ),
            )
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        val behandlingId = randomUUID()
        val brukertokeninfo = BrukerTokenInfo.of("token", "s1", null, null, null)

        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(behandlingId, brukertokeninfo) } returns hentOpplysningsgrunnlag
        assertThrows<BPBeregningsgrunnlagSoeskenIkkeAvdoedesBarnException> {
            runBlocking {
                beregningsGrunnlagService.lagreBarnepensjonBeregningsGrunnlag(
                    behandlingId,
                    BarnepensjonBeregningsGrunnlag(soeskenMedIBeregning, null),
                    brukertokeninfo,
                )
            }
        }
    }

    @Test
    fun `kan ikke lagre beregningsgrunnlag hvis et søsken har dødsdato og er død`() {
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
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        val behandlingId = randomUUID()
        val brukertokeninfo = BrukerTokenInfo.of("token", "s1", null, null, null)

        val hentOpplysningsgrunnlag = GrunnlagTestData().hentGrunnlagMedEgneAvdoedesBarn()

        coEvery { grunnlagKlient.hentGrunnlag(behandlingId, brukertokeninfo) } returns hentOpplysningsgrunnlag
        assertThrows<BPBeregningsgrunnlagSoeskenMarkertDoedException> {
            runBlocking {
                beregningsGrunnlagService.lagreBarnepensjonBeregningsGrunnlag(
                    behandlingId,
                    BarnepensjonBeregningsGrunnlag(soeskenMedIBeregning, null),
                    brukertokeninfo,
                )
            }
        }
    }

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
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns hentOpplysningsgrunnlag
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

        val soesken = listOf(HELSOESKEN_FOEDSELSNUMMER, HALVSOESKEN_FOEDSELSNUMMER)

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
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns hentOpplysningsgrunnlag
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

        val soesken = listOf(HELSOESKEN_FOEDSELSNUMMER, HALVSOESKEN_FOEDSELSNUMMER)

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
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns hentOpplysningsgrunnlag
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
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns hentOpplysningsgrunnlag
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
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns hentOpplysningsgrunnlag
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
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns hentOpplysningsgrunnlag
        runBlocking {
            assertThrows<RuntimeException> {
                beregningsGrunnlagService.dupliserBeregningsGrunnlagBP(omregningsId, behandlingsId)

                verify(exactly = 0) { beregningsGrunnlagRepository.lagre(any()) }
            }
        }
    }

    @Test
    fun `skal lagre beregningsmetode BP etter reformtidspunkt`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON, randomUUID())
        val slot = slot<BeregningsGrunnlag>()

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnBarnepensjonGrunnlagForBehandling(any()) } returns null
        every { beregningsGrunnlagRepository.lagre(capture(slot)) } returns true
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns hentOpplysningsgrunnlag
        runBlocking {
            beregningsGrunnlagService.lagreBarnepensjonBeregningsGrunnlag(
                randomUUID(),
                BarnepensjonBeregningsGrunnlag(
                    emptyList(),
                    emptyList(),
                    BeregningsMetodeBeregningsgrunnlag(BeregningsMetode.BEST),
                ),
                mockk {
                    every { ident() } returns "Z123456"
                },
            )

            assertEquals(BeregningsMetode.BEST, slot.captured.beregningsMetode.beregningsMetode)

            verify(exactly = 1) { beregningsGrunnlagRepository.lagre(any()) }
        }
    }

    @Test
    fun `skal lagre beregningsmetode BP før reform, må ha med søskenperioder`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON, randomUUID(), virkningstidspunktdato = YearMonth.of(2023, 11))
        val slot = slot<BeregningsGrunnlag>()

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnBarnepensjonGrunnlagForBehandling(any()) } returns null
        every { beregningsGrunnlagRepository.lagre(capture(slot)) } returns true
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns hentOpplysningsgrunnlag

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

        runBlocking {
            beregningsGrunnlagService.lagreBarnepensjonBeregningsGrunnlag(
                randomUUID(),
                BarnepensjonBeregningsGrunnlag(
                    soeskenMedIBeregning,
                    emptyList(),
                    BeregningsMetodeBeregningsgrunnlag(BeregningsMetode.BEST),
                ),
                mockk {
                    every { ident() } returns "Z123456"
                },
            )

            assertEquals(BeregningsMetode.BEST, slot.captured.beregningsMetode.beregningsMetode)

            verify(exactly = 1) { beregningsGrunnlagRepository.lagre(any()) }
        }
    }

    @Test
    fun `skal lagre beregningsmetode BP før reform uten søsken kaster feil VirkningstidsErFoerReformMenManglerSoeskenjustering`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON, randomUUID(), virkningstidspunktdato = YearMonth.of(2023, 11))
        val slot = slot<BeregningsGrunnlag>()

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnBarnepensjonGrunnlagForBehandling(any()) } returns null
        every { beregningsGrunnlagRepository.lagre(capture(slot)) } returns true
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns hentOpplysningsgrunnlag

        assertThrows<VirkningstidspunktBPErFoerReformMenManglerSoeskenjustering> {
            runBlocking {
                beregningsGrunnlagService.lagreBarnepensjonBeregningsGrunnlag(
                    randomUUID(),
                    BarnepensjonBeregningsGrunnlag(
                        emptyList(),
                        emptyList(),
                        BeregningsMetodeBeregningsgrunnlag(BeregningsMetode.BEST),
                    ),
                    mockk {
                        every { ident() } returns "Z123456"
                    },
                )
            }
        }
    }

    @Test
    fun `skal hente overstyr beregning grunnlag`() {
        val behandlingId = randomUUID()

        every { beregningsGrunnlagRepository.finnOverstyrBeregningGrunnlagForBehandling(behandlingId) } returns
            listOf(
                OverstyrBeregningGrunnlagDao(
                    id = randomUUID(),
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.now().minusYears(12L),
                    datoTOM = LocalDate.now().minusYears(6L),
                    utbetaltBeloep = 123L,
                    trygdetid = 10L,
                    prorataBroekTeller = null,
                    prorataBroekNevner = null,
                    sakId = 1L,
                    beskrivelse = "test periode 1",
                    kilde =
                        mockk {
                            every { ident } returns "Z123456"
                            every { tidspunkt } returns Tidspunkt.now()
                            every { type } returns ""
                        },
                ),
                OverstyrBeregningGrunnlagDao(
                    id = randomUUID(),
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.now().minusYears(6L),
                    datoTOM = null,
                    utbetaltBeloep = 456L,
                    trygdetid = 20L,
                    prorataBroekTeller = null,
                    prorataBroekNevner = null,
                    sakId = 1L,
                    beskrivelse = "test periode 2",
                    kilde =
                        mockk {
                            every { ident } returns "Z123456"
                            every { tidspunkt } returns Tidspunkt.now()
                            every { type } returns ""
                        },
                ),
            )

        val grunnlag = beregningsGrunnlagService.hentOverstyrBeregningGrunnlag(behandlingId)

        grunnlag.perioder.let { perioder ->
            perioder.size shouldBe 2
            perioder.minBy { it.fom }.let { periode ->
                periode.fom shouldBe LocalDate.now().minusYears(12L)
                periode.tom shouldBe LocalDate.now().minusYears(6L)
                periode.data.utbetaltBeloep shouldBe 123L
                periode.data.trygdetid shouldBe 10L
            }
            perioder.maxBy { it.fom }.let { periode ->
                periode.fom shouldBe LocalDate.now().minusYears(6L)
                periode.tom shouldBe null
                periode.data.utbetaltBeloep shouldBe 456L
                periode.data.trygdetid shouldBe 20L
            }
        }

        verify(exactly = 1) {
            beregningsGrunnlagRepository.finnOverstyrBeregningGrunnlagForBehandling(behandlingId)
        }
    }

    @Test
    fun `skal lagre overstyr beregning grunnlag`() {
        val behandlingId = randomUUID()

        val slot = slot<List<OverstyrBeregningGrunnlagDao>>()

        val fom = LocalDate.now().minusYears(12)
        val tom = LocalDate.now().minusYears(6)

        coEvery {
            behandlingKlient.hentBehandling(any(), any())
        } returns mockBehandling(SakType.BARNEPENSJON, randomUUID(), BehandlingType.FØRSTEGANGSBEHANDLING, 3L)

        every {
            beregningsGrunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(
                behandlingId,
                capture(slot),
            )
        } just runs
        every { beregningsGrunnlagRepository.finnOverstyrBeregningGrunnlagForBehandling(behandlingId) } returns emptyList()

        runBlocking {
            beregningsGrunnlagService.lagreOverstyrBeregningGrunnlag(
                behandlingId = behandlingId,
                data =
                    OverstyrBeregningGrunnlagDTO(
                        perioder =
                            listOf(
                                GrunnlagMedPeriode(
                                    data =
                                        OverstyrBeregningGrunnlagData(
                                            utbetaltBeloep = 12L,
                                            trygdetid = 25L,
                                            prorataBroekTeller = null,
                                            prorataBroekNevner = null,
                                            beskrivelse = "test periode 1",
                                        ),
                                    fom = fom,
                                    tom = tom,
                                ),
                            ),
                    ),
                brukerTokenInfo =
                    mockk {
                        every { ident() } returns "Z123456"
                    },
            )

            slot.captured.first().let { dao ->
                dao.behandlingId shouldBe behandlingId
                dao.datoFOM shouldBe fom
                dao.datoTOM shouldBe tom
                dao.utbetaltBeloep shouldBe 12L
                dao.trygdetid shouldBe 25L
                dao.sakId shouldBe 3L
            }

            verify(exactly = 1) {
                beregningsGrunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(behandlingId, any())
            }
        }
    }

    private fun mockBehandling(
        type: SakType,
        uuid: UUID,
        behandlingstype: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        sakId: Long = 1L,
        virkningstidspunktdato: YearMonth = REFORM_TIDSPUNKT_BP,
    ): DetaljertBehandling =
        mockk<DetaljertBehandling>().apply {
            every { id } returns uuid
            every { sak } returns sakId
            every { sakType } returns type
            every { behandlingType } returns behandlingstype
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt(virkningstidspunktdato)
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
