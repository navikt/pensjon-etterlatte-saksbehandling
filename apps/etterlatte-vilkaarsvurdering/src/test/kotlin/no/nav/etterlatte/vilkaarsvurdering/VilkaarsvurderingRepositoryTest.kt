package no.nav.etterlatte.vilkaarsvurdering

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.BarnepensjonVilkaar1967
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VilkaarsvurderingRepositoryTest(
    ds: DataSource,
) {
    private val vilkaarsvurderingRepository = VilkaarsvurderingRepository(ds, DelvilkaarRepository())

    @AfterEach
    fun afterEach() {
        dbExtension.resetDb()
    }

    @Test
    fun `skal opprette vilkaarsvurdering`() {
        val opprettetVilkaarsvurdering = vilkaarsvurderingRepository.opprettVilkaarsvurdering(vilkaarsvurdering)

        opprettetVilkaarsvurdering shouldNotBe null
    }

    @Test
    fun `skal kun slette vilkaarsvurdering for en gitt behandlingId`() {
        val opprettetVilkaarsvurdering = vilkaarsvurderingRepository.opprettVilkaarsvurdering(vilkaarsvurdering)
        val opprettVilkaarsvurderingMedResultat =
            vilkaarsvurderingRepository.lagreVilkaarsvurderingResultat(
                opprettetVilkaarsvurdering.behandlingId,
                vilkaarsvurdering.virkningstidspunkt.atDay(1),
                vilkaarsvurderingResultat,
            )

        val revurdertVilkaarsvurdering =
            vilkaarsvurderingRepository.kopierVilkaarsvurdering(
                opprettVilkaarsvurderingMedResultat.copy(
                    behandlingId = UUID.randomUUID(),
                    id = UUID.randomUUID(),
                    vilkaar = vilkaarsvurdering.vilkaar.map { it.copy(id = UUID.randomUUID()) },
                ),
                opprettetVilkaarsvurdering.id,
            )

        vilkaarsvurderingRepository.hent(revurdertVilkaarsvurdering.behandlingId) shouldNotBe null
        vilkaarsvurderingRepository.slettVilkaarvurdering(revurdertVilkaarsvurdering.id) shouldBe true
        vilkaarsvurderingRepository.hent(revurdertVilkaarsvurdering.behandlingId) shouldBe null
        vilkaarsvurderingRepository.hent(opprettetVilkaarsvurdering.behandlingId) shouldNotBe null
    }

    @Test
    fun `skal hente vilkaarsvurdering`() {
        vilkaarsvurderingRepository.opprettVilkaarsvurdering(vilkaarsvurdering)

        val hentetVilkaarsvurdering = vilkaarsvurderingRepository.hent(vilkaarsvurdering.behandlingId)

        hentetVilkaarsvurdering shouldNotBe null
    }

    @Test
    fun `skal oppdatere virkningstidspunkt og sette resultat paa vilkaarsvurdering`() {
        val opprettetVilkaarsvurdering = vilkaarsvurderingRepository.opprettVilkaarsvurdering(vilkaarsvurdering)
        val nyttVirkningstidspunkt = opprettetVilkaarsvurdering.virkningstidspunkt.minusYears(1)

        val oppdatertVilkaarsvurdering =
            vilkaarsvurderingRepository.lagreVilkaarsvurderingResultat(
                opprettetVilkaarsvurdering.behandlingId,
                nyttVirkningstidspunkt.atDay(1),
                vilkaarsvurderingResultat,
            )

        oppdatertVilkaarsvurdering.virkningstidspunkt shouldBe nyttVirkningstidspunkt
        with(oppdatertVilkaarsvurdering.resultat!!) {
            utfall shouldBe vilkaarsvurderingResultat.utfall
            kommentar shouldBe vilkaarsvurderingResultat.kommentar
            tidspunkt shouldBe vilkaarsvurderingResultat.tidspunkt
            saksbehandler shouldBe vilkaarsvurderingResultat.saksbehandler
        }
    }

    @Test
    fun `skal sette resultat paa vilkaarsvurdering og slette det`() {
        val opprettetVilkaarsvurdering = vilkaarsvurderingRepository.opprettVilkaarsvurdering(vilkaarsvurdering)

        vilkaarsvurderingRepository.lagreVilkaarsvurderingResultat(
            opprettetVilkaarsvurdering.behandlingId,
            vilkaarsvurdering.virkningstidspunkt.atDay(1),
            vilkaarsvurderingResultat,
        )
        val oppdatertVilkaarsvurdering =
            vilkaarsvurderingRepository.slettVilkaarsvurderingResultat(opprettetVilkaarsvurdering.behandlingId)

        oppdatertVilkaarsvurdering.resultat shouldBe null
    }

    @Test
    fun `skal sette resultat paa vilkaar`() {
        val opprettetVilkaarsvurdering = vilkaarsvurderingRepository.opprettVilkaarsvurdering(vilkaarsvurdering)
        val vilkaar = opprettetVilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.BP_ALDER_BARN)
        val vurdertVilkaar =
            VurdertVilkaar(
                vilkaarId = vilkaar?.id!!,
                vurdering =
                    VilkaarVurderingData(
                        kommentar = "En test",
                        tidspunkt = Tidspunkt.now().toLocalDatetimeUTC(),
                        saksbehandler = "saksbehandler1",
                    ),
                hovedvilkaar = VilkaarTypeOgUtfall(vilkaar.hovedvilkaar.type, Utfall.IKKE_VURDERT),
                unntaksvilkaar =
                    vilkaar.unntaksvilkaar.let {
                        val unntaksvilkaar =
                            it.first { delvilkaar -> delvilkaar.type == VilkaarType.BP_ALDER_BARN_UNNTAK_UTDANNING }
                        VilkaarTypeOgUtfall(unntaksvilkaar.type, Utfall.IKKE_VURDERT)
                    },
            )

        val oppdatertVilkaarsvurdering =
            vilkaarsvurderingRepository.lagreVilkaarResultat(
                opprettetVilkaarsvurdering.behandlingId,
                vurdertVilkaar,
            )

        oppdatertVilkaarsvurdering.vilkaar.first { it.id == vilkaar.id }.let {
            with(it.vurdering!!) {
                kommentar shouldBe vurdertVilkaar.vurdering.kommentar
                tidspunkt shouldBe vurdertVilkaar.vurdering.tidspunkt
                saksbehandler shouldBe vurdertVilkaar.vurdering.saksbehandler
            }
            it.hovedvilkaar.resultat shouldBe Utfall.IKKE_VURDERT
            it.unntaksvilkaar
                .first { delvilkaar -> delvilkaar.type == VilkaarType.BP_ALDER_BARN_UNNTAK_UTDANNING }
                .resultat shouldBe Utfall.IKKE_VURDERT
        }
    }

    @Test
    fun `skal slette resultat paa vilkaar`() {
        val opprettetVilkaarsvurdering = vilkaarsvurderingRepository.opprettVilkaarsvurdering(vilkaarsvurdering)
        val vilkaar = opprettetVilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.BP_ALDER_BARN)
        val vurdertVilkaar =
            VurdertVilkaar(
                vilkaarId = vilkaar?.id!!,
                vurdering =
                    VilkaarVurderingData(
                        kommentar = "En test",
                        tidspunkt = Tidspunkt.now().toLocalDatetimeUTC(),
                        saksbehandler = "saksbehandler1",
                    ),
                hovedvilkaar = VilkaarTypeOgUtfall(vilkaar.hovedvilkaar.type, Utfall.IKKE_VURDERT),
                unntaksvilkaar =
                    vilkaar.unntaksvilkaar.let {
                        val unntaksvilkaar =
                            it.first { delvilkaar -> delvilkaar.type == VilkaarType.BP_ALDER_BARN_UNNTAK_UTDANNING }
                        VilkaarTypeOgUtfall(unntaksvilkaar.type, Utfall.IKKE_VURDERT)
                    },
            )

        vilkaarsvurderingRepository.lagreVilkaarResultat(
            opprettetVilkaarsvurdering.behandlingId,
            vurdertVilkaar,
        )

        val oppdatertVilkaarsvurdering =
            vilkaarsvurderingRepository.slettVilkaarResultat(
                opprettetVilkaarsvurdering.behandlingId,
                requireNotNull(vilkaar.id),
            )

        oppdatertVilkaarsvurdering.vilkaar.first { it.id == vilkaar.id }.let {
            it.vurdering shouldBe null
            it.hovedvilkaar.resultat shouldBe null
            it.unntaksvilkaar
                .first { delvilkaar -> delvilkaar.type == VilkaarType.BP_ALDER_BARN_UNNTAK_UTDANNING }
                .resultat shouldBe null
        }
    }

    @Test
    fun `naar vi setter et ikke-oppfylt vilkaar oppfylt, skal resultatet fjernes fra alle unntakene`() {
        val opprettetVilkaarsvurdering = vilkaarsvurderingRepository.opprettVilkaarsvurdering(vilkaarsvurdering)
        val vilkaar = opprettetVilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP)
        val vurdertVilkaarIkkeOppfylt =
            VurdertVilkaar(
                vilkaarId = vilkaar?.id!!,
                vurdering =
                    VilkaarVurderingData(
                        kommentar = "En test",
                        tidspunkt = Tidspunkt.now().toLocalDatetimeUTC(),
                        saksbehandler = "saksbehandler1",
                    ),
                hovedvilkaar = VilkaarTypeOgUtfall(vilkaar.hovedvilkaar.type, Utfall.IKKE_OPPFYLT),
                unntaksvilkaar =
                    VilkaarTypeOgUtfall(
                        VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_MEDLEM_ETTER_16_AAR,
                        Utfall.OPPFYLT,
                    ),
            )
        vilkaarsvurderingRepository.lagreVilkaarResultat(
            opprettetVilkaarsvurdering.behandlingId,
            vurdertVilkaarIkkeOppfylt,
        )

        val vurdertVilkaarOppfylt =
            VurdertVilkaar(
                vilkaarId = vilkaar.id,
                vurdering =
                    VilkaarVurderingData(
                        kommentar = "En test",
                        tidspunkt = Tidspunkt.now().toLocalDatetimeUTC(),
                        saksbehandler = "saksbehandler1",
                    ),
                hovedvilkaar = VilkaarTypeOgUtfall(vilkaar.hovedvilkaar.type, Utfall.OPPFYLT),
                unntaksvilkaar = null,
            )
        vilkaarsvurderingRepository.lagreVilkaarResultat(
            opprettetVilkaarsvurdering.behandlingId,
            vurdertVilkaarOppfylt,
        )

        val resultatVilkaar =
            vilkaarsvurderingRepository
                .hent(behandlingId = opprettetVilkaarsvurdering.behandlingId)!!
                .vilkaar
                .first { it.hovedvilkaar.type == VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP }

        resultatVilkaar.hovedvilkaar.resultat shouldBe Utfall.OPPFYLT
        resultatVilkaar.unntaksvilkaar.forEach { it.resultat shouldBe null }
    }

    @Test
    fun `naar vi setter et vilkaar til ikke oppfylt, og ingen unntak treffer, setter alle unntak til ikke oppfylt`() {
        val opprettetVilkaarsvurdering = vilkaarsvurderingRepository.opprettVilkaarsvurdering(vilkaarsvurdering)
        val vilkaar = opprettetVilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP)
        val vurdertVilkaarIkkeOppfylt =
            VurdertVilkaar(
                vilkaarId = vilkaar?.id!!,
                vurdering =
                    VilkaarVurderingData(
                        kommentar = "En test",
                        tidspunkt = Tidspunkt.now().toLocalDatetimeUTC(),
                        saksbehandler = "saksbehandler1",
                    ),
                hovedvilkaar = VilkaarTypeOgUtfall(vilkaar.hovedvilkaar.type, Utfall.IKKE_OPPFYLT),
                unntaksvilkaar = null,
            )
        vilkaarsvurderingRepository.lagreVilkaarResultat(
            opprettetVilkaarsvurdering.behandlingId,
            vurdertVilkaarIkkeOppfylt,
        )

        val resultatVilkaar =
            vilkaarsvurderingRepository
                .hent(behandlingId = opprettetVilkaarsvurdering.behandlingId)!!
                .vilkaar
                .first { it.hovedvilkaar.type == VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP }

        resultatVilkaar.hovedvilkaar.resultat shouldBe Utfall.IKKE_OPPFYLT
        resultatVilkaar.unntaksvilkaar.forEach { it.resultat shouldBe Utfall.IKKE_OPPFYLT }
    }

    companion object {
        @RegisterExtension
        val dbExtension = DatabaseExtension()

        val vilkaarsvurdering =
            Vilkaarsvurdering(
                behandlingId = UUID.randomUUID(),
                grunnlagVersjon = 1L,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt().dato,
                vilkaar =
                    BarnepensjonVilkaar1967.inngangsvilkaar(),
            )

        val vilkaarsvurderingResultat =
            VilkaarsvurderingResultat(
                utfall = VilkaarsvurderingUtfall.OPPFYLT,
                kommentar = "Alt ser bra ut",
                tidspunkt = Tidspunkt.now().toLocalDatetimeUTC(),
                saksbehandler = "saksbehandler1",
            )
    }
}

fun Vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(vilkaarType: VilkaarType): Vilkaar? =
    vilkaar.find { it.hovedvilkaar.type == vilkaarType }
