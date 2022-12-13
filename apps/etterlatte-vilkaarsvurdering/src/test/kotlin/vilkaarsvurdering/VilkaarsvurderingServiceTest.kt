package no.nav.etterlatte.vilkaarsvurdering

import GrunnlagTestData
import behandling.VirkningstidspunktTestData
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarOpplysningType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarTypeOgUtfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VurdertVilkaar
import no.nav.etterlatte.vilkaarsvurdering.behandling.BehandlingKlient
import no.nav.etterlatte.vilkaarsvurdering.config.DataSourceBuilder
import no.nav.etterlatte.vilkaarsvurdering.grunnlag.GrunnlagKlient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
    private lateinit var repository: VilkaarsvurderingRepository2
    private lateinit var ds: DataSource
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val grunnlagKlient = mockk<GrunnlagKlient>()
    private val uuid: UUID = UUID.randomUUID()
    private val accesstoken = "token"

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        ds = DataSourceBuilder(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password
        ).apply { migrate() }.dataSource()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns mockk<DetaljertBehandling>().apply {
            every { id } returns UUID.randomUUID()
            every { sak } returns 1L
            every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
            every { soeker } returns "10095512345"
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt()
            every { revurderingsaarsak } returns null
        }

        repository = VilkaarsvurderingRepository2Impl(ds)
        service = VilkaarsvurderingService(
            repository,
            behandlingKlient,
            grunnlagKlient
        )
    }

    private fun cleanDatabase() {
        ds.connection.use {
            it.prepareStatement("TRUNCATE vilkaarsvurdering_v2 CASCADE").apply { execute() }
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
            service.hentEllerOpprettVilkaarsvurdering(
                uuid,
                accesstoken
            )
        }

        vilkaarsvurdering shouldNotBe null
        vilkaarsvurdering.behandlingId shouldBe uuid
        vilkaarsvurdering.vilkaar shouldHaveSize 5
        vilkaarsvurdering.vilkaar.first { it.hovedvilkaar.type == VilkaarType.ALDER_BARN }.let { vilkaar ->
            vilkaar.grunnlag shouldNotBe null
            vilkaar.grunnlag!! shouldHaveSize 3

            requireNotNull(vilkaar.grunnlag?.get(0)).let {
                it.opplysningsType shouldBe VilkaarOpplysningType.SOEKER_FOEDSELSDATO
                val opplysning: LocalDate = objectMapper.readValue(it.opplysning!!.toJson())
                opplysning shouldBe grunnlag.soeker.hentFoedselsdato()?.verdi
            }
            requireNotNull(vilkaar.grunnlag?.get(1)).let {
                it.opplysningsType shouldBe VilkaarOpplysningType.AVDOED_DOEDSDATO
                val opplysning: LocalDate? = objectMapper.readValue(it.opplysning!!.toJson())
                opplysning shouldBe grunnlag.hentAvdoed().hentDoedsdato()?.verdi
            }
        }
    }

    @Test
    fun `Skal oppdatere en vilkaarsvurdering med vilkaar som har oppfylt hovedvilkaar`() {
        val vilkaarsvurdering = runBlocking { opprettVilkaarsvurdering() }

        val vurdering = vilkaarsVurderingData()
        val vurdertVilkaar = VurdertVilkaar(
            vilkaarId = vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.FORTSATT_MEDLEMSKAP)?.id!!,
            hovedvilkaar = VilkaarTypeOgUtfall(type = VilkaarType.FORTSATT_MEDLEMSKAP, resultat = Utfall.OPPFYLT),
            vurdering = vurdering
        )

        val vilkaarsvurderingOppdatert = service.oppdaterVurderingPaaVilkaar(uuid, vurdertVilkaar)

        vilkaarsvurderingOppdatert shouldNotBe null
        vilkaarsvurderingOppdatert.vilkaar
            .first { it.hovedvilkaar.type == VilkaarType.FORTSATT_MEDLEMSKAP }
            .let { vilkaar ->
                vilkaar.hovedvilkaar.resultat shouldBe Utfall.OPPFYLT
                vilkaar.unntaksvilkaar?.forEach {
                    it.resultat shouldBe null
                }
                vilkaar.vurdering?.let {
                    it.saksbehandler shouldBe vurdering.saksbehandler
                    it.kommentar shouldBe vurdering.kommentar
                    it.tidspunkt shouldNotBe null
                }
            }
    }

    @Test
    fun `Skal oppdatere en vilkaarsvurdering med vilkaar som har oppfylt unntaksvilkaar`() {
        val vilkaarsvurdering = runBlocking { opprettVilkaarsvurdering() }

        val vurdering = vilkaarsVurderingData()
        val vurdertVilkaar = VurdertVilkaar(
            vilkaarId = vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.FORTSATT_MEDLEMSKAP)?.id!!,
            hovedvilkaar = VilkaarTypeOgUtfall(type = VilkaarType.FORTSATT_MEDLEMSKAP, resultat = Utfall.IKKE_OPPFYLT),
            unntaksvilkaar = VilkaarTypeOgUtfall(
                type = VilkaarType.FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRE_MINST_20_AAR_SAMLET_BOTID,
                resultat = Utfall.OPPFYLT
            ),
            vurdering = vurdering
        )

        val vilkaarsvurderingOppdatert = service.oppdaterVurderingPaaVilkaar(uuid, vurdertVilkaar)

        vilkaarsvurderingOppdatert shouldNotBe null
        vilkaarsvurderingOppdatert.vilkaar
            .first { it.hovedvilkaar.type == VilkaarType.FORTSATT_MEDLEMSKAP }
            .let { vilkaar ->
                vilkaar.hovedvilkaar.resultat shouldBe Utfall.IKKE_OPPFYLT
                val unntaksvilkaar = vilkaar.unntaksvilkaar?.first { it.type == vurdertVilkaar.unntaksvilkaar?.type }
                unntaksvilkaar?.resultat shouldBe Utfall.OPPFYLT

                vilkaar.vurdering?.let {
                    it.saksbehandler shouldBe vurdering.saksbehandler
                    it.kommentar shouldBe vurdering.kommentar
                    it.tidspunkt shouldNotBe null
                }
            }
    }

    @Test
    fun `Skal oppdatere vilkaarsvurdering med vilkaar som ikke har oppfylt hovedvilkaar el unntaksvilkaar`() {
        val vilkaarsvurdering = runBlocking { opprettVilkaarsvurdering() }

        val vurdering = vilkaarsVurderingData()
        val vurdertVilkaar = VurdertVilkaar(
            vilkaarId = vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.FORTSATT_MEDLEMSKAP)?.id!!,
            hovedvilkaar = VilkaarTypeOgUtfall(type = VilkaarType.FORTSATT_MEDLEMSKAP, resultat = Utfall.IKKE_OPPFYLT),
            vurdering = vurdering
        )

        val vilkaarsvurderingOppdatert = service.oppdaterVurderingPaaVilkaar(uuid, vurdertVilkaar)

        vilkaarsvurderingOppdatert shouldNotBe null
        vilkaarsvurderingOppdatert.vilkaar
            .first { it.hovedvilkaar.type == VilkaarType.FORTSATT_MEDLEMSKAP }
            .let { vilkaar ->
                vilkaar.hovedvilkaar.resultat shouldBe Utfall.IKKE_OPPFYLT
                vilkaar.unntaksvilkaar?.forEach {
                    it.resultat shouldBe Utfall.IKKE_OPPFYLT
                }
                vilkaar.vurdering?.let {
                    it.saksbehandler shouldBe vurdering.saksbehandler
                    it.kommentar shouldBe vurdering.kommentar
                    it.tidspunkt shouldNotBe null
                }
            }
    }

    @Test
    fun `Skal opprette en vilkaarsvurdering for revurdering for doed soeker`() {
        val uuid = UUID.randomUUID()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { behandlingKlient.hentBehandling(uuid, any()) } returns mockk<DetaljertBehandling>().apply {
            every { id } returns uuid
            every { sak } returns 1L
            every { behandlingType } returns BehandlingType.REVURDERING
            every { soeker } returns "10095512345"
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt()
            every { revurderingsaarsak } returns RevurderingAarsak.SOEKER_DOD
        }

        val vilkaarsvurdering = runBlocking {
            service.hentEllerOpprettVilkaarsvurdering(
                uuid,
                accesstoken
            )
        }

        vilkaarsvurdering shouldNotBe null
        vilkaarsvurdering.behandlingId shouldBe uuid
        vilkaarsvurdering.vilkaar shouldHaveSize 1
        vilkaarsvurdering.vilkaar.first { it.hovedvilkaar.type == VilkaarType.FORMAAL }.let { vilkaar ->
            vilkaar.grunnlag shouldBe emptyList()
            vilkaar.hovedvilkaar.type shouldBe VilkaarType.FORMAAL
        }
    }

    @Test
    fun `Skal slette alle vilkaarsvurderinger i en sak ved vedlikehold`() {
        val vilkaarsvurdering = runBlocking { service.hentEllerOpprettVilkaarsvurdering(uuid, accesstoken) }
        assertNotNull(vilkaarsvurdering)
        assertEquals(vilkaarsvurdering.grunnlagsmetadata.sakId, 1)

        service.slettSak(1)

        assertNull(repository.hent(uuid))
    }

    private suspend fun opprettVilkaarsvurdering(): VilkaarsvurderingIntern {
        return service.hentEllerOpprettVilkaarsvurdering(
            uuid,
            accesstoken
        )
    }

    private fun vilkaarsVurderingData() =
        VilkaarVurderingData("en kommentar", LocalDateTime.now(), "saksbehandler")
}