package behandling.domain

import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.kommerBarnetTilGodeVurdering
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.virkningstidspunktVurdering
import org.junit.jupiter.api.Test
import java.util.*

internal class RevurderingTest {

    @Test
    fun `regulering kan endre tilstander`() {
        Revurdering(
            id = UUID.randomUUID(),
            sak = Sak(
                ident = "",
                sakType = SakType.BARNEPENSJON,
                id = 1
            ),
            behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
            sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
            status = BehandlingStatus.OPPRETTET,
            persongalleri = Persongalleri(""),
            kommerBarnetTilgode = kommerBarnetTilGodeVurdering(),
            vilkaarUtfall = null,
            virkningstidspunkt = virkningstidspunktVurdering(),
            revurderingsaarsak = RevurderingAarsak.REGULERING,
            prosesstype = Prosesstype.AUTOMATISK
        ).tilVilkaarsvurdert(VilkaarsvurderingUtfall.IKKE_OPPFYLT).tilBeregnet()
            .tilVilkaarsvurdert(VilkaarsvurderingUtfall.OPPFYLT).tilBeregnet().tilFattetVedtak().tilAttestert()
            .tilIverksatt()
    }
}