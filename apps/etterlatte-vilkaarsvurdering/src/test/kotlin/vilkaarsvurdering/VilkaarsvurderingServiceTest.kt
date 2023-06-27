package no.nav.etterlatte.vilkaarsvurdering

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SOEKNAD_MOTTATT_DATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadMottattDato
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarOpplysningType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.vilkaarsvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vilkaarsvurdering.klienter.GrunnlagKlient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VilkaarsvurderingServiceTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var service: VilkaarsvurderingService
    private lateinit var repository: VilkaarsvurderingRepository
    private lateinit var ds: DataSource
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val grunnlagKlient = mockk<GrunnlagKlient>()
    private val uuid: UUID = UUID.randomUUID()
    private val brukerTokenInfo = BrukerTokenInfo.of("token", "s1", null, null, null)

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        ds = DataSourceBuilder.createDataSource(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password
        ).also { it.migrate() }
    }

    @BeforeEach
    fun beforeEach() {
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { behandlingKlient.kanSetteBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns mockk<DetaljertBehandling>().apply {
            every { id } returns UUID.randomUUID()
            every { sak } returns 1L
            every { sakType } returns SakType.BARNEPENSJON
            every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
            every { soeker } returns "10095512345"
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt()
            every { revurderingsaarsak } returns null
        }

        repository = VilkaarsvurderingRepository(ds, DelvilkaarRepository())
        service = VilkaarsvurderingService(
            repository,
            behandlingKlient,
            grunnlagKlient
        )
    }

    private fun cleanDatabase() {
        ds.connection.use {
            it.prepareStatement("TRUNCATE vilkaarsvurdering CASCADE").apply { execute() }
        }
    }

    @AfterEach
    fun afterEach() {
        cleanDatabase()
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `Skal opprette vilkaarsvurdering for foerstegangsbehandling barnepensjon med grunnlagsopplysninger`() {
        val grunnlag: Grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        val vilkaarsvurdering = runBlocking {
            service.opprettVilkaarsvurdering(uuid, brukerTokenInfo)
        }

        vilkaarsvurdering shouldNotBe null
        vilkaarsvurdering.behandlingId shouldBe uuid
        vilkaarsvurdering.vilkaar shouldHaveSize 6
        vilkaarsvurdering.vilkaar.first { it.hovedvilkaar.type == VilkaarType.BP_ALDER_BARN }.let { vilkaar ->
            vilkaar.grunnlag shouldNotBe null
            vilkaar.grunnlag shouldHaveSize 2

            vilkaar.grunnlag[0].let {
                it.opplysningsType shouldBe VilkaarOpplysningType.SOEKER_FOEDSELSDATO
                val opplysning: LocalDate = objectMapper.readValue(it.opplysning!!.toJson())
                opplysning shouldBe grunnlag.soeker.hentFoedselsdato()?.verdi
            }
            vilkaar.grunnlag[1].let {
                it.opplysningsType shouldBe VilkaarOpplysningType.AVDOED_DOEDSDATO
                val opplysning: LocalDate? = objectMapper.readValue(it.opplysning!!.toJson())
                opplysning shouldBe grunnlag.hentAvdoed().hentDoedsdato()?.verdi
            }
        }
    }

    @Test
    fun `Skal opprette vilkaarsvurdering for foerstegangsbehandling omstillingssoeknad med grunnlagsopplysninger`() {
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns mockk<DetaljertBehandling>().apply {
            every { id } returns UUID.randomUUID()
            every { sak } returns 2L
            every { sakType } returns SakType.OMSTILLINGSSTOENAD
            every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
            every { soeker } returns "10095512345"
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt()
            every { revurderingsaarsak } returns null
        }

        val soeknadMottattDato = LocalDateTime.now()
        val soeknadMottattDatoOpplysning = Opplysning.Konstant(
            UUID.randomUUID(),
            kilde,
            SoeknadMottattDato(mottattDato = soeknadMottattDato).toJsonNode()
        )
        val grunnlag = GrunnlagTestData(
            opplysningsmapSakOverrides = mapOf(SOEKNAD_MOTTATT_DATO to soeknadMottattDatoOpplysning)
        ).hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag

        val vilkaarsvurdering = runBlocking {
            service.opprettVilkaarsvurdering(uuid, brukerTokenInfo)
        }

        vilkaarsvurdering shouldNotBe null
        vilkaarsvurdering.behandlingId shouldBe uuid
        vilkaarsvurdering.vilkaar shouldHaveSize 8
        vilkaarsvurdering.vilkaar.first { it.hovedvilkaar.type == VilkaarType.OMS_ETTERLATTE_LEVER }.let { vilkaar ->
            vilkaar.grunnlag shouldBe emptyList()
        }
        vilkaarsvurdering.vilkaar.find { it.hovedvilkaar.type == VilkaarType.OMS_AKTIVITET_ETTER_6_MND }?.let { vilkaar ->
            vilkaar.grunnlag shouldNotBe null
            vilkaar.grunnlag shouldHaveSize 2

            vilkaar.grunnlag[0].let {
                it.opplysningsType shouldBe VilkaarOpplysningType.AVDOED_DOEDSDATO
                val opplysning: LocalDate = objectMapper.readValue(it.opplysning!!.toJson())
                opplysning shouldBe grunnlag.hentAvdoed().hentDoedsdato()?.verdi
            }
            vilkaar.grunnlag[1].let {
                it.opplysningsType shouldBe VilkaarOpplysningType.SOEKNAD_MOTTATT_DATO
                val opplysning: SoeknadMottattDato = objectMapper.readValue(it.opplysning!!.toJson())
                opplysning.mottattDato shouldBe soeknadMottattDato
            }
        }
    }

    @Test
    fun `Skal oppdatere en vilkaarsvurdering med vilkaar som har oppfylt hovedvilkaar`() {
        val vilkaarsvurdering = runBlocking { opprettVilkaarsvurdering() }

        val vurdering = vilkaarsVurderingData()
        val vurdertVilkaar = VurdertVilkaar(
            vilkaarId = vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.BP_FORTSATT_MEDLEMSKAP)?.id!!,
            hovedvilkaar = VilkaarTypeOgUtfall(type = VilkaarType.BP_FORTSATT_MEDLEMSKAP, resultat = Utfall.OPPFYLT),
            vurdering = vurdering
        )

        runBlocking {
            val vilkaarsvurderingOppdatert =
                service.oppdaterVurderingPaaVilkaar(uuid, brukerTokenInfo, vurdertVilkaar)

            vilkaarsvurderingOppdatert shouldNotBe null
            vilkaarsvurderingOppdatert.vilkaar
                .first { it.hovedvilkaar.type == VilkaarType.BP_FORTSATT_MEDLEMSKAP }
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
        val vurdertVilkaar = VurdertVilkaar(
            vilkaarId = vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.BP_FORTSATT_MEDLEMSKAP)?.id!!,
            hovedvilkaar = VilkaarTypeOgUtfall(
                type = VilkaarType.BP_FORTSATT_MEDLEMSKAP,
                resultat = Utfall.IKKE_OPPFYLT
            ),
            unntaksvilkaar = VilkaarTypeOgUtfall(
                type = VilkaarType.BP_FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRE_MINST_20_AAR_SAMLET_BOTID,
                resultat = Utfall.OPPFYLT
            ),
            vurdering = vurdering
        )

        runBlocking {
            val vilkaarsvurderingOppdatert = service.oppdaterVurderingPaaVilkaar(uuid, brukerTokenInfo, vurdertVilkaar)

            vilkaarsvurderingOppdatert shouldNotBe null
            vilkaarsvurderingOppdatert.vilkaar
                .first { it.hovedvilkaar.type == VilkaarType.BP_FORTSATT_MEDLEMSKAP }
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
        val vurdertVilkaar = VurdertVilkaar(
            vilkaarId = vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.BP_FORTSATT_MEDLEMSKAP)?.id!!,
            hovedvilkaar = VilkaarTypeOgUtfall(
                type = VilkaarType.BP_FORTSATT_MEDLEMSKAP,
                resultat = Utfall.IKKE_OPPFYLT
            ),
            vurdering = vurdering
        )

        runBlocking {
            val vilkaarsvurderingOppdatert = service.oppdaterVurderingPaaVilkaar(uuid, brukerTokenInfo, vurdertVilkaar)

            vilkaarsvurderingOppdatert shouldNotBe null
            vilkaarsvurderingOppdatert.vilkaar
                .first { it.hovedvilkaar.type == VilkaarType.BP_FORTSATT_MEDLEMSKAP }
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
        val vurdertVilkaar = VurdertVilkaar(
            vilkaarId = vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.BP_FORTSATT_MEDLEMSKAP)?.id!!,
            hovedvilkaar = VilkaarTypeOgUtfall(
                type = VilkaarType.BP_FORTSATT_MEDLEMSKAP,
                resultat = Utfall.IKKE_OPPFYLT
            ),
            vurdering = vurdering
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
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
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
                        VilkaarVurderingData("kommentar", LocalDateTime.now(), "saksbehandler")
                    )
                )
            }
            service.oppdaterTotalVurdering(
                uuid,
                brukerTokenInfo,
                VilkaarsvurderingResultat(
                    VilkaarsvurderingUtfall.OPPFYLT,
                    "kommentar",
                    LocalDateTime.now(),
                    "saksbehandler"
                )
            )
        }

        val vilkaarsvurdering = service.hentVilkaarsvurdering(uuid)!!
        val kopiertVilkaarsvurdering =
            runBlocking {
                service.kopierVilkaarsvurdering(
                    nyBehandlingId,
                    vilkaarsvurdering.behandlingId,
                    brukerTokenInfo
                )
            }

        Assertions.assertNotNull(kopiertVilkaarsvurdering.resultat)
        Assertions.assertEquals(vilkaarsvurdering.resultat, kopiertVilkaarsvurdering.resultat)
        Assertions.assertEquals(
            vilkaarsvurdering.vilkaar.map { it.vurdering },
            kopiertVilkaarsvurdering.vilkaar.map { it.vurdering }
        )
    }

    @Test
    fun `revurdering kopierer forrige vilkaarsvurdering ved opprettelse`() {
        val grunnlag: Grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val revurderingId = UUID.randomUUID()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery { behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(revurderingId, any()) } returns mockk {
            every { id } returns revurderingId
            every { sak } returns 1L
            every { sakType } returns SakType.BARNEPENSJON
            every { behandlingType } returns BehandlingType.REVURDERING
            every { soeker } returns "10095512345"
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt()
            every { revurderingsaarsak } returns RevurderingAarsak.REGULERING
        }

        val detaljertBehandling = mockk<DetaljertBehandling>()
        every { detaljertBehandling.id } returns uuid
        coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns detaljertBehandling

        val foerstegangsvilkaar = runBlocking {
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
                        VilkaarVurderingData("kommentar", LocalDateTime.now(), "saksbehandler")
                    )
                )
            }
            service.oppdaterTotalVurdering(
                uuid,
                brukerTokenInfo,
                VilkaarsvurderingResultat(
                    VilkaarsvurderingUtfall.OPPFYLT,
                    "kommentar",
                    LocalDateTime.now(),
                    "saksbehandler"
                )
            )

            service.hentVilkaarsvurdering(uuid)!!
        }

        val revurderingsvilkaar = runBlocking { service.opprettVilkaarsvurdering(revurderingId, brukerTokenInfo) }
        assertIsSimilar(foerstegangsvilkaar, revurderingsvilkaar)
        coVerify(exactly = 1) { behandlingKlient.settBehandlingStatusVilkaarsvurdert(revurderingId, brukerTokenInfo) }
    }

    @Test
    fun `kopier vilkaarsvurdering gir NullpointerException hvis det ikke finnes tidligere vilkaarsvurdering`() {
        assertThrows<NullPointerException> {
            runBlocking { service.kopierVilkaarsvurdering(UUID.randomUUID(), uuid, brukerTokenInfo) }
        }
    }

    private fun assertIsSimilar(v1: Vilkaarsvurdering, v2: Vilkaarsvurdering) {
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

    private suspend fun opprettVilkaarsvurdering(): Vilkaarsvurdering {
        return service.opprettVilkaarsvurdering(uuid, brukerTokenInfo)
    }

    private fun vilkaarsVurderingData() =
        VilkaarVurderingData("en kommentar", Tidspunkt.now().toLocalDatetimeUTC(), "saksbehandler")
}