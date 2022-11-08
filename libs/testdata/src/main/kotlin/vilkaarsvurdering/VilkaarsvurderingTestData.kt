package vilkaarsvurdering

import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import java.time.Instant
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.util.*

class VilkaarsvurderingTestData {
    companion object {
        val virkningstidspunkt = Virkningstidspunkt(
            dato = YearMonth.of(2022, Month.JANUARY),
            kilde = Grunnlagsopplysning.Saksbehandler("ident", Instant.now())
        )

        val oppfylt = Vilkaarsvurdering(
            UUID.randomUUID(),
            emptyList(),
            virkningstidspunkt,
            VilkaarsvurderingResultat(VilkaarsvurderingUtfall.OPPFYLT, null, LocalDateTime.now(), "ABCDEF")
        )

        val ikkeOppfylt = Vilkaarsvurdering(
            UUID.randomUUID(),
            emptyList(),
            virkningstidspunkt,
            VilkaarsvurderingResultat(VilkaarsvurderingUtfall.IKKE_OPPFYLT, null, LocalDateTime.now(), "ABCDEF")
        )
    }
}