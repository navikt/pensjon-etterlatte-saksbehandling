package no.nav.etterlatte.beregning.grunnlag

import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
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
import no.nav.etterlatte.beregning.BeregningRepository
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.beregning.regler.toGrunnlag
import no.nav.etterlatte.klienter.BehandlingKlientImpl
import no.nav.etterlatte.klienter.GrunnlagKlient
import no.nav.etterlatte.klienter.VedtaksvurderingKlientImpl
import no.nav.etterlatte.ktor.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.BeregningsMetodeBeregningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.HALVSOESKEN_ANNEN_FORELDER
import no.nav.etterlatte.libs.testdata.grunnlag.HALVSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN_FOEDSELSNUMMER
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.Month.APRIL
import java.time.Month.JANUARY
import java.time.Month.JULY
import java.time.Month.JUNE
import java.time.Month.MARCH
import java.time.Month.MAY
import java.time.YearMonth
import java.util.UUID
import java.util.UUID.randomUUID

internal class BeregningsGrunnlagServiceTest {
    private val behandlingKlient = mockk<BehandlingKlientImpl>()
    private val vedtaksvurderingKlient = mockk<VedtaksvurderingKlientImpl>()
    private val beregningsGrunnlagRepository = mockk<BeregningsGrunnlagRepository>()
    private val beregningRepository = mockk<BeregningRepository>()
    private val grunnlagKlient = mockk<GrunnlagKlient>()
    private val beregningsGrunnlagService: BeregningsGrunnlagService =
        BeregningsGrunnlagService(
            beregningsGrunnlagRepository,
            beregningRepository,
            behandlingKlient,
            vedtaksvurderingKlient,
            grunnlagKlient,
        )

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
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns
            mockk {
                coEvery { sakType } returns SakType.BARNEPENSJON
            }
        val behandlingId = randomUUID()
        val brukertokeninfo = simpleSaksbehandler()

        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(behandlingId, brukertokeninfo) } returns hentOpplysningsgrunnlag
        assertThrows<BPBeregningsgrunnlagSoeskenIkkeAvdoedesBarnException> {
            runBlocking {
                beregningsGrunnlagService.lagreBeregningsGrunnlag(
                    behandlingId,
                    LagreBeregningsGrunnlag(soeskenMedIBeregning, null),
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
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns
            mockk {
                coEvery { sakType } returns SakType.BARNEPENSJON
            }
        val behandlingId = randomUUID()
        val brukertokeninfo = simpleSaksbehandler()

        val hentOpplysningsgrunnlag = GrunnlagTestData().hentGrunnlagMedEgneAvdoedesBarn()

        coEvery { grunnlagKlient.hentGrunnlag(behandlingId, brukertokeninfo) } returns hentOpplysningsgrunnlag
        assertThrows<BPBeregningsgrunnlagSoeskenMarkertDoedException> {
            runBlocking {
                beregningsGrunnlagService.lagreBeregningsGrunnlag(
                    behandlingId,
                    LagreBeregningsGrunnlag(soeskenMedIBeregning, null),
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
        every { beregningsGrunnlagRepository.finnBeregningsGrunnlag(any()) } returns null
        every { beregningsGrunnlagRepository.lagreBeregningsGrunnlag(any()) } returns true
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns hentOpplysningsgrunnlag
        runBlocking {
            beregningsGrunnlagService.lagreBeregningsGrunnlag(
                randomUUID(),
                LagreBeregningsGrunnlag(soeskenMedIBeregning, institusjonsoppholdBeregningsgrunnlag),
                mockk {
                    every { ident() } returns "Z123456"
                },
            )

            verify(exactly = 1) { beregningsGrunnlagRepository.lagreBeregningsGrunnlag(any()) }
        }
    }

    @Test
    fun `skal ikke tillate endringer i beregningsgrunnlaget før virk på revurdering`() {
        val sakId = 1337L
        val foerstegangsbehandling = mockBehandling(type = SakType.BARNEPENSJON, uuid = randomUUID(), sakId = sakId)

        val virk = YearMonth.of(2023, JANUARY)
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
            vedtaksvurderingKlient.hentIverksatteVedtak(sakId, any())
        } returns listOf(mockVedtak(foerstegangsbehandling.id, VedtakType.INNVILGELSE))
        coEvery { behandlingKlient.hentBehandling(revurdering.id, any()) } returns revurdering

        every {
            beregningsGrunnlagRepository.finnBeregningsGrunnlag(foerstegangsbehandling.id)
        } returns grunnlagIverksatt
        every {
            beregningsGrunnlagRepository.finnBeregningsGrunnlag(revurdering.id)
        } returns grunnlagEndring
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns hentOpplysningsgrunnlag
        runBlocking {
            val lagret =
                beregningsGrunnlagService.lagreBeregningsGrunnlag(
                    behandlingId = revurdering.id,
                    beregningsGrunnlag =
                        LagreBeregningsGrunnlag(
                            soeskenMedIBeregning = grunnlagEndring.soeskenMedIBeregning,
                            institusjonsopphold = grunnlagEndring.institusjonsoppholdBeregningsgrunnlag,
                        ),
                    brukerTokenInfo = mockk(relaxed = true),
                )
            assertFalse(lagret)
        }

        coVerify(exactly = 0) { beregningsGrunnlagRepository.lagreBeregningsGrunnlag(any()) }
    }

    @Test
    fun `skal tillate endringer i beregningsgrunnlaget etter virk på revurdering`() {
        val sakId = 1337L
        val foerstegangsbehandling = mockBehandling(type = SakType.BARNEPENSJON, uuid = randomUUID(), sakId = sakId)

        val virk = YearMonth.of(2023, JANUARY)
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
            vedtaksvurderingKlient.hentIverksatteVedtak(sakId, any())
        } returns listOf(mockVedtak(foerstegangsbehandling.id, VedtakType.INNVILGELSE))
        coEvery { behandlingKlient.hentBehandling(revurdering.id, any()) } returns revurdering

        every {
            beregningsGrunnlagRepository.finnBeregningsGrunnlag(foerstegangsbehandling.id)
        } returns grunnlagIverksatt
        every {
            beregningsGrunnlagRepository.finnBeregningsGrunnlag(revurdering.id)
        } returns grunnlagEndring
        every { beregningsGrunnlagRepository.lagreBeregningsGrunnlag(any()) } returns true
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns hentOpplysningsgrunnlag
        runBlocking {
            val lagret =
                beregningsGrunnlagService.lagreBeregningsGrunnlag(
                    behandlingId = revurdering.id,
                    beregningsGrunnlag =
                        LagreBeregningsGrunnlag(
                            soeskenMedIBeregning = grunnlagEndring.soeskenMedIBeregning,
                            institusjonsopphold = grunnlagEndring.institusjonsoppholdBeregningsgrunnlag,
                        ),
                    brukerTokenInfo = mockk(relaxed = true),
                )
            assertTrue(lagret)
        }

        coVerify(exactly = 1) { beregningsGrunnlagRepository.lagreBeregningsGrunnlag(any()) }
    }

    @Test
    fun `skal lage et kopi av grunnlaget`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON, randomUUID())

        val omregningsId = randomUUID()
        val behandlingsId = randomUUID()

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnBeregningsGrunnlag(omregningsId) } returns null
        every { beregningsGrunnlagRepository.finnOverstyrBeregningGrunnlagForBehandling(any()) } returns emptyList()
        every {
            beregningsGrunnlagRepository.finnBeregningsGrunnlag(behandlingsId)
        } returns
            BeregningsGrunnlag(
                behandlingsId,
                Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
                emptyList(),
                BeregningsMetode.BEST.toGrunnlag(),
                emptyList(),
            )

        every { beregningsGrunnlagRepository.lagreBeregningsGrunnlag(any()) } returns true
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns hentOpplysningsgrunnlag
        runBlocking {
            beregningsGrunnlagService.dupliserBeregningsGrunnlag(omregningsId, behandlingsId, bruker)

            verify(exactly = 1) { beregningsGrunnlagRepository.lagreBeregningsGrunnlag(any()) }
            verify(exactly = 0) { beregningsGrunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(any(), any()) }
        }
    }

    @Test
    fun `skal lage et kopi av grunnlaget med overstyrt`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON, randomUUID())

        val omregningsId = randomUUID()
        val behandlingsId = randomUUID()

        val overstyrBeregningGrunnlagDao = mockk<OverstyrBeregningGrunnlagDao>()

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnBeregningsGrunnlag(omregningsId) } returns null
        every {
            beregningsGrunnlagRepository.finnOverstyrBeregningGrunnlagForBehandling(
                any(),
            )
        } returns listOf(overstyrBeregningGrunnlagDao)
        every { beregningsGrunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(any(), any()) } just runs
        every { overstyrBeregningGrunnlagDao.copy(any(), any()) } returns overstyrBeregningGrunnlagDao
        every {
            beregningsGrunnlagRepository.finnBeregningsGrunnlag(behandlingsId)
        } returns
            BeregningsGrunnlag(
                behandlingsId,
                Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
                emptyList(),
                BeregningsMetode.BEST.toGrunnlag(),
                emptyList(),
            )

        every { beregningsGrunnlagRepository.lagreBeregningsGrunnlag(any()) } returns true
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns hentOpplysningsgrunnlag
        runBlocking {
            beregningsGrunnlagService.dupliserBeregningsGrunnlag(omregningsId, behandlingsId, bruker)

            verify(exactly = 1) { beregningsGrunnlagRepository.lagreBeregningsGrunnlag(any()) }
            verify(exactly = 1) { beregningsGrunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(any(), any()) }
        }
    }

    @Test
    fun `kan ikke lage et kopi av grunnlaget hvis forrige mangler`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON, randomUUID())

        val behandlingsId = randomUUID()

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnBeregningsGrunnlag(behandlingsId) } returns null
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns hentOpplysningsgrunnlag
        runBlocking {
            assertThrows<RuntimeException> {
                beregningsGrunnlagService.dupliserBeregningsGrunnlag(randomUUID(), behandlingsId, bruker)

                verify(exactly = 0) { beregningsGrunnlagRepository.lagreBeregningsGrunnlag(any()) }
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
        every { beregningsGrunnlagRepository.finnBeregningsGrunnlag(any()) } returns
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
                beregningsGrunnlagService.dupliserBeregningsGrunnlag(omregningsId, behandlingsId, bruker)

                verify(exactly = 0) { beregningsGrunnlagRepository.lagreBeregningsGrunnlag(any()) }
            }
        }
    }

    @Test
    fun `skal lagre beregningsmetode BP etter reformtidspunkt`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON, randomUUID())
        val slot = slot<BeregningsGrunnlag>()

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnBeregningsGrunnlag(any()) } returns null
        every { beregningsGrunnlagRepository.lagreBeregningsGrunnlag(capture(slot)) } returns true
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns hentOpplysningsgrunnlag
        runBlocking {
            beregningsGrunnlagService.lagreBeregningsGrunnlag(
                randomUUID(),
                LagreBeregningsGrunnlag(
                    emptyList(),
                    emptyList(),
                    BeregningsMetodeBeregningsgrunnlag(BeregningsMetode.BEST),
                ),
                mockk {
                    every { ident() } returns "Z123456"
                },
            )

            assertEquals(BeregningsMetode.BEST, slot.captured.beregningsMetode.beregningsMetode)

            verify(exactly = 1) { beregningsGrunnlagRepository.lagreBeregningsGrunnlag(any()) }
        }
    }

    @Test
    fun `skal lagre beregningsmetode BP før reform, må ha med søskenperioder`() {
        val behandling =
            mockBehandling(SakType.BARNEPENSJON, randomUUID(), virkningstidspunktdato = YearMonth.of(2023, 11))
        val slot = slot<BeregningsGrunnlag>()

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnBeregningsGrunnlag(any()) } returns null
        every { beregningsGrunnlagRepository.lagreBeregningsGrunnlag(capture(slot)) } returns true
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
            beregningsGrunnlagService.lagreBeregningsGrunnlag(
                randomUUID(),
                LagreBeregningsGrunnlag(
                    soeskenMedIBeregning,
                    emptyList(),
                    BeregningsMetodeBeregningsgrunnlag(BeregningsMetode.BEST),
                ),
                mockk {
                    every { ident() } returns "Z123456"
                },
            )

            assertEquals(BeregningsMetode.BEST, slot.captured.beregningsMetode.beregningsMetode)

            verify(exactly = 1) { beregningsGrunnlagRepository.lagreBeregningsGrunnlag(any()) }
        }
    }

    @Test
    fun `skal lagre beregningsmetode BP før reform uten søsken kaster feil VirkningstidsErFoerReformMenManglerSoeskenjustering`() {
        val behandling =
            mockBehandling(SakType.BARNEPENSJON, randomUUID(), virkningstidspunktdato = YearMonth.of(2023, 11))
        val slot = slot<BeregningsGrunnlag>()

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        every { beregningsGrunnlagRepository.finnBeregningsGrunnlag(any()) } returns null
        every { beregningsGrunnlagRepository.lagreBeregningsGrunnlag(capture(slot)) } returns true
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns hentOpplysningsgrunnlag

        runBlocking {
            beregningsGrunnlagService.lagreBeregningsGrunnlag(
                randomUUID(),
                LagreBeregningsGrunnlag(
                    emptyList(),
                    emptyList(),
                    BeregningsMetodeBeregningsgrunnlag(BeregningsMetode.BEST),
                ),
                mockk {
                    every { ident() } returns "Z123456"
                },
            )
        }
        assertEquals(
            listOf(GrunnlagMedPeriode<List<SoeskenMedIBeregning>>(emptyList(), behandling.virkningstidspunkt?.dato?.atDay(1)!!, null)),
            slot.captured.soeskenMedIBeregning,
        )
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
                    trygdetidForIdent = null,
                    prorataBroekTeller = null,
                    prorataBroekNevner = null,
                    sakId = 1L,
                    beskrivelse = "test periode 1",
                    aarsak = "ANNET",
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
                    trygdetidForIdent = null,
                    prorataBroekTeller = null,
                    prorataBroekNevner = null,
                    sakId = 1L,
                    beskrivelse = "test periode 2",
                    aarsak = "ANNET",
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
                                            trygdetidForIdent = null,
                                            prorataBroekTeller = null,
                                            prorataBroekNevner = null,
                                            beskrivelse = "test periode 1",
                                            aarsak = "ANNET",
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

    @Test
    fun `reguler overstyrt beregningsgrunnlag til sak med utbetalingsperioder før reguleringsmåned`() {
        val behandlingId = UUID.randomUUID()
        val behandling = mockk<DetaljertBehandling>()
        every { beregningsGrunnlagRepository.finnOverstyrBeregningGrunnlagForBehandling(behandlingId) } returns
            listOf(
                overstyrtBeregningsgrunnlag(
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.of(2023, JANUARY, 1),
                    datoTOM = LocalDate.of(2023, MARCH, 31),
                    utbetaltBeloep = 1000,
                ),
                overstyrtBeregningsgrunnlag(
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.of(2023, APRIL, 1),
                    utbetaltBeloep = 2000,
                ),
            )
        coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling
        every { behandling.virkningstidspunkt() } returns
            Virkningstidspunkt(
                dato = YearMonth.of(2023, MAY),
                kilde = Grunnlagsopplysning.Saksbehandler.create(""),
                begrunnelse = "",
            )
        every { beregningsGrunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(any(), any()) } returns Unit

        beregningsGrunnlagService.tilpassOverstyrtBeregningsgrunnlagForRegulering(
            behandlingId = behandlingId,
            bruker,
        )

        val forventet =
            listOf(
                overstyrtBeregningsgrunnlag(
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.of(2023, JANUARY, 1),
                    datoTOM = LocalDate.of(2023, MARCH, 31),
                    utbetaltBeloep = 1000,
                ),
                overstyrtBeregningsgrunnlag(
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.of(2023, APRIL, 1),
                    datoTOM = LocalDate.of(2023, APRIL, 30),
                    utbetaltBeloep = 2000,
                ),
                overstyrtBeregningsgrunnlag(
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.of(2023, MAY, 1),
                    utbetaltBeloep = 2128,
                ),
            )
        verify {
            beregningsGrunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(
                behandlingId,
                withArg {
                    it.size shouldBe 3
                    it[0].shouldBeEqualToIgnoringFields(
                        forventet[0],
                        OverstyrBeregningGrunnlagDao::id,
                        OverstyrBeregningGrunnlagDao::kilde,
                    )
                    it[1].shouldBeEqualToIgnoringFields(
                        forventet[1],
                        OverstyrBeregningGrunnlagDao::id,
                        OverstyrBeregningGrunnlagDao::kilde,
                    )
                    it[2].shouldBeEqualToIgnoringFields(
                        forventet[2],
                        OverstyrBeregningGrunnlagDao::id,
                        OverstyrBeregningGrunnlagDao::kilde,
                        OverstyrBeregningGrunnlagDao::reguleringRegelresultat,
                        OverstyrBeregningGrunnlagDao::beskrivelse,
                    )
                },
            )
        }
    }

    @Test
    fun `reguler overstyrt beregningsgrunnlag til sak med utbetalingsperioder etter reguleringsmåned`() {
        val behandlingId = UUID.randomUUID()
        val behandling = mockk<DetaljertBehandling>()
        every { beregningsGrunnlagRepository.finnOverstyrBeregningGrunnlagForBehandling(behandlingId) } returns
            listOf(
                overstyrtBeregningsgrunnlag(
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.of(2023, JUNE, 1),
                    utbetaltBeloep = 2000,
                    beskrivelse = "gammel beskrivelse",
                ),
            )
        coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling
        every { behandling.virkningstidspunkt() } returns
            Virkningstidspunkt(
                dato = YearMonth.of(2023, JUNE),
                kilde = Grunnlagsopplysning.Saksbehandler.create(""),
                begrunnelse = "",
            )
        every { beregningsGrunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(any(), any()) } returns Unit

        beregningsGrunnlagService.tilpassOverstyrtBeregningsgrunnlagForRegulering(
            behandlingId = behandlingId,
            bruker,
        )

        val forventet =
            overstyrtBeregningsgrunnlag(
                behandlingId = behandlingId,
                datoFOM = LocalDate.of(2023, JUNE, 1),
                datoTOM = null,
                utbetaltBeloep = 2128,
                beskrivelse = "gammel beskrivelse",
            )
        verify {
            beregningsGrunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(
                behandlingId,
                withArg {
                    it.size shouldBe 1
                    it[0].shouldBeEqualToIgnoringFields(
                        forventet,
                        OverstyrBeregningGrunnlagDao::id,
                        OverstyrBeregningGrunnlagDao::kilde,
                        OverstyrBeregningGrunnlagDao::reguleringRegelresultat,
                    )
                },
            )
        }
    }

    @Test
    fun `reguler overstyrt beregningsgrunnlag til sak med utbetalingsperuoder før og etter reguleringsmåned`() {
        val behandlingId = UUID.randomUUID()
        val behandling = mockk<DetaljertBehandling>()
        every { beregningsGrunnlagRepository.finnOverstyrBeregningGrunnlagForBehandling(behandlingId) } returns
            listOf(
                overstyrtBeregningsgrunnlag(
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.of(2023, APRIL, 1),
                    datoTOM = LocalDate.of(2023, JUNE, 30),
                    utbetaltBeloep = 2000,
                    beskrivelse = "gammel beskrivelse",
                ),
                overstyrtBeregningsgrunnlag(
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.of(2023, JULY, 1),
                    utbetaltBeloep = 3000,
                    beskrivelse = "gammel beskrivelse",
                ),
            )
        coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling
        every { behandling.virkningstidspunkt() } returns
            Virkningstidspunkt(
                dato = YearMonth.of(2023, APRIL),
                kilde = Grunnlagsopplysning.Saksbehandler.create(""),
                begrunnelse = "",
            )
        every { beregningsGrunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(any(), any()) } returns Unit

        beregningsGrunnlagService.tilpassOverstyrtBeregningsgrunnlagForRegulering(
            behandlingId = behandlingId,
            bruker,
        )

        val forventet =
            listOf(
                overstyrtBeregningsgrunnlag(
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.of(2023, APRIL, 1),
                    datoTOM = LocalDate.of(2023, APRIL, 30),
                    utbetaltBeloep = 2000,
                    beskrivelse = "gammel beskrivelse",
                ),
                overstyrtBeregningsgrunnlag(
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.of(2023, MAY, 1),
                    datoTOM = LocalDate.of(2023, JUNE, 30),
                    utbetaltBeloep = 2128,
                    beskrivelse = "gammel beskrivelse",
                ),
                overstyrtBeregningsgrunnlag(
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.of(2023, JULY, 1),
                    datoTOM = null,
                    utbetaltBeloep = 3192,
                    beskrivelse = "gammel beskrivelse",
                ),
            )
        verify {
            beregningsGrunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(
                behandlingId,
                withArg {
                    it.size shouldBe 3
                    it[0].shouldBeEqualToIgnoringFields(
                        forventet[0],
                        OverstyrBeregningGrunnlagDao::id,
                        OverstyrBeregningGrunnlagDao::kilde,
                        OverstyrBeregningGrunnlagDao::reguleringRegelresultat,
                    )
                    it[1].shouldBeEqualToIgnoringFields(
                        forventet[1],
                        OverstyrBeregningGrunnlagDao::id,
                        OverstyrBeregningGrunnlagDao::kilde,
                        OverstyrBeregningGrunnlagDao::reguleringRegelresultat,
                    )
                    it[2].shouldBeEqualToIgnoringFields(
                        forventet[2],
                        OverstyrBeregningGrunnlagDao::id,
                        OverstyrBeregningGrunnlagDao::kilde,
                        OverstyrBeregningGrunnlagDao::reguleringRegelresultat,
                    )
                },
            )
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
    ): BeregningsGrunnlag =
        BeregningsGrunnlag(
            behandlingId = behandlingId,
            kilde = kilde,
            soeskenMedIBeregning = soeskenMedIBeregning,
            institusjonsoppholdBeregningsgrunnlag = institusjonsoppholdBeregningsgrunnlag,
            beregningsMetode = BeregningsMetode.NASJONAL.toGrunnlag(),
        )

    private fun mockVedtak(
        behandlingId: UUID,
        type: VedtakType,
    ) = VedtakSammendragDto(randomUUID().toString(), behandlingId, type, null, null, null, null, null, null)

    private fun overstyrtBeregningsgrunnlag(
        behandlingId: UUID = UUID.randomUUID(),
        utbetaltBeloep: Long = 0L,
        datoFOM: LocalDate,
        datoTOM: LocalDate? = null,
        beskrivelse: String = "",
    ) = OverstyrBeregningGrunnlagDao(
        id = UUID.randomUUID(),
        behandlingId = behandlingId,
        datoFOM = datoFOM,
        datoTOM = datoTOM,
        utbetaltBeloep = utbetaltBeloep,
        trygdetid = 0,
        trygdetidForIdent = "",
        prorataBroekTeller = null,
        prorataBroekNevner = null,
        sakId = 123L,
        beskrivelse = beskrivelse,
        aarsak = "ANNET",
        kilde = Grunnlagsopplysning.Saksbehandler.create(""),
    )
}
