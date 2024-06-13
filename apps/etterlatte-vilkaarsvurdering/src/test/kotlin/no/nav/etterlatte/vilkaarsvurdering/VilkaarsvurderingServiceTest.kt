package no.nav.etterlatte.vilkaarsvurdering

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
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
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.vilkaarsvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vilkaarsvurdering.klienter.GrunnlagKlient
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

    private lateinit var service: VilkaarsvurderingService
    private lateinit var repository: VilkaarsvurderingRepository
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val grunnlagKlient = mockk<GrunnlagKlient>()
    private val uuid: UUID = UUID.randomUUID()
    private val brukerTokenInfo = BrukerTokenInfo.of("token", "s1", null, null, null)
    private val grunnlagMock: Grunnlag = mockk()

    @BeforeAll
    fun beforeAll() {
        val metadataMock = mockk<Metadata>()
        every { grunnlagMock.metadata } returns metadataMock
        every { metadataMock.versjon } returns 1
    }

    @BeforeEach
    fun beforeEach() {
        coEvery { grunnlagKlient.hentGrunnlagForBehandling(any(), any()) } returns GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { behandlingKlient.kanSetteBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns
            mockk<DetaljertBehandling>().apply {
                every { id } returns UUID.randomUUID()
                every { sak } returns 1L
                every { sakType } returns SakType.BARNEPENSJON
                every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
                every { soeker } returns "10095512345"
                every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt()
                every { revurderingsaarsak } returns null
            }

        repository = VilkaarsvurderingRepository(ds, DelvilkaarRepository())
        service =
            VilkaarsvurderingService(
                repository,
                behandlingKlient,
                grunnlagKlient,
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
                service.opprettVilkaarsvurdering(uuid, brukerTokenInfo)
            }

        vilkaarsvurdering shouldNotBe null
        vilkaarsvurdering.behandlingId shouldBe uuid
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
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns
            mockk<DetaljertBehandling>().apply {
                every { id } returns UUID.randomUUID()
                every { sak } returns 1L
                every { sakType } returns SakType.BARNEPENSJON
                every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
                every { soeker } returns "10095512345"
                every { virkningstidspunkt } returns
                    VirkningstidspunktTestData
                        .virkningstidsunkt()
                        .copy(dato = YearMonth.of(2024, 1))
                every { revurderingsaarsak } returns null
            }

        val (vilkaarsvurdering) =
            runBlocking {
                service.opprettVilkaarsvurdering(uuid, brukerTokenInfo)
            }

        vilkaarsvurdering shouldNotBe null
        vilkaarsvurdering.behandlingId shouldBe uuid
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
        runBlocking { service.opprettVilkaarsvurdering(uuid, brukerTokenInfo) }
        service.hentVilkaarsvurdering(uuid) shouldNotBe null
        coEvery { behandlingKlient.settBehandlingStatusOpprettet(uuid, brukerTokenInfo, any()) } returns true

        runBlocking { service.slettVilkaarsvurdering(uuid, brukerTokenInfo) }
        service.hentVilkaarsvurdering(uuid) shouldBe null
    }

    @Test
    fun `Skal opprette vilkaarsvurdering for foerstegangsbehandling omstillingssoeknad med grunnlagsopplysninger`() {
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns
            mockk<DetaljertBehandling>().apply {
                every { id } returns UUID.randomUUID()
                every { sak } returns 2L
                every { sakType } returns SakType.OMSTILLINGSSTOENAD
                every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
                every { soeker } returns "10095512345"
                every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt()
                every { revurderingsaarsak } returns null
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

        val (vilkaarsvurdering) =
            runBlocking {
                service.opprettVilkaarsvurdering(uuid, brukerTokenInfo)
            }

        vilkaarsvurdering shouldNotBe null
        vilkaarsvurdering.behandlingId shouldBe uuid
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
                service.oppdaterVurderingPaaVilkaar(uuid, brukerTokenInfo, vurdertVilkaar)

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
            val vilkaarsvurderingOppdatert = service.oppdaterVurderingPaaVilkaar(uuid, brukerTokenInfo, vurdertVilkaar)

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
            val vilkaarsvurderingOppdatert = service.oppdaterVurderingPaaVilkaar(uuid, brukerTokenInfo, vurdertVilkaar)

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
        coEvery { behandlingKlient.kanSetteBehandlingStatusVilkaarsvurdert(any(), any()) } returns false

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
            assertThrows<IllegalStateException> {
                service.oppdaterVurderingPaaVilkaar(uuid, brukerTokenInfo, vurdertVilkaar)
            }
            val actual = service.hentVilkaarsvurdering(uuid)
            actual shouldBe vilkaarsvurdering
        }
    }

    @Test
    fun `kan opprette og kopiere vilkaarsvurdering fra forrige behandling`() {
        val grunnlag: Grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val nyBehandlingId = UUID.randomUUID()
        coEvery { grunnlagKlient.hentGrunnlagForBehandling(any(), any()) } returns grunnlag
        coEvery { behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any()) } returns true

        runBlocking {
            service.opprettVilkaarsvurdering(uuid, brukerTokenInfo)
            val vilkaarsvurdering = service.hentVilkaarsvurdering(uuid)!!
            vilkaarsvurdering.vilkaar.forEach { vilkaar ->
                service.oppdaterVurderingPaaVilkaar(
                    uuid,
                    brukerTokenInfo,
                    VurdertVilkaar(
                        vilkaar.id,
                        VilkaarTypeOgUtfall(vilkaar.hovedvilkaar.type, Utfall.OPPFYLT),
                        null,
                        VilkaarVurderingData("kommentar", LocalDateTime.now(), "saksbehandler"),
                    ),
                )
            }
            service.oppdaterTotalVurdering(
                uuid,
                brukerTokenInfo,
                VilkaarsvurderingResultat(
                    VilkaarsvurderingUtfall.OPPFYLT,
                    "kommentar",
                    LocalDateTime.now(),
                    "saksbehandler",
                ),
            )
        }

        val vilkaarsvurdering = service.hentVilkaarsvurdering(uuid)!!
        val (kopiertVilkaarsvurdering) =
            runBlocking {
                service.kopierVilkaarsvurdering(
                    nyBehandlingId,
                    vilkaarsvurdering.behandlingId,
                    brukerTokenInfo,
                )
            }

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
        coEvery { behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(revurderingId, any()) } returns
            mockk {
                every { id } returns revurderingId
                every { sak } returns 1L
                every { sakType } returns SakType.BARNEPENSJON
                every { behandlingType } returns BehandlingType.REVURDERING
                every { soeker } returns "10095512345"
                every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt()
                every { revurderingsaarsak } returns Revurderingaarsak.REGULERING
            }

        coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns SisteIverksatteBehandling(uuid)

        val foerstegangsvilkaar =
            runBlocking {
                service.opprettVilkaarsvurdering(uuid, brukerTokenInfo)
                val vilkaarsvurdering = service.hentVilkaarsvurdering(uuid)!!
                vilkaarsvurdering.vilkaar.forEach { vilkaar ->
                    service.oppdaterVurderingPaaVilkaar(
                        uuid,
                        brukerTokenInfo,
                        VurdertVilkaar(
                            vilkaar.id,
                            VilkaarTypeOgUtfall(vilkaar.hovedvilkaar.type, Utfall.OPPFYLT),
                            null,
                            VilkaarVurderingData("kommentar", LocalDateTime.now(), "saksbehandler"),
                        ),
                    )
                }
                service.oppdaterTotalVurdering(
                    uuid,
                    brukerTokenInfo,
                    VilkaarsvurderingResultat(
                        VilkaarsvurderingUtfall.OPPFYLT,
                        "kommentar",
                        LocalDateTime.now(),
                        "saksbehandler",
                    ),
                )

                service.hentVilkaarsvurdering(uuid)!!
            }

        val (revurderingsvilkaar) = runBlocking { service.opprettVilkaarsvurdering(revurderingId, brukerTokenInfo) }
        assertIsSimilar(foerstegangsvilkaar, revurderingsvilkaar)
        coVerify(exactly = 1) { behandlingKlient.settBehandlingStatusVilkaarsvurdert(revurderingId, brukerTokenInfo) }
    }

    @Test
    fun `revurdering kopierer ikke forrige vilkaarsvurdering ved opprettelse naar kopierVedRevurdering er false`() {
        val grunnlag: Grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val revurderingId = UUID.randomUUID()

        coEvery { grunnlagKlient.hentGrunnlagForBehandling(any(), any()) } returns grunnlag
        coEvery { behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(revurderingId, any()) } returns
            mockk {
                every { id } returns revurderingId
                every { sak } returns 1L
                every { sakType } returns SakType.BARNEPENSJON
                every { behandlingType } returns BehandlingType.REVURDERING
                every { soeker } returns "10095512345"
                every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt()
                every { revurderingsaarsak } returns Revurderingaarsak.REGULERING
            }

        coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns SisteIverksatteBehandling(uuid)

        runBlocking {
            service.opprettVilkaarsvurdering(uuid, brukerTokenInfo)
            val vilkaarsvurdering = service.hentVilkaarsvurdering(uuid)!!
            vilkaarsvurdering.vilkaar.forEach { vilkaar ->
                service.oppdaterVurderingPaaVilkaar(
                    uuid,
                    brukerTokenInfo,
                    VurdertVilkaar(
                        vilkaar.id,
                        VilkaarTypeOgUtfall(vilkaar.hovedvilkaar.type, Utfall.OPPFYLT),
                        null,
                        VilkaarVurderingData("kommentar", LocalDateTime.now(), "saksbehandler"),
                    ),
                )
            }
            service.oppdaterTotalVurdering(
                uuid,
                brukerTokenInfo,
                VilkaarsvurderingResultat(
                    VilkaarsvurderingUtfall.OPPFYLT,
                    "kommentar",
                    LocalDateTime.now(),
                    "saksbehandler",
                ),
            )

            service.hentVilkaarsvurdering(uuid)!!
        }

        val (nyeVilkaar) = runBlocking { service.opprettVilkaarsvurdering(revurderingId, brukerTokenInfo, false) }

        nyeVilkaar.resultat shouldBe null
        nyeVilkaar.vilkaar.forEach { it.vurdering shouldBe null }
        coVerify(exactly = 0) { behandlingKlient.settBehandlingStatusVilkaarsvurdert(revurderingId, brukerTokenInfo) }
    }

    @Test
    fun `kopier vilkaarsvurdering gir NullpointerException hvis det ikke finnes tidligere vilkaarsvurdering`() {
        assertThrows<NullPointerException> {
            runBlocking { service.kopierVilkaarsvurdering(UUID.randomUUID(), uuid, brukerTokenInfo) }
        }
    }

    @Test
    fun `skal legge til nye og fjerne slettede vilkaar ved kopiering av vilkaarsvurdering - totalvurdering slettes`() {
        val nyBehandlingId = UUID.randomUUID()
        val opprinneligBehandlingId = UUID.randomUUID()

        coEvery { behandlingKlient.hentBehandling(nyBehandlingId, any()) } returns detaljertBehandling()

        opprettVilkaarsvurderingMedResultat(
            behandlingId = opprinneligBehandlingId,
            vilkaar = ikkeGjeldendeVilkaar(),
        )

        val (vilkaarsvurderingMedKopierteOppdaterteVilkaar) =
            runBlocking {
                service.kopierVilkaarsvurdering(nyBehandlingId, opprinneligBehandlingId, brukerTokenInfo)
            }

        with(vilkaarsvurderingMedKopierteOppdaterteVilkaar.vilkaar.map { it.hovedvilkaar.type }) {
            val gjeldendeVilkaar = BarnepensjonVilkaar2024.inngangsvilkaar()

            this shouldNotContain VilkaarType.BP_FORMAAL
            this shouldContainAll gjeldendeVilkaar.map { it.hovedvilkaar.type }
            this shouldHaveSize gjeldendeVilkaar.size
        }

        vilkaarsvurderingMedKopierteOppdaterteVilkaar.resultat shouldBe null

        coVerify { behandlingKlient.hentBehandling(any(), brukerTokenInfo) }

        coVerify(exactly = 0) {
            behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), brukerTokenInfo)
        }
    }

    @Test
    fun `skal ikke endre vilkaar ved kopiering av vilkaarsvurdering ved regulering - totalvurdering er uforandret`() {
        val nyBehandlingId = UUID.randomUUID()
        val opprinneligBehandlingId = UUID.randomUUID()

        coEvery { behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(nyBehandlingId, any()) } returns
            detaljertBehandling(
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
            runBlocking {
                service.kopierVilkaarsvurdering(nyBehandlingId, opprinneligBehandlingId, brukerTokenInfo)
            }

        with(vilkaarsvurderingMedKopierteOppdaterteVilkaar.vilkaar.map { it.hovedvilkaar.type }) {
            val gjeldendeVilkaar = ikkeGjeldendeVilkaar()

            this shouldContainAll gjeldendeVilkaar.map { it.hovedvilkaar.type }
            this shouldHaveSize gjeldendeVilkaar.size
        }

        vilkaarsvurderingMedKopierteOppdaterteVilkaar.resultat shouldNotBe null

        coVerify {
            behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), brukerTokenInfo)
        }
    }

    @Test
    fun `skal kopiere eksisterende vilkaarsvurdering uten endringer paa vilkaar - totalvurdering er uforandret`() {
        val nyBehandlingId = UUID.randomUUID()
        val opprinneligBehandlingId = UUID.randomUUID()

        coEvery { behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(nyBehandlingId, any()) } returns detaljertBehandling()

        opprettVilkaarsvurderingMedResultat(
            behandlingId = opprinneligBehandlingId,
            vilkaar =
                BarnepensjonVilkaar2024.inngangsvilkaar().map {
                    it.copy(vurdering = vurdering())
                },
        )

        val (vilkaarsvurderingMedKopierteOppdaterteVilkaar) =
            runBlocking {
                service.kopierVilkaarsvurdering(nyBehandlingId, opprinneligBehandlingId, brukerTokenInfo)
            }

        vilkaarsvurderingMedKopierteOppdaterteVilkaar.resultat shouldNotBe null

        coVerify {
            behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), brukerTokenInfo)
        }
    }

    @Test
    fun `Er ikke yrkesskade hvis ikke det er en yrkesskade oppfylt delvilkaar`() {
        val (vilkaarsvurdering) =
            runBlocking {
                service.opprettVilkaarsvurdering(uuid, brukerTokenInfo)
            }

        vilkaarsvurdering shouldNotBe null
        vilkaarsvurdering.behandlingId shouldBe uuid
        toDto(vilkaarsvurdering, 0L).isYrkesskade() shouldBe false
    }

    @Test
    fun `Er yrkesskade hvis det er en yrkesskade oppfylt delvilkaar`() {
        val (vilkaarsvurdering) =
            runBlocking {
                service.opprettVilkaarsvurdering(uuid, brukerTokenInfo)
            }

        val delvilkaar = vilkaarsvurdering.vilkaar.find { it.hovedvilkaar.type == VilkaarType.BP_YRKESSKADE_AVDOED_2024 }

        runBlocking {
            service.oppdaterVurderingPaaVilkaar(
                uuid,
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
                service.hentVilkaarsvurdering(uuid)
            }

        oppdatertVilkaarsvurdering shouldNotBe null
        oppdatertVilkaarsvurdering!!.behandlingId shouldBe uuid
        toDto(oppdatertVilkaarsvurdering, 0L).isYrkesskade() shouldBe true
    }

    @Test
    fun `skal sjekke gyldighet og oppdatere status hvis vilkaarsvurdering er oppfylt men status er OPPRETTET`() {
        coEvery { grunnlagKlient.hentGrunnlagForBehandling(any(), any()) } returns grunnlag()
        coEvery { behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns
            detaljertBehandling(behandlingStatus = BehandlingStatus.OPPRETTET)

        val statusOppdatert =
            runBlocking {
                service.opprettVilkaarsvurdering(uuid, brukerTokenInfo)
                service.oppdaterTotalVurdering(
                    behandlingId = uuid,
                    brukerTokenInfo = brukerTokenInfo,
                    resultat = vilkaarsvurderingResultat(VilkaarsvurderingUtfall.OPPFYLT),
                )
                service.sjekkGyldighetOgOppdaterBehandlingStatus(uuid, brukerTokenInfo)
            }

        statusOppdatert shouldBe true

        coVerify(exactly = 2) {
            /*
            Kalles to ganger, først en gang under oppdaterTotalVurdering, deretter under
            sjekkGyldighetOgOppdaterBehandlingStatus siden detaljerBehandling har mocket status OPPRETTET.
             */
            behandlingKlient.settBehandlingStatusVilkaarsvurdert(uuid, brukerTokenInfo)
        }
    }

    @Test
    fun `skal feile ved sjekking av gyldighet dersom vilkaarsvurdering mangler totalvurdering`() {
        coEvery { grunnlagKlient.hentGrunnlagForBehandling(any(), any()) } returns grunnlag()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns detaljertBehandling()

        runBlocking {
            service.opprettVilkaarsvurdering(uuid, brukerTokenInfo)

            assertThrows<VilkaarsvurderingManglerResultat> {
                service.sjekkGyldighetOgOppdaterBehandlingStatus(uuid, brukerTokenInfo)
            }
        }
    }

    @Test
    fun `skal feile ved sjekking av gyldighet dersom vilkaarsvurdering har virk som avviker fra behandling`() {
        val virkBehandling = YearMonth.of(2023, 1)

        coEvery { grunnlagKlient.hentGrunnlagForBehandling(any(), any()) } returns grunnlag()
        coEvery { behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns
            detaljertBehandling(virk = virkBehandling) andThen // opprettelse
            detaljertBehandling(virk = virkBehandling) andThen // oppdatering ved totalvurdering
            detaljertBehandling() // sjekk av gyldighet

        runBlocking {
            service.opprettVilkaarsvurdering(uuid, brukerTokenInfo)
            service.oppdaterTotalVurdering(
                behandlingId = uuid,
                brukerTokenInfo = brukerTokenInfo,
                resultat = vilkaarsvurderingResultat(VilkaarsvurderingUtfall.OPPFYLT),
            )

            assertThrows<VirkningstidspunktSamsvarerIkke> {
                service.sjekkGyldighetOgOppdaterBehandlingStatus(uuid, brukerTokenInfo)
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

        return repository.lagreVilkaarsvurderingResultat(
            behandlingId = opprettetVilkaarsvudering.behandlingId,
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

    private fun detaljertBehandling(
        behandlingStatus: BehandlingStatus = BehandlingStatus.OPPRETTET,
        virk: YearMonth = YearMonth.now(),
        behandlingstype: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        revurderingaarsak: Revurderingaarsak? = null,
    ) = mockk<DetaljertBehandling> {
        every { id } returns uuid
        every { sak } returns 1L
        every { sakType } returns SakType.BARNEPENSJON
        every { status } returns behandlingStatus
        every { behandlingType } returns behandlingstype
        every { soeker } returns "10095512345"
        every { revurderingsaarsak } returns revurderingaarsak
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

    private suspend fun opprettVilkaarsvurdering(): Vilkaarsvurdering =
        service.opprettVilkaarsvurdering(uuid, brukerTokenInfo).vilkaarsvurdering

    private fun vilkaarsVurderingData() = VilkaarVurderingData("en kommentar", Tidspunkt.now().toLocalDatetimeUTC(), "saksbehandler")

    private fun vilkaarsvurderingResultat(utfall: VilkaarsvurderingUtfall) =
        VilkaarsvurderingResultat(
            utfall = utfall,
            kommentar = "Kommentar",
            tidspunkt = LocalDateTime.now(),
            saksbehandler = "Saksbehandler",
        )
}
