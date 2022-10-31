package vilkaarsvurdering

import GrunnlagTestData
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Foedselsdato
import no.nav.etterlatte.vilkaarsvurdering.SakType
import no.nav.etterlatte.vilkaarsvurdering.Utfall
import no.nav.etterlatte.vilkaarsvurdering.VilkaarOpplysningsType
import no.nav.etterlatte.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.vilkaarsvurdering.VilkaarTypeOgUtfall
import no.nav.etterlatte.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepositoryImpl
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.vilkaarsvurdering.VurdertVilkaar
import no.nav.etterlatte.vilkaarsvurdering.config.DataSourceBuilder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VilkaarsvurderingServiceTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var service: VilkaarsvurderingService
    private val uuid: UUID = UUID.randomUUID()

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        val ds = DataSourceBuilder(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password
        ).apply { migrate() }
        service = VilkaarsvurderingService((VilkaarsvurderingRepositoryImpl(ds.dataSource())))
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
            objectMapper.createObjectNode(),
            grunnlag
        )

        vilkaarsvurdering shouldNotBe null
        vilkaarsvurdering.behandlingId shouldBe uuid
        vilkaarsvurdering.vilkaar shouldHaveSize 5
        vilkaarsvurdering.vilkaar.first { it.hovedvilkaar.type == VilkaarType.ALDER_BARN }.let { vilkaar ->
            vilkaar.grunnlag shouldNotBe null
            vilkaar.grunnlag!! shouldHaveSize 2

            requireNotNull(vilkaar.grunnlag?.get(0)).let {
                it.opplysningsType shouldBe VilkaarOpplysningsType.FOEDSELSDATO
                val opplysning = it.opplysning as Foedselsdato
                opplysning.foedselsdato shouldBe grunnlag.soeker.hentFoedselsdato()?.verdi
                opplysning.foedselsnummer shouldBe grunnlag.soeker.hentFoedselsnummer()?.verdi
            }
            requireNotNull(vilkaar.grunnlag?.get(1)).let {
                it.opplysningsType shouldBe VilkaarOpplysningsType.DOEDSDATO
                val opplysning = it.opplysning as Doedsdato
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

    private fun opprettVilkaarsvurdering() {
        service.opprettVilkaarsvurdering(
            uuid,
            SakType.BARNEPENSJON,
            BehandlingType.FØRSTEGANGSBEHANDLING,
            objectMapper.createObjectNode(),
            GrunnlagTestData().hentOpplysningsgrunnlag()
        )
    }

    private fun vilkaarsVurderingData() =
        VilkaarVurderingData("en kommentar", LocalDateTime.now(), "saksbehandler")
}