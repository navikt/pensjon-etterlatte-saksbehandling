package no.nav.etterlatte.vilkaarsvurdering

import GrunnlagTestData
import behandling.VirkningstidspunktTestData
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldInclude
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Foedselsdato
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarOpplysningsType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarTypeOgUtfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VurdertVilkaar
import no.nav.etterlatte.vilkaarsvurdering.config.DataSourceBuilder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import vilkaarsvurdering.VilkaarsvurderingTestData
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VilkaarsvurderingServiceTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var service: VilkaarsvurderingService
    private val uuid: UUID = UUID.randomUUID()
    private val sendToRapid: (String, UUID) -> Unit = mockk(relaxed = true)

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        val ds = DataSourceBuilder(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password
        ).apply { migrate() }
        service = VilkaarsvurderingService(VilkaarsvurderingRepositoryImpl(ds.dataSource()), sendToRapid)
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `Skal opprette en vilkaarsvurdering for foerstegangsbehandling av barnepensjon med grunnlagsopplysninger`() {
        val grunnlag: Grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        val vilkaarsvurdering = service.opprettVilkaarsvurdering(
            uuid,
            SakType.BARNEPENSJON,
            BehandlingType.FØRSTEGANGSBEHANDLING,
            VirkningstidspunktTestData.virkningstidsunkt(),
            grunnlag,
            null
        )

        vilkaarsvurdering shouldNotBe null
        vilkaarsvurdering.behandlingId shouldBe uuid
        vilkaarsvurdering.vilkaar shouldHaveSize 5
        vilkaarsvurdering.vilkaar.first { it.hovedvilkaar.type == VilkaarType.ALDER_BARN }.let { vilkaar ->
            vilkaar.grunnlag shouldNotBe null
            vilkaar.grunnlag!! shouldHaveSize 3

            requireNotNull(vilkaar.grunnlag?.get(0)).let {
                it.opplysningsType shouldBe VilkaarOpplysningsType.FOEDSELSDATO
                val opplysning: Foedselsdato = objectMapper.readValue(it.opplysning.toJson())
                opplysning.foedselsdato shouldBe grunnlag.soeker.hentFoedselsdato()?.verdi
                opplysning.foedselsnummer shouldBe grunnlag.soeker.hentFoedselsnummer()?.verdi
            }
            requireNotNull(vilkaar.grunnlag?.get(1)).let {
                it.opplysningsType shouldBe VilkaarOpplysningsType.DOEDSDATO
                val opplysning: Doedsdato = objectMapper.readValue(it.opplysning.toJson())
                opplysning.foedselsnummer shouldBe grunnlag.hentAvdoed().hentFoedselsnummer()?.verdi
                opplysning.doedsdato shouldBe grunnlag.hentAvdoed().hentDoedsdato()?.verdi
            }
        }
    }

    @Test
    fun `Skal oppdatere en vilkaarsvurdering med vilkaar som har oppfylt hovedvilkaar`() {
        opprettVilkaarsvurdering()

        val vurdering = vilkaarsVurderingData()
        val vurdertVilkaar = VurdertVilkaar(
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
        opprettVilkaarsvurdering()

        val vurdering = vilkaarsVurderingData()
        val vurdertVilkaar = VurdertVilkaar(
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
    fun `Skal oppdatere en vilkaarsvurdering med vilkaar som ikke har oppfylt hovedvilkaar eller unntaksvilkaar`() {
        opprettVilkaarsvurdering()

        val vurdering = vilkaarsVurderingData()
        val vurdertVilkaar = VurdertVilkaar(
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
        val grunnlag: Grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        val vilkaarsvurdering = service.opprettVilkaarsvurdering(
            uuid,
            SakType.BARNEPENSJON,
            BehandlingType.REVURDERING,
            VirkningstidspunktTestData.virkningstidsunkt(),
            grunnlag,
            RevurderingAarsak.SOEKER_DOD
        )

        vilkaarsvurdering shouldNotBe null
        vilkaarsvurdering.behandlingId shouldBe uuid
        vilkaarsvurdering.vilkaar shouldHaveSize 1
        vilkaarsvurdering.vilkaar.first { it.hovedvilkaar.type == VilkaarType.FORMAAL }.let { vilkaar ->
            vilkaar.grunnlag shouldBe null
            vilkaar.hovedvilkaar.type shouldBe VilkaarType.FORMAAL
        }
    }

    @Test
    fun `Skal publisere vilkaarsvurdering paa kafka paa et format vedtaksvurering og beregning forstaar`() {
        val vilkaarsvurdering = VilkaarsvurderingTestData.oppfylt
        val emptyGrunnlag = Grunnlag.empty()
        val vilkaarsvurderingIntern = VilkaarsvurderingIntern(
            vilkaarsvurdering.behandlingId,
            emptyList(),
            VirkningstidspunktTestData.virkningstidsunkt(),
            vilkaarsvurdering.resultat,
            emptyGrunnlag.metadata
        )
        val payloadContent = slot<String>()

        service.publiserVilkaarsvurdering(vilkaarsvurderingIntern, emptyGrunnlag, detaljertBehandling())

        verify(exactly = 1) {
            sendToRapid.invoke(capture(payloadContent), vilkaarsvurdering.behandlingId)
        }
        payloadContent.captured shouldInclude "vilkaarsvurdering"
        payloadContent.captured shouldInclude "grunnlag"
        payloadContent.captured shouldInclude "sak"
        payloadContent.captured shouldInclude "virkningstidspunkt"
        payloadContent.captured shouldInclude "behandling"
        payloadContent.captured shouldInclude "fnrSoeker"
    }

    @Test
    fun `Skal slette alle vilkaarsvurderinger i en sak ved vedlikehold`() {
        opprettVilkaarsvurdering()

        val vilkaarsvurdering = service.hentVilkaarsvurdering(uuid)
        assertNotNull(vilkaarsvurdering)
        assertEquals(vilkaarsvurdering?.grunnlagsmetadata?.sakId, 1)

        service.slettSak(1)

        assertNull(service.hentVilkaarsvurdering(uuid))
    }

    private fun opprettVilkaarsvurdering() {
        service.opprettVilkaarsvurdering(
            uuid,
            SakType.BARNEPENSJON,
            BehandlingType.FØRSTEGANGSBEHANDLING,
            VirkningstidspunktTestData.virkningstidsunkt(),
            GrunnlagTestData().hentOpplysningsgrunnlag(),
            null
        )
    }

    private fun detaljertBehandling() = mockk<DetaljertBehandling>().apply {
        every { id } returns UUID.randomUUID()
        every { sak } returns 1L
        every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
        every { soeker } returns "10095512345"
    }

    private fun vilkaarsVurderingData() =
        VilkaarVurderingData("en kommentar", LocalDateTime.now(), "saksbehandler")
}