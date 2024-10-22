package no.nav.etterlatte.vilkaarsvurdering

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.behandling.BehandlingStatusServiceImpl
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.behandling.sakId2
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SOEKNAD_MOTTATT_DATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadMottattDato
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Delvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Lovreferanse
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarTypeOgUtfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VurdertVilkaar
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.mockSaksbehandler
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.vilkaarsvurdering.dao.VilkaarsvurderingRepositoryWrapperDatabase
import no.nav.etterlatte.vilkaarsvurdering.ektedao.DelvilkaarRepository
import no.nav.etterlatte.vilkaarsvurdering.ektedao.VilkaarsvurderingRepository
import no.nav.etterlatte.vilkaarsvurdering.service.BehandlingstilstandException
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingManglerResultat
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService
import no.nav.etterlatte.vilkaarsvurdering.service.VirkningstidspunktSamsvarerIkke
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.BarnepensjonVilkaar2024
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VilkaarsvurderingServiceTest(
    private val ds: DataSource,
) {
    companion object {
        @RegisterExtension
        val dbExtension = DatabaseExtension()
    }

    private lateinit var vilkaarsvurderingServiceImpl: VilkaarsvurderingService
    private lateinit var repository: VilkaarsvurderingRepository
    private val behandlingService = mockk<BehandlingService>()
    private val behandlingStatus: BehandlingStatusService = mockk<BehandlingStatusServiceImpl>()
    private val grunnlagKlient = mockk<GrunnlagKlient>()
    private val behandlingId: UUID = UUID.randomUUID()

    private val brukerTokenInfo = simpleSaksbehandler()
    private val grunnlagMock: Grunnlag = mockk()

    @BeforeAll
    fun beforeAll() {
        val metadataMock = mockk<Metadata>()
        every { grunnlagMock.metadata } returns metadataMock
        every { metadataMock.versjon } returns 1
        val saksbehandler = mockSaksbehandler("User")
        nyKontekstMedBrukerOgDatabase(saksbehandler, ds)
    }

    @BeforeEach
    fun beforeEach() {
        coEvery {
            grunnlagKlient.hentGrunnlagForBehandling(
                any(),
                any(),
            )
        } returns GrunnlagTestData().hentOpplysningsgrunnlag()
        every { behandlingStatus.settVilkaarsvurdert(any(), any()) } just Runs
        coEvery { behandlingService.hentBehandling(any()) } returns
            mockk<Behandling>().apply {
                every { id } returns UUID.randomUUID()
                every { sak.id } returns sakId1
                every { sak.sakType } returns SakType.BARNEPENSJON
                every { type } returns BehandlingType.FØRSTEGANGSBEHANDLING
                every { prosesstype } returns Prosesstype.MANUELL
                every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt()
                every { revurderingsaarsak() } returns null
            }

        repository = VilkaarsvurderingRepository(ConnectionAutoclosingTest(ds), DelvilkaarRepository())
        vilkaarsvurderingServiceImpl =
            VilkaarsvurderingService(
                VilkaarsvurderingRepositoryWrapperDatabase(
                    VilkaarsvurderingRepository(ConnectionAutoclosingTest(ds), DelvilkaarRepository()),
                ),
                behandlingService,
                grunnlagKlient,
                behandlingStatus,
            )
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
        dbExtension.resetDb()
    }

    @Test
    fun `Skal opprette vilkaarsvurdering for foerstegangsbehandling barnepensjon med grunnlagsopplysninger`() {
        val (vilkaarsvurdering) =
            runBlocking {
                vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(behandlingId, brukerTokenInfo)
            }

        vilkaarsvurdering shouldNotBe null
        vilkaarsvurdering.behandlingId shouldBe behandlingId
        vilkaarsvurdering.vilkaar.map { it.hovedvilkaar.type } shouldContainExactly
            listOf(
                VilkaarType.BP_FORMAAL_2024,
                VilkaarType.BP_DOEDSFALL_FORELDER_2024,
                VilkaarType.BP_YRKESSKADE_AVDOED_2024,
                VilkaarType.BP_ALDER_BARN_2024,
                VilkaarType.BP_FORTSATT_MEDLEMSKAP_2024,
                VilkaarType.BP_VURDERING_AV_EKSPORT_2024,
                VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_2024,
                VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_EOES_2024,
            )
    }

    @Test
    fun `Ny vilkaarsvurdering for BP med virk fom 1-1-2024 skal ha vilkaar etter nytt regelverk`() {
        coEvery { behandlingService.hentBehandling(any()) } returns
            mockk<Behandling>().apply {
                every { id } returns UUID.randomUUID()
                every { sak.id } returns sakId1
                every { sak.sakType } returns SakType.BARNEPENSJON
                every { type } returns BehandlingType.FØRSTEGANGSBEHANDLING
                every { virkningstidspunkt } returns
                    VirkningstidspunktTestData
                        .virkningstidsunkt()
                        .copy(dato = YearMonth.of(2024, 1))
                every { revurderingsaarsak() } returns null
            }

        val (vilkaarsvurdering) =
            runBlocking {
                vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(behandlingId, brukerTokenInfo)
            }

        vilkaarsvurdering shouldNotBe null
        vilkaarsvurdering.behandlingId shouldBe behandlingId
        vilkaarsvurdering.vilkaar.map { it.hovedvilkaar.type } shouldContainExactly
            listOf(
                VilkaarType.BP_FORMAAL_2024,
                VilkaarType.BP_DOEDSFALL_FORELDER_2024,
                VilkaarType.BP_YRKESSKADE_AVDOED_2024,
                VilkaarType.BP_ALDER_BARN_2024,
                VilkaarType.BP_FORTSATT_MEDLEMSKAP_2024,
                VilkaarType.BP_VURDERING_AV_EKSPORT_2024,
                VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_2024,
                VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_EOES_2024,
            )
    }

    @Test
    fun `Skal slette vilkaarsvurdering`() {
        runBlocking { vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(behandlingId, brukerTokenInfo) }
        vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId) shouldNotBe null
        coEvery { behandlingStatus.settOpprettet(behandlingId, brukerTokenInfo, any()) } just Runs

        runBlocking { vilkaarsvurderingServiceImpl.slettVilkaarsvurdering(behandlingId, brukerTokenInfo) }
        vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId) shouldBe null
    }

    @Test
    fun `Skal opprette vilkaarsvurdering for foerstegangsbehandling omstillingssoeknad med grunnlagsopplysninger`() {
        coEvery { behandlingService.hentBehandling(any()) } returns
            mockk<Behandling>().apply {
                every { id } returns UUID.randomUUID()
                every { sak.id } returns sakId2
                every { sak.sakType } returns SakType.OMSTILLINGSSTOENAD
                every { type } returns BehandlingType.FØRSTEGANGSBEHANDLING
                every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt()
                every { revurderingsaarsak() } returns null
            }

        val soeknadMottattDato = LocalDateTime.now()
        val soeknadMottattDatoOpplysning =
            Opplysning.Konstant(
                UUID.randomUUID(),
                kilde,
                SoeknadMottattDato(mottattDato = soeknadMottattDato).toJsonNode(),
            )
        val grunnlag =
            GrunnlagTestData(
                opplysningsmapSakOverrides = mapOf(SOEKNAD_MOTTATT_DATO to soeknadMottattDatoOpplysning),
            ).hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlagForBehandling(any(), any()) } returns grunnlag

        val (vilkaarsvurdering) = vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(behandlingId, brukerTokenInfo)

        vilkaarsvurdering shouldNotBe null
        vilkaarsvurdering.behandlingId shouldBe behandlingId
        vilkaarsvurdering.vilkaar shouldHaveSize 12
    }

    @Test
    fun `Skal oppdatere en vilkaarsvurdering med vilkaar som har oppfylt hovedvilkaar`() {
        val vilkaarsvurdering = runBlocking { opprettVilkaarsvurdering() }

        val vurdering = vilkaarsVurderingData()
        val vurdertVilkaar =
            VurdertVilkaar(
                vilkaarId = vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.BP_FORTSATT_MEDLEMSKAP_2024)?.id!!,
                hovedvilkaar =
                    VilkaarTypeOgUtfall(
                        type = VilkaarType.BP_FORTSATT_MEDLEMSKAP_2024,
                        resultat = Utfall.OPPFYLT,
                    ),
                vurdering = vurdering,
            )

        runBlocking {
            val vilkaarsvurderingOppdatert =
                vilkaarsvurderingServiceImpl.oppdaterVurderingPaaVilkaar(behandlingId, brukerTokenInfo, vurdertVilkaar)

            vilkaarsvurderingOppdatert shouldNotBe null
            vilkaarsvurderingOppdatert.vilkaar
                .first { it.hovedvilkaar.type == VilkaarType.BP_FORTSATT_MEDLEMSKAP_2024 }
                .let { vilkaar ->
                    vilkaar.hovedvilkaar.resultat shouldBe Utfall.OPPFYLT
                    vilkaar.unntaksvilkaar.forEach {
                        it.resultat shouldBe null
                    }
                    vilkaar.vurdering?.let {
                        it.saksbehandler shouldBe vurdering.saksbehandler
                        it.kommentar shouldBe vurdering.kommentar
                        it.tidspunkt shouldNotBe null
                    }
                }
        }
    }

    @Test
    fun `Skal oppdatere en vilkaarsvurdering med vilkaar som har oppfylt unntaksvilkaar`() {
        val vilkaarsvurdering = runBlocking { opprettVilkaarsvurdering() }

        val vurdering = vilkaarsVurderingData()
        val vurdertVilkaar =
            VurdertVilkaar(
                vilkaarId = vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.BP_FORTSATT_MEDLEMSKAP_2024)?.id!!,
                hovedvilkaar =
                    VilkaarTypeOgUtfall(
                        type = VilkaarType.BP_FORTSATT_MEDLEMSKAP_2024,
                        resultat = Utfall.IKKE_OPPFYLT,
                    ),
                unntaksvilkaar =
                    VilkaarTypeOgUtfall(
                        type = VilkaarType.BP_FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRE_MINST_20_AAR_SAMLET_BOTID_2024,
                        resultat = Utfall.OPPFYLT,
                    ),
                vurdering = vurdering,
            )

        runBlocking {
            val vilkaarsvurderingOppdatert =
                vilkaarsvurderingServiceImpl.oppdaterVurderingPaaVilkaar(
                    behandlingId,
                    brukerTokenInfo,
                    vurdertVilkaar,
                )

            vilkaarsvurderingOppdatert shouldNotBe null
            vilkaarsvurderingOppdatert.vilkaar
                .first { it.hovedvilkaar.type == VilkaarType.BP_FORTSATT_MEDLEMSKAP_2024 }
                .let { vilkaar ->
                    vilkaar.hovedvilkaar.resultat shouldBe Utfall.IKKE_OPPFYLT
                    val unntaksvilkaar =
                        vilkaar.unntaksvilkaar.first { it.type == vurdertVilkaar.unntaksvilkaar?.type }
                    unntaksvilkaar.resultat shouldBe Utfall.OPPFYLT

                    vilkaar.vurdering?.let {
                        it.saksbehandler shouldBe vurdering.saksbehandler
                        it.kommentar shouldBe vurdering.kommentar
                        it.tidspunkt shouldNotBe null
                    }
                }
        }
    }

    @Test
    fun `Skal oppdatere vilkaarsvurdering med vilkaar som ikke har oppfylt hovedvilkaar el unntaksvilkaar`() {
        val vilkaarsvurdering = runBlocking { opprettVilkaarsvurdering() }

        val vurdering = vilkaarsVurderingData()
        val vurdertVilkaar =
            VurdertVilkaar(
                vilkaarId = vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.BP_FORTSATT_MEDLEMSKAP_2024)?.id!!,
                hovedvilkaar =
                    VilkaarTypeOgUtfall(
                        type = VilkaarType.BP_FORTSATT_MEDLEMSKAP_2024,
                        resultat = Utfall.IKKE_OPPFYLT,
                    ),
                vurdering = vurdering,
            )

        runBlocking {
            val vilkaarsvurderingOppdatert =
                vilkaarsvurderingServiceImpl.oppdaterVurderingPaaVilkaar(
                    behandlingId,
                    brukerTokenInfo,
                    vurdertVilkaar,
                )

            vilkaarsvurderingOppdatert shouldNotBe null
            vilkaarsvurderingOppdatert.vilkaar
                .first { it.hovedvilkaar.type == VilkaarType.BP_FORTSATT_MEDLEMSKAP_2024 }
                .let { vilkaar ->
                    vilkaar.hovedvilkaar.resultat shouldBe Utfall.IKKE_OPPFYLT
                    vilkaar.unntaksvilkaar.forEach {
                        it.resultat shouldBe Utfall.IKKE_OPPFYLT
                    }
                    vilkaar.vurdering?.let {
                        it.saksbehandler shouldBe vurdering.saksbehandler
                        it.kommentar shouldBe vurdering.kommentar
                        it.tidspunkt shouldNotBe null
                    }
                }
        }
    }

    @Test
    fun `skal ikke lagre hvis status-sjekk mot behandling feiler`() {
        val vilkaarsvurdering = runBlocking { opprettVilkaarsvurdering() }
        every { behandlingStatus.settVilkaarsvurdert(any(), any(), any()) } throws BehandlingstilstandException()

        val vurdering = vilkaarsVurderingData()
        val vurdertVilkaar =
            VurdertVilkaar(
                vilkaarId = vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.BP_FORTSATT_MEDLEMSKAP_2024)?.id!!,
                hovedvilkaar =
                    VilkaarTypeOgUtfall(
                        type = VilkaarType.BP_FORTSATT_MEDLEMSKAP_2024,
                        resultat = Utfall.IKKE_OPPFYLT,
                    ),
                vurdering = vurdering,
            )

        runBlocking {
            assertThrows<BehandlingstilstandException> {
                vilkaarsvurderingServiceImpl.oppdaterVurderingPaaVilkaar(behandlingId, brukerTokenInfo, vurdertVilkaar)
            }
            val actual = vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId)
            actual shouldBe vilkaarsvurdering
        }
    }

    @Test
    fun `kan opprette og kopiere vilkaarsvurdering fra forrige behandling`() {
        val grunnlag: Grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val nyBehandlingId = UUID.randomUUID()
        coEvery { grunnlagKlient.hentGrunnlagForBehandling(any(), any()) } returns grunnlag
        every { behandlingStatus.settVilkaarsvurdert(any(), any(), any()) } just Runs

        runBlocking {
            vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(behandlingId, brukerTokenInfo)
            val vilkaarsvurdering = vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId)!!
            vilkaarsvurdering.vilkaar.forEach { vilkaar ->
                vilkaarsvurderingServiceImpl.oppdaterVurderingPaaVilkaar(
                    behandlingId,
                    brukerTokenInfo,
                    VurdertVilkaar(
                        vilkaar.id,
                        VilkaarTypeOgUtfall(vilkaar.hovedvilkaar.type, Utfall.OPPFYLT),
                        null,
                        VilkaarVurderingData("kommentar", LocalDateTime.now(), "saksbehandler"),
                    ),
                )
            }
            vilkaarsvurderingServiceImpl.oppdaterTotalVurdering(
                behandlingId,
                brukerTokenInfo,
                VilkaarsvurderingResultat(
                    VilkaarsvurderingUtfall.OPPFYLT,
                    "kommentar",
                    LocalDateTime.now(),
                    "saksbehandler",
                ),
            )
        }

        val vilkaarsvurdering = vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId)!!
        val (kopiertVilkaarsvurdering) =
            vilkaarsvurderingServiceImpl.kopierVilkaarsvurdering(
                nyBehandlingId,
                vilkaarsvurdering.behandlingId,
                brukerTokenInfo,
            )

        Assertions.assertNotNull(kopiertVilkaarsvurdering.resultat)
        Assertions.assertEquals(vilkaarsvurdering.resultat, kopiertVilkaarsvurdering.resultat)
        Assertions.assertEquals(
            vilkaarsvurdering.vilkaar.map { it.vurdering },
            kopiertVilkaarsvurdering.vilkaar.map { it.vurdering },
        )
    }

    @Test
    fun `revurdering kopierer forrige vilkaarsvurdering ved opprettelse`() {
        val grunnlag: Grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val revurderingId = UUID.randomUUID()

        coEvery { grunnlagKlient.hentGrunnlagForBehandling(any(), any()) } returns grunnlag
        every { behandlingStatus.settVilkaarsvurdert(any(), any(), any()) } just Runs
        coEvery { behandlingService.hentBehandling(revurderingId) } returns
            mockk {
                every { id } returns revurderingId
                every { sak.id } returns sakId1
                every { sak.sakType } returns SakType.BARNEPENSJON
                every { type } returns BehandlingType.REVURDERING
                every { prosesstype } returns Prosesstype.MANUELL
                every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt()
                every { revurderingsaarsak() } returns Revurderingaarsak.REGULERING
            }

        every { behandlingService.hentSisteIverksatte(any()) } returns foerstegangsbehandling(behandlingId, sakId1)

        val foerstegangsvilkaar =
            runBlocking {
                vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(behandlingId, brukerTokenInfo)
                val vilkaarsvurdering = vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId)!!
                vilkaarsvurdering.vilkaar.forEach { vilkaar ->
                    vilkaarsvurderingServiceImpl.oppdaterVurderingPaaVilkaar(
                        behandlingId,
                        brukerTokenInfo,
                        VurdertVilkaar(
                            vilkaar.id,
                            VilkaarTypeOgUtfall(vilkaar.hovedvilkaar.type, Utfall.OPPFYLT),
                            null,
                            VilkaarVurderingData("kommentar", LocalDateTime.now(), "saksbehandler"),
                        ),
                    )
                }
                vilkaarsvurderingServiceImpl.oppdaterTotalVurdering(
                    behandlingId,
                    brukerTokenInfo,
                    VilkaarsvurderingResultat(
                        VilkaarsvurderingUtfall.OPPFYLT,
                        "kommentar",
                        LocalDateTime.now(),
                        "saksbehandler",
                    ),
                )

                vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId)!!
            }

        val (revurderingsvilkaar) =
            runBlocking {
                vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(
                    revurderingId,
                    brukerTokenInfo,
                )
            }
        assertIsSimilar(foerstegangsvilkaar, revurderingsvilkaar)
        verify(exactly = 1) { behandlingStatus.settVilkaarsvurdert(revurderingId, brukerTokenInfo, false) }
    }

    @Test
    fun `revurdering kopierer ikke forrige vilkaarsvurdering ved opprettelse naar kopierVedRevurdering er false`() {
        val grunnlag: Grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val revurderingId = UUID.randomUUID()

        coEvery { grunnlagKlient.hentGrunnlagForBehandling(any(), any()) } returns grunnlag
        every { behandlingStatus.settVilkaarsvurdert(any(), any(), any()) } just Runs
        coEvery { behandlingService.hentBehandling(revurderingId) } returns
            mockk {
                every { id } returns revurderingId
                every { sak.id } returns sakId1
                every { sak.sakType } returns SakType.BARNEPENSJON
                every { type } returns BehandlingType.REVURDERING
                every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt()
                every { revurderingsaarsak() } returns Revurderingaarsak.REGULERING
            }

        every { behandlingService.hentSisteIverksatte(any()) } returns foerstegangsbehandling(behandlingId, sakId1)

        runBlocking {
            vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(behandlingId, brukerTokenInfo)
            val vilkaarsvurdering = vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId)!!
            vilkaarsvurdering.vilkaar.forEach { vilkaar ->
                vilkaarsvurderingServiceImpl.oppdaterVurderingPaaVilkaar(
                    behandlingId,
                    brukerTokenInfo,
                    VurdertVilkaar(
                        vilkaar.id,
                        VilkaarTypeOgUtfall(vilkaar.hovedvilkaar.type, Utfall.OPPFYLT),
                        null,
                        VilkaarVurderingData("kommentar", LocalDateTime.now(), "saksbehandler"),
                    ),
                )
            }
            vilkaarsvurderingServiceImpl.oppdaterTotalVurdering(
                behandlingId,
                brukerTokenInfo,
                VilkaarsvurderingResultat(
                    VilkaarsvurderingUtfall.OPPFYLT,
                    "kommentar",
                    LocalDateTime.now(),
                    "saksbehandler",
                ),
            )

            vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId)!!
        }

        val (nyeVilkaar) =
            runBlocking {
                vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(
                    revurderingId,
                    brukerTokenInfo,
                    false,
                )
            }

        nyeVilkaar.resultat shouldBe null
        nyeVilkaar.vilkaar.forEach { it.vurdering shouldBe null }
        verify(exactly = 0) { behandlingStatus.settVilkaarsvurdert(revurderingId, brukerTokenInfo, false) }
    }

    @Test
    fun `kopier vilkaarsvurdering gir NullpointerException hvis det ikke finnes tidligere vilkaarsvurdering`() {
        assertThrows<NullPointerException> {
            vilkaarsvurderingServiceImpl.kopierVilkaarsvurdering(
                UUID.randomUUID(),
                behandlingId,
                brukerTokenInfo,
            )
        }
    }

    @Test
    fun `skal legge til nye og fjerne slettede vilkaar ved kopiering av vilkaarsvurdering - totalvurdering slettes`() {
        val nyBehandlingId = UUID.randomUUID()
        val opprinneligBehandlingId = UUID.randomUUID()

        coEvery { behandlingService.hentBehandling(nyBehandlingId) } returns behandling()

        opprettVilkaarsvurderingMedResultat(
            behandlingId = opprinneligBehandlingId,
            vilkaar = ikkeGjeldendeVilkaar(),
        )

        val (vilkaarsvurderingMedKopierteOppdaterteVilkaar) =
            vilkaarsvurderingServiceImpl.kopierVilkaarsvurdering(
                nyBehandlingId,
                opprinneligBehandlingId,
                brukerTokenInfo,
            )

        with(vilkaarsvurderingMedKopierteOppdaterteVilkaar.vilkaar.map { it.hovedvilkaar.type }) {
            val gjeldendeVilkaar = BarnepensjonVilkaar2024.inngangsvilkaar()

            this shouldNotContain VilkaarType.BP_FORMAAL
            this shouldContainAll gjeldendeVilkaar.map { it.hovedvilkaar.type }
            this shouldHaveSize gjeldendeVilkaar.size
        }

        vilkaarsvurderingMedKopierteOppdaterteVilkaar.resultat shouldBe null

        coVerify { behandlingService.hentBehandling(any()) }

        verify(exactly = 1) { behandlingStatus.settVilkaarsvurdert(any(), brukerTokenInfo, true) }
        verify(exactly = 0) { behandlingStatus.settVilkaarsvurdert(any(), brukerTokenInfo, false) }
    }

    @Test
    fun `skal ikke endre vilkaar ved kopiering av vilkaarsvurdering ved regulering - totalvurdering er uforandret`() {
        val nyBehandlingId = UUID.randomUUID()
        val opprinneligBehandlingId = UUID.randomUUID()

        every { behandlingStatus.settVilkaarsvurdert(any(), any(), any()) } just Runs
        coEvery { behandlingService.hentBehandling(nyBehandlingId) } returns
            behandling(
                behandlingstype = BehandlingType.REVURDERING,
                revurderingaarsak = Revurderingaarsak.REGULERING,
            )

        opprettVilkaarsvurderingMedResultat(
            behandlingId = opprinneligBehandlingId,
            vilkaar =
                ikkeGjeldendeVilkaar().map {
                    it.copy(vurdering = vurdering())
                },
        )

        val (vilkaarsvurderingMedKopierteOppdaterteVilkaar) =
            vilkaarsvurderingServiceImpl.kopierVilkaarsvurdering(
                nyBehandlingId,
                opprinneligBehandlingId,
                brukerTokenInfo,
            )

        with(vilkaarsvurderingMedKopierteOppdaterteVilkaar.vilkaar.map { it.hovedvilkaar.type }) {
            val gjeldendeVilkaar = ikkeGjeldendeVilkaar()

            this shouldContainAll gjeldendeVilkaar.map { it.hovedvilkaar.type }
            this shouldHaveSize gjeldendeVilkaar.size
        }

        vilkaarsvurderingMedKopierteOppdaterteVilkaar.resultat shouldNotBe null
        verify(exactly = 1) { behandlingStatus.settVilkaarsvurdert(any(), brukerTokenInfo, true) }
        verify(exactly = 1) { behandlingStatus.settVilkaarsvurdert(any(), brukerTokenInfo, false) }
    }

    @Test
    fun `skal kopiere eksisterende vilkaarsvurdering uten endringer paa vilkaar - totalvurdering er uforandret`() {
        val nyBehandlingId = UUID.randomUUID()
        val opprinneligBehandlingId = UUID.randomUUID()

        every { behandlingStatus.settVilkaarsvurdert(any(), any(), any()) } just Runs
        coEvery { behandlingService.hentBehandling(nyBehandlingId) } returns behandling()

        opprettVilkaarsvurderingMedResultat(
            behandlingId = opprinneligBehandlingId,
            vilkaar =
                BarnepensjonVilkaar2024.inngangsvilkaar().map {
                    it.copy(vurdering = vurdering())
                },
        )

        val (vilkaarsvurderingMedKopierteOppdaterteVilkaar) =
            vilkaarsvurderingServiceImpl.kopierVilkaarsvurdering(
                nyBehandlingId,
                opprinneligBehandlingId,
                brukerTokenInfo,
            )

        vilkaarsvurderingMedKopierteOppdaterteVilkaar.resultat shouldNotBe null

        verify(exactly = 1) { behandlingStatus.settVilkaarsvurdert(any(), brukerTokenInfo, true) }
        verify(exactly = 1) { behandlingStatus.settVilkaarsvurdert(any(), brukerTokenInfo, false) }
    }

    @Test
    fun `Er ikke yrkesskade hvis ikke det er en yrkesskade oppfylt delvilkaar`() {
        every { behandlingStatus.settVilkaarsvurdert(any(), any()) } just Runs
        val (vilkaarsvurdering) =
            runBlocking {
                vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(behandlingId, brukerTokenInfo)
            }

        vilkaarsvurdering shouldNotBe null
        vilkaarsvurdering.behandlingId shouldBe behandlingId
        toDto(vilkaarsvurdering, 0L).isYrkesskade() shouldBe false
    }

    @Test
    fun `Er yrkesskade hvis det er en yrkesskade oppfylt delvilkaar`() {
        val (vilkaarsvurdering) =
            runBlocking {
                vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(behandlingId, brukerTokenInfo)
            }

        val delvilkaar =
            vilkaarsvurdering.vilkaar.find { it.hovedvilkaar.type == VilkaarType.BP_YRKESSKADE_AVDOED_2024 }

        runBlocking {
            vilkaarsvurderingServiceImpl.oppdaterVurderingPaaVilkaar(
                behandlingId,
                brukerTokenInfo,
                VurdertVilkaar(
                    delvilkaar!!.id,
                    VilkaarTypeOgUtfall(VilkaarType.BP_YRKESSKADE_AVDOED_2024, Utfall.OPPFYLT),
                    null,
                    vilkaarsVurderingData(),
                ),
            )
        }

        val oppdatertVilkaarsvurdering =
            runBlocking {
                vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId)
            }

        oppdatertVilkaarsvurdering shouldNotBe null
        oppdatertVilkaarsvurdering!!.behandlingId shouldBe behandlingId
        toDto(oppdatertVilkaarsvurdering, 0L).isYrkesskade() shouldBe true
    }

    @Test
    fun `skal sjekke gyldighet og oppdatere status hvis vilkaarsvurdering er oppfylt men status er OPPRETTET`() {
        coEvery { grunnlagKlient.hentGrunnlagForBehandling(any(), any()) } returns grunnlag()
        every { behandlingStatus.settVilkaarsvurdert(any(), any(), any()) } just Runs
        coEvery { behandlingService.hentBehandling(any()) } returns
            behandling(behandlingStatus = BehandlingStatus.OPPRETTET)

        val statusOppdatert =
            runBlocking {
                vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(behandlingId, brukerTokenInfo)
                vilkaarsvurderingServiceImpl.oppdaterTotalVurdering(
                    behandlingId = behandlingId,
                    brukerTokenInfo = brukerTokenInfo,
                    resultat = vilkaarsvurderingResultat(VilkaarsvurderingUtfall.OPPFYLT),
                )
                vilkaarsvurderingServiceImpl.sjekkGyldighetOgOppdaterBehandlingStatus(behandlingId, brukerTokenInfo)
            }

        statusOppdatert shouldBe true

        verify(exactly = 3) { behandlingStatus.settVilkaarsvurdert(behandlingId, brukerTokenInfo, true) }
        verify(exactly = 2) { behandlingStatus.settVilkaarsvurdert(behandlingId, brukerTokenInfo, false) }
    }

    @Test
    fun `skal feile ved sjekking av gyldighet dersom vilkaarsvurdering mangler totalvurdering`() {
        coEvery { grunnlagKlient.hentGrunnlagForBehandling(any(), any()) } returns grunnlag()
        coEvery { behandlingService.hentBehandling(any()) } returns behandling()

        runBlocking {
            vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(behandlingId, brukerTokenInfo)

            assertThrows<VilkaarsvurderingManglerResultat> {
                vilkaarsvurderingServiceImpl.sjekkGyldighetOgOppdaterBehandlingStatus(behandlingId, brukerTokenInfo)
            }
        }
    }

    @Test
    fun `skal feile ved sjekking av gyldighet dersom vilkaarsvurdering har virk som avviker fra behandling`() {
        val virkBehandling = YearMonth.of(2023, 1)

        coEvery { grunnlagKlient.hentGrunnlagForBehandling(any(), any()) } returns grunnlag()
        every { behandlingStatus.settVilkaarsvurdert(any(), any(), any()) } just Runs
        coEvery { behandlingService.hentBehandling(any()) } returns
            behandling(virk = virkBehandling) andThen // opprettelse
            behandling(virk = virkBehandling) andThen // oppdatering ved totalvurdering
            behandling() // sjekk av gyldighet

        runBlocking {
            vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(behandlingId, brukerTokenInfo)
            vilkaarsvurderingServiceImpl.oppdaterTotalVurdering(
                behandlingId = behandlingId,
                brukerTokenInfo = brukerTokenInfo,
                resultat = vilkaarsvurderingResultat(VilkaarsvurderingUtfall.OPPFYLT),
            )

            assertThrows<VirkningstidspunktSamsvarerIkke> {
                vilkaarsvurderingServiceImpl.sjekkGyldighetOgOppdaterBehandlingStatus(behandlingId, brukerTokenInfo)
            }
        }
    }

    private fun opprettVilkaarsvurderingMedResultat(
        behandlingId: UUID,
        vilkaar: List<Vilkaar>,
    ): Vilkaarsvurdering {
        val opprettetVilkaarsvudering =
            repository.opprettVilkaarsvurdering(
                Vilkaarsvurdering(
                    behandlingId = behandlingId,
                    grunnlagVersjon = 1L,
                    virkningstidspunkt = YearMonth.of(2024, Month.JANUARY),
                    vilkaar = vilkaar,
                ),
            )

        return repository.lagreVilkaarsvurderingResultatvanlig(
            vilkaarsvurdering = opprettetVilkaarsvudering,
            virkningstidspunkt = LocalDate.of(2024, 1, 1),
            resultat =
                VilkaarsvurderingResultat(
                    utfall = VilkaarsvurderingUtfall.OPPFYLT,
                    kommentar = "Oppfylt",
                    tidspunkt = LocalDateTime.now(),
                    saksbehandler = "Z123456",
                ),
        )
    }

    private fun ikkeGjeldendeVilkaar(): List<Vilkaar> =
        BarnepensjonVilkaar2024
            .inngangsvilkaar()
            .subList(0, 2) // Reduserer til de 3 første vilkårene
            .toMutableList()
            .apply {
                // Legger til et mocket vilkår
                add(
                    Vilkaar(
                        hovedvilkaar =
                            Delvilkaar(
                                type = VilkaarType.BP_FORMAAL,
                                tittel = "Tittel",
                                beskrivelse = "Beskrivelse",
                                spoersmaal = "Spørsmål",
                                lovreferanse =
                                    Lovreferanse(
                                        paragraf = "§ 18-1",
                                    ),
                            ),
                    ),
                )
            }

    private fun grunnlag() = GrunnlagTestData().hentOpplysningsgrunnlag()

    private fun behandling(
        behandlingStatus: BehandlingStatus = BehandlingStatus.OPPRETTET,
        virk: YearMonth = YearMonth.now(),
        behandlingstype: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        revurderingaarsak: Revurderingaarsak? = null,
    ) = mockk<Behandling> {
        every { id } returns behandlingId
        every { sak.id } returns sakId1
        every { sak.sakType } returns SakType.BARNEPENSJON
        every { prosesstype } returns Prosesstype.MANUELL
        every { status } returns behandlingStatus
        every { type } returns behandlingstype
        every { revurderingsaarsak() } returns revurderingaarsak
        every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt(virk)
    }

    private fun vurdering() =
        VilkaarVurderingData(
            kommentar = "kommentar",
            tidspunkt = LocalDateTime.now(),
            saksbehandler = "Z123456",
        )

    private fun assertIsSimilar(
        v1: Vilkaarsvurdering,
        v2: Vilkaarsvurdering,
    ) {
        Assertions.assertEquals(v1.resultat, v2.resultat)
        Assertions.assertEquals(v1.virkningstidspunkt, v2.virkningstidspunkt)
        Assertions.assertEquals(v1.grunnlagVersjon, v2.grunnlagVersjon)

        v1.vilkaar.forEach { v1Vilkaar ->
            val v2Vilkaar = v2.vilkaar.find { it.hovedvilkaar.type == v1Vilkaar.hovedvilkaar.type }
            Assertions.assertNotNull(v2Vilkaar)
            Assertions.assertEquals(v1Vilkaar.vurdering, v2Vilkaar?.vurdering)
            Assertions.assertEquals(v1Vilkaar.hovedvilkaar, v2Vilkaar?.hovedvilkaar)
            Assertions.assertEquals(v1Vilkaar.unntaksvilkaar, v2Vilkaar?.unntaksvilkaar)
        }
    }

    private fun opprettVilkaarsvurdering(): Vilkaarsvurdering =
        vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(behandlingId, brukerTokenInfo).vilkaarsvurdering

    private fun vilkaarsVurderingData() = VilkaarVurderingData("en kommentar", Tidspunkt.now().toLocalDatetimeUTC(), "saksbehandler")

    private fun vilkaarsvurderingResultat(utfall: VilkaarsvurderingUtfall) =
        VilkaarsvurderingResultat(
            utfall = utfall,
            kommentar = "Kommentar",
            tidspunkt = LocalDateTime.now(),
            saksbehandler = "Saksbehandler",
        )
}
