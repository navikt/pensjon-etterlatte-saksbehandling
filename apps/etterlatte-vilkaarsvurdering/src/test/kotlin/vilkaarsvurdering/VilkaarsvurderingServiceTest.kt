package no.nav.etterlatte.vilkaarsvurdering

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
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarOpplysningType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.vilkaarsvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vilkaarsvurdering.klienter.GrunnlagKlient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.lang.IllegalStateException
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
    private val accesstoken = "token"

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        ds = DataSourceBuilder.createDataSource(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password
        ).also { it.migrate() }

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { behandlingKlient.kanSetteBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns mockk<DetaljertBehandling>().apply {
            every { id } returns UUID.randomUUID()
            every { sak } returns 1L
            every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
            every { soeker } returns "10095512345"
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt()
            every { revurderingsaarsak } returns null
        }
        coEvery { behandlingKlient.hentSak(any(), any()) } returns mockk<Sak>().apply {
            every { id } returns 1L
            every { ident } returns "ident"
            every { sakType } returns SakType.BARNEPENSJON
        }

        repository = VilkaarsvurderingRepository(ds)
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
            vilkaar.grunnlag!! shouldHaveSize 2

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

        runBlocking {
            val vilkaarsvurderingOppdatert =
                service.oppdaterVurderingPaaVilkaar(uuid, accesstoken, vurdertVilkaar)

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

        runBlocking {
            val vilkaarsvurderingOppdatert = service.oppdaterVurderingPaaVilkaar(uuid, accesstoken, vurdertVilkaar)

            vilkaarsvurderingOppdatert shouldNotBe null
            vilkaarsvurderingOppdatert.vilkaar
                .first { it.hovedvilkaar.type == VilkaarType.FORTSATT_MEDLEMSKAP }
                .let { vilkaar ->
                    vilkaar.hovedvilkaar.resultat shouldBe Utfall.IKKE_OPPFYLT
                    val unntaksvilkaar =
                        vilkaar.unntaksvilkaar?.first { it.type == vurdertVilkaar.unntaksvilkaar?.type }
                    unntaksvilkaar?.resultat shouldBe Utfall.OPPFYLT

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
            vilkaarId = vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.FORTSATT_MEDLEMSKAP)?.id!!,
            hovedvilkaar = VilkaarTypeOgUtfall(type = VilkaarType.FORTSATT_MEDLEMSKAP, resultat = Utfall.IKKE_OPPFYLT),
            vurdering = vurdering
        )

        runBlocking {
            val vilkaarsvurderingOppdatert = service.oppdaterVurderingPaaVilkaar(uuid, accesstoken, vurdertVilkaar)

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
    }

    @Test
    fun `skal ikke lagre hvis status-sjekk mot behandling feiler`() {
        val vilkaarsvurdering = runBlocking { opprettVilkaarsvurdering() }
        coEvery { behandlingKlient.kanSetteBehandlingStatusVilkaarsvurdert(any(), any()) } returns false

        val vurdering = vilkaarsVurderingData()
        val vurdertVilkaar = VurdertVilkaar(
            vilkaarId = vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.FORTSATT_MEDLEMSKAP)?.id!!,
            hovedvilkaar = VilkaarTypeOgUtfall(type = VilkaarType.FORTSATT_MEDLEMSKAP, resultat = Utfall.IKKE_OPPFYLT),
            vurdering = vurdering
        )

        runBlocking {
            assertThrows<IllegalStateException> {
                service.oppdaterVurderingPaaVilkaar(uuid, accesstoken, vurdertVilkaar)
            }
            val actual = service.hentEllerOpprettVilkaarsvurdering(uuid, accesstoken)
            actual shouldBe vilkaarsvurdering
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

    private suspend fun opprettVilkaarsvurdering(): Vilkaarsvurdering {
        return service.hentEllerOpprettVilkaarsvurdering(
            uuid,
            accesstoken
        )
    }

    private fun vilkaarsVurderingData() =
        VilkaarVurderingData("en kommentar", LocalDateTime.now(), "saksbehandler")
}