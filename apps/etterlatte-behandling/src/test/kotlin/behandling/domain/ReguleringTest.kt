package behandling.domain

import no.nav.etterlatte.behandling.domain.Regulering
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class ReguleringTest {

    @Test
    fun `regulering kan endre tilstander`() {
        Regulering(
            UUID.randomUUID(), 1, LocalDateTime.now(), LocalDateTime.now(), BehandlingStatus.OPPRETTET,
            Persongalleri(""), null, null, null
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