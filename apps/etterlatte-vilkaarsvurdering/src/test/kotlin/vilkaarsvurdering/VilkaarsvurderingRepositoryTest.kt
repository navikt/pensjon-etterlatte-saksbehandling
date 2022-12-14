package no.nav.etterlatte.vilkaarsvurdering

import GrunnlagTestData
import behandling.VirkningstidspunktTestData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.vilkaarsvurdering.barnepensjon.BarnepensjonVilkaar
import no.nav.etterlatte.vilkaarsvurdering.config.DataSourceBuilder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VilkaarsvurderingRepository2Test {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var vilkaarsvurderingRepository: VilkaarsvurderingRepository
    private lateinit var ds: DataSource

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()

        ds = DataSourceBuilder(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password
        ).apply { migrate() }.dataSource()

        vilkaarsvurderingRepository = VilkaarsvurderingRepository(ds)
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
    fun `skal opprette vilkaarsvurdering`() {
        val opprettetVilkaarsvurdering = vilkaarsvurderingRepository.opprettVilkaarsvurdering(vilkaarsvurdering)

        opprettetVilkaarsvurdering shouldNotBe null
    }

    @Test
    fun `skal hente vilkaarsvurdering`() {
        vilkaarsvurderingRepository.opprettVilkaarsvurdering(vilkaarsvurdering)

        val hentetVilkaarsvurdering = vilkaarsvurderingRepository.hent(vilkaarsvurdering.behandlingId)

        hentetVilkaarsvurdering shouldNotBe null
    }

    @Test
    fun `skal sette resultat paa vilkaarsvurdering`() {
        val opprettetVilkaarsvurdering = vilkaarsvurderingRepository.opprettVilkaarsvurdering(vilkaarsvurdering)

        val oppdatertVilkaarsvurdering =
            vilkaarsvurderingRepository.lagreVilkaarsvurderingResultat(
                opprettetVilkaarsvurdering.behandlingId,
                vilkaarsvurderingResultat
            )

        with(oppdatertVilkaarsvurdering.resultat!!) {
            utfall shouldBe vilkaarsvurderingResultat.utfall
            kommentar shouldBe vilkaarsvurderingResultat.kommentar
            // TODO tidspunkt shouldBe vilkaarsvurderingResultat.tidspunkt
            saksbehandler shouldBe vilkaarsvurderingResultat.saksbehandler
        }
    }

    @Test
    fun `skal sette resultat paa vilkaarsvurdering og slette det`() {
        val opprettetVilkaarsvurdering = vilkaarsvurderingRepository.opprettVilkaarsvurdering(vilkaarsvurdering)

        vilkaarsvurderingRepository.lagreVilkaarsvurderingResultat(
            opprettetVilkaarsvurdering.behandlingId,
            vilkaarsvurderingResultat
        )
        val oppdatertVilkaarsvurdering =
            vilkaarsvurderingRepository.slettVilkaarsvurderingResultat(opprettetVilkaarsvurdering.behandlingId)

        oppdatertVilkaarsvurdering.resultat shouldBe null
    }

    @Test
    fun `skal sette resultat paa vilkaar`() {
        val opprettetVilkaarsvurdering = vilkaarsvurderingRepository.opprettVilkaarsvurdering(vilkaarsvurdering)
        val vilkaar = opprettetVilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.ALDER_BARN)
        val vurdertVilkaar = VurdertVilkaar(
            vilkaarId = vilkaar?.id!!,
            vurdering = VilkaarVurderingData(
                kommentar = "En test",
                tidspunkt = LocalDateTime.now(),
                saksbehandler = "saksbehandler1"
            ),
            hovedvilkaar = VilkaarTypeOgUtfall(vilkaar.hovedvilkaar.type, Utfall.IKKE_VURDERT),
            unntaksvilkaar = vilkaar.unntaksvilkaar?.let {
                val unntaksvilkaar =
                    it.first { delvilkaar -> delvilkaar.type == VilkaarType.ALDER_BARN_UNNTAK_UTDANNING }
                VilkaarTypeOgUtfall(unntaksvilkaar.type, Utfall.IKKE_VURDERT)
            }
        )

        val oppdatertVilkaarsvurdering =
            vilkaarsvurderingRepository.lagreVilkaarResultat(
                opprettetVilkaarsvurdering.behandlingId,
                vurdertVilkaar
            )

        oppdatertVilkaarsvurdering.vilkaar.first { it.id == vilkaar.id }.let {
            with(it.vurdering!!) {
                kommentar shouldBe vurdertVilkaar.vurdering.kommentar
                // TODO tidspunkt shouldBe vurdertVilkaar.vurdering.tidspunkt
                saksbehandler shouldBe vurdertVilkaar.vurdering.saksbehandler
            }
            it.hovedvilkaar.resultat shouldBe Utfall.IKKE_VURDERT
            it.unntaksvilkaar?.first { delvilkaar -> delvilkaar.type == VilkaarType.ALDER_BARN_UNNTAK_UTDANNING }
                ?.resultat shouldBe Utfall.IKKE_VURDERT
        }
    }

    @Test
    fun `skal slette resultat paa vilkaar`() {
        val opprettetVilkaarsvurdering = vilkaarsvurderingRepository.opprettVilkaarsvurdering(vilkaarsvurdering)
        val vilkaar = opprettetVilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.ALDER_BARN)
        val vurdertVilkaar = VurdertVilkaar(
            vilkaarId = vilkaar?.id!!,
            vurdering = VilkaarVurderingData(
                kommentar = "En test",
                tidspunkt = LocalDateTime.now(),
                saksbehandler = "saksbehandler1"
            ),
            hovedvilkaar = VilkaarTypeOgUtfall(vilkaar.hovedvilkaar.type, Utfall.IKKE_VURDERT),
            unntaksvilkaar = vilkaar.unntaksvilkaar?.let {
                val unntaksvilkaar =
                    it.first { delvilkaar -> delvilkaar.type == VilkaarType.ALDER_BARN_UNNTAK_UTDANNING }
                VilkaarTypeOgUtfall(unntaksvilkaar.type, Utfall.IKKE_VURDERT)
            }
        )

        vilkaarsvurderingRepository.lagreVilkaarResultat(
            opprettetVilkaarsvurdering.behandlingId,
            vurdertVilkaar
        )

        val oppdatertVilkaarsvurdering =
            vilkaarsvurderingRepository.slettVilkaarResultat(
                opprettetVilkaarsvurdering.behandlingId,
                requireNotNull(vilkaar.id)
            )

        oppdatertVilkaarsvurdering.vilkaar.first { it.id == vilkaar.id }.let {
            it.vurdering shouldBe null
            it.hovedvilkaar.resultat shouldBe null
            it.unntaksvilkaar?.first { delvilkaar -> delvilkaar.type == VilkaarType.ALDER_BARN_UNNTAK_UTDANNING }
                ?.resultat shouldBe null
        }
    }

    companion object {
        val vilkaarsvurdering = Vilkaarsvurdering(
            sakId = 1234L,
            behandlingId = UUID.randomUUID(),
            grunnlagVersjon = 1L,
            virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt().dato,
            vilkaar = BarnepensjonVilkaar.inngangsvilkaar(
                grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag(),
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt()
            )
        )

        val vilkaarsvurderingResultat = VilkaarsvurderingResultat(
            utfall = VilkaarsvurderingUtfall.OPPFYLT,
            kommentar = "Alt ser bra ut",
            tidspunkt = LocalDateTime.now(),
            saksbehandler = "saksbehandler1"
        )
    }
}

fun Vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(vilkaarType: VilkaarType): Vilkaar? =
    vilkaar.find { it.hovedvilkaar.type == vilkaarType }