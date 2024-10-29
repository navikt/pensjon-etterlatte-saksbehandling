package no.nav.etterlatte.vilkaarsvurdering

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.insert
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Delvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Lovreferanse
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.vilkaarsvurdering.dao.VilkaarsvurderingRepositoryWrapperDatabase
import no.nav.etterlatte.vilkaarsvurdering.ektedao.DelvilkaarRepository
import no.nav.etterlatte.vilkaarsvurdering.ektedao.VilkaarsvurderingRepository
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService
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
        val repository = VilkaarsvurderingRepository(ConnectionAutoclosingTest(dataSource), delvilkaarRepository = delvilkaarRepository)
        val sakId = SakId(10L)
        val behandlingService =
            mockk<BehandlingService> {
                every { hentBehandling(any()) } returns
                    mockk<Behandling>().also {
                        every { it.sak.id } returns sakId
                        every { it.id } returns UUID.randomUUID()
                    }
            }
        val service =
            VilkaarsvurderingService(
                VilkaarsvurderingRepositoryWrapperDatabase(repository),
                behandlingService,
                grunnlagKlient = mockk(),
                mockk(),
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
        runBlocking {
            Assertions.assertFalse(service.erMigrertYrkesskadefordel(behandlingId))
            dataSource.insert("migrert_yrkesskade", params = {
                mapOf(
                    "behandling_id" to behandlingId,
                    "sak_id" to sakId.sakId,
                )
            })
            Assertions.assertTrue(service.erMigrertYrkesskadefordel(behandlingId))
        }
    }
}
