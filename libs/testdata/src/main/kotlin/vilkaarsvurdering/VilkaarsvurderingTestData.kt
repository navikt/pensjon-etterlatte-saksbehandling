package no.nav.etterlatte.libs.testdata.vilkaarsvurdering

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import java.time.YearMonth
import java.util.*

class VilkaarsvurderingTestData {
    companion object {
        val oppfylt = VilkaarsvurderingDto(
            UUID.randomUUID(),
            emptyList(),
            YearMonth.of(2022, 1),
            VilkaarsvurderingResultat(
                VilkaarsvurderingUtfall.OPPFYLT,
                null,
                Tidspunkt.now().toLocalDatetimeUTC(),
                "ABCDEF"
            )
        )
    }
}