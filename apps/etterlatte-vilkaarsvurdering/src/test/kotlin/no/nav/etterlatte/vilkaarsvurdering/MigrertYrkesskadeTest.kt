package no.nav.etterlatte.vilkaarsvurdering

import io.mockk.mockk
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Delvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Lovreferanse
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigrertYrkesskadeTest(
    private val dataSource: DataSource,
) {
    @Test
    fun `er migrert yrkesskadefordel`() {
        val delvilkaarRepository = DelvilkaarRepository()
        val repository = VilkaarsvurderingRepository(ds = dataSource, delvilkaarRepository = delvilkaarRepository)
        val service =
            VilkaarsvurderingService(
                vilkaarsvurderingRepository = repository,
                behandlingKlient = mockk(),
                grunnlagKlient = mockk(),
            )
        val behandlingId = UUID.randomUUID()
        val vilkaarMigrertYrkesskade =
            Vilkaar(
                hovedvilkaar =
                    Delvilkaar(
                        type = VilkaarType.BP_YRKESSKADE_AVDOED_2024,
                        tittel = "Yrkesskade",
                        lovreferanse = Lovreferanse("Paragraf 1"),
                        resultat = Utfall.OPPFYLT,
                    ),
                vurdering =
                    VilkaarVurderingData(
                        kommentar = "Ingen kommentar",
                        tidspunkt = LocalDateTime.now(),
                        saksbehandler = Vedtaksloesning.PESYS.name,
                    ),
            )

        val vilkaarsvurdering =
            Vilkaarsvurdering(
                behandlingId = behandlingId,
                grunnlagVersjon = 1L,
                virkningstidspunkt = YearMonth.now(),
                vilkaar = listOf(vilkaarMigrertYrkesskade),
            )
        repository.opprettVilkaarsvurdering(vilkaarsvurdering)
        Assertions.assertTrue(service.erMigrertYrkesskadefordel(behandlingId))
    }
}
