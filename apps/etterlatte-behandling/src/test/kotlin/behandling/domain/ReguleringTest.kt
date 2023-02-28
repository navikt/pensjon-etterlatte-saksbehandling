package behandling.domain

import no.nav.etterlatte.behandling.domain.Regulering
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class ReguleringTest {

    @Test
    fun `regulering kan endre tilstander`() {
        Regulering(
            id = UUID.randomUUID(),
            sak = 1,
            sakType = SakType.BARNEPENSJON,
            behandlingOpprettet = LocalDateTime.now(),
            sistEndret = LocalDateTime.now(),
            status = BehandlingStatus.OPPRETTET,
            persongalleri = Persongalleri(""),
            kommerBarnetTilgode = null,
            vilkaarUtfall = null,
            virkningstidspunkt = null
        )
            .tilReturnert()
            .tilOpprettet()
            .tilVilkaarsvurdert(VilkaarsvurderingUtfall.IKKE_OPPFYLT)
            .tilBeregnet()
            .tilVilkaarsvurdert(VilkaarsvurderingUtfall.OPPFYLT)
            .tilBeregnet()
            .tilFattetVedtak()
            .tilAttestert()
            .tilIverksatt()
    }
}